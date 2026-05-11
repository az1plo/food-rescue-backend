package sk.posam.fsa.foodrescue.domain.review;

import sk.posam.fsa.foodrescue.domain.review.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {

    Review save(Review review);

    List<Review> findAllByBusinessIds(List<Long> businessIds);

    List<Review> findAllByReservationIds(List<Long> reservationIds);

    Optional<Review> findByReservationId(Long reservationId);
}


