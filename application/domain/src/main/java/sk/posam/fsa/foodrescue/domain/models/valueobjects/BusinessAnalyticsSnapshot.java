package sk.posam.fsa.foodrescue.domain.models.valueobjects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BusinessAnalyticsSnapshot(
        Long businessId,
        String businessName,
        LocalDateTime generatedAt,
        Overview overview,
        List<CatalogStatus> catalogStatus,
        List<DailyActivityPoint> dailyActivity,
        List<DaypartPerformance> daypartPerformance,
        List<ItemPerformance> topItems,
        List<Insight> insights
) {

    public BusinessAnalyticsSnapshot {
        catalogStatus = catalogStatus == null ? List.of() : List.copyOf(catalogStatus);
        dailyActivity = dailyActivity == null ? List.of() : List.copyOf(dailyActivity);
        daypartPerformance = daypartPerformance == null ? List.of() : List.copyOf(daypartPerformance);
        topItems = topItems == null ? List.of() : List.copyOf(topItems);
        insights = insights == null ? List.of() : List.copyOf(insights);
    }

    public record Overview(
            int totalOffers,
            int availableOffers,
            int offersClaimed,
            int activeReservations,
            int completedPickups,
            int cancelledReservations,
            BigDecimal recoveredRevenue,
            BigDecimal pendingReservedRevenue,
            double offerClaimRate,
            double pickupSuccessRate,
            double cancellationRate,
            Double averageHoursToFirstReservation
    ) {
    }

    public record CatalogStatus(
            String status,
            int count,
            double share
    ) {
    }

    public record DailyActivityPoint(
            LocalDate date,
            int offersPublished,
            int reservationsCreated,
            int cancellations,
            int pickupsConfirmed
    ) {
    }

    public record DaypartPerformance(
            String slotKey,
            String label,
            int offersScheduled,
            int offersClaimed,
            int pickupsConfirmed,
            BigDecimal recoveredRevenue,
            double claimRate,
            double pickupRate
    ) {
    }

    public record ItemPerformance(
            String itemName,
            int offeredQuantity,
            int reservedQuantity,
            int pickedUpQuantity
    ) {
    }

    public record Insight(
            String tone,
            String title,
            String message
    ) {
    }
}
