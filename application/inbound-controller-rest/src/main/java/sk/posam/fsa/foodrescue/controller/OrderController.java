package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.order.OrderDetailsView;
import sk.posam.fsa.foodrescue.domain.order.OrderFacade;
import sk.posam.fsa.foodrescue.domain.order.OrderPickupPass;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.mapper.OrderMapper;
import sk.posam.fsa.foodrescue.mapper.OrderMapper.OrderCreateInput;
import sk.posam.fsa.foodrescue.mapper.OrderMapper.OrderReviewInput;
import sk.posam.fsa.foodrescue.rest.api.OrdersApi;
import sk.posam.fsa.foodrescue.rest.dto.ConfirmOrderPickupRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.CreateOrderRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.CreateOrderReviewRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.OrderPickupPassResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.OrderResponseDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

import java.net.URI;
import java.util.List;

@RestController
public class OrderController implements OrdersApi {

    private final OrderFacade orderFacade;
    private final OrderMapper orderMapper;
    private final CurrentUserDetailService currentUserDetailService;

    public OrderController(OrderFacade orderFacade,
                           OrderMapper orderMapper,
                           CurrentUserDetailService currentUserDetailService) {
        this.orderFacade = orderFacade;
        this.orderMapper = orderMapper;
        this.currentUserDetailService = currentUserDetailService;
    }

    @Override
    public ResponseEntity<List<OrderResponseDto>> getOrders(Long businessId) {
        User user = currentUserDetailService.getFullCurrentUser();
        return ResponseEntity.ok(orderMapper.toDtos(orderFacade.getOrders(user, businessId)));
    }

    @Override
    public ResponseEntity<OrderResponseDto> createOrder(CreateOrderRequestDto createOrderRequestDto) {
        User user = currentUserDetailService.getFullCurrentUser();
        OrderCreateInput input = orderMapper.toCreateInput(createOrderRequestDto);
        OrderDetailsView order = orderFacade.create(
                user,
                input.offerId(),
                input.quantity(),
                input.cardHolderName(),
                input.cardLast4()
        );
        return ResponseEntity.created(URI.create("/orders/" + order.order().getId()))
                .body(orderMapper.toDto(order));
    }

    @Override
    public ResponseEntity<OrderResponseDto> getOrder(Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        return ResponseEntity.ok(orderMapper.toDto(orderFacade.get(user, id)));
    }

    @Override
    public ResponseEntity<OrderPickupPassResponseDto> getOrderPickupPass(Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        OrderPickupPass pickupPass = orderFacade.getPickupPass(user, id);
        return ResponseEntity.ok(orderMapper.toDto(pickupPass));
    }

    @Override
    public ResponseEntity<OrderResponseDto> confirmOrderPickup(Long id,
                                                               ConfirmOrderPickupRequestDto confirmOrderPickupRequestDto) {
        User user = currentUserDetailService.getFullCurrentUser();
        OrderDetailsView order = orderFacade.confirmPickup(
                user,
                id,
                orderMapper.toPickupToken(confirmOrderPickupRequestDto)
        );
        return ResponseEntity.ok(orderMapper.toDto(order));
    }

    @Override
    public ResponseEntity<OrderResponseDto> submitOrderReview(Long id,
                                                              CreateOrderReviewRequestDto createOrderReviewRequestDto) {
        User user = currentUserDetailService.getFullCurrentUser();
        OrderReviewInput input = orderMapper.toReviewInput(createOrderReviewRequestDto);
        OrderDetailsView order = orderFacade.submitReview(
                user,
                id,
                input.rating(),
                input.comment()
        );
        return ResponseEntity.ok(orderMapper.toDto(order));
    }
}
