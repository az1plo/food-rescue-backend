package sk.posam.fsa.foodrescue.domain.offerassistant;

public record OfferDraftRequest(
        Long businessId,
        String businessName,
        OfferImageUpload image
) {
}
