package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.shared.AddressCoordinatesProvider;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.reservation.ReservationRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferFacade;
import sk.posam.fsa.foodrescue.domain.offer.OfferService;

@Configuration
public class OfferBeanConfiguration {

    @Bean
    public OfferFacade offerFacade(OfferRepository offerRepository,
                                   BusinessRepository businessRepository,
                                   ReservationRepository reservationRepository,
                                   AddressCoordinatesProvider addressCoordinatesProvider) {
        return new OfferService(offerRepository, businessRepository, reservationRepository, addressCoordinatesProvider);
    }
}

