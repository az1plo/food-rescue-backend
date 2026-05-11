package sk.posam.fsa.foodrescue.domain.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.business.BusinessStatus;
import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.offer.OfferCategory;
import sk.posam.fsa.foodrescue.domain.offer.OfferItem;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.offer.PickupLocation;
import sk.posam.fsa.foodrescue.domain.offer.PickupTimeWindow;
import sk.posam.fsa.foodrescue.domain.review.ReviewRepository;
import sk.posam.fsa.foodrescue.domain.shared.Address;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Test
    void marksOwnBusinessOfferAsNotReservable() {
        Business business = activeBusiness(7L, 3L);
        Offer offer = activeOffer(business);
        User currentUser = activeUser(3L);

        when(offerRepository.findAll()).thenReturn(List.of(offer));
        when(businessRepository.findAll()).thenReturn(List.of(business));
        when(reviewRepository.findAllByBusinessIds(List.of(business.getId()))).thenReturn(List.of());

        MarketplaceService service = new MarketplaceService(offerRepository, businessRepository, reviewRepository);

        List<MarketplaceOfferView> offers = service.findOffers(currentUser, MarketplaceOfferCriteria.defaultCriteria());

        assertEquals(1, offers.size());
        assertFalse(offers.get(0).canReserve());
    }

    @Test
    void keepsOtherBusinessOfferReservable() {
        Business business = activeBusiness(7L, 3L);
        Offer offer = activeOffer(business);
        User currentUser = activeUser(99L);

        when(offerRepository.findAll()).thenReturn(List.of(offer));
        when(businessRepository.findAll()).thenReturn(List.of(business));
        when(reviewRepository.findAllByBusinessIds(List.of(business.getId()))).thenReturn(List.of());

        MarketplaceService service = new MarketplaceService(offerRepository, businessRepository, reviewRepository);

        List<MarketplaceOfferView> offers = service.findOffers(currentUser, MarketplaceOfferCriteria.defaultCriteria());

        assertEquals(1, offers.size());
        assertTrue(offers.get(0).canReserve());
    }

    private Business activeBusiness(Long businessId, Long ownerId) {
        Business business = Business.fromProfile(
                "Savr Bakery",
                "Local bakery",
                new Address("Hlavna 10", "Kosice", "04011", "Slovakia")
        );
        business.setId(businessId);
        business.assignOwner(ownerId);
        business.setStatus(BusinessStatus.ACTIVE);
        return business;
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
        return offer;
    }

    private User activeUser(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }
}
