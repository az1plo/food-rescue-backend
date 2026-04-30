package sk.posam.fsa.foodrescue.domain.marketplace;

import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.review.Review;
import sk.posam.fsa.foodrescue.domain.offer.OfferStatus;
import sk.posam.fsa.foodrescue.domain.shared.Address;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.review.ReviewRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MarketplaceService implements MarketplaceFacade {

    private final OfferRepository offerRepository;
    private final BusinessRepository businessRepository;
    private final ReviewRepository reviewRepository;

    public MarketplaceService(OfferRepository offerRepository,
                              BusinessRepository businessRepository,
                              ReviewRepository reviewRepository) {
        this.offerRepository = offerRepository;
        this.businessRepository = businessRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public List<MarketplaceOfferView> findOffers(MarketplaceOfferCriteria criteria) {
        MarketplaceOfferCriteria resolvedCriteria = criteria == null
                ? MarketplaceOfferCriteria.defaultCriteria()
                : criteria.normalize();
        Map<Long, Business> activeBusinessesById = businessRepository.findAll().stream()
                .filter(Business::isActive)
                .filter(business -> business.getId() != null)
                .collect(Collectors.toMap(Business::getId, Function.identity(), (first, second) -> first));

        String normalizedQuery = resolvedCriteria.query();

        List<CatalogEntry> matchedEntries = offerRepository.findAll().stream()
                .filter(offer -> offer.getBusinessId() != null)
                .map(offer -> toCatalogEntry(offer, activeBusinessesById.get(offer.getBusinessId())))
                .filter(Objects::nonNull)
                .filter(entry -> entry.status() == OfferStatus.AVAILABLE
                        || entry.status() == OfferStatus.SOLD_OUT
                        || entry.status() == OfferStatus.EXPIRED)
                .filter(entry -> matchesQuery(entry, normalizedQuery))
                .toList();

        Map<Long, RatingSummary> ratingsByBusinessId = buildRatingsByBusinessId(matchedEntries);
        Map<Long, BusinessOfferCountSummary> countsByBusinessId = buildCountsByBusinessId(matchedEntries);

        List<CatalogEntry> visibleEntries = resolvedCriteria.includeUnavailable()
                ? matchedEntries
                : matchedEntries.stream()
                        .filter(entry -> entry.status() == OfferStatus.AVAILABLE)
                        .toList();

        return visibleEntries.stream()
                .map(entry -> toMarketplaceOfferView(
                        entry,
                        ratingsByBusinessId,
                        countsByBusinessId,
                        resolvedCriteria.viewerLatitude(),
                        resolvedCriteria.viewerLongitude()))
                .sorted(comparatorFor(resolvedCriteria.sort()))
                .toList();
    }

    private CatalogEntry toCatalogEntry(Offer offer, Business business) {
        if (offer == null || business == null) {
            return null;
        }

        OfferStatus status = resolveMarketplaceStatus(offer);
        return new CatalogEntry(offer, business, status);
    }

    private OfferStatus resolveMarketplaceStatus(Offer offer) {
        if (offer.getPickupTimeWindow() != null && offer.getPickupTimeWindow().hasEnded(LocalDateTime.now())) {
            return OfferStatus.EXPIRED;
        }

        if (offer.getStatus() == OfferStatus.SOLD_OUT || offer.getStatus() == OfferStatus.RESERVED || offer.getQuantityAvailable() <= 0) {
            return OfferStatus.SOLD_OUT;
        }

        return OfferStatus.AVAILABLE;
    }

    private boolean matchesQuery(CatalogEntry entry, String normalizedQuery) {
        if (normalizedQuery == null) {
            return true;
        }

        String searchableText = String.join(
                " ",
                safeValue(entry.offer().getTitle()),
                safeValue(entry.offer().getDescription()),
                safeValue(entry.business().getName()),
                safeValue(entry.business().getDescription()),
                safeValue(entry.business().getAddress() == null ? null : entry.business().getAddress().getStreet()),
                safeValue(entry.business().getAddress() == null ? null : entry.business().getAddress().getCity()),
                entry.offer().getItems().stream()
                        .filter(Objects::nonNull)
                        .map(item -> safeValue(item.getName()))
                        .collect(Collectors.joining(" "))
        ).toLowerCase(Locale.ROOT);

        return searchableText.contains(normalizedQuery);
    }

    private Map<Long, RatingSummary> buildRatingsByBusinessId(List<CatalogEntry> entries) {
        List<Long> businessIds = entries.stream()
                .map(entry -> entry.business().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (businessIds.isEmpty()) {
            return Map.of();
        }

        return reviewRepository.findAllByBusinessIds(businessIds).stream()
                .collect(Collectors.groupingBy(
                        Review::getBusinessId,
                        Collectors.collectingAndThen(Collectors.toList(), reviews -> {
                            int count = reviews.size();
                            if (count == 0) {
                                return new RatingSummary(null, 0);
                            }

                            double average = reviews.stream()
                                    .map(Review::getRating)
                                    .filter(Objects::nonNull)
                                    .mapToInt(Integer::intValue)
                                    .average()
                                    .orElse(0.0);

                            return new RatingSummary(roundToOneDecimal(average), count);
                        })
                ));
    }

    private Map<Long, BusinessOfferCountSummary> buildCountsByBusinessId(List<CatalogEntry> entries) {
        return entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.business().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), businessEntries -> {
                            int available = (int) businessEntries.stream()
                                    .filter(entry -> entry.status() == OfferStatus.AVAILABLE)
                                    .count();
                            int unavailable = (int) businessEntries.stream()
                                    .filter(entry -> entry.status() != OfferStatus.AVAILABLE)
                                    .count();
                            return new BusinessOfferCountSummary(available, unavailable);
                        })
                ));
    }

    private MarketplaceOfferView toMarketplaceOfferView(CatalogEntry entry,
                                                        Map<Long, RatingSummary> ratingsByBusinessId,
                                                        Map<Long, BusinessOfferCountSummary> countsByBusinessId,
                                                        Double viewerLatitude,
                                                        Double viewerLongitude) {
        RatingSummary rating = ratingsByBusinessId.getOrDefault(entry.business().getId(), new RatingSummary(null, 0));
        BusinessOfferCountSummary counts = countsByBusinessId.getOrDefault(entry.business().getId(), new BusinessOfferCountSummary(0, 0));
        Double distanceMeters = resolveDistanceMeters(viewerLatitude, viewerLongitude, entry.offer().getPickupLocation() == null
                ? null
                : entry.offer().getPickupLocation().getAddress());

        return new MarketplaceOfferView(
                entry.offer().getId(),
                entry.offer().getTitle(),
                entry.offer().getDescription(),
                entry.offer().getImageUrl(),
                entry.offer().getPrice().doubleValue(),
                entry.offer().getOriginalPrice() == null ? null : entry.offer().getOriginalPrice().doubleValue(),
                entry.offer().getQuantityAvailable(),
                entry.status(),
                badgeText(entry),
                distanceMeters,
                entry.status() == OfferStatus.AVAILABLE && entry.offer().getQuantityAvailable() > 0,
                entry.offer().getPickupLocation(),
                entry.offer().getPickupTimeWindow(),
                new MarketplaceBusinessView(
                        entry.business().getId(),
                        entry.business().getName(),
                        entry.business().getDescription(),
                        entry.business().getAddress(),
                        rating.average(),
                        rating.count(),
                        counts.available(),
                        counts.unavailable()
                )
        );
    }

    private Comparator<MarketplaceOfferView> comparatorFor(MarketplaceOfferSort sort) {
        MarketplaceOfferSort resolvedSort = sort == null ? MarketplaceOfferSort.DISTANCE : sort;
        return switch (resolvedSort) {
            case NEWEST -> Comparator.comparing(MarketplaceOfferView::pickupTimeWindow, Comparator.nullsLast(
                    Comparator.comparing(window -> window.getFrom(), Comparator.nullsLast(Comparator.reverseOrder()))
            ));
            case PRICE_ASC -> Comparator.comparing(MarketplaceOfferView::price)
                    .thenComparing(MarketplaceOfferView::title, String.CASE_INSENSITIVE_ORDER);
            case PICKUP_SOONEST -> Comparator.comparing(MarketplaceOfferView::pickupTimeWindow, Comparator.nullsLast(
                            Comparator.comparing(window -> window.getFrom(), Comparator.nullsLast(Comparator.naturalOrder()))
                    ))
                    .thenComparing(MarketplaceOfferView::title, String.CASE_INSENSITIVE_ORDER);
            case DISTANCE -> Comparator
                    .comparing(MarketplaceOfferView::distanceMeters, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(MarketplaceOfferView::pickupTimeWindow, Comparator.nullsLast(
                            Comparator.comparing(window -> window.getFrom(), Comparator.nullsLast(Comparator.naturalOrder()))
                    ))
                    .thenComparing(MarketplaceOfferView::title, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private Double resolveDistanceMeters(Double viewerLatitude, Double viewerLongitude, Address address) {
        if (viewerLatitude == null || viewerLongitude == null || address == null
                || address.getLatitude() == null || address.getLongitude() == null) {
            return null;
        }

        double earthRadiusMeters = 6_371_000.0;
        double startLatitude = Math.toRadians(viewerLatitude);
        double endLatitude = Math.toRadians(address.getLatitude());
        double latitudeDelta = Math.toRadians(address.getLatitude() - viewerLatitude);
        double longitudeDelta = Math.toRadians(address.getLongitude() - viewerLongitude);

        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(startLatitude) * Math.cos(endLatitude)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(earthRadiusMeters * c * 10.0) / 10.0;
    }

    private String badgeText(CatalogEntry entry) {
        return switch (entry.status()) {
            case SOLD_OUT -> "Sold out";
            case EXPIRED -> "Expired";
            case AVAILABLE -> {
                int quantity = entry.offer().getQuantityAvailable();
                if (quantity <= 5) {
                    yield quantity + (quantity == 1 ? " left" : " left");
                }
                yield null;
            }
            default -> null;
        };
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private record CatalogEntry(Offer offer, Business business, OfferStatus status) {
    }

    private record RatingSummary(Double average, int count) {
    }

    private record BusinessOfferCountSummary(int available, int unavailable) {
    }
}

