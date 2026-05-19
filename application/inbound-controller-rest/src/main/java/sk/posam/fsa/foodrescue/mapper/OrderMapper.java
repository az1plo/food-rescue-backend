package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.order.OrderDetailsView;
import sk.posam.fsa.foodrescue.domain.order.OrderItem;
import sk.posam.fsa.foodrescue.domain.order.OrderPayment;
import sk.posam.fsa.foodrescue.domain.order.OrderPickupConfirmation;
import sk.posam.fsa.foodrescue.domain.order.OrderPickupPass;
import sk.posam.fsa.foodrescue.domain.order.OrderReviewView;
import sk.posam.fsa.foodrescue.rest.dto.ConfirmOrderPickupRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.CreateOrderRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.CreateOrderReviewRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.OrderItemSnapshotDto;
import sk.posam.fsa.foodrescue.rest.dto.OrderPaymentResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.OrderPickupPassResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.OrderResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.OrderReviewResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.OrderStatusDto;
import sk.posam.fsa.foodrescue.rest.dto.PickupConfirmationDto;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    private final PickupLocationMapper pickupLocationMapper;
    private final PickupTimeWindowMapper pickupTimeWindowMapper;

    public OrderMapper(PickupLocationMapper pickupLocationMapper,
                       PickupTimeWindowMapper pickupTimeWindowMapper) {
        this.pickupLocationMapper = pickupLocationMapper;
        this.pickupTimeWindowMapper = pickupTimeWindowMapper;
    }

    public OrderCreateInput toCreateInput(CreateOrderRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return new OrderCreateInput(
                dto.getOfferId(),
                dto.getQuantity(),
                dto.getCardHolderName(),
                dto.getCardLast4()
        );
    }

    public String toPickupToken(ConfirmOrderPickupRequestDto dto) {
        return dto == null ? null : dto.getPickupToken();
    }

    public OrderReviewInput toReviewInput(CreateOrderReviewRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return new OrderReviewInput(
                dto.getRating(),
                dto.getComment()
        );
    }

    public List<OrderResponseDto> toDtos(List<OrderDetailsView> orderViews) {
        return orderViews == null ? List.of() : orderViews.stream()
                .map(this::toDto)
                .toList();
    }

    public OrderResponseDto toDto(OrderDetailsView orderView) {
        if (orderView == null || orderView.order() == null) {
            return null;
        }

        var order = orderView.order();
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(order.getId());
        dto.setBusinessId(order.getBusinessId());
        dto.setUserId(order.getUserId());
        dto.setBusinessName(order.getBusinessName());
        dto.setItem(toDto(order.getItem()));
        dto.setPickupLocation(pickupLocationMapper.toDto(order.getPickupLocation()));
        dto.setPickupTimeWindow(pickupTimeWindowMapper.toDto(order.getPickupTimeWindow()));
        dto.setStatus(toDto(order.getStatus()));
        dto.setCreatedAt(ApiDateTimeMapper.toUtcOffsetDateTime(order.getCreatedAt()));
        dto.setCancelledAt(ApiDateTimeMapper.toUtcOffsetDateTime(order.getCancelledAt()));
        dto.setPickupConfirmation(toDto(order.getPickupConfirmation()));
        dto.setPayment(toDto(order.getPayment()));
        dto.setReview(toDto(orderView.review()));
        return dto;
    }

    public OrderPickupPassResponseDto toDto(OrderPickupPass pickupPass) {
        if (pickupPass == null) {
            return null;
        }

        OrderPickupPassResponseDto dto = new OrderPickupPassResponseDto();
        dto.setOrderId(pickupPass.orderId());
        dto.setBusinessId(pickupPass.businessId());
        dto.setPickupToken(pickupPass.pickupToken());
        dto.setQrPayload(pickupPass.qrPayload());
        dto.setAmount(toDouble(pickupPass.amount()));
        dto.setCurrency(pickupPass.currency());
        dto.setIssuedAt(ApiDateTimeMapper.toUtcOffsetDateTime(pickupPass.issuedAt()));
        dto.setPaidAt(ApiDateTimeMapper.toUtcOffsetDateTime(pickupPass.paidAt()));
        dto.setProviderReference(pickupPass.providerReference());
        return dto;
    }

    private OrderItemSnapshotDto toDto(OrderItem item) {
        if (item == null) {
            return null;
        }

        OrderItemSnapshotDto dto = new OrderItemSnapshotDto();
        dto.setOfferId(item.getOfferId());
        dto.setQuantity(item.getQuantity());
        dto.setTitle(item.getTitle());
        dto.setImageUrl(item.getImageUrl());
        dto.setUnitPrice(toDouble(item.getUnitPrice()));
        return dto;
    }

    private PickupConfirmationDto toDto(OrderPickupConfirmation confirmation) {
        if (confirmation == null) {
            return null;
        }

        PickupConfirmationDto dto = new PickupConfirmationDto();
        dto.setConfirmedByUserId(confirmation.getConfirmedByUserId());
        dto.setConfirmedAt(ApiDateTimeMapper.toUtcOffsetDateTime(confirmation.getConfirmedAt()));
        return dto;
    }

    private OrderPaymentResponseDto toDto(OrderPayment payment) {
        if (payment == null) {
            return null;
        }

        OrderPaymentResponseDto dto = new OrderPaymentResponseDto();
        dto.setPaidByUserId(payment.getPaidByUserId());
        dto.setAmount(toDouble(payment.getAmount()));
        dto.setCurrency(payment.getCurrency());
        dto.setCardHolderName(payment.getCardHolderName());
        dto.setCardLast4(payment.getCardLast4());
        dto.setProviderReference(payment.getProviderReference());
        dto.setPickupToken(payment.getPickupToken());
        dto.setPaidAt(ApiDateTimeMapper.toUtcOffsetDateTime(payment.getPaidAt()));
        dto.setTransferredToBusinessAt(ApiDateTimeMapper.toUtcOffsetDateTime(payment.getTransferredToBusinessAt()));
        dto.setTransferReference(payment.getTransferReference());
        return dto;
    }

    private OrderReviewResponseDto toDto(OrderReviewView review) {
        if (review == null) {
            return null;
        }

        OrderReviewResponseDto dto = new OrderReviewResponseDto();
        dto.setRating(review.rating());
        dto.setComment(review.comment());
        dto.setCreatedAt(ApiDateTimeMapper.toUtcOffsetDateTime(review.createdAt()));
        return dto;
    }

    private OrderStatusDto toDto(sk.posam.fsa.foodrescue.domain.order.OrderStatus status) {
        return status == null ? null : OrderStatusDto.valueOf(status.name());
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    public record OrderCreateInput(
            Long offerId,
            Integer quantity,
            String cardHolderName,
            String cardLast4
    ) {
    }

    public record OrderReviewInput(
            Integer rating,
            String comment
    ) {
    }
}
