package sk.posam.fsa.foodrescue.domain.support;

import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.review.Review;
import sk.posam.fsa.foodrescue.domain.review.ReviewRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SupportOfferLookupTool implements SupportAssistantTool {

    private final OfferRepository offerRepository;
    private final BusinessRepository businessRepository;
    private final ReviewRepository reviewRepository;

    public SupportOfferLookupTool(OfferRepository offerRepository,
                                  BusinessRepository businessRepository,
                                  ReviewRepository reviewRepository) {
        this.offerRepository = offerRepository;
        this.businessRepository = businessRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public SupportAssistantToolDefinition definition() {
        return new SupportAssistantToolDefinition(
                "get_offer_details",
                "Look up one Savr marketplace offer by id and return its current business, pricing, pickup, availability, and rating context.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "offerId", Map.of(
                                        "type", "integer",
                                        "description", "The numeric id of the offer to inspect."
                                )
                        ),
                        "required", List.of("offerId"),
                        "additionalProperties", false
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, SupportAssistantToolContext context) {
        Long offerId = readLong(arguments, "offerId");
        if (offerId == null) {
            return Map.of("found", false, "message", "offerId is required");
        }

        Optional<Offer> offerCandidate = offerRepository.findById(offerId);
        if (offerCandidate.isEmpty()) {
            return Map.of("found", false, "message", "Offer not found");
        }

        Offer offer = offerCandidate.get();
        Business business = offer.getBusinessId() == null
                ? null
                : businessRepository.findById(offer.getBusinessId()).orElse(null);

        Double averageRating = null;
        Integer ratingCount = 0;
        if (business != null && business.getId() != null) {
            List<Review> reviews = reviewRepository.findAllByBusinessIds(List.of(business.getId()));
            ratingCount = reviews.size();
            averageRating = reviews.isEmpty()
                    ? null
                    : Math.round(reviews.stream()
                    .map(Review::getRating)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0) * 10.0) / 10.0;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("offerId", offer.getId());
        result.put("title", offer.getTitle());
        result.put("description", offer.getDescription());
        result.put("status", offer.getStatus() == null ? null : offer.getStatus().name());
        result.put("price", offer.getPrice());
        result.put("originalPrice", offer.getOriginalPrice());
        result.put("quantityAvailable", offer.getQuantityAvailable());
        result.put("imageUrl", offer.getImageUrl());
        result.put("pickupLocation", offer.getPickupLocation());
        result.put("pickupTimeWindow", offer.getPickupTimeWindow());
        result.put("items", offer.getItems());
        result.put("business", business == null ? null : businessSummary(business));
        result.put("ratingAverage", averageRating);
        result.put("ratingCount", ratingCount);
        return result;
    }

    private Map<String, Object> businessSummary(Business business) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", business.getId());
        summary.put("name", business.getName());
        summary.put("status", business.getStatus() == null ? null : business.getStatus().name());
        summary.put("address", business.getAddress());
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
