package sk.posam.fsa.foodrescue.domain.marketplace;

import sk.posam.fsa.foodrescue.domain.user.User;

import java.util.List;

public interface MarketplaceFacade {

    List<MarketplaceOfferView> findOffers(User currentUser, MarketplaceOfferCriteria criteria);
}
