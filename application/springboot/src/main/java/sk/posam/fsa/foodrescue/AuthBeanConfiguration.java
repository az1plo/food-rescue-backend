package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.user.AuthTokenExchangeProvider;
import sk.posam.fsa.foodrescue.domain.user.AuthFacade;
import sk.posam.fsa.foodrescue.domain.user.AuthService;

@Configuration
public class AuthBeanConfiguration {

    @Bean
    public AuthFacade authFacade(AuthTokenExchangeProvider authTokenExchangeProvider) {
        return new AuthService(authTokenExchangeProvider);
    }
}

