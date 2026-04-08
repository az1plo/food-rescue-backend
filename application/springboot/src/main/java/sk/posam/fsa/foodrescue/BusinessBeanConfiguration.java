package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.services.BusinessFacade;
import sk.posam.fsa.foodrescue.domain.repositories.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.services.BusinessService;

@Configuration
public class BusinessBeanConfiguration {

    @Bean
    public BusinessFacade businessFacade(BusinessRepository businessRepository) {
        return new BusinessService(businessRepository);
    }
}
