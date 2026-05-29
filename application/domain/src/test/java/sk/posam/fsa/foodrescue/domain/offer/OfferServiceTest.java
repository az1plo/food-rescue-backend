package sk.posam.fsa.foodrescue.domain.offer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.business.BusinessStatus;
import sk.posam.fsa.foodrescue.domain.order.OrderRepository;
import sk.posam.fsa.foodrescue.domain.shared.Address;
import sk.posam.fsa.foodrescue.domain.shared.AddressCoordinatesProvider;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.user.UserRole;
import sk.posam.fsa.foodrescue.domain.user.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AddressCoordinatesProvider addressCoordinatesProvider;

    @Test
    void browseAvailableOffersSkipsOffersThatExpireDuringRead() {
        Business business = activeBusiness(7L, 70L);
        Offer expiredOffer = availableOffer(business, LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(1));
        Offer freshOffer = availableOffer(business, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3));

        when(offerRepository.findAllAvailable()).thenReturn(List.of(expiredOffer, freshOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));

        OfferService service = new OfferService(
                offerRepository,
                businessRepository,
                orderRepository,
                addressCoordinatesProvider
        );

        List<Offer> result = service.browseAvailableOffers(activeUser(100L));

        assertEquals(1, result.size());
        assertEquals(freshOffer.getId(), result.get(0).getId());
        assertEquals(OfferStatus.EXPIRED, expiredOffer.getStatus());
        verify(offerRepository).save(expiredOffer);
    }

    @Test
    void settleExpiredOffersExpiresAvailableAndReservedOffers() {
        Business business = activeBusiness(7L, 70L);
        Offer expiredAvailable = availableOffer(business, LocalDateTime.now().minusHours(4), LocalDateTime.now().minusHours(2));
        Offer expiredReserved = reservedOffer(business, LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(1));
        Offer freshAvailable = availableOffer(business, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));

        when(offerRepository.findAll()).thenReturn(List.of(expiredAvailable, expiredReserved, freshAvailable));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OfferService service = new OfferService(
                offerRepository,
                businessRepository,
                orderRepository,
                addressCoordinatesProvider
        );

        int settledCount = service.settleExpiredOffers();

        assertEquals(2, settledCount);
        assertEquals(OfferStatus.EXPIRED, expiredAvailable.getStatus());
        assertEquals(OfferStatus.EXPIRED, expiredReserved.getStatus());
        assertEquals(OfferStatus.AVAILABLE, freshAvailable.getStatus());
        verify(offerRepository, times(2)).save(any(Offer.class));
    }

    @Test
    void getReturnsExpiredOfferForPublicUserAfterImmediateSettlement() {
        Business business = activeBusiness(7L, 70L);
        Offer expiredOffer = availableOffer(business, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusMinutes(10));
        User publicUser = activeUser(12L);

        when(offerRepository.findById(expiredOffer.getId())).thenReturn(Optional.of(expiredOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));

        OfferService service = new OfferService(
                offerRepository,
                businessRepository,
                orderRepository,
                addressCoordinatesProvider
        );

        Offer result = service.get(publicUser, expiredOffer.getId());

        assertEquals(expiredOffer.getId(), result.getId());
        assertEquals(OfferStatus.EXPIRED, expiredOffer.getStatus());
        verify(offerRepository).save(expiredOffer);
    }

    @Test
    void getBusinessOffersReturnsPublicCatalogForAnonymousUser() {
        Business business = activeBusiness(7L, 70L);
        Offer availableOffer = availableOffer(business, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3));
        Offer soldOutOffer = availableOffer(business, LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(4));
        soldOutOffer.markSoldOut();
        Offer draftOffer = baseOffer(business, LocalDateTime.now().plusHours(3), LocalDateTime.now().plusHours(5));
        draftOffer.setStatus(OfferStatus.DRAFT);

        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));
        when(offerRepository.findAllByBusinessId(business.getId())).thenReturn(List.of(availableOffer, soldOutOffer, draftOffer));

        OfferService service = new OfferService(
                offerRepository,
                businessRepository,
                orderRepository,
                addressCoordinatesProvider
        );

        List<Offer> result = service.getBusinessOffers(null, business.getId());

        assertEquals(List.of(availableOffer.getId(), soldOutOffer.getId()), result.stream().map(Offer::getId).toList());
    }

    @Test
    void getBusinessOffersRejectsAnonymousUserForInactiveBusiness() {
        Business business = activeBusiness(7L, 70L);
        business.setStatus(BusinessStatus.PENDING);

        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));
        when(offerRepository.findAllByBusinessId(business.getId())).thenReturn(List.of());

        OfferService service = new OfferService(
                offerRepository,
                businessRepository,
                orderRepository,
                addressCoordinatesProvider
        );

        FoodRescueException exception = assertThrows(
                FoodRescueException.class,
                () -> service.getBusinessOffers(null, business.getId())
        );

        assertEquals(FoodRescueException.Type.FORBIDDEN, exception.getType());
        assertTrue(exception.getMessage().contains("not allowed to browse offers"));
    }

    @Test
    void createPublishesOfferImmediatelyWithoutDraftStatus() {
        Business business = activeBusiness(7L, 70L);
        User currentUser = activeUser(70L);
        Offer newOffer = baseOffer(business, LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(5));

        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));
        when(addressCoordinatesProvider.populateCoordinates(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OfferService service = new OfferService(
                offerRepository,
                businessRepository,
                orderRepository,
                addressCoordinatesProvider
        );

        Offer createdOffer = service.create(currentUser, newOffer);

        assertEquals(OfferStatus.AVAILABLE, createdOffer.getStatus());
        verify(offerRepository).save(createdOffer);
    }

    @Test
    void createDueAutoRepeatOffersCreatesNextOccurrenceFromRecurringSeries() {
        Business business = activeBusiness(7L, 70L);
        Offer recurringOffer = autoRepeatOffer(
                business,
                LocalDateTime.now().minusDays(1).withHour(8).withMinute(0),
                LocalDateTime.now().minusDays(1).withHour(12).withMinute(0)
        );
        ArgumentCaptor<Offer> savedOfferCaptor = ArgumentCaptor.forClass(Offer.class);

        when(offerRepository.findAll()).thenReturn(List.of(recurringOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));

        OfferService service = new OfferService(
                offerRepository,
                businessRepository,
                orderRepository,
                addressCoordinatesProvider
        );

        int createdCount = service.createDueAutoRepeatOffers();

        assertEquals(1, createdCount);
        verify(offerRepository).save(savedOfferCaptor.capture());
        Offer createdOffer = savedOfferCaptor.getValue();
        assertEquals(recurringOffer.getRecurrenceKey(), createdOffer.getRecurrenceKey());
        assertEquals(recurringOffer.getAutoRepeatQuantity(), createdOffer.getQuantityAvailable());
        assertEquals(LocalTime.of(8, 0), createdOffer.getPickupTimeWindow().getFrom().toLocalTime());
        assertEquals(LocalTime.of(12, 0), createdOffer.getPickupTimeWindow().getTo().toLocalTime());
        assertTrue(
                createdOffer.getPickupTimeWindow().getFrom().toLocalDate().isAfter(
                        recurringOffer.getPickupTimeWindow().getFrom().toLocalDate()
                )
        );
    }

    private Offer availableOffer(Business business, LocalDateTime from, LocalDateTime to) {
        Offer offer = baseOffer(business, from, to);
        offer.publish(business);
        offer.setId(Math.abs(from.hashCode() + to.hashCode()) + 1L);
        return offer;
    }

    private Offer reservedOffer(Business business, LocalDateTime from, LocalDateTime to) {
        Offer offer = availableOffer(business, from, to);
        offer.markReserved();
        return offer;
    }

    private Offer baseOffer(Business business, LocalDateTime from, LocalDateTime to) {
        Offer offer = Offer.fromDraft(
                business.getId(),
                "Test offer",
                "Fresh rescue offer",
                null,
                OfferCategory.BAKERY,
                false,
                List.of(),
                List.of(),
                null,
                new BigDecimal("4.90"),
                new BigDecimal("10.00"),
                3,
                List.of(OfferItem.of("Croissant", 2)),
                PickupLocation.of(new Address("Hlavna 10", "Kosice", "04011", "Slovakia"), null),
                PickupTimeWindow.of(from, to)
        );
        offer.prepareForCreation();
        return offer;
    }

    private Offer autoRepeatOffer(Business business, LocalDateTime from, LocalDateTime to) {
        Offer offer = Offer.fromDraft(
                business.getId(),
                "Daily rescue bag",
                "Repeat every day",
                null,
                OfferCategory.BAKERY,
                false,
                List.of(),
                List.of(),
                null,
                new BigDecimal("4.90"),
                new BigDecimal("10.00"),
                3,
                List.of(OfferItem.of("Croissant", 2)),
                PickupLocation.of(new Address("Hlavna 10", "Kosice", "04011", "Slovakia"), null),
                PickupTimeWindow.of(from, to),
                true,
                null,
                5,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0)
        );
        offer.prepareForCreation();
        offer.publish(business);
        offer.setId(500L);
        return offer;
    }

    private Business activeBusiness(Long businessId, Long ownerId) {
        Business business = new Business();
        business.setId(businessId);
        business.assignOwner(ownerId);
        business.setStatus(BusinessStatus.ACTIVE);
        return business;
    }

    private User activeUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
