package sk.posam.fsa.foodrescue.domain.order;

import sk.posam.fsa.foodrescue.domain.shared.ValidationException;

import java.math.BigDecimal;

public class OrderItem {

    private Long offerId;
    private Integer quantity;
    private String title;
    private String imageUrl;
    private BigDecimal unitPrice;

    public OrderItem() {
    }

    public static OrderItem of(Long offerId,
                               Integer quantity,
                               String title,
                               String imageUrl,
                               BigDecimal unitPrice) {
        OrderItem item = new OrderItem();
        item.offerId = offerId;
        item.quantity = quantity;
        item.title = title;
        item.imageUrl = imageUrl;
        item.unitPrice = unitPrice;
        return item;
    }

    public Long getOfferId() {
        return offerId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void prepareForCreation() {
        require(offerId != null, "Order item offer id is required");
        require(quantity != null && quantity > 0, "Order item quantity must be greater than zero");
        require(title != null && !title.isBlank(), "Order item title is required");
        require(unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) >= 0, "Order item unit price must not be negative");

        title = title.trim();

        if (imageUrl != null) {
            imageUrl = imageUrl.trim();
            if (imageUrl.isBlank()) {
                imageUrl = null;
            }
        }
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}
