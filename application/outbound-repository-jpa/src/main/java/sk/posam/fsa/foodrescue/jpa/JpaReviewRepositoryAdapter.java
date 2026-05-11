package sk.posam.fsa.foodrescue.jpa;

import org.springframework.stereotype.Repository;
import sk.posam.fsa.foodrescue.domain.review.Review;
import sk.posam.fsa.foodrescue.domain.review.ReviewRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaReviewRepositoryAdapter implements ReviewRepository {

    private final ReviewSpringDataRepository reviewSpringDataRepository;

    public JpaReviewRepositoryAdapter(ReviewSpringDataRepository reviewSpringDataRepository) {
        this.reviewSpringDataRepository = reviewSpringDataRepository;
    }

    @Override
    public Review save(Review review) {
        return reviewSpringDataRepository.save(review);
    }

    @Override
    public List<Review> findAllByBusinessIds(List<Long> businessIds) {
        if (businessIds == null || businessIds.isEmpty()) {
            return List.of();
        }

        return reviewSpringDataRepository.findAllByBusinessIdInOrderByCreatedAtDesc(businessIds);
    }

    @Override
    public List<Review> findAllByReservationIds(List<Long> reservationIds) {
        if (reservationIds == null || reservationIds.isEmpty()) {
            return List.of();
        }

        return reviewSpringDataRepository.findAllByReservationIdIn(reservationIds);
    }

    @Override
    public Optional<Review> findByReservationId(Long reservationId) {
        if (reservationId == null) {
            return Optional.empty();
        }

        return reviewSpringDataRepository.findByReservationId(reservationId);
    }
}

