package sk.posam.fsa.foodrescue.domain.support;

import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.reservation.Reservation;
import sk.posam.fsa.foodrescue.domain.reservation.ReservationRepository;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SupportUserReservationsTool implements SupportAssistantTool {

    private final ReservationRepository reservationRepository;
    private final OfferRepository offerRepository;

    public SupportUserReservationsTool(ReservationRepository reservationRepository,
                                       OfferRepository offerRepository) {
        this.reservationRepository = reservationRepository;
        this.offerRepository = offerRepository;
    }

    @Override
    public SupportAssistantToolDefinition definition() {
        return new SupportAssistantToolDefinition(
                "get_user_reservations",
                "Return the authenticated user's latest reservations together with linked offer details.",
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

        List<Reservation> reservations = reservationRepository.findAllByUserId(currentUser.getId());
        List<Long> offerIds = reservations.stream()
                .map(Reservation::getOfferId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Offer> offersById = offerIds.stream()
                .map(offerRepository::findById)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toMap(Offer::getId, Function.identity(), (first, second) -> first));

        return Map.of(
                "authenticated", true,
                "reservationCount", reservations.size(),
                "reservations", reservations.stream()
                        .limit(5)
                        .map(reservation -> reservationSummary(reservation, offersById.get(reservation.getOfferId())))
                        .toList()
        );
    }

    private Map<String, Object> reservationSummary(Reservation reservation, Offer offer) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("reservationId", reservation.getId());
        summary.put("status", reservation.getStatus() == null ? null : reservation.getStatus().name());
        summary.put("quantity", reservation.getQuantity());
        summary.put("createdAt", reservation.getCreatedAt());
        summary.put("cancelledAt", reservation.getCancelledAt());
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
