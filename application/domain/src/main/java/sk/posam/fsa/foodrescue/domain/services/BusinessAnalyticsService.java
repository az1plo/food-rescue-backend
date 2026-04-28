package sk.posam.fsa.foodrescue.domain.services;

import sk.posam.fsa.foodrescue.domain.exceptions.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.models.entities.Business;
import sk.posam.fsa.foodrescue.domain.models.entities.Offer;
import sk.posam.fsa.foodrescue.domain.models.entities.Reservation;
import sk.posam.fsa.foodrescue.domain.models.entities.User;
import sk.posam.fsa.foodrescue.domain.models.enums.OfferStatus;
import sk.posam.fsa.foodrescue.domain.models.enums.ReservationStatus;
import sk.posam.fsa.foodrescue.domain.models.valueobjects.BusinessAnalyticsSnapshot;
import sk.posam.fsa.foodrescue.domain.models.valueobjects.OfferItem;
import sk.posam.fsa.foodrescue.domain.repositories.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.repositories.OfferRepository;
import sk.posam.fsa.foodrescue.domain.repositories.ReservationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class BusinessAnalyticsService implements BusinessAnalyticsFacade {

    private static final List<DaypartSlot> DAYPART_SLOTS = List.of(
            new DaypartSlot("breakfast", "Breakfast", 6, 10),
            new DaypartSlot("lunch", "Lunch", 10, 14),
            new DaypartSlot("afternoon", "Afternoon", 14, 17),
            new DaypartSlot("evening", "Evening", 17, 21),
            new DaypartSlot("late", "Late hours", 21, 6)
    );

    private final BusinessRepository businessRepository;
    private final OfferRepository offerRepository;
    private final ReservationRepository reservationRepository;

    public BusinessAnalyticsService(BusinessRepository businessRepository,
                                    OfferRepository offerRepository,
                                    ReservationRepository reservationRepository) {
        this.businessRepository = businessRepository;
        this.offerRepository = offerRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public BusinessAnalyticsSnapshot getAnalytics(User currentUser, Long businessId) {
        Business business = resolveBusiness(businessId);
        ensureManagedBusiness(currentUser, business);

        List<Offer> offers = offerRepository.findAllByBusinessId(businessId);
        Map<Long, Offer> offersById = offers.stream()
                .filter(offer -> offer.getId() != null)
                .collect(Collectors.toMap(Offer::getId, offer -> offer));

        List<Reservation> reservations = offersById.isEmpty()
                ? List.of()
                : reservationRepository.findAllByOfferIds(new ArrayList<>(offersById.keySet()));

        Map<Long, List<Reservation>> reservationsByOfferId = reservations.stream()
                .filter(reservation -> reservation.getOfferId() != null)
                .collect(Collectors.groupingBy(Reservation::getOfferId));

        BusinessAnalyticsSnapshot.Overview overview = buildOverview(offers, reservations, reservationsByOfferId, offersById);

        return new BusinessAnalyticsSnapshot(
                business.getId(),
                business.getName(),
                LocalDateTime.now(),
                overview,
                buildCatalogStatus(offers),
                buildDailyActivity(offers, reservations),
                buildDaypartPerformance(offers, reservationsByOfferId, offersById),
                buildTopItems(offers, reservationsByOfferId),
                buildInsights(business, overview, offers, reservations, reservationsByOfferId)
        );
    }

    private BusinessAnalyticsSnapshot.Overview buildOverview(List<Offer> offers,
                                                             List<Reservation> reservations,
                                                             Map<Long, List<Reservation>> reservationsByOfferId,
                                                             Map<Long, Offer> offersById) {
        int totalOffers = offers.size();
        int availableOffers = (int) offers.stream()
                .filter(offer -> offer.getStatus() == OfferStatus.AVAILABLE)
                .count();
        int offersClaimed = (int) offers.stream()
                .filter(offer -> offer.getId() != null)
                .filter(offer -> {
                    List<Reservation> offerReservations = reservationsByOfferId.get(offer.getId());
                    return offerReservations != null && !offerReservations.isEmpty();
                })
                .count();
        int activeReservations = countReservationsByStatus(reservations, ReservationStatus.ACTIVE);
        int completedPickups = countReservationsByStatus(reservations, ReservationStatus.PICKED_UP);
        int cancelledReservations = countReservationsByStatus(reservations, ReservationStatus.CANCELLED);

        BigDecimal recoveredRevenue = reservations.stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.PICKED_UP)
                .map(reservation -> priceFor(reservation.getOfferId(), offersById))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingReservedRevenue = reservations.stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.ACTIVE)
                .map(reservation -> priceFor(reservation.getOfferId(), offersById))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BusinessAnalyticsSnapshot.Overview(
                totalOffers,
                availableOffers,
                offersClaimed,
                activeReservations,
                completedPickups,
                cancelledReservations,
                recoveredRevenue,
                pendingReservedRevenue,
                percentage(offersClaimed, totalOffers),
                percentage(completedPickups, reservations.size()),
                percentage(cancelledReservations, reservations.size()),
                averageHoursToFirstReservation(offers, reservationsByOfferId)
        );
    }

    private List<BusinessAnalyticsSnapshot.CatalogStatus> buildCatalogStatus(List<Offer> offers) {
        if (offers.isEmpty()) {
            return List.of();
        }

        Map<OfferStatus, Integer> countsByStatus = new EnumMap<>(OfferStatus.class);
        for (Offer offer : offers) {
            OfferStatus status = offer.getStatus();
            if (status == null) {
                continue;
            }

            countsByStatus.merge(status, 1, Integer::sum);
        }

        int totalOffers = offers.size();
        return countsByStatus.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new BusinessAnalyticsSnapshot.CatalogStatus(
                        entry.getKey().name(),
                        entry.getValue(),
                        percentage(entry.getValue(), totalOffers)
                ))
                .toList();
    }

    private List<BusinessAnalyticsSnapshot.DailyActivityPoint> buildDailyActivity(List<Offer> offers,
                                                                                  List<Reservation> reservations) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(13);
        Map<LocalDate, DailyActivityCounter> countersByDate = new LinkedHashMap<>();

        for (int index = 0; index < 14; index++) {
            LocalDate date = startDate.plusDays(index);
            countersByDate.put(date, new DailyActivityCounter());
        }

        for (Offer offer : offers) {
            incrementIfTracked(countersByDate, toDate(offer.getCreatedAt()), DailyActivityCounter::incrementOffersPublished);
        }

        for (Reservation reservation : reservations) {
            incrementIfTracked(countersByDate, toDate(reservation.getCreatedAt()), DailyActivityCounter::incrementReservationsCreated);
            incrementIfTracked(countersByDate, toDate(reservation.getCancelledAt()), DailyActivityCounter::incrementCancellations);
            incrementIfTracked(
                    countersByDate,
                    reservation.getPickupConfirmation() == null ? null : toDate(reservation.getPickupConfirmation().getConfirmedAt()),
                    DailyActivityCounter::incrementPickupsConfirmed
            );
        }

        return countersByDate.entrySet().stream()
                .map(entry -> new BusinessAnalyticsSnapshot.DailyActivityPoint(
                        entry.getKey(),
                        entry.getValue().offersPublished(),
                        entry.getValue().reservationsCreated(),
                        entry.getValue().cancellations(),
                        entry.getValue().pickupsConfirmed()
                ))
                .toList();
    }

    private List<BusinessAnalyticsSnapshot.DaypartPerformance> buildDaypartPerformance(List<Offer> offers,
                                                                                       Map<Long, List<Reservation>> reservationsByOfferId,
                                                                                       Map<Long, Offer> offersById) {
        Map<String, DaypartCounter> counters = DAYPART_SLOTS.stream()
                .collect(Collectors.toMap(DaypartSlot::key, slot -> new DaypartCounter(), (first, second) -> first, LinkedHashMap::new));

        for (Offer offer : offers) {
            DaypartSlot slot = resolveDaypartSlot(offer);
            DaypartCounter counter = counters.get(slot.key());
            counter.incrementOffersScheduled();

            List<Reservation> offerReservations = offer.getId() == null
                    ? List.of()
                    : reservationsByOfferId.getOrDefault(offer.getId(), List.of());

            if (!offerReservations.isEmpty()) {
                counter.incrementOffersClaimed();
            }

            boolean pickedUp = offerReservations.stream()
                    .anyMatch(reservation -> reservation.getStatus() == ReservationStatus.PICKED_UP);

            if (pickedUp) {
                counter.incrementPickupsConfirmed();
                counter.addRecoveredRevenue(priceFor(offer.getId(), offersById));
            }
        }

        return DAYPART_SLOTS.stream()
                .map(slot -> {
                    DaypartCounter counter = counters.get(slot.key());
                    return new BusinessAnalyticsSnapshot.DaypartPerformance(
                            slot.key(),
                            slot.label(),
                            counter.offersScheduled(),
                            counter.offersClaimed(),
                            counter.pickupsConfirmed(),
                            counter.recoveredRevenue(),
                            percentage(counter.offersClaimed(), counter.offersScheduled()),
                            percentage(counter.pickupsConfirmed(), counter.offersScheduled())
                    );
                })
                .toList();
    }

    private List<BusinessAnalyticsSnapshot.ItemPerformance> buildTopItems(List<Offer> offers,
                                                                          Map<Long, List<Reservation>> reservationsByOfferId) {
        Map<String, ItemCounter> counters = new HashMap<>();

        for (Offer offer : offers) {
            List<Reservation> offerReservations = offer.getId() == null
                    ? List.of()
                    : reservationsByOfferId.getOrDefault(offer.getId(), List.of());

            boolean reserved = !offerReservations.isEmpty();
            boolean pickedUp = offerReservations.stream()
                    .anyMatch(reservation -> reservation.getStatus() == ReservationStatus.PICKED_UP);

            for (OfferItem item : offer.getItems()) {
                if (item == null || item.getName() == null || item.getQuantity() == null) {
                    continue;
                }

                String normalizedName = item.getName().trim();
                if (normalizedName.isBlank()) {
                    continue;
                }

                String key = normalizedName.toLowerCase();
                ItemCounter counter = counters.computeIfAbsent(key, ignored -> new ItemCounter(normalizedName));
                counter.addOfferedQuantity(item.getQuantity());

                if (reserved) {
                    counter.addReservedQuantity(item.getQuantity());
                }

                if (pickedUp) {
                    counter.addPickedUpQuantity(item.getQuantity());
                }
            }
        }

        return counters.values().stream()
                .sorted(Comparator
                        .comparingInt(ItemCounter::pickedUpQuantity).reversed()
                        .thenComparingInt(ItemCounter::reservedQuantity).reversed()
                        .thenComparingInt(ItemCounter::offeredQuantity).reversed()
                        .thenComparing(ItemCounter::displayName))
                .limit(6)
                .map(counter -> new BusinessAnalyticsSnapshot.ItemPerformance(
                        counter.displayName(),
                        counter.offeredQuantity(),
                        counter.reservedQuantity(),
                        counter.pickedUpQuantity()
                ))
                .toList();
    }

    private List<BusinessAnalyticsSnapshot.Insight> buildInsights(Business business,
                                                                  BusinessAnalyticsSnapshot.Overview overview,
                                                                  List<Offer> offers,
                                                                  List<Reservation> reservations,
                                                                  Map<Long, List<Reservation>> reservationsByOfferId) {
        if (offers.isEmpty()) {
            return List.of(new BusinessAnalyticsSnapshot.Insight(
                    "info",
                    "No rescue history yet",
                    "Publish the first offer for this business to unlock claim-speed, pickup, and item-level analytics."
            ));
        }

        List<BusinessAnalyticsSnapshot.Insight> insights = new ArrayList<>();

        if (overview.pendingReservedRevenue().compareTo(BigDecimal.ZERO) > 0) {
            insights.add(new BusinessAnalyticsSnapshot.Insight(
                    "success",
                    "Reserved revenue is already in motion",
                    formatCurrency(overview.pendingReservedRevenue()) + " are currently tied to active reservations waiting for pickup."
            ));
        }

        if (reservations.size() >= 2 && overview.cancellationRate() >= 25.0) {
            insights.add(new BusinessAnalyticsSnapshot.Insight(
                    "warning",
                    "Cancellations need attention",
                    "Around " + formatPercent(overview.cancellationRate()) + " of reservations end in cancellation. Review pickup notes and time windows for clarity."
            ));
        }

        bestDaypartInsight(offers, reservationsByOfferId).ifPresent(insights::add);
        topItemInsight(offers, reservationsByOfferId).ifPresent(insights::add);

        if (overview.averageHoursToFirstReservation() != null) {
            String tone = overview.averageHoursToFirstReservation() <= 6.0 ? "success" : "info";
            insights.add(new BusinessAnalyticsSnapshot.Insight(
                    tone,
                    "Claim speed",
                    "Offers are claimed in about " + formatHours(overview.averageHoursToFirstReservation()) + " on average from publish to first reservation."
            ));
        }

        if (insights.size() < 4 && business.isActive() && overview.availableOffers() == 0) {
            insights.add(new BusinessAnalyticsSnapshot.Insight(
                    "info",
                    "No live offers right now",
                    "This business is active, but there are currently no available offers in the catalog. Publishing consistently will make the trend chart more useful."
            ));
        }

        return insights.stream().limit(4).toList();
    }

    private Optional<BusinessAnalyticsSnapshot.Insight> bestDaypartInsight(List<Offer> offers,
                                                                           Map<Long, List<Reservation>> reservationsByOfferId) {
        return buildDaypartPerformance(offers, reservationsByOfferId, offers.stream()
                .filter(offer -> offer.getId() != null)
                .collect(Collectors.toMap(Offer::getId, offer -> offer)))
                .stream()
                .filter(slot -> slot.offersScheduled() >= 2)
                .max(Comparator
                        .comparingDouble(BusinessAnalyticsSnapshot.DaypartPerformance::claimRate)
                        .thenComparingInt(BusinessAnalyticsSnapshot.DaypartPerformance::offersScheduled))
                .map(slot -> new BusinessAnalyticsSnapshot.Insight(
                        slot.claimRate() >= 70.0 ? "success" : "info",
                        "Best pickup window",
                        slot.label() + " performs best so far with " + formatPercent(slot.claimRate()) + " of scheduled offers getting claimed."
                ));
    }

    private Optional<BusinessAnalyticsSnapshot.Insight> topItemInsight(List<Offer> offers,
                                                                       Map<Long, List<Reservation>> reservationsByOfferId) {
        return buildTopItems(offers, reservationsByOfferId).stream()
                .filter(item -> item.pickedUpQuantity() > 0)
                .findFirst()
                .map(item -> new BusinessAnalyticsSnapshot.Insight(
                        "info",
                        "Top rescued item",
                        item.itemName() + " leads completed pickups with " + item.pickedUpQuantity() + " units successfully rescued."
                ));
    }

    private Double averageHoursToFirstReservation(List<Offer> offers, Map<Long, List<Reservation>> reservationsByOfferId) {
        return offers.stream()
                .map(offer -> firstReservationHours(offer, reservationsByOfferId.get(offer.getId())))
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .stream()
                .map(this::roundToOneDecimal)
                .boxed()
                .findFirst()
                .orElse(null);
    }

    private Double firstReservationHours(Offer offer, List<Reservation> reservations) {
        if (offer == null || offer.getCreatedAt() == null || reservations == null || reservations.isEmpty()) {
            return null;
        }

        return reservations.stream()
                .map(Reservation::getCreatedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .map(firstReservationAt -> {
                    Duration duration = Duration.between(offer.getCreatedAt(), firstReservationAt);
                    if (duration.isNegative()) {
                        return null;
                    }

                    return roundToOneDecimal(duration.toMinutes() / 60.0);
                })
                .orElse(null);
    }

    private int countReservationsByStatus(List<Reservation> reservations, ReservationStatus status) {
        return (int) reservations.stream()
                .filter(reservation -> reservation.getStatus() == status)
                .count();
    }

    private BigDecimal priceFor(Long offerId, Map<Long, Offer> offersById) {
        if (offerId == null) {
            return BigDecimal.ZERO;
        }

        Offer offer = offersById.get(offerId);
        return offer == null || offer.getPrice() == null ? BigDecimal.ZERO : offer.getPrice();
    }

    private double percentage(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }

        return roundToOneDecimal((numerator * 100.0) / denominator);
    }

    private double roundToOneDecimal(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String formatCurrency(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " EUR";
    }

    private String formatPercent(double value) {
        return roundToOneDecimal(value) + "%";
    }

    private String formatHours(double value) {
        return roundToOneDecimal(value) + " hours";
    }

    private LocalDate toDate(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    private void incrementIfTracked(Map<LocalDate, DailyActivityCounter> countersByDate,
                                    LocalDate date,
                                    java.util.function.Consumer<DailyActivityCounter> incrementer) {
        if (date == null) {
            return;
        }

        DailyActivityCounter counter = countersByDate.get(date);
        if (counter == null) {
            return;
        }

        incrementer.accept(counter);
    }

    private DaypartSlot resolveDaypartSlot(Offer offer) {
        if (offer == null || offer.getPickupTimeWindow() == null || offer.getPickupTimeWindow().getFrom() == null) {
            return DAYPART_SLOTS.getLast();
        }

        LocalTime time = offer.getPickupTimeWindow().getFrom().toLocalTime();
        return DAYPART_SLOTS.stream()
                .filter(slot -> slot.matches(time))
                .findFirst()
                .orElse(DAYPART_SLOTS.getLast());
    }

    private Business resolveBusiness(Long businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Business with id=" + businessId + " was not found"
                ));
    }

    private void ensureManagedBusiness(User currentUser, Business business) {
        if (!business.canBeManagedBy(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You are not allowed to view analytics for this business"
            );
        }
    }

    private static final class DailyActivityCounter {

        private int offersPublished;
        private int reservationsCreated;
        private int cancellations;
        private int pickupsConfirmed;

        int offersPublished() {
            return offersPublished;
        }

        int reservationsCreated() {
            return reservationsCreated;
        }

        int cancellations() {
            return cancellations;
        }

        int pickupsConfirmed() {
            return pickupsConfirmed;
        }

        void incrementOffersPublished() {
            offersPublished++;
        }

        void incrementReservationsCreated() {
            reservationsCreated++;
        }

        void incrementCancellations() {
            cancellations++;
        }

        void incrementPickupsConfirmed() {
            pickupsConfirmed++;
        }
    }

    private static final class DaypartCounter {

        private int offersScheduled;
        private int offersClaimed;
        private int pickupsConfirmed;
        private BigDecimal recoveredRevenue = BigDecimal.ZERO;

        int offersScheduled() {
            return offersScheduled;
        }

        int offersClaimed() {
            return offersClaimed;
        }

        int pickupsConfirmed() {
            return pickupsConfirmed;
        }

        BigDecimal recoveredRevenue() {
            return recoveredRevenue;
        }

        void incrementOffersScheduled() {
            offersScheduled++;
        }

        void incrementOffersClaimed() {
            offersClaimed++;
        }

        void incrementPickupsConfirmed() {
            pickupsConfirmed++;
        }

        void addRecoveredRevenue(BigDecimal value) {
            recoveredRevenue = recoveredRevenue.add(value == null ? BigDecimal.ZERO : value);
        }
    }

    private static final class ItemCounter {

        private final String displayName;
        private int offeredQuantity;
        private int reservedQuantity;
        private int pickedUpQuantity;

        private ItemCounter(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }

        int offeredQuantity() {
            return offeredQuantity;
        }

        int reservedQuantity() {
            return reservedQuantity;
        }

        int pickedUpQuantity() {
            return pickedUpQuantity;
        }

        void addOfferedQuantity(int quantity) {
            offeredQuantity += quantity;
        }

        void addReservedQuantity(int quantity) {
            reservedQuantity += quantity;
        }

        void addPickedUpQuantity(int quantity) {
            pickedUpQuantity += quantity;
        }
    }

    private record DaypartSlot(String key, String label, int startHourInclusive, int endHourExclusive) {

        boolean matches(LocalTime time) {
            if (time == null) {
                return false;
            }

            int hour = time.getHour();
            if (startHourInclusive < endHourExclusive) {
                return hour >= startHourInclusive && hour < endHourExclusive;
            }

            return hour >= startHourInclusive || hour < endHourExclusive;
        }
    }
}
