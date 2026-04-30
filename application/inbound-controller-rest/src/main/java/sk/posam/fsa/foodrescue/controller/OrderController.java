package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.offer.PickupLocation;
import sk.posam.fsa.foodrescue.domain.offer.PickupTimeWindow;
import sk.posam.fsa.foodrescue.domain.order.Order;
import sk.posam.fsa.foodrescue.domain.order.OrderFacade;
import sk.posam.fsa.foodrescue.domain.order.OrderItem;
import sk.posam.fsa.foodrescue.domain.order.OrderPayment;
import sk.posam.fsa.foodrescue.domain.order.OrderPickupConfirmation;
import sk.posam.fsa.foodrescue.domain.order.OrderPickupPass;
import sk.posam.fsa.foodrescue.domain.order.OrderStatus;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderFacade orderFacade;
    private final CurrentUserDetailService currentUserDetailService;

    public OrderController(OrderFacade orderFacade,
                           CurrentUserDetailService currentUserDetailService) {
        this.orderFacade = orderFacade;
        this.currentUserDetailService = currentUserDetailService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@RequestParam(required = false) Long businessId) {
        User user = currentUserDetailService.getFullCurrentUser();
        List<OrderResponse> orders = orderFacade.getOrders(user, businessId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        User user = currentUserDetailService.getFullCurrentUser();
        Order order = orderFacade.create(
                user,
                request.offerId(),
                request.quantity(),
                request.cardHolderName(),
                request.cardLast4()
        );
        return ResponseEntity.created(URI.create("/orders/" + order.getId()))
                .body(toResponse(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        return ResponseEntity.ok(toResponse(orderFacade.get(user, id)));
    }

    @GetMapping("/{id}/pickup-pass")
    public ResponseEntity<OrderPickupPassResponse> getPickupPass(@PathVariable Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        return ResponseEntity.ok(toPickupPassResponse(orderFacade.getPickupPass(user, id)));
    }

    @PostMapping("/{id}/pickup")
    public ResponseEntity<OrderResponse> confirmPickup(@PathVariable Long id,
                                                       @RequestBody ConfirmOrderPickupRequest request) {
        User user = currentUserDetailService.getFullCurrentUser();
        Order order = orderFacade.confirmPickup(user, id, request.pickupToken());
        return ResponseEntity.ok(toResponse(order));
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getBusinessId(),
                order.getUserId(),
                order.getBusinessName(),
                toItemResponse(order.getItem()),
                order.getPickupLocation(),
                order.getPickupTimeWindow(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getCancelledAt(),
                toPickupConfirmationResponse(order.getPickupConfirmation()),
                toPaymentResponse(order.getPayment())
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }

        return new OrderItemResponse(
                item.getOfferId(),
                item.getQuantity(),
                item.getTitle(),
                item.getImageUrl(),
                item.getUnitPrice()
        );
    }

    private OrderPickupConfirmationResponse toPickupConfirmationResponse(OrderPickupConfirmation confirmation) {
        if (confirmation == null) {
            return null;
        }

        return new OrderPickupConfirmationResponse(
                confirmation.getConfirmedByUserId(),
                confirmation.getConfirmedAt()
        );
    }

    private OrderPaymentResponse toPaymentResponse(OrderPayment payment) {
        if (payment == null) {
            return null;
        }

        return new OrderPaymentResponse(
                payment.getPaidByUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getCardHolderName(),
                payment.getCardLast4(),
                payment.getProviderReference(),
                payment.getPickupToken(),
                payment.getPaidAt(),
                payment.getTransferredToBusinessAt(),
                payment.getTransferReference()
        );
    }

    private OrderPickupPassResponse toPickupPassResponse(OrderPickupPass pickupPass) {
        return new OrderPickupPassResponse(
                pickupPass.orderId(),
                pickupPass.businessId(),
                pickupPass.pickupToken(),
                pickupPass.qrPayload(),
                pickupPass.amount(),
                pickupPass.currency(),
                pickupPass.issuedAt(),
                pickupPass.paidAt(),
                pickupPass.providerReference()
        );
    }

    public record CreateOrderRequest(
            Long offerId,
            Integer quantity,
            String cardHolderName,
            String cardLast4
    ) {
    }

    public record ConfirmOrderPickupRequest(String pickupToken) {
    }

    public record OrderResponse(
            Long id,
            Long businessId,
            Long userId,
            String businessName,
            OrderItemResponse item,
            PickupLocation pickupLocation,
            PickupTimeWindow pickupTimeWindow,
            OrderStatus status,
            LocalDateTime createdAt,
            LocalDateTime cancelledAt,
            OrderPickupConfirmationResponse pickupConfirmation,
            OrderPaymentResponse payment
    ) {
    }

    public record OrderItemResponse(
            Long offerId,
            Integer quantity,
            String title,
            String imageUrl,
            BigDecimal unitPrice
    ) {
    }

    public record OrderPickupConfirmationResponse(
            Long confirmedByUserId,
            LocalDateTime confirmedAt
    ) {
    }

    public record OrderPaymentResponse(
            Long paidByUserId,
            BigDecimal amount,
            String currency,
            String cardHolderName,
            String cardLast4,
            String providerReference,
            String pickupToken,
            LocalDateTime paidAt,
            LocalDateTime transferredToBusinessAt,
            String transferReference
    ) {
    }

    public record OrderPickupPassResponse(
            Long orderId,
            Long businessId,
            String pickupToken,
            String qrPayload,
            BigDecimal amount,
            String currency,
            LocalDateTime issuedAt,
            LocalDateTime paidAt,
            String providerReference
    ) {
    }
}
