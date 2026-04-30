package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.user.UserIdentityProvider;
import sk.posam.fsa.foodrescue.domain.user.UserRepository;
import sk.posam.fsa.foodrescue.domain.user.UserFacade;
import sk.posam.fsa.foodrescue.domain.user.UserService;

@Configuration
public class UserBeanConfiguration {

    @Bean
    public UserFacade userFacade(UserRepository userRepository,
                                 UserIdentityProvider userIdentityProvider) {
        return new UserService(userRepository, userIdentityProvider);
    }
}

