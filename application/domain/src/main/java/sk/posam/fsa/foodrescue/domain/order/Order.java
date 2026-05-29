package sk.posam.fsa.foodrescue.domain.order;

import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.offer.PickupLocation;
import sk.posam.fsa.foodrescue.domain.offer.PickupTimeWindow;
import sk.posam.fsa.foodrescue.domain.shared.ValidationException;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {

    private Long id;
    private Long businessId;
    private Long userId;
    private String businessName;
    private OrderItem item;
    private PickupLocation pickupLocation;
    private PickupTimeWindow pickupTimeWindow;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
    private OrderPickupConfirmation pickupConfirmation;
    private OrderPayment payment;

    public Order() {
    }

    public static Order create(Long businessId,
                               Long userId,
                               String businessName,
                               OrderItem item,
                               PickupLocation pickupLocation,
                               PickupTimeWindow pickupTimeWindow) {
        Order order = new Order();
        order.businessId = businessId;
        order.userId = userId;
        order.businessName = businessName;
        order.item = item;
        order.pickupLocation = pickupLocation == null ? null : pickupLocation.copy();
        order.pickupTimeWindow = pickupTimeWindow == null ? null : pickupTimeWindow.copy();
        return order;
    }

    public Long getId() {
        return id;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public OrderItem getItem() {
        return item;
    }

    public PickupLocation getPickupLocation() {
        return pickupLocation == null ? null : pickupLocation.copy();
    }

    public PickupTimeWindow getPickupTimeWindow() {
        return pickupTimeWindow == null ? null : pickupTimeWindow.copy();
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public OrderPickupConfirmation getPickupConfirmation() {
        return pickupConfirmation;
    }

    public OrderPayment getPayment() {
        return payment;
    }

    void setId(Long id) {
        this.id = id;
    }

    void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    void setUserId(Long userId) {
        this.userId = userId;
    }

    void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    void setItem(OrderItem item) {
        this.item = item;
    }

    void setPickupLocation(PickupLocation pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    void setPickupTimeWindow(PickupTimeWindow pickupTimeWindow) {
        this.pickupTimeWindow = pickupTimeWindow;
    }

    void setStatus(OrderStatus status) {
        this.status = status;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    void setPickupConfirmation(OrderPickupConfirmation pickupConfirmation) {
        this.pickupConfirmation = pickupConfirmation;
    }

    void setPayment(OrderPayment payment) {
        this.payment = payment;
    }

    public boolean belongsTo(User user) {
        return user != null
                && userId != null
                && user.getId() != null
                && userId.equals(user.getId());
    }

    public boolean isPaid() {
        return payment != null && payment.getPaidAt() != null;
    }

    public void prepareForCreation() {
        require(businessId != null, "Order business is required");
        require(userId != null, "Order user is required");
        require(businessName != null && !businessName.isBlank(), "Order business name is required");
        require(item != null, "Order item is required");
        require(pickupLocation != null, "Order pickup location is required");
        require(pickupTimeWindow != null, "Order pickup time window is required");

        businessName = businessName.trim();
        item.prepareForCreation();
        pickupLocation.prepareForCreation();
        pickupTimeWindow.prepareForCreation();

        if (status == null) {
            status = OrderStatus.ACTIVE;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void markPaid(OrderPayment nextPayment) {
        require(status == OrderStatus.ACTIVE, "Only active orders can be paid");
        require(payment == null, "Order payment already exists");
        require(nextPayment != null, "Order payment is required");

        nextPayment.prepareForCreation();
        payment = nextPayment;
    }

    public void markPaid(Long paidByUserId,
                         BigDecimal amount,
                         String currency,
                         String cardHolderName,
                         String cardLast4,
                         String providerReference,
                         String pickupToken) {
        markPaid(OrderPayment.create(
                paidByUserId,
                amount,
                currency,
                cardHolderName,
                cardLast4,
                providerReference,
                pickupToken
        ));
    }

    public void markPickedUp(Long confirmedByUserId, String pickupToken, String transferReference) {
        require(status == OrderStatus.ACTIVE, "Only active orders can be picked up");
        require(payment != null, "Only paid orders can be picked up");
        require(matchesPickupToken(pickupToken), "Pickup token is invalid");

        payment.markTransferredToBusiness(transferReference);

        OrderPickupConfirmation confirmation = OrderPickupConfirmation.create(confirmedByUserId);
        confirmation.prepareForCreation();

        pickupConfirmation = confirmation;
        status = OrderStatus.PICKED_UP;
    }

    public void markNoShow(String transferReference) {
        require(status == OrderStatus.ACTIVE, "Only active orders can be marked as no-show");
        require(payment != null, "Only paid orders can be marked as no-show");

        payment.markTransferredToBusiness(transferReference);
        status = OrderStatus.NO_SHOW;
    }

    public boolean canBeManagedBy(User user, Business business) {
        return belongsTo(user) || (business != null && business.canBeManagedBy(user));
    }

    private boolean matchesPickupToken(String pickupToken) {
        return payment != null
                && normalize(payment.getPickupToken()).equals(normalize(pickupToken));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}
