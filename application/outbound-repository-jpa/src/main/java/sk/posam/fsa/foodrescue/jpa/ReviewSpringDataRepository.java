package sk.posam.fsa.foodrescue.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.posam.fsa.foodrescue.domain.review.Review;

import java.util.List;

interface ReviewSpringDataRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByBusinessIdInOrderByCreatedAtDesc(Iterable<Long> businessIds);
}

