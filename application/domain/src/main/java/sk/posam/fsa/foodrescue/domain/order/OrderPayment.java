package sk.posam.fsa.foodrescue.domain.order;

import sk.posam.fsa.foodrescue.domain.shared.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderPayment {

    private Long paidByUserId;
    private BigDecimal amount;
    private String currency;
    private String cardHolderName;
    private String cardLast4;
    private String providerReference;
    private String pickupToken;
    private LocalDateTime paidAt;
    private LocalDateTime transferredToBusinessAt;
    private String transferReference;

    public Long getPaidByUserId() {
        return paidByUserId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getPickupToken() {
        return pickupToken;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getTransferredToBusinessAt() {
        return transferredToBusinessAt;
    }

    public String getTransferReference() {
        return transferReference;
    }

    public void setPaidByUserId(Long paidByUserId) {
        this.paidByUserId = paidByUserId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }

    public void setPickupToken(String pickupToken) {
        this.pickupToken = pickupToken;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public void setTransferredToBusinessAt(LocalDateTime transferredToBusinessAt) {
        this.transferredToBusinessAt = transferredToBusinessAt;
    }

    public void setTransferReference(String transferReference) {
        this.transferReference = transferReference;
    }

    public void prepareForCreation() {
        require(paidByUserId != null, "Order payment user is required");
        require(amount != null && amount.compareTo(BigDecimal.ZERO) >= 0, "Order payment amount must not be negative");
        require(currency != null && !currency.isBlank(), "Order payment currency is required");
        require(cardHolderName != null && !cardHolderName.isBlank(), "Order payment card holder name is required");
        require(cardLast4 != null && !cardLast4.isBlank(), "Order payment card last4 is required");
        require(providerReference != null && !providerReference.isBlank(), "Order payment provider reference is required");
        require(pickupToken != null && !pickupToken.isBlank(), "Order payment pickup token is required");

        currency = currency.trim();
        cardHolderName = cardHolderName.trim();
        cardLast4 = cardLast4.trim();
        providerReference = providerReference.trim();
        pickupToken = pickupToken.trim();

        if (paidAt == null) {
            paidAt = LocalDateTime.now();
        }
    }

    public void markTransferredToBusiness(String nextTransferReference) {
        require(nextTransferReference != null && !nextTransferReference.isBlank(), "Transfer reference is required");

        if (transferredToBusinessAt == null) {
            transferredToBusinessAt = LocalDateTime.now();
        }
        if (transferReference == null) {
            transferReference = nextTransferReference.trim();
        }
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}
