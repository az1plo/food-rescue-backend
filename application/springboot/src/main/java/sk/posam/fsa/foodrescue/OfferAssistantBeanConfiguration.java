package sk.posam.fsa.foodrescue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import sk.posam.fsa.foodrescue.config.OfferAiProviderWithFallback;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferAiProvider;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferAssistantFacade;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferAssistantService;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferImageStorage;
import sk.posam.fsa.foodrescue.openai.OpenAiOfferAssistantProvider;
import sk.posam.fsa.foodrescue.supportstub.StubOfferAiProvider;

@Configuration
public class OfferAssistantBeanConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(OfferAssistantBeanConfiguration.class);

    @Bean
    public StubOfferAiProvider stubOfferAiProvider() {
        return new StubOfferAiProvider();
    }

    @Bean
    @Primary
    public OfferAiProvider offerAiProvider(
            @Value("${food-rescue.offer-assistant.provider:stub}") String provider,
            StubOfferAiProvider stubOfferAiProvider,
            ObjectProvider<OpenAiOfferAssistantProvider> openAiOfferAssistantProviderProvider) {
        if ("openai".equalsIgnoreCase(provider)) {
            OpenAiOfferAssistantProvider openAiOfferAssistantProvider = openAiOfferAssistantProviderProvider.getIfAvailable();
            if (openAiOfferAssistantProvider != null && openAiOfferAssistantProvider.isConfigured()) {
                LOGGER.info("Offer assistant provider selected: openai (with stub fallback)");
                return new OfferAiProviderWithFallback(openAiOfferAssistantProvider, stubOfferAiProvider);
            }

            LOGGER.warn("Offer assistant provider requested: openai, but OpenAI provider is not configured. Falling back to stub.");
        } else {
            LOGGER.info("Offer assistant provider selected: stub");
        }

        return stubOfferAiProvider;
    }

    @Bean
    public OfferAssistantFacade offerAssistantFacade(OfferAiProvider offerAiProvider,
                                                     OfferImageStorage offerImageStorage,
                                                     BusinessRepository businessRepository) {
        return new OfferAssistantService(offerAiProvider, offerImageStorage, businessRepository);
    }
}
