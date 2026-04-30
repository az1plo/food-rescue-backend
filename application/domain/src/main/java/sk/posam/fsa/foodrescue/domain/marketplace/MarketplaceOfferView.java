package sk.posam.fsa.foodrescue.domain.marketplace;

import sk.posam.fsa.foodrescue.domain.offer.OfferStatus;
import sk.posam.fsa.foodrescue.domain.offer.PickupLocation;
import sk.posam.fsa.foodrescue.domain.offer.PickupTimeWindow;

public record MarketplaceOfferView(
        Long id,
        String title,
        String description,
        String imageUrl,
        double price,
        Double originalPrice,
        int quantityAvailable,
        OfferStatus status,
        String badgeText,
        Double distanceMeters,
        boolean canReserve,
        PickupLocation pickupLocation,
        PickupTimeWindow pickupTimeWindow,
        MarketplaceBusinessView business
) {
}

