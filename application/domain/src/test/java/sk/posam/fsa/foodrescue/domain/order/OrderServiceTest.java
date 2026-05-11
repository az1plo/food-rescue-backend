package sk.posam.fsa.foodrescue.domain.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.business.BusinessStatus;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.notification.NotificationRepository;
import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.offer.OfferCategory;
import sk.posam.fsa.foodrescue.domain.offer.OfferItem;
import sk.posam.fsa.foodrescue.domain.offer.PickupLocation;
import sk.posam.fsa.foodrescue.domain.offer.PickupTimeWindow;
import sk.posam.fsa.foodrescue.domain.offer.OfferStatus;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.review.Review;
import sk.posam.fsa.foodrescue.domain.review.ReviewRepository;
import sk.posam.fsa.foodrescue.domain.shared.Address;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.user.UserRole;
import sk.posam.fsa.foodrescue.domain.user.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Test
    void getOrdersSettlesExpiredActiveOrderAsNoShow() {
        Order order = expiredActivePaidOrder();
        User currentUser = activeUser(order.getUserId());
        Business business = activeBusiness(order.getBusinessId(), 77L);

        when(orderRepository.findAllByUserId(currentUser.getId())).thenReturn(List.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessRepository.findById(order.getBusinessId())).thenReturn(Optional.of(business));
        when(reviewRepository.findAllByReservationIds(List.of(order.getId()))).thenReturn(List.of());

        OrderService service = new OrderService(
                orderRepository,
                offerRepository,
                businessRepository,
                notificationRepository,
                reviewRepository
        );

        List<OrderDetailsView> result = service.getOrders(currentUser, null);

        assertEquals(1, result.size());
        assertEquals(OrderStatus.NO_SHOW, result.get(0).order().getStatus());
        assertNotNull(result.get(0).order().getPayment().getTransferredToBusinessAt());
        verify(orderRepository).save(order);
        verify(notificationRepository, times(2)).save(any());
    }

    @Test
    void confirmPickupRejectsExpiredActiveOrderAfterNoShowSettlement() {
        Order order = expiredActivePaidOrder();
        Business business = activeBusiness(order.getBusinessId(), 77L);
        User manager = activeManager(business.getOwnerId());

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessRepository.findById(order.getBusinessId())).thenReturn(Optional.of(business));

        OrderService service = new OrderService(
                orderRepository,
                offerRepository,
                businessRepository,
                notificationRepository,
                reviewRepository
        );

        FoodRescueException exception = assertThrows(
                FoodRescueException.class,
                () -> service.confirmPickup(manager, order.getId(), order.getPayment().getPickupToken())
        );

        assertEquals(FoodRescueException.Type.CONFLICT, exception.getType());
        assertTrue(exception.getMessage().contains("no longer awaiting pickup"));
        assertEquals(OrderStatus.NO_SHOW, order.getStatus());
        assertNotNull(order.getPayment().getTransferredToBusinessAt());
    }

    @Test
    void submitReviewStoresVoteAndUpdatesBusinessAggregate() {
        Order order = pickedUpOrder();
        Business business = ratedBusiness();
        User currentUser = activeUser(order.getUserId());

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(businessRepository.findById(order.getBusinessId())).thenReturn(Optional.of(business));
        when(reviewRepository.findByReservationId(order.getId())).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessRepository.save(any(Business.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderService service = new OrderService(
                orderRepository,
                offerRepository,
                businessRepository,
                notificationRepository,
                reviewRepository
        );

        OrderDetailsView result = service.submitReview(currentUser, order.getId(), 5, null);

        assertNotNull(result.review());
        assertEquals(5, result.review().rating());

        ArgumentCaptor<Business> businessCaptor = ArgumentCaptor.forClass(Business.class);
        verify(businessRepository).save(businessCaptor.capture());

        Business savedBusiness = businessCaptor.getValue();
        assertEquals(3, savedBusiness.getRatingCount());
        assertEquals(4.3, savedBusiness.getRatingAverage(), 0.0001);
    }

    @Test
    void submitReviewRejectsOrderThatWasNotPickedUp() {
        Order order = pickedUpOrder();
        order.setStatus(OrderStatus.ACTIVE);
        User currentUser = activeUser(order.getUserId());

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        OrderService service = new OrderService(
                orderRepository,
                offerRepository,
                businessRepository,
                notificationRepository,
                reviewRepository
        );

        FoodRescueException exception = assertThrows(
                FoodRescueException.class,
                () -> service.submitReview(currentUser, order.getId(), 4, null)
        );

        assertEquals(FoodRescueException.Type.CONFLICT, exception.getType());
        assertTrue(exception.getMessage().contains("picked up"));
    }

    @Test
    void createRejectsOfferOwnedByCurrentUser() {
        Business business = activeBusiness(7L, 3L);
        Offer offer = activeOffer(business);
        User currentUser = activeUser(3L);

        when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));
        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));

        OrderService service = new OrderService(
                orderRepository,
                offerRepository,
                businessRepository,
                notificationRepository,
                reviewRepository
        );

        FoodRescueException exception = assertThrows(
                FoodRescueException.class,
                () -> service.create(currentUser, offer.getId(), 1, "Savr Owner", "1234")
        );

        assertEquals(FoodRescueException.Type.FORBIDDEN, exception.getType());
        assertTrue(exception.getMessage().contains("own business"));
    }

    private Order pickedUpOrder() {
        Order order = new Order();
        order.setId(11L);
        order.setBusinessId(7L);
        order.setUserId(3L);
        order.setBusinessName("Savr Bakery");
        order.setStatus(OrderStatus.PICKED_UP);
        return order;
    }

    private Offer activeOffer(Business business) {
        Offer offer = Offer.fromDraft(
                business.getId(),
                "Pastry Bag",
                "Fresh pastries from today's service",
                null,
                OfferCategory.BAKERY,
                false,
                List.of(),
                List.of(),
                null,
                new BigDecimal("4.90"),
                null,
                4,
                List.of(OfferItem.of("Croissant", 2)),
                PickupLocation.of(new Address("Hlavna 10", "Kosice", "04011", "Slovakia"), null),
                PickupTimeWindow.of(
                        LocalDateTime.now().plusHours(1),
                        LocalDateTime.now().plusHours(3)
                )
        );
        offer.setId(101L);
        offer.prepareForCreation();
        offer.publish(business);
        offer.setStatus(OfferStatus.AVAILABLE);
        return offer;
    }

    private Business ratedBusiness() {
        Business business = new Business();
        business.setId(7L);
        business.setRatingAverage(4.0);
        business.setRatingCount(2);
        return business;
    }

    private Business activeBusiness(Long businessId, Long ownerId) {
        Business business = new Business();
        business.setId(businessId);
        business.assignOwner(ownerId);
        business.setStatus(BusinessStatus.ACTIVE);
        return business;
    }

    private Order expiredActivePaidOrder() {
        Order order = new Order();
        order.setId(25L);
        order.setBusinessId(7L);
        order.setUserId(3L);
        order.setBusinessName("Savr Bakery");
        order.setStatus(OrderStatus.ACTIVE);
        order.setItem(OrderItem.of(101L, 1, "Pastry Bag", null, new BigDecimal("4.90")));
        order.setPickupLocation(PickupLocation.of(new Address("Hlavna 10", "Kosice", "04011", "Slovakia"), null));
        order.setPickupTimeWindow(PickupTimeWindow.of(
                LocalDateTime.now().minusHours(3),
                LocalDateTime.now().minusHours(1)
        ));

        OrderPayment payment = new OrderPayment();
        payment.setPaidByUserId(order.getUserId());
        payment.setAmount(new BigDecimal("4.90"));
        payment.setCurrency("EUR");
        payment.setCardHolderName("Savr Customer");
        payment.setCardLast4("2222");
        payment.setProviderReference("PAY-TEST");
        payment.setPickupToken("SAVR-TESTTOKEN");
        payment.setPaidAt(LocalDateTime.now().minusHours(4));
        order.setPayment(payment);

        return order;
    }

    private User activeUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private User activeManager(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
