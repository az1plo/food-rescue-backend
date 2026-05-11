package sk.posam.fsa.foodrescue.domain.offerassistant;

import sk.posam.fsa.foodrescue.domain.user.User;

public interface OfferAssistantFacade {

    OfferDraftSuggestion createDraftFromImage(User currentUser, OfferDraftRequest request);

    GeneratedOfferImage generateIllustrativeCover(User currentUser, OfferIllustrativeCoverRequest request);

    StoredOfferImage uploadOfferImage(User currentUser,
                                      Long businessId,
                                      OfferImageUpload upload,
                                      boolean illustrativeImage);

    StoredOfferImageContent getOfferImage(String imageId);
}
