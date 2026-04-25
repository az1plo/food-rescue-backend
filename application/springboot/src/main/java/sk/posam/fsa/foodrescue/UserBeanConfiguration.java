package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.ports.UserIdentityProvider;
import sk.posam.fsa.foodrescue.domain.repositories.UserRepository;
import sk.posam.fsa.foodrescue.domain.services.UserFacade;
import sk.posam.fsa.foodrescue.domain.services.UserService;

@Configuration
public class UserBeanConfiguration {

    @Bean
    public UserFacade userFacade(UserRepository userRepository,
                                 UserIdentityProvider userIdentityProvider) {
        return new UserService(userRepository, userIdentityProvider);
    }
}
