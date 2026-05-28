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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        List<Offer> offers = offerRepository.findAllAvailable();
        settleExpiredOffers(offers);

        return offers.stream()
                .filter(Offer::isAvailable)
                .filter(offer -> resolveBusiness(offer.getBusinessId()).isActive())
                .toList();
    }

    @Override
    public List<Offer> getBusinessOffers(User currentUser, Long businessId) {
        Business business = resolveBusiness(businessId);
        ensureManagedBusiness(currentUser, business);

        List<Offer> offers = offerRepository.findAllByBusinessId(businessId);
        settleExpiredOffers(offers);
        return offers;
    }

    @Override
    public Offer get(User currentUser, Long id) {
        Offer offer = settleExpiredOfferIfNeeded(resolveOffer(id));
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

    public int settleExpiredOffers() {
        return settleExpiredOffers(offerRepository.findAll()).size();
    }

    public int createDueAutoRepeatOffers() {
        LocalDateTime now = LocalDateTime.now();
        List<Offer> offers = new ArrayList<>(offerRepository.findAll());
        Map<String, Offer> latestOffersByRecurrenceKey = offers.stream()
                .filter(offer -> offer.getRecurrenceKey() != null)
                .collect(Collectors.toMap(
                        Offer::getRecurrenceKey,
                        Function.identity(),
                        this::resolveLatestRecurringOffer
                ));

        int createdOffers = 0;
        for (Offer templateOffer : latestOffersByRecurrenceKey.values()) {
            if (!templateOffer.isAutoRepeatEnabled() || templateOffer.getStatus() == OfferStatus.CANCELLED) {
                continue;
            }

            Business business = resolveBusiness(templateOffer.getBusinessId());
            if (!business.isActive()) {
                continue;
            }

            LocalDate targetDate = resolveNextAutoRepeatDate(templateOffer, now);
            if (targetDate == null || hasRecurringOfferForDate(offers, templateOffer.getRecurrenceKey(), targetDate)) {
                continue;
            }

            Offer repeatedOffer = templateOffer.createRepeatedOccurrenceFor(targetDate);
            repeatedOffer.publish(business);

            Offer savedOffer = offerRepository.save(repeatedOffer);
            offers.add(savedOffer);
            createdOffers++;
        }

        return createdOffers;
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

    private List<Offer> settleExpiredOffers(List<Offer> offers) {
        if (offers == null || offers.isEmpty()) {
            return List.of();
        }

        List<Offer> settledOffers = new ArrayList<>();
        for (Offer offer : offers) {
            if (!shouldExpire(offer)) {
                continue;
            }

            offer.expire();
            settledOffers.add(offerRepository.save(offer));
        }

        return settledOffers;
    }

    private Offer settleExpiredOfferIfNeeded(Offer offer) {
        if (!shouldExpire(offer)) {
            return offer;
        }

        offer.expire();
        return offerRepository.save(offer);
    }

    private boolean shouldExpire(Offer offer) {
        return offer != null
                && (offer.getStatus() == OfferStatus.AVAILABLE || offer.getStatus() == OfferStatus.RESERVED)
                && offer.getPickupTimeWindow() != null
                && offer.getPickupTimeWindow().hasEnded(LocalDateTime.now());
    }

    private Offer resolveLatestRecurringOffer(Offer left, Offer right) {
        return Comparator
                .comparing(this::resolveOfferOccurrenceAnchor, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(Offer::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .compare(left, right) >= 0 ? left : right;
    }

    private LocalDateTime resolveOfferOccurrenceAnchor(Offer offer) {
        if (offer == null) {
            return null;
        }

        if (offer.getPickupTimeWindow() != null && offer.getPickupTimeWindow().getFrom() != null) {
            return offer.getPickupTimeWindow().getFrom();
        }

        return offer.getCreatedAt();
    }

    private LocalDate resolveNextAutoRepeatDate(Offer offer, LocalDateTime now) {
        if (offer == null || offer.getPickupTimeWindow() == null) {
            return null;
        }

        LocalDate today = now.toLocalDate();
        LocalTime repeatStartTime = offer.getAutoRepeatPickupStartTime() != null
                ? offer.getAutoRepeatPickupStartTime()
                : offer.getPickupTimeWindow().getFrom().toLocalTime();
        LocalTime repeatEndTime = offer.getAutoRepeatPickupEndTime() != null
                ? offer.getAutoRepeatPickupEndTime()
                : offer.getPickupTimeWindow().getTo().toLocalTime();

        LocalDateTime todaysPickupFrom = LocalDateTime.of(today, repeatStartTime);
        LocalDateTime todaysPickupTo = LocalDateTime.of(today, repeatEndTime);
        if (!todaysPickupTo.isAfter(todaysPickupFrom)) {
            todaysPickupTo = todaysPickupTo.plusDays(1);
        }

        LocalDate targetDate = now.isBefore(todaysPickupTo) ? today : today.plusDays(1);
        LocalDate latestOfferDate = offer.getPickupTimeWindow().getFrom().toLocalDate();

        return latestOfferDate.isBefore(targetDate) ? targetDate : null;
    }

    private boolean hasRecurringOfferForDate(List<Offer> offers, String recurrenceKey, LocalDate date) {
        return offers.stream()
                .filter(Objects::nonNull)
                .filter(offer -> recurrenceKey.equals(offer.getRecurrenceKey()))
                .anyMatch(offer -> offer.getPickupTimeWindow() != null
                        && offer.getPickupTimeWindow().getFrom() != null
                        && offer.getPickupTimeWindow().getFrom().toLocalDate().equals(date));
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


