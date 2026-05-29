package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.shared.AddressCoordinatesProvider;
import sk.posam.fsa.foodrescue.domain.business.BusinessFacade;
import sk.posam.fsa.foodrescue.domain.business.BusinessIconStorage;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.business.BusinessService;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.order.OrderRepository;
import sk.posam.fsa.foodrescue.transactional.TransactionalBusinessFacade;

@Configuration
public class BusinessBeanConfiguration {

    @Bean
    public BusinessFacade businessFacade(BusinessRepository businessRepository,
                                         OfferRepository offerRepository,
                                         OrderRepository orderRepository,
                                         AddressCoordinatesProvider addressCoordinatesProvider,
                                         BusinessIconStorage businessIconStorage) {
        BusinessService businessService = new BusinessService(
                businessRepository,
                offerRepository,
                orderRepository,
                addressCoordinatesProvider,
                businessIconStorage
        );
        return new TransactionalBusinessFacade(businessService);
    }
}

