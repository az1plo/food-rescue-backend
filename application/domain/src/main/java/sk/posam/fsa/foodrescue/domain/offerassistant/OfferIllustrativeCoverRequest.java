package sk.posam.fsa.foodrescue.domain.offerassistant;

import sk.posam.fsa.foodrescue.domain.offer.OfferCategory;

import java.util.List;

public record OfferIllustrativeCoverRequest(
        Long businessId,
        String title,
        String description,
        OfferCategory category,
        List<String> detectedItems
) {
}
