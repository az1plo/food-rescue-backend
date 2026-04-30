package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.shared.AddressCoordinatesProvider;
import sk.posam.fsa.foodrescue.domain.business.BusinessFacade;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.business.BusinessService;

@Configuration
public class BusinessBeanConfiguration {

    @Bean
    public BusinessFacade businessFacade(BusinessRepository businessRepository,
                                         AddressCoordinatesProvider addressCoordinatesProvider) {
        return new BusinessService(businessRepository, addressCoordinatesProvider);
    }
}

