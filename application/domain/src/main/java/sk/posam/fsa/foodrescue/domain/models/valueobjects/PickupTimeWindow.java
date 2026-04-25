package sk.posam.fsa.foodrescue.domain.models.valueobjects;

import sk.posam.fsa.foodrescue.domain.exceptions.ValidationException;

import java.time.LocalDateTime;

public class PickupTimeWindow {

    private LocalDateTime from;
    private LocalDateTime to;

    public PickupTimeWindow() {
    }

    public PickupTimeWindow(LocalDateTime from, LocalDateTime to) {
        this.from = from;
        this.to = to;
    }

    public static PickupTimeWindow of(LocalDateTime from, LocalDateTime to) {
        return new PickupTimeWindow(from, to);
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }

    public boolean hasStarted(LocalDateTime referenceTime) {
        return referenceTime != null && from != null && !referenceTime.isBefore(from);
    }

    public boolean hasEnded(LocalDateTime referenceTime) {
        return referenceTime != null && to != null && !referenceTime.isBefore(to);
    }

    public void prepareForCreation() {
        require(from != null, "Pickup time window start is required");
        require(to != null, "Pickup time window end is required");
        require(from.isBefore(to), "Pickup time window must have start before end");
    }

    public PickupTimeWindow copy() {
        return new PickupTimeWindow(from, to);
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}
