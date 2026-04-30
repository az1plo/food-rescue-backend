package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.notification.NotificationRepository;
import sk.posam.fsa.foodrescue.domain.notification.NotificationFacade;
import sk.posam.fsa.foodrescue.domain.notification.NotificationService;

@Configuration
public class NotificationBeanConfiguration {

    @Bean
    public NotificationFacade notificationFacade(NotificationRepository notificationRepository) {
        return new NotificationService(notificationRepository);
    }
}

