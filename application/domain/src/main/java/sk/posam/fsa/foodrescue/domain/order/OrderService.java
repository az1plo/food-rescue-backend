package sk.posam.fsa.foodrescue.domain.order;

import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.notification.Notification;
import sk.posam.fsa.foodrescue.domain.notification.NotificationRepository;
import sk.posam.fsa.foodrescue.domain.notification.NotificationType;
import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.review.Review;
import sk.posam.fsa.foodrescue.domain.review.ReviewRepository;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OrderService implements OrderFacade {

    private final OrderRepository orderRepository;
    private final OfferRepository offerRepository;
    private final BusinessRepository businessRepository;
    private final NotificationRepository notificationRepository;
    private final ReviewRepository reviewRepository;

    public OrderService(OrderRepository orderRepository,
                        OfferRepository offerRepository,
                        BusinessRepository businessRepository,
                        NotificationRepository notificationRepository,
                        ReviewRepository reviewRepository) {
        this.orderRepository = orderRepository;
        this.offerRepository = offerRepository;
        this.businessRepository = businessRepository;
        this.notificationRepository = notificationRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public List<OrderDetailsView> getOrders(User currentUser, Long businessId) {
        if (businessId == null) {
            ensureActiveUser(currentUser, "Only active users can view orders");
            List<Order> userOrders = orderRepository.findAllByUserId(currentUser.getId());
            settleExpiredOrders(userOrders);
            return enrichOrders(userOrders);
        }

        Business business = resolveBusiness(businessId);
        ensureBusinessManager(currentUser, business, "You are not allowed to view orders for this business");
        List<Order> businessOrders = orderRepository.findAllByBusinessId(businessId);
        settleExpiredOrders(businessOrders);
        return enrichOrders(businessOrders);
    }

    @Override
    public OrderDetailsView get(User currentUser, Long id) {
        Order order = resolveOrder(id);
        Business business = resolveBusiness(order.getBusinessId());

        if (!order.canBeManagedBy(currentUser, business)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You do not have access to order with id=" + id
            );
        }

        order = settleExpiredOrderIfNeeded(order, business);

        return toOrderDetailsView(order, reviewRepository.findByReservationId(order.getId()).orElse(null));
    }

    @Override
    public OrderDetailsView create(User currentUser, Long offerId, Integer quantity, String cardHolderName, String cardLast4) {
        ensureActiveUser(currentUser, "Only active users can create an order");

        Offer offer = resolveOffer(offerId);
        Business business = resolveBusiness(offer.getBusinessId());

        if (!business.isActive()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "Only offers of active businesses can be purchased"
            );
        }

        if (business.belongsTo(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You cannot reserve an offer from your own business"
            );
        }

        if (!offer.isAvailable()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Offer with id=" + offerId + " is no longer available"
            );
        }

        int requestedQuantity = quantity == null ? 1 : quantity;
        if (offer.getQuantityAvailable() < requestedQuantity) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Offer with id=" + offerId + " does not have enough remaining quantity"
            );
        }

        Order order = Order.create(
                business.getId(),
                currentUser.getId(),
                business.getName(),
                OrderItem.of(
                        offer.getId(),
                        requestedQuantity,
                        offer.getTitle(),
                        offer.getImageUrl(),
                        offer.getPrice()
                ),
                offer.getPickupLocation(),
                offer.getPickupTimeWindow()
        );
        order.prepareForCreation();

        OrderPayment payment = new OrderPayment();
        payment.setPaidByUserId(currentUser.getId());
        payment.setAmount(resolveOrderAmount(order.getItem()));
        payment.setCurrency("EUR");
        payment.setCardHolderName(resolveCardHolderName(currentUser, cardHolderName));
        payment.setCardLast4(normalizeCardLast4(cardLast4));
        payment.setProviderReference(generateReference("PAY"));
        payment.setPickupToken(generatePickupToken());
        order.markPaid(payment);

        offer.reserveQuantity(requestedQuantity);

        Order savedOrder = orderRepository.save(order);
        offerRepository.save(offer);

        createNotification(
                savedOrder.getUserId(),
                NotificationType.RESERVATION_STATUS_CHANGED,
                "Order paid",
                "Your order for \"" + offer.getTitle() + "\" is paid and ready for pickup."
        );
        if (!savedOrder.getUserId().equals(business.getOwnerId())) {
            createNotification(
                    business.getOwnerId(),
                    NotificationType.RESERVATION_STATUS_CHANGED,
                    "Order paid",
                    "Order #" + savedOrder.getId() + " for \"" + offer.getTitle() + "\" was paid for " + formatCurrency(payment.getAmount()) + "."
            );
        }

        return toOrderDetailsView(savedOrder, null);
    }

    @Override
    public OrderPickupPass getPickupPass(User currentUser, Long id) {
        Order order = resolveOrder(id);
        ensureOrderOwnerOrAdmin(currentUser, order, "You are not allowed to access the pickup pass for this order");
        order = settleExpiredOrderIfNeeded(order);
        ensurePickupPassAvailable(order, id);

        OrderPayment payment = order.getPayment();
        return new OrderPickupPass(
                order.getId(),
                order.getBusinessId(),
                payment.getPickupToken(),
                buildQrPayload(order),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaidAt(),
                payment.getPaidAt(),
                payment.getProviderReference()
        );
    }

    @Override
    public OrderDetailsView confirmPickup(User currentUser, Long id, String pickupToken) {
        Order order = resolveOrder(id);
        Business business = resolveBusiness(order.getBusinessId());

        ensureActiveUser(currentUser, "Only active users can confirm pickup");
        ensureBusinessManager(currentUser, business, "You are not allowed to confirm pickup for this order");
        order = settleExpiredOrderIfNeeded(order, business);
        ensurePickupCanBeConfirmed(order, id);

        order.markPickedUp(currentUser.getId(), pickupToken, generateReference("TRN"));
        Order savedOrder = orderRepository.save(order);

        createNotification(
                savedOrder.getUserId(),
                NotificationType.RESERVATION_STATUS_CHANGED,
                "Pickup confirmed",
                "Pickup for \"" + savedOrder.getItem().getTitle() + "\" was confirmed and the simulated payout is now assigned to the business."
        );

        return toOrderDetailsView(savedOrder, null);
    }

    public int settleExpiredNoShows() {
        return settleExpiredOrders(orderRepository.findAllByStatus(OrderStatus.ACTIVE)).size();
    }

    @Override
    public OrderDetailsView submitReview(User currentUser, Long id, Integer rating, String comment) {
        ensureActiveUser(currentUser, "Only active users can rate a business");

        Order order = resolveOrder(id);
        ensureOrderOwner(currentUser, order, "You are not allowed to rate this order");

        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Only picked up orders can be rated"
            );
        }

        if (reviewRepository.findByReservationId(order.getId()).isPresent()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "This order has already been rated"
            );
        }

        Business business = resolveBusiness(order.getBusinessId());
        Review review = Review.create(order.getId(), business.getId(), currentUser.getId(), rating, comment);
        review.prepareForCreation();

        Review savedReview = reviewRepository.save(review);
        business.registerRating(savedReview.getRating());
        businessRepository.save(business);

        return toOrderDetailsView(order, savedReview);
    }

    private Order resolveOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Order with id=" + id + " was not found"
                ));
    }

    private Offer resolveOffer(Long id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Offer with id=" + id + " was not found"
                ));
    }

    private Business resolveBusiness(Long id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Business with id=" + id + " was not found"
                ));
    }

    private void ensureActiveUser(User currentUser, String message) {
        if (currentUser == null || !currentUser.isActive()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    message
            );
        }
    }

    private void ensureBusinessManager(User currentUser, Business business, String message) {
        if (business == null || !business.canBeManagedBy(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    message
            );
        }
    }

    private void ensureOrderOwnerOrAdmin(User currentUser, Order order, String message) {
        if (currentUser != null && currentUser.isAdmin()) {
            return;
        }

        ensureOrderOwner(currentUser, order, message);
    }

    private void ensureOrderOwner(User currentUser, Order order, String message) {
        if (!order.belongsTo(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    message
            );
        }
    }

    private void ensurePickupPassAvailable(Order order, Long orderId) {
        if (order.getStatus() != OrderStatus.ACTIVE) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Order with id=" + orderId + " is no longer available for pickup"
            );
        }

        if (!order.isPaid() || order.getPayment() == null) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Order with id=" + orderId + " does not have a paid pickup pass yet"
            );
        }
    }

    private void ensurePickupCanBeConfirmed(Order order, Long orderId) {
        if (order.getStatus() != OrderStatus.ACTIVE) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Order with id=" + orderId + " is no longer awaiting pickup"
            );
        }
    }

    private BigDecimal resolveOrderAmount(OrderItem item) {
        BigDecimal unitPrice = item == null || item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
        long quantity = item == null || item.getQuantity() == null ? 1L : item.getQuantity().longValue();
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveCardHolderName(User currentUser, String cardHolderName) {
        if (cardHolderName != null && !cardHolderName.isBlank()) {
            return cardHolderName.trim();
        }

        String firstName = currentUser == null ? "" : safeValue(currentUser.getFirstName());
        String lastName = currentUser == null ? "" : safeValue(currentUser.getLastName());
        String fallbackName = (firstName + " " + lastName).trim();
        return fallbackName.isBlank() ? "Savr customer" : fallbackName;
    }

    private String normalizeCardLast4(String cardLast4) {
        String digitsOnly = cardLast4 == null ? "" : cardLast4.replaceAll("\\D", "");
        if (digitsOnly.length() < 4) {
            throw new FoodRescueException(
                    FoodRescueException.Type.VALIDATION,
                    "Card last4 must contain at least four digits"
            );
        }

        return digitsOnly.substring(digitsOnly.length() - 4);
    }

    private String buildQrPayload(Order order) {
        OrderPayment payment = order.getPayment();
        return "SAVR|ORDER|" + order.getId() + "|BUSINESS|" + order.getBusinessId() + "|TOKEN|" + payment.getPickupToken();
    }

    private String generatePickupToken() {
        return "SAVR-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    private String generateReference(String prefix) {
        return prefix + "-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    private String formatCurrency(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP) + " EUR";
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private void createNotification(Long userId, NotificationType type, String title, String message) {
        if (userId == null) {
            return;
        }

        Notification notification = Notification.create(userId, type, title, message);
        notification.prepareForCreation();
        notificationRepository.save(notification);
    }

    private List<OrderDetailsView> enrichOrders(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Review> reviewsByOrderId = reviewRepository.findAllByReservationIds(orderIds).stream()
                .filter(review -> review.getReservationId() != null)
                .collect(Collectors.toMap(Review::getReservationId, Function.identity(), (first, second) -> first));

        return orders.stream()
                .map(order -> toOrderDetailsView(order, reviewsByOrderId.get(order.getId())))
                .toList();
    }

    private OrderDetailsView toOrderDetailsView(Order order, Review review) {
        return new OrderDetailsView(
                order,
                review == null
                        ? null
                        : new OrderReviewView(review.getRating(), review.getComment(), review.getCreatedAt())
        );
    }

    private List<Order> settleExpiredOrders(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        Map<Long, Business> businessesById = new HashMap<>();
        List<Order> settledOrders = new ArrayList<>();

        for (Order order : orders) {
            if (!shouldMarkNoShow(order)) {
                continue;
            }

            Business business = businessesById.computeIfAbsent(order.getBusinessId(), this::resolveBusiness);
            settledOrders.add(settleExpiredOrder(order, business));
        }

        return settledOrders;
    }

    private Order settleExpiredOrderIfNeeded(Order order) {
        if (!shouldMarkNoShow(order)) {
            return order;
        }

        return settleExpiredOrder(order, resolveBusiness(order.getBusinessId()));
    }

    private Order settleExpiredOrderIfNeeded(Order order, Business business) {
        if (!shouldMarkNoShow(order)) {
            return order;
        }

        return settleExpiredOrder(order, business == null ? resolveBusiness(order.getBusinessId()) : business);
    }

    private Order settleExpiredOrder(Order order, Business business) {
        order.markNoShow(generateReference("TRN"));
        Order savedOrder = orderRepository.save(order);

        createNotification(
                savedOrder.getUserId(),
                NotificationType.RESERVATION_STATUS_CHANGED,
                "Pickup missed",
                "The pickup window for \"" + savedOrder.getItem().getTitle() + "\" ended and the order was not collected in time. Payment was not refunded."
        );

        if (!Objects.equals(savedOrder.getUserId(), business.getOwnerId())) {
            createNotification(
                    business.getOwnerId(),
                    NotificationType.RESERVATION_STATUS_CHANGED,
                    "No-show settled",
                    "Order #" + savedOrder.getId() + " for \"" + savedOrder.getItem().getTitle() + "\" was marked as no-show after the pickup window ended. Payout was assigned to the business."
            );
        }

        return savedOrder;
    }

    private boolean shouldMarkNoShow(Order order) {
        return order != null
                && order.getStatus() == OrderStatus.ACTIVE
                && order.isPaid()
                && order.getPickupTimeWindow() != null
                && order.getPickupTimeWindow().hasEnded(LocalDateTime.now());
    }
}
