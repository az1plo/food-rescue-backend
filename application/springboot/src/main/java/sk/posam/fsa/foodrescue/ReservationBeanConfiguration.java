package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.repositories.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.repositories.NotificationRepository;
import sk.posam.fsa.foodrescue.domain.repositories.OfferRepository;
import sk.posam.fsa.foodrescue.domain.repositories.ReservationRepository;
import sk.posam.fsa.foodrescue.domain.services.ReservationFacade;
import sk.posam.fsa.foodrescue.domain.services.ReservationService;

@Configuration
public class ReservationBeanConfiguration {

    @Bean
    public ReservationFacade reservationFacade(ReservationRepository reservationRepository,
                                               OfferRepository offerRepository,
                                               BusinessRepository businessRepository,
                                               NotificationRepository notificationRepository) {
        return new ReservationService(reservationRepository, offerRepository, businessRepository, notificationRepository);
    }
}
