package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.models.valueobjects.BusinessAnalyticsSnapshot;
import sk.posam.fsa.foodrescue.rest.dto.BusinessAnalyticsCatalogStatusDto;
import sk.posam.fsa.foodrescue.rest.dto.BusinessAnalyticsDailyActivityPointDto;
import sk.posam.fsa.foodrescue.rest.dto.BusinessAnalyticsDaypartPerformanceDto;
import sk.posam.fsa.foodrescue.rest.dto.BusinessAnalyticsInsightDto;
import sk.posam.fsa.foodrescue.rest.dto.BusinessAnalyticsItemPerformanceDto;
import sk.posam.fsa.foodrescue.rest.dto.BusinessAnalyticsOverviewDto;
import sk.posam.fsa.foodrescue.rest.dto.BusinessAnalyticsResponseDto;

import java.math.BigDecimal;
import java.time.ZoneOffset;

@Component
public class BusinessAnalyticsMapper {

    public BusinessAnalyticsResponseDto toDto(BusinessAnalyticsSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }

        BusinessAnalyticsResponseDto dto = new BusinessAnalyticsResponseDto();
        dto.setBusinessId(snapshot.businessId());
        dto.setBusinessName(snapshot.businessName());
        dto.setGeneratedAt(snapshot.generatedAt() == null ? null : snapshot.generatedAt().atOffset(ZoneOffset.UTC));
        dto.setOverview(toOverviewDto(snapshot.overview()));
        dto.setCatalogStatus(snapshot.catalogStatus().stream()
                .map(this::toCatalogStatusDto)
                .toList());
        dto.setDailyActivity(snapshot.dailyActivity().stream()
                .map(this::toDailyActivityPointDto)
                .toList());
        dto.setDaypartPerformance(snapshot.daypartPerformance().stream()
                .map(this::toDaypartPerformanceDto)
                .toList());
        dto.setTopItems(snapshot.topItems().stream()
                .map(this::toItemPerformanceDto)
                .toList());
        dto.setInsights(snapshot.insights().stream()
                .map(this::toInsightDto)
                .toList());
        return dto;
    }

    private BusinessAnalyticsOverviewDto toOverviewDto(BusinessAnalyticsSnapshot.Overview overview) {
        if (overview == null) {
            return null;
        }

        BusinessAnalyticsOverviewDto dto = new BusinessAnalyticsOverviewDto();
        dto.setTotalOffers(overview.totalOffers());
        dto.setAvailableOffers(overview.availableOffers());
        dto.setOffersClaimed(overview.offersClaimed());
        dto.setActiveReservations(overview.activeReservations());
        dto.setCompletedPickups(overview.completedPickups());
        dto.setCancelledReservations(overview.cancelledReservations());
        dto.setRecoveredRevenue(toDouble(overview.recoveredRevenue()));
        dto.setPendingReservedRevenue(toDouble(overview.pendingReservedRevenue()));
        dto.setOfferClaimRate(overview.offerClaimRate());
        dto.setPickupSuccessRate(overview.pickupSuccessRate());
        dto.setCancellationRate(overview.cancellationRate());
        dto.setAverageHoursToFirstReservation(overview.averageHoursToFirstReservation());
        return dto;
    }

    private BusinessAnalyticsCatalogStatusDto toCatalogStatusDto(BusinessAnalyticsSnapshot.CatalogStatus status) {
        BusinessAnalyticsCatalogStatusDto dto = new BusinessAnalyticsCatalogStatusDto();
        dto.setStatus(status.status());
        dto.setCount(status.count());
        dto.setShare(status.share());
        return dto;
    }

    private BusinessAnalyticsDailyActivityPointDto toDailyActivityPointDto(BusinessAnalyticsSnapshot.DailyActivityPoint point) {
        BusinessAnalyticsDailyActivityPointDto dto = new BusinessAnalyticsDailyActivityPointDto();
        dto.setDate(point.date());
        dto.setOffersPublished(point.offersPublished());
        dto.setReservationsCreated(point.reservationsCreated());
        dto.setCancellations(point.cancellations());
        dto.setPickupsConfirmed(point.pickupsConfirmed());
        return dto;
    }

    private BusinessAnalyticsDaypartPerformanceDto toDaypartPerformanceDto(BusinessAnalyticsSnapshot.DaypartPerformance slot) {
        BusinessAnalyticsDaypartPerformanceDto dto = new BusinessAnalyticsDaypartPerformanceDto();
        dto.setSlotKey(slot.slotKey());
        dto.setLabel(slot.label());
        dto.setOffersScheduled(slot.offersScheduled());
        dto.setOffersClaimed(slot.offersClaimed());
        dto.setPickupsConfirmed(slot.pickupsConfirmed());
        dto.setRecoveredRevenue(toDouble(slot.recoveredRevenue()));
        dto.setClaimRate(slot.claimRate());
        dto.setPickupRate(slot.pickupRate());
        return dto;
    }

    private BusinessAnalyticsItemPerformanceDto toItemPerformanceDto(BusinessAnalyticsSnapshot.ItemPerformance item) {
        BusinessAnalyticsItemPerformanceDto dto = new BusinessAnalyticsItemPerformanceDto();
        dto.setItemName(item.itemName());
        dto.setOfferedQuantity(item.offeredQuantity());
        dto.setReservedQuantity(item.reservedQuantity());
        dto.setPickedUpQuantity(item.pickedUpQuantity());
        return dto;
    }

    private BusinessAnalyticsInsightDto toInsightDto(BusinessAnalyticsSnapshot.Insight insight) {
        BusinessAnalyticsInsightDto dto = new BusinessAnalyticsInsightDto();
        dto.setTone(insight.tone());
        dto.setTitle(insight.title());
        dto.setMessage(insight.message());
        return dto;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
