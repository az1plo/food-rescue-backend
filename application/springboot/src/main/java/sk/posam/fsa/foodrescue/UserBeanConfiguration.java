package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.repositories.UserRepository;
import sk.posam.fsa.foodrescue.domain.service.UserFacade;
import sk.posam.fsa.foodrescue.domain.service.UserService;

@Configuration
public class UserBeanConfiguration {

    @Bean
    public UserFacade userFacade(UserRepository userRepository) {
        return new UserService(userRepository);
    }
}
