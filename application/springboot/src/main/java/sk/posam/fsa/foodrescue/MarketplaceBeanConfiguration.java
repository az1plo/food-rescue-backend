package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.marketplace.MarketplaceFacade;
import sk.posam.fsa.foodrescue.domain.marketplace.MarketplaceService;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.review.ReviewRepository;

@Configuration
public class MarketplaceBeanConfiguration {

    @Bean
    public MarketplaceFacade marketplaceFacade(OfferRepository offerRepository,
                                               BusinessRepository businessRepository,
                                               ReviewRepository reviewRepository) {
        return new MarketplaceService(offerRepository, businessRepository, reviewRepository);
    }
}

