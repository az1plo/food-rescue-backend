package sk.posam.fsa.foodrescue.domain.order;

import sk.posam.fsa.foodrescue.domain.shared.ValidationException;

import java.time.LocalDateTime;

public class OrderPickupConfirmation {

    private Long confirmedByUserId;
    private LocalDateTime confirmedAt;

    public Long getConfirmedByUserId() {
        return confirmedByUserId;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedByUserId(Long confirmedByUserId) {
        this.confirmedByUserId = confirmedByUserId;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public void prepareForCreation() {
        require(confirmedByUserId != null, "Pickup confirmation user is required");

        if (confirmedAt == null) {
            confirmedAt = LocalDateTime.now();
        }
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}
