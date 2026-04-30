package sk.posam.fsa.foodrescue.domain.offer;

import sk.posam.fsa.foodrescue.domain.shared.ValidationException;

public class OfferItem {

    private String name;
    private Integer quantity;

    public OfferItem() {
    }

    public OfferItem(String name, Integer quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    public static OfferItem of(String name, Integer quantity) {
        return new OfferItem(name, quantity);
    }

    public String getName() {
        return name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void prepareForCreation() {
        require(name != null && !name.isBlank(), "Offer item name is required");
        require(quantity != null && quantity > 0, "Offer item quantity must be greater than zero");

        name = name.trim();
    }

    public OfferItem copy() {
        return new OfferItem(name, quantity);
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}


