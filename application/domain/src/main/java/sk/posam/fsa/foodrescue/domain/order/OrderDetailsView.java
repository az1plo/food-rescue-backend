package sk.posam.fsa.foodrescue.domain.order;

public record OrderDetailsView(
        Order order,
        OrderReviewView review
) {
}
