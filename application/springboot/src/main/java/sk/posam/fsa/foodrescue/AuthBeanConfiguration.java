package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.ports.AuthTokenExchangeProvider;
import sk.posam.fsa.foodrescue.domain.services.AuthFacade;
import sk.posam.fsa.foodrescue.domain.services.AuthService;

@Configuration
public class AuthBeanConfiguration {

    @Bean
    public AuthFacade authFacade(AuthTokenExchangeProvider authTokenExchangeProvider) {
        return new AuthService(authTokenExchangeProvider);
    }
}
