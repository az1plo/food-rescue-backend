package sk.posam.fsa.foodrescue.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.posam.fsa.foodrescue.domain.offerassistant.GeneratedOfferImage;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferAiProvider;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferDraftRequest;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferDraftSuggestion;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferIllustrativeCoverRequest;

public class OfferAiProviderWithFallback implements OfferAiProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OfferAiProviderWithFallback.class);

    private final OfferAiProvider primaryProvider;
    private final OfferAiProvider fallbackProvider;

    public OfferAiProviderWithFallback(OfferAiProvider primaryProvider, OfferAiProvider fallbackProvider) {
        this.primaryProvider = primaryProvider;
        this.fallbackProvider = fallbackProvider;
    }

    @Override
    public OfferDraftSuggestion suggestDraft(OfferDraftRequest request) {
        try {
            return primaryProvider.suggestDraft(request);
        } catch (RuntimeException ex) {
            LOGGER.error("OpenAI offer draft generation failed. Falling back to stub provider. Cause: {}",
                    ex.getMessage(), ex);
            return fallbackProvider.suggestDraft(request);
        }
    }

    @Override
    public GeneratedOfferImage generateIllustrativeCover(OfferIllustrativeCoverRequest request) {
        try {
            return primaryProvider.generateIllustrativeCover(request);
        } catch (RuntimeException ex) {
            LOGGER.error("OpenAI offer cover generation failed. Falling back to stub provider. Cause: {}",
                    ex.getMessage(), ex);
            return fallbackProvider.generateIllustrativeCover(request);
        }
    }
}
