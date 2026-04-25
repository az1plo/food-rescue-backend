package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.repositories.NotificationRepository;
import sk.posam.fsa.foodrescue.domain.services.NotificationFacade;
import sk.posam.fsa.foodrescue.domain.services.NotificationService;

@Configuration
public class NotificationBeanConfiguration {

    @Bean
    public NotificationFacade notificationFacade(NotificationRepository notificationRepository) {
        return new NotificationService(notificationRepository);
    }
}
