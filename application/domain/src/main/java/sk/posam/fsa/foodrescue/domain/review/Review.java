package sk.posam.fsa.foodrescue.domain.review;

import sk.posam.fsa.foodrescue.domain.shared.ValidationException;

import java.time.LocalDateTime;

public class Review {

    private Long id;
    private Long reservationId;
    private Long businessId;
    private Long userId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public Review() {
    }

    public static Review create(Long reservationId, Long businessId, Long userId, Integer rating, String comment) {
        Review review = new Review();
        review.reservationId = reservationId;
        review.businessId = businessId;
        review.userId = userId;
        review.rating = rating;
        review.comment = comment;
        return review;
    }

    public Long getId() {
        return id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void prepareForCreation() {
        require(reservationId != null, "Reservation is required");
        require(businessId != null, "Business is required");
        require(userId != null, "User is required");
        require(rating != null && rating >= 1 && rating <= 5, "Rating must be between 1 and 5");

        if (comment != null) {
            comment = comment.trim();
            if (comment.isBlank()) {
                comment = null;
            }
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}


