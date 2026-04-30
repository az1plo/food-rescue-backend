package sk.posam.fsa.foodrescue.domain.business;

import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.order.Order;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.offer.OfferStatus;
import sk.posam.fsa.foodrescue.domain.order.OrderStatus;
import sk.posam.fsa.foodrescue.domain.business.BusinessAnalyticsSnapshot;
import sk.posam.fsa.foodrescue.domain.offer.OfferItem;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.order.OrderRepository;

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
    private final OrderRepository orderRepository;

    public BusinessAnalyticsService(BusinessRepository businessRepository,
                                    OfferRepository offerRepository,
                                    OrderRepository orderRepository) {
        this.businessRepository = businessRepository;
        this.offerRepository = offerRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public BusinessAnalyticsSnapshot getAnalytics(User currentUser, Long businessId) {
        Business business = resolveBusiness(businessId);
        ensureManagedBusiness(currentUser, business);

        List<Offer> offers = offerRepository.findAllByBusinessId(businessId);
        Map<Long, Offer> offersById = offers.stream()
                .filter(offer -> offer.getId() != null)
                .collect(Collectors.toMap(Offer::getId, offer -> offer));

        List<Order> orders = offersById.isEmpty()
                ? List.of()
                : orderRepository.findAllByBusinessId(businessId);

        Map<Long, List<Order>> ordersByOfferId = orders.stream()
                .filter(order -> order.getItem() != null && order.getItem().getOfferId() != null)
                .collect(Collectors.groupingBy(order -> order.getItem().getOfferId()));

        BusinessAnalyticsSnapshot.Overview overview = buildOverview(offers, orders, ordersByOfferId, offersById);

        return new BusinessAnalyticsSnapshot(
                business.getId(),
                business.getName(),
                LocalDateTime.now(),
                overview,
                buildCatalogStatus(offers),
                buildDailyActivity(offers, orders),
                buildDaypartPerformance(offers, ordersByOfferId),
                buildTopItems(offers, ordersByOfferId),
                buildInsights(business, overview, offers, orders, ordersByOfferId)
        );
    }

    private BusinessAnalyticsSnapshot.Overview buildOverview(List<Offer> offers,
                                                             List<Order> orders,
                                                             Map<Long, List<Order>> ordersByOfferId,
                                                             Map<Long, Offer> offersById) {
        int totalOffers = offers.size();
        int availableOffers = (int) offers.stream()
                .filter(offer -> offer.getStatus() == OfferStatus.AVAILABLE)
                .count();
        int offersClaimed = (int) offers.stream()
                .filter(offer -> offer.getId() != null)
                .filter(offer -> {
                    List<Order> offerOrders = ordersByOfferId.get(offer.getId());
                    return offerOrders != null && !offerOrders.isEmpty();
                })
                .count();
        int activeReservations = countOrdersByStatus(orders, OrderStatus.ACTIVE);
        int completedPickups = countOrdersByStatus(orders, OrderStatus.PICKED_UP);
        int cancelledReservations = countOrdersByStatus(orders, OrderStatus.CANCELLED);

        BigDecimal recoveredRevenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PICKED_UP)
                .map(order -> order.getPayment() == null ? BigDecimal.ZERO : order.getPayment().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingReservedRevenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.ACTIVE)
                .map(order -> order.getPayment() == null ? BigDecimal.ZERO : order.getPayment().getAmount())
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
                percentage(completedPickups, orders.size()),
                percentage(cancelledReservations, orders.size()),
                averageHoursToFirstReservation(offers, ordersByOfferId)
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
                                                                                  List<Order> orders) {
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

        for (Order order : orders) {
            incrementIfTracked(countersByDate, toDate(order.getCreatedAt()), DailyActivityCounter::incrementReservationsCreated);
            incrementIfTracked(countersByDate, toDate(order.getCancelledAt()), DailyActivityCounter::incrementCancellations);
            incrementIfTracked(
                    countersByDate,
                    order.getPickupConfirmation() == null ? null : toDate(order.getPickupConfirmation().getConfirmedAt()),
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
                                                                                       Map<Long, List<Order>> ordersByOfferId) {
        Map<String, DaypartCounter> counters = DAYPART_SLOTS.stream()
                .collect(Collectors.toMap(DaypartSlot::key, slot -> new DaypartCounter(), (first, second) -> first, LinkedHashMap::new));

        for (Offer offer : offers) {
            DaypartSlot slot = resolveDaypartSlot(offer);
            DaypartCounter counter = counters.get(slot.key());
            counter.incrementOffersScheduled();

            List<Order> offerOrders = offer.getId() == null
                    ? List.of()
                    : ordersByOfferId.getOrDefault(offer.getId(), List.of());

            if (!offerOrders.isEmpty()) {
                counter.incrementOffersClaimed();
            }

            List<Order> pickedUpOrders = offerOrders.stream()
                    .filter(order -> order.getStatus() == OrderStatus.PICKED_UP)
                    .toList();

            if (!pickedUpOrders.isEmpty()) {
                counter.incrementPickupsConfirmed();
                counter.addRecoveredRevenue(
                        pickedUpOrders.stream()
                                .map(order -> order.getPayment() == null ? BigDecimal.ZERO : order.getPayment().getAmount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                );
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
                                                                          Map<Long, List<Order>> ordersByOfferId) {
        Map<String, ItemCounter> counters = new HashMap<>();

        for (Offer offer : offers) {
            List<Order> offerOrders = offer.getId() == null
                    ? List.of()
                    : ordersByOfferId.getOrDefault(offer.getId(), List.of());

            int reservedMultiplier = offerOrders.stream()
                    .filter(order -> order.getStatus() == OrderStatus.ACTIVE || order.getStatus() == OrderStatus.PICKED_UP)
                    .map(order -> order.getItem() == null ? null : order.getItem().getQuantity())
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            boolean reserved = reservedMultiplier > 0;
            int pickedQuantityMultiplier = offerOrders.stream()
                    .filter(order -> order.getStatus() == OrderStatus.PICKED_UP)
                    .map(order -> order.getItem() == null ? null : order.getItem().getQuantity())
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            boolean pickedUp = offerOrders.stream()
                    .anyMatch(order -> order.getStatus() == OrderStatus.PICKED_UP);

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
                    counter.addReservedQuantity(item.getQuantity() * reservedMultiplier);
                }

                if (pickedUp) {
                    counter.addPickedUpQuantity(item.getQuantity() * pickedQuantityMultiplier);
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
                                                                  List<Order> orders,
                                                                  Map<Long, List<Order>> ordersByOfferId) {
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
                    formatCurrency(overview.pendingReservedRevenue()) + " are currently tied to paid orders waiting for pickup."
            ));
        }

        if (orders.size() >= 2 && overview.cancellationRate() >= 25.0) {
            insights.add(new BusinessAnalyticsSnapshot.Insight(
                    "warning",
                    "Cancellations need attention",
                    "Around " + formatPercent(overview.cancellationRate()) + " of orders end in cancellation. Review pickup notes and time windows for clarity."
            ));
        }

        bestDaypartInsight(offers, ordersByOfferId).ifPresent(insights::add);
        topItemInsight(offers, ordersByOfferId).ifPresent(insights::add);

        if (overview.averageHoursToFirstReservation() != null) {
            String tone = overview.averageHoursToFirstReservation() <= 6.0 ? "success" : "info";
            insights.add(new BusinessAnalyticsSnapshot.Insight(
                    tone,
                    "Claim speed",
                    "Offers are claimed in about " + formatHours(overview.averageHoursToFirstReservation()) + " on average from publish to first paid order."
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
                                                                           Map<Long, List<Order>> ordersByOfferId) {
        return buildDaypartPerformance(offers, ordersByOfferId)
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
                                                                       Map<Long, List<Order>> ordersByOfferId) {
        return buildTopItems(offers, ordersByOfferId).stream()
                .filter(item -> item.pickedUpQuantity() > 0)
                .findFirst()
                .map(item -> new BusinessAnalyticsSnapshot.Insight(
                        "info",
                        "Top rescued item",
                        item.itemName() + " leads completed pickups with " + item.pickedUpQuantity() + " units successfully rescued."
                ));
    }

    private Double averageHoursToFirstReservation(List<Offer> offers, Map<Long, List<Order>> ordersByOfferId) {
        return offers.stream()
                .map(offer -> firstOrderHours(offer, ordersByOfferId.get(offer.getId())))
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .stream()
                .map(this::roundToOneDecimal)
                .boxed()
                .findFirst()
                .orElse(null);
    }

    private Double firstOrderHours(Offer offer, List<Order> orders) {
        if (offer == null || offer.getCreatedAt() == null || orders == null || orders.isEmpty()) {
            return null;
        }

        return orders.stream()
                .map(Order::getCreatedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .map(firstOrderAt -> {
                    Duration duration = Duration.between(offer.getCreatedAt(), firstOrderAt);
                    if (duration.isNegative()) {
                        return null;
                    }

                    return roundToOneDecimal(duration.toMinutes() / 60.0);
                })
                .orElse(null);
    }

    private int countOrdersByStatus(List<Order> orders, OrderStatus status) {
        return (int) orders.stream()
                .filter(order -> order.getStatus() == status)
                .count();
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


