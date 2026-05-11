package sk.posam.fsa.foodrescue.domain.offerassistant;

public interface OfferAiProvider {

    OfferDraftSuggestion suggestDraft(OfferDraftRequest request);

    GeneratedOfferImage generateIllustrativeCover(OfferIllustrativeCoverRequest request);
}
