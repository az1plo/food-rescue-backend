package sk.posam.fsa.foodrescue.domain.marketplace;

import java.util.List;

public interface MarketplaceFacade {

    List<MarketplaceOfferView> findOffers(MarketplaceOfferCriteria criteria);
}
