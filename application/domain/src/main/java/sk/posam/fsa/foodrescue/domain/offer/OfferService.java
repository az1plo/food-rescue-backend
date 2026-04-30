package sk.posam.fsa.foodrescue.domain.offer;

import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.shared.ValidationException;
import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.order.Order;
import sk.posam.fsa.foodrescue.domain.order.OrderRepository;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.shared.AddressCoordinatesProvider;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;

import java.util.List;

public class OfferService implements OfferFacade {

    private final OfferRepository offerRepository;
    private final BusinessRepository businessRepository;
    private final OrderRepository orderRepository;
    private final AddressCoordinatesProvider addressCoordinatesProvider;

    public OfferService(OfferRepository offerRepository,
                        BusinessRepository businessRepository,
                        OrderRepository orderRepository,
                        AddressCoordinatesProvider addressCoordinatesProvider) {
        this.offerRepository = offerRepository;
        this.businessRepository = businessRepository;
        this.orderRepository = orderRepository;
        this.addressCoordinatesProvider = addressCoordinatesProvider;
    }

    @Override
    public List<Offer> browseAvailableOffers(User currentUser) {
        return offerRepository.findAllAvailable().stream()
                .filter(offer -> resolveBusiness(offer.getBusinessId()).isActive())
                .toList();
    }

    @Override
    public List<Offer> getBusinessOffers(User currentUser, Long businessId) {
        Business business = resolveBusiness(businessId);
        ensureManagedBusiness(currentUser, business);

        return offerRepository.findAllByBusinessId(businessId);
    }

    @Override
    public Offer get(User currentUser, Long id) {
        Offer offer = resolveOffer(id);
        Business business = resolveBusiness(offer.getBusinessId());

        if (business.canBeManagedBy(currentUser)) {
            return offer;
        }

        if (offer.isAvailable() && business.isActive()) {
            return offer;
        }

        if (hasUserOrderForOffer(currentUser, offer.getId())) {
            return offer;
        }

        throw new FoodRescueException(
                FoodRescueException.Type.FORBIDDEN,
                "You do not have access to offer with id=" + id
        );
    }

    @Override
    public Offer create(User currentUser, Offer offer) {
        if (offer == null) {
            throw new ValidationException("Offer must not be null");
        }

        ensureActiveUser(currentUser, "Only active users can create an offer");
        populatePickupCoordinates(offer);
        offer.prepareForCreation();

        Business business = resolveBusiness(offer.getBusinessId());
        ensureManagedBusiness(currentUser, business);

        if (!business.canPublishOffers()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "Only active businesses can create offers"
            );
        }

        offer.publish(business);

        return offerRepository.save(offer);
    }

    @Override
    public Offer update(User currentUser, Long id, Offer offerData) {
        if (offerData == null) {
            throw new ValidationException("Offer update data must not be null");
        }

        ensureActiveUser(currentUser, "Only active users can update an offer");

        Offer offer = resolveOffer(id);
        Business business = resolveBusiness(offer.getBusinessId());
        ensureManagedBusiness(currentUser, business);

        populatePickupCoordinates(offerData);
        offer.update(offerData);

        return offerRepository.save(offer);
    }

    @Override
    public void delete(User currentUser, Long id) {
        ensureActiveUser(currentUser, "Only active users can cancel an offer");

        Offer offer = resolveOffer(id);
        Business business = resolveBusiness(offer.getBusinessId());
        ensureManagedBusiness(currentUser, business);

        offer.cancel();
        offerRepository.save(offer);
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

    private void ensureManagedBusiness(User currentUser, Business business) {
        if (!business.canBeManagedBy(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You are not allowed to manage offers for this business"
            );
        }
    }

    private void ensureActiveUser(User currentUser, String message) {
        if (!currentUser.isActive()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    message
            );
        }
    }

    private boolean hasUserOrderForOffer(User currentUser, Long offerId) {
        if (currentUser == null || currentUser.getId() == null || offerId == null) {
            return false;
        }

        return orderRepository.findAllByUserId(currentUser.getId()).stream()
                .map(Order::getItem)
                .filter(java.util.Objects::nonNull)
                .map(item -> item.getOfferId())
                .anyMatch(offerId::equals);
    }

    private void populatePickupCoordinates(Offer offer) {
        if (offer == null || offer.getPickupLocation() == null) {
            return;
        }

        var resolvedAddress = addressCoordinatesProvider.populateCoordinates(offer.getPickupLocation().getAddress());
        if (resolvedAddress == null) {
            return;
        }

        offer.setPickupLocationCoordinates(resolvedAddress.getLatitude(), resolvedAddress.getLongitude());
    }
}


