package sk.posam.fsa.foodrescue.scheduling;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.offer.OfferService;

@Component
public class OfferExpirationScheduler {

    private final OfferService offerService;

    public OfferExpirationScheduler(OfferService offerService) {
        this.offerService = offerService;
    }

    @Scheduled(
            initialDelayString = "${foodrescue.offers.expiration-initial-delay-ms:60000}",
            fixedDelayString = "${foodrescue.offers.expiration-delay-ms:60000}"
    )
    public void settleExpiredOffers() {
        offerService.settleExpiredOffers();
        offerService.createDueAutoRepeatOffers();
    }
}
