package sk.posam.fsa.foodrescue.domain.models.entities;

import sk.posam.fsa.foodrescue.domain.exceptions.ValidationException;
import sk.posam.fsa.foodrescue.domain.models.enums.ReservationStatus;
import sk.posam.fsa.foodrescue.domain.models.valueobjects.PickupConfirmation;

import java.time.LocalDateTime;

public class Reservation {

    private Long id;
    private Long offerId;
    private Long userId;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
    private PickupConfirmation pickupConfirmation;

    public Reservation() {
    }

    public static Reservation reserve(Long offerId, Long userId) {
        Reservation reservation = new Reservation();
        reservation.offerId = offerId;
        reservation.userId = userId;
        return reservation;
    }

    public Long getId() {
        return id;
    }

    public Long getOfferId() {
        return offerId;
    }

    public Long getUserId() {
        return userId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public PickupConfirmation getPickupConfirmation() {
        return pickupConfirmation;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public void setPickupConfirmation(PickupConfirmation pickupConfirmation) {
        this.pickupConfirmation = pickupConfirmation;
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    public boolean belongsTo(User user) {
        return user != null
                && userId != null
                && user.getId() != null
                && userId.equals(user.getId());
    }

    public void prepareForCreation() {
        require(offerId != null, "Offer is required");
        require(userId != null, "User is required");

        if (status == null) {
            status = ReservationStatus.ACTIVE;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void cancel() {
        require(status == ReservationStatus.ACTIVE, "Only active reservations can be cancelled");

        status = ReservationStatus.CANCELLED;

        if (cancelledAt == null) {
            cancelledAt = LocalDateTime.now();
        }
    }

    public void markPickedUp(Long confirmedByUserId) {
        require(status == ReservationStatus.ACTIVE, "Only active reservations can be picked up");

        PickupConfirmation confirmation = new PickupConfirmation();
        confirmation.setConfirmedByUserId(confirmedByUserId);
        confirmation.prepareForCreation();

        pickupConfirmation = confirmation;
        status = ReservationStatus.PICKED_UP;
    }

    public void markNoShow() {
        require(status == ReservationStatus.ACTIVE, "Only active reservations can be marked as no show");
        status = ReservationStatus.NO_SHOW;
    }

    public boolean canBeManagedBy(User user, Business business) {
        return belongsTo(user) || (business != null && business.canBeManagedBy(user));
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}
