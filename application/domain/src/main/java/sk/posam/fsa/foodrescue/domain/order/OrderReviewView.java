package sk.posam.fsa.foodrescue.domain.order;

import java.time.LocalDateTime;

public record OrderReviewView(
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
}
