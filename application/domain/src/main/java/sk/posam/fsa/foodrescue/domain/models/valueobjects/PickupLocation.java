package sk.posam.fsa.foodrescue.domain.models.valueobjects;

import sk.posam.fsa.foodrescue.domain.exceptions.ValidationException;

public class PickupLocation {

    private Address address;
    private String note;

    public PickupLocation() {
    }

    public PickupLocation(Address address, String note) {
        this.address = address;
        this.note = note;
    }

    public static PickupLocation of(Address address, String note) {
        return new PickupLocation(address, note);
    }

    public Address getAddress() {
        return address == null ? null : address.copy();
    }

    public String getNote() {
        return note;
    }

    public void prepareForCreation() {
        require(address != null, "Pickup location address is required");

        address = address.copy();
        address.prepareForCreation();

        if (note != null) {
            note = note.trim();
            if (note.isBlank()) {
                note = null;
            }
        }
    }

    public PickupLocation copy() {
        return new PickupLocation(address == null ? null : address.copy(), note);
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}
