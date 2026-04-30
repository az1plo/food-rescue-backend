package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.business.BusinessAnalyticsFacade;
import sk.posam.fsa.foodrescue.domain.business.BusinessAnalyticsService;
import sk.posam.fsa.foodrescue.domain.order.OrderRepository;

@Configuration
public class BusinessAnalyticsBeanConfiguration {

    @Bean
    public BusinessAnalyticsFacade businessAnalyticsFacade(BusinessRepository businessRepository,
                                                           OfferRepository offerRepository,
                                                           OrderRepository orderRepository) {
        return new BusinessAnalyticsService(businessRepository, offerRepository, orderRepository);
    }
}

