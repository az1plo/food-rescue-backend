package sk.posam.fsa.foodrescue.domain.support;

import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.order.Order;
import sk.posam.fsa.foodrescue.domain.order.OrderRepository;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SupportUserOrdersTool implements SupportAssistantTool {

    private final OrderRepository orderRepository;
    private final OfferRepository offerRepository;

    public SupportUserOrdersTool(OrderRepository orderRepository,
                                 OfferRepository offerRepository) {
        this.orderRepository = orderRepository;
        this.offerRepository = offerRepository;
    }

    @Override
    public SupportAssistantToolDefinition definition() {
        return new SupportAssistantToolDefinition(
                "get_user_orders",
                "Return the authenticated user's latest paid orders together with linked offer details.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "additionalProperties", false
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, SupportAssistantToolContext context) {
        User currentUser = context == null ? null : context.currentUser();
        if (currentUser == null || currentUser.getId() == null) {
            return Map.of(
                    "authenticated", false,
                    "message", "The user is not signed in."
            );
        }

        List<Order> orders = orderRepository.findAllByUserId(currentUser.getId());
        List<Long> offerIds = orders.stream()
                .map(order -> order.getItem() == null ? null : order.getItem().getOfferId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Offer> offersById = offerIds.stream()
                .map(offerRepository::findById)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toMap(Offer::getId, Function.identity(), (first, second) -> first));

        return Map.of(
                "authenticated", true,
                "orderCount", orders.size(),
                "orders", orders.stream()
                        .limit(5)
                        .map(order -> orderSummary(order, offersById.get(order.getItem() == null ? null : order.getItem().getOfferId())))
                        .toList()
        );
    }

    private Map<String, Object> orderSummary(Order order, Offer offer) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("orderId", order.getId());
        summary.put("status", order.getStatus() == null ? null : order.getStatus().name());
        summary.put("quantity", order.getItem() == null ? null : order.getItem().getQuantity());
        summary.put("createdAt", order.getCreatedAt());
        summary.put("businessName", order.getBusinessName());
        summary.put("offer", offer == null ? null : offerSummary(offer));
        return summary;
    }

    private Map<String, Object> offerSummary(Offer offer) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", offer.getId());
        summary.put("title", offer.getTitle());
        summary.put("pickupLocation", offer.getPickupLocation());
        summary.put("pickupTimeWindow", offer.getPickupTimeWindow());
        summary.put("status", offer.getStatus() == null ? null : offer.getStatus().name());
        return summary;
    }
}
