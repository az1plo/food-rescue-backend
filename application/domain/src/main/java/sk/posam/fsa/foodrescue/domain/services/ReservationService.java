package sk.posam.fsa.foodrescue.domain.services;

import sk.posam.fsa.foodrescue.domain.exceptions.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.models.entities.Business;
import sk.posam.fsa.foodrescue.domain.models.entities.Notification;
import sk.posam.fsa.foodrescue.domain.models.entities.Offer;
import sk.posam.fsa.foodrescue.domain.models.entities.Reservation;
import sk.posam.fsa.foodrescue.domain.models.entities.User;
import sk.posam.fsa.foodrescue.domain.models.enums.NotificationType;
import sk.posam.fsa.foodrescue.domain.models.valueobjects.PickupTimeWindow;
import sk.posam.fsa.foodrescue.domain.repositories.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.repositories.NotificationRepository;
import sk.posam.fsa.foodrescue.domain.repositories.OfferRepository;
import sk.posam.fsa.foodrescue.domain.repositories.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;

public class ReservationService implements ReservationFacade {

    private final ReservationRepository reservationRepository;
    private final OfferRepository offerRepository;
    private final BusinessRepository businessRepository;
    private final NotificationRepository notificationRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              OfferRepository offerRepository,
                              BusinessRepository businessRepository,
                              NotificationRepository notificationRepository) {
        this.reservationRepository = reservationRepository;
        this.offerRepository = offerRepository;
        this.businessRepository = businessRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Reservation> getReservations(User currentUser, Long offerId) {
        if (offerId == null) {
            return reservationRepository.findAllByUserId(currentUser.getId());
        }

        Offer offer = resolveOffer(offerId);
        Business business = resolveBusiness(offer.getBusinessId());
        ensureBusinessManager(currentUser, business, "You are not allowed to view reservations for this offer");
        return reservationRepository.findAllByOfferId(offerId);
    }

    @Override
    public Reservation get(User currentUser, Long id) {
        Reservation reservation = resolveReservation(id);
        Offer offer = resolveOffer(reservation.getOfferId());
        Business business = resolveBusiness(offer.getBusinessId());

        if (!reservation.canBeManagedBy(currentUser, business)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You do not have access to reservation with id=" + id
            );
        }

        return reservation;
    }

    @Override
    public Reservation create(User currentUser, Long offerId) {
        ensureActiveUser(currentUser, "Only active users can create a reservation");

        Offer offer = resolveOffer(offerId);
        Business business = resolveBusiness(offer.getBusinessId());

        if (!business.isActive()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "Only offers of active businesses can be reserved"
            );
        }

        if (!offer.isAvailable()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Offer with id=" + offerId + " is no longer available"
            );
        }

        if (reservationRepository.findActiveByOfferId(offerId).isPresent()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Offer with id=" + offerId + " already has an active reservation"
            );
        }

        Reservation reservation = Reservation.reserve(offerId, currentUser.getId());
        reservation.prepareForCreation();
        offer.markReserved();

        Reservation savedReservation = reservationRepository.save(reservation);
        offerRepository.save(offer);

        createNotification(
                currentUser.getId(),
                NotificationType.OFFER_RESERVED,
                "Reservation confirmed",
                "Your reservation for \"" + offer.getTitle() + "\" is active."
        );
        if (!currentUser.getId().equals(business.getOwnerId())) {
            createNotification(
                    business.getOwnerId(),
                    NotificationType.OFFER_RESERVED,
                    "Offer reserved",
                    "Offer \"" + offer.getTitle() + "\" was reserved by user #" + currentUser.getId() + "."
            );
        }

        return savedReservation;
    }

    @Override
    public Reservation cancel(User currentUser, Long id) {
        Reservation reservation = resolveReservation(id);
        Offer offer = resolveOffer(reservation.getOfferId());
        Business business = resolveBusiness(offer.getBusinessId());

        ensureReservationOwnerOrAdmin(currentUser, reservation);
        ensureCancellationAllowed(offer.getPickupTimeWindow(), id);

        reservation.cancel();
        offer.reopenAfterReservationCancellation();

        Reservation savedReservation = reservationRepository.save(reservation);
        offerRepository.save(offer);

        createNotification(
                reservation.getUserId(),
                NotificationType.RESERVATION_STATUS_CHANGED,
                "Reservation cancelled",
                "Reservation for \"" + offer.getTitle() + "\" was cancelled."
        );
        if (!reservation.getUserId().equals(business.getOwnerId())) {
            createNotification(
                    business.getOwnerId(),
                    NotificationType.RESERVATION_STATUS_CHANGED,
                    "Reservation cancelled",
                    "Reservation for offer \"" + offer.getTitle() + "\" was cancelled."
            );
        }

        return savedReservation;
    }

    @Override
    public Reservation confirmPickup(User currentUser, Long id) {
        Reservation reservation = resolveReservation(id);
        Offer offer = resolveOffer(reservation.getOfferId());
        Business business = resolveBusiness(offer.getBusinessId());

        ensureActiveUser(currentUser, "Only active users can confirm pickup");
        ensureBusinessManager(currentUser, business, "You are not allowed to confirm pickup for this reservation");

        reservation.markPickedUp(currentUser.getId());
        offer.markPickedUp();

        Reservation savedReservation = reservationRepository.save(reservation);
        offerRepository.save(offer);

        createNotification(
                reservation.getUserId(),
                NotificationType.RESERVATION_STATUS_CHANGED,
                "Pickup confirmed",
                "Pickup for \"" + offer.getTitle() + "\" was confirmed."
        );

        return savedReservation;
    }

    private Reservation resolveReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Reservation with id=" + id + " was not found"
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

    private void ensureReservationOwnerOrAdmin(User currentUser, Reservation reservation) {
        if (currentUser != null && currentUser.isAdmin()) {
            return;
        }

        if (!reservation.belongsTo(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You are not allowed to cancel this reservation"
            );
        }
    }

    private void ensureCancellationAllowed(PickupTimeWindow pickupTimeWindow, Long reservationId) {
        if (pickupTimeWindow != null && pickupTimeWindow.hasEnded(LocalDateTime.now())) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Reservation with id=" + reservationId + " can no longer be cancelled"
            );
        }
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
