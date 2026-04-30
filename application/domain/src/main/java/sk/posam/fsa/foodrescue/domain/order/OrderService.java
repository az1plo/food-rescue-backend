package sk.posam.fsa.foodrescue.domain.order;

import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.notification.Notification;
import sk.posam.fsa.foodrescue.domain.notification.NotificationRepository;
import sk.posam.fsa.foodrescue.domain.notification.NotificationType;
import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

public class OrderService implements OrderFacade {

    private final OrderRepository orderRepository;
    private final OfferRepository offerRepository;
    private final BusinessRepository businessRepository;
    private final NotificationRepository notificationRepository;

    public OrderService(OrderRepository orderRepository,
                        OfferRepository offerRepository,
                        BusinessRepository businessRepository,
                        NotificationRepository notificationRepository) {
        this.orderRepository = orderRepository;
        this.offerRepository = offerRepository;
        this.businessRepository = businessRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Order> getOrders(User currentUser, Long businessId) {
        if (businessId == null) {
            ensureActiveUser(currentUser, "Only active users can view orders");
            return orderRepository.findAllByUserId(currentUser.getId());
        }

        Business business = resolveBusiness(businessId);
        ensureBusinessManager(currentUser, business, "You are not allowed to view orders for this business");
        return orderRepository.findAllByBusinessId(businessId);
    }

    @Override
    public Order get(User currentUser, Long id) {
        Order order = resolveOrder(id);
        Business business = resolveBusiness(order.getBusinessId());

        if (!order.canBeManagedBy(currentUser, business)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You do not have access to order with id=" + id
            );
        }

        return order;
    }

    @Override
    public Order create(User currentUser, Long offerId, Integer quantity, String cardHolderName, String cardLast4) {
        ensureActiveUser(currentUser, "Only active users can create an order");

        Offer offer = resolveOffer(offerId);
        Business business = resolveBusiness(offer.getBusinessId());

        if (!business.isActive()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "Only offers of active businesses can be purchased"
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

        return savedOrder;
    }

    @Override
    public OrderPickupPass getPickupPass(User currentUser, Long id) {
        Order order = resolveOrder(id);
        ensureOrderOwnerOrAdmin(currentUser, order, "You are not allowed to access the pickup pass for this order");
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
    public Order confirmPickup(User currentUser, Long id, String pickupToken) {
        Order order = resolveOrder(id);
        Business business = resolveBusiness(order.getBusinessId());

        ensureActiveUser(currentUser, "Only active users can confirm pickup");
        ensureBusinessManager(currentUser, business, "You are not allowed to confirm pickup for this order");

        order.markPickedUp(currentUser.getId(), pickupToken, generateReference("TRN"));
        Order savedOrder = orderRepository.save(order);

        createNotification(
                savedOrder.getUserId(),
                NotificationType.RESERVATION_STATUS_CHANGED,
                "Pickup confirmed",
                "Pickup for \"" + savedOrder.getItem().getTitle() + "\" was confirmed and the simulated payout is now assigned to the business."
        );

        return savedOrder;
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

        if (!order.belongsTo(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    message
            );
        }
    }

    private void ensurePickupPassAvailable(Order order, Long orderId) {
        if (!order.isPaid() || order.getPayment() == null) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Order with id=" + orderId + " does not have a paid pickup pass yet"
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
}
