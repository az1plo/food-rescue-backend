package sk.posam.fsa.foodrescue.domain.support;

import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferStatus;
import sk.posam.fsa.foodrescue.domain.review.Review;
import sk.posam.fsa.foodrescue.domain.review.ReviewRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SupportBusinessLookupTool implements SupportAssistantTool {

    private final BusinessRepository businessRepository;
    private final OfferRepository offerRepository;
    private final ReviewRepository reviewRepository;

    public SupportBusinessLookupTool(BusinessRepository businessRepository,
                                     OfferRepository offerRepository,
                                     ReviewRepository reviewRepository) {
        this.businessRepository = businessRepository;
        this.offerRepository = offerRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public SupportAssistantToolDefinition definition() {
        return new SupportAssistantToolDefinition(
                "get_business_catalog",
                "Look up one business and summarize its public Savr catalog, offer statuses, and average rating.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "businessId", Map.of(
                                        "type", "integer",
                                        "description", "The numeric id of the business to inspect."
                                )
                        ),
                        "required", List.of("businessId"),
                        "additionalProperties", false
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, SupportAssistantToolContext context) {
        Long businessId = readLong(arguments, "businessId");
        if (businessId == null) {
            return Map.of("found", false, "message", "businessId is required");
        }

        Business business = businessRepository.findById(businessId).orElse(null);
        if (business == null) {
            return Map.of("found", false, "message", "Business not found");
        }

        List<Offer> offers = offerRepository.findAllByBusinessId(businessId);
        List<Review> reviews = reviewRepository.findAllByBusinessIds(List.of(businessId));

        long availableOffers = offers.stream()
                .filter(offer -> offer.getStatus() == OfferStatus.AVAILABLE)
                .count();
        long unavailableOffers = offers.stream()
                .filter(offer -> offer.getStatus() != OfferStatus.AVAILABLE)
                .count();
        Double averageRating = reviews.isEmpty()
                ? null
                : Math.round(reviews.stream()
                .map(Review::getRating)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0) * 10.0) / 10.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("businessId", business.getId());
        result.put("name", business.getName());
        result.put("description", business.getDescription());
        result.put("status", business.getStatus() == null ? null : business.getStatus().name());
        result.put("address", business.getAddress());
        result.put("ratingAverage", averageRating);
        result.put("ratingCount", reviews.size());
        result.put("availableOffers", availableOffers);
        result.put("unavailableOffers", unavailableOffers);
        result.put("offers", offers.stream()
                .limit(8)
                .map(this::offerSummary)
                .toList());
        return result;
    }

    private Map<String, Object> offerSummary(Offer offer) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", offer.getId());
        summary.put("title", offer.getTitle());
        summary.put("status", offer.getStatus() == null ? null : offer.getStatus().name());
        summary.put("price", offer.getPrice());
        summary.put("originalPrice", offer.getOriginalPrice());
        summary.put("quantityAvailable", offer.getQuantityAvailable());
        return summary;
    }

    private Long readLong(Map<String, Object> arguments, String key) {
        Object value = arguments == null ? null : arguments.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
