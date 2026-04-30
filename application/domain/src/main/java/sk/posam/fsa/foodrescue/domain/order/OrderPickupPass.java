package sk.posam.fsa.foodrescue.domain.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderPickupPass(
        Long orderId,
        Long businessId,
        String pickupToken,
        String qrPayload,
        BigDecimal amount,
        String currency,
        LocalDateTime issuedAt,
        LocalDateTime paidAt,
        String providerReference
) {
}
