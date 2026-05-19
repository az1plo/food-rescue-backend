package sk.posam.fsa.foodrescue.domain.marketplace;

import java.util.Locale;

public record MarketplaceOfferCriteria(
        String query,
        Double viewerLatitude,
        Double viewerLongitude,
        Integer radiusKm,
        MarketplaceOfferSort sort,
        boolean includeUnavailable
) {

    public static MarketplaceOfferCriteria defaultCriteria() {
        return new MarketplaceOfferCriteria(null, null, null, null, MarketplaceOfferSort.DISTANCE, false);
    }

    public MarketplaceOfferCriteria normalize() {
        String normalizedQuery = query == null ? null : query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery != null && normalizedQuery.isBlank()) {
            normalizedQuery = null;
        }

        return new MarketplaceOfferCriteria(
                normalizedQuery,
                viewerLatitude,
                viewerLongitude,
                radiusKm == null || radiusKm < 1 ? null : radiusKm,
                sort == null ? MarketplaceOfferSort.DISTANCE : sort,
                includeUnavailable
        );
    }
}
