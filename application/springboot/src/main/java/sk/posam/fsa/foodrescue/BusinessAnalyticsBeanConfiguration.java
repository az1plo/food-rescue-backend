package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.repositories.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.repositories.OfferRepository;
import sk.posam.fsa.foodrescue.domain.repositories.ReservationRepository;
import sk.posam.fsa.foodrescue.domain.services.BusinessAnalyticsFacade;
import sk.posam.fsa.foodrescue.domain.services.BusinessAnalyticsService;

@Configuration
public class BusinessAnalyticsBeanConfiguration {

    @Bean
    public BusinessAnalyticsFacade businessAnalyticsFacade(BusinessRepository businessRepository,
                                                           OfferRepository offerRepository,
                                                           ReservationRepository reservationRepository) {
        return new BusinessAnalyticsService(businessRepository, offerRepository, reservationRepository);
    }
}
