package sk.posam.fsa.foodrescue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.reservation.ReservationRepository;
import sk.posam.fsa.foodrescue.domain.review.ReviewRepository;
import sk.posam.fsa.foodrescue.domain.support.SupportAssistantTool;
import sk.posam.fsa.foodrescue.domain.support.SupportAssistantProvider;
import sk.posam.fsa.foodrescue.domain.support.SupportFacade;
import sk.posam.fsa.foodrescue.domain.support.SupportBusinessLookupTool;
import sk.posam.fsa.foodrescue.domain.support.SupportConversationRepository;
import sk.posam.fsa.foodrescue.domain.support.SupportOfferLookupTool;
import sk.posam.fsa.foodrescue.domain.support.SupportService;
import sk.posam.fsa.foodrescue.domain.support.SupportUserReservationsTool;
import sk.posam.fsa.foodrescue.openai.OpenAiSupportAssistantProvider;
import sk.posam.fsa.foodrescue.supportstub.StubSupportAssistantProvider;

@Configuration
public class SupportBeanConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupportBeanConfiguration.class);

    @Bean
    public StubSupportAssistantProvider stubSupportAssistantProvider() {
        return new StubSupportAssistantProvider();
    }

    @Bean
    public SupportAssistantTool supportOfferLookupTool(OfferRepository offerRepository,
                                                       BusinessRepository businessRepository,
                                                       ReviewRepository reviewRepository) {
        return new SupportOfferLookupTool(offerRepository, businessRepository, reviewRepository);
    }

    @Bean
    public SupportAssistantTool supportBusinessLookupTool(BusinessRepository businessRepository,
                                                          OfferRepository offerRepository,
                                                          ReviewRepository reviewRepository) {
        return new SupportBusinessLookupTool(businessRepository, offerRepository, reviewRepository);
    }

    @Bean
    public SupportAssistantTool supportUserReservationsTool(ReservationRepository reservationRepository,
                                                            OfferRepository offerRepository) {
        return new SupportUserReservationsTool(reservationRepository, offerRepository);
    }

    @Bean
    public SupportAssistantProvider supportAssistantProvider(
            @Value("${food-rescue.support.provider:stub}") String provider,
            StubSupportAssistantProvider stubSupportAssistantProvider,
            ObjectProvider<OpenAiSupportAssistantProvider> openAiSupportAssistantProviderProvider) {
        if ("openai".equalsIgnoreCase(provider)) {
            OpenAiSupportAssistantProvider openAiSupportAssistantProvider = openAiSupportAssistantProviderProvider.getIfAvailable();
            if (openAiSupportAssistantProvider != null && openAiSupportAssistantProvider.isConfigured()) {
                LOGGER.info("Support assistant provider selected: openai (with stub fallback)");
                return prompt -> {
                    try {
                        return openAiSupportAssistantProvider.reply(prompt);
                    } catch (RuntimeException ex) {
                        LOGGER.error("OpenAI support provider failed during chat reply. Falling back to stub provider. Cause: {}",
                                ex.getMessage(), ex);
                        return stubSupportAssistantProvider.reply(prompt);
                    }
                };
            }

            LOGGER.warn("Support assistant provider requested: openai, but OpenAI provider is not configured. Falling back to stub.");
            return stubSupportAssistantProvider;
        }

        LOGGER.info("Support assistant provider selected: stub");
        return stubSupportAssistantProvider;
    }

    @Bean
    public SupportFacade supportFacade(SupportAssistantProvider supportAssistantProvider,
                                       SupportConversationRepository supportConversationRepository) {
        return new SupportService(supportAssistantProvider, supportConversationRepository);
    }
}
