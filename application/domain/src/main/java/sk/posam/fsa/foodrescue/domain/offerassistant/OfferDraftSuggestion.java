package sk.posam.fsa.foodrescue.domain.offerassistant;

import sk.posam.fsa.foodrescue.domain.offer.OfferCategory;

import java.util.List;

public record OfferDraftSuggestion(
        List<String> detectedItems,
        String suggestedTitle,
        String suggestedDescription,
        OfferCategory suggestedCategory
) {
}
