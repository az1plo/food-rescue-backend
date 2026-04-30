package sk.posam.fsa.foodrescue.domain.review;

import sk.posam.fsa.foodrescue.domain.review.Review;

import java.util.List;

public interface ReviewRepository {

    Review save(Review review);

    List<Review> findAllByBusinessIds(List<Long> businessIds);
}


