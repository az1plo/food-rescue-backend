package sk.posam.fsa.foodrescue.domain.marketplace;

import sk.posam.fsa.foodrescue.domain.offer.AllergenCode;
import sk.posam.fsa.foodrescue.domain.offer.OfferCategory;
import sk.posam.fsa.foodrescue.domain.offer.OfferStatus;
import sk.posam.fsa.foodrescue.domain.offer.PickupLocation;
import sk.posam.fsa.foodrescue.domain.offer.PickupTimeWindow;

import java.util.List;

public record MarketplaceOfferView(
        Long id,
        String title,
        String description,
        String imageUrl,
        OfferCategory category,
        boolean illustrativeImage,
        List<AllergenCode> containsAllergens,
        List<AllergenCode> mayContainAllergens,
        String otherAllergenNote,
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

