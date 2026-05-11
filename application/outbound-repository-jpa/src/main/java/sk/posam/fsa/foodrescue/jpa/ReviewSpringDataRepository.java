package sk.posam.fsa.foodrescue.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.posam.fsa.foodrescue.domain.review.Review;

import java.util.List;
import java.util.Optional;

interface ReviewSpringDataRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByBusinessIdInOrderByCreatedAtDesc(Iterable<Long> businessIds);

    List<Review> findAllByReservationIdIn(Iterable<Long> reservationIds);

    Optional<Review> findByReservationId(Long reservationId);
}

