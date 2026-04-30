package sk.posam.fsa.foodrescue.domain.marketplace;

import sk.posam.fsa.foodrescue.domain.shared.Address;

public record MarketplaceBusinessView(
        Long id,
        String name,
        String description,
        Address address,
        Double ratingAverage,
        Integer ratingCount,
        Integer availableOfferCount,
        Integer unavailableOfferCount
) {
}

