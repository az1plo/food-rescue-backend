package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.repositories.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.repositories.OfferRepository;
import sk.posam.fsa.foodrescue.domain.services.OfferFacade;
import sk.posam.fsa.foodrescue.domain.services.OfferService;

@Configuration
public class OfferBeanConfiguration {

    @Bean
    public OfferFacade offerFacade(OfferRepository offerRepository, BusinessRepository businessRepository) {
        return new OfferService(offerRepository, businessRepository);
    }
}
