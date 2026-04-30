package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.notification.NotificationRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.reservation.ReservationRepository;
import sk.posam.fsa.foodrescue.domain.reservation.ReservationFacade;
import sk.posam.fsa.foodrescue.domain.reservation.ReservationService;

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

