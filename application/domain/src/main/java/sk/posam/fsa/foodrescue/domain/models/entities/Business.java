package sk.posam.fsa.foodrescue.domain.models.entities;

import sk.posam.fsa.foodrescue.domain.exceptions.ValidationException;
import sk.posam.fsa.foodrescue.domain.models.enums.BusinessStatus;
import sk.posam.fsa.foodrescue.domain.models.valueobjects.Address;

import java.time.LocalDateTime;

public class Business {

    private Long id;
    private Long ownerId;
    private String name;
    private String description;
    private BusinessStatus status;
    private Address address;
    private LocalDateTime createdAt;

    public Business() {
    }

    public static Business fromProfile(String name, String description, Address address) {
        Business business = new Business();
        business.name = name;
        business.description = description;
        business.address = copyAddress(address);
        return business;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BusinessStatus getStatus() {
        return status;
    }

    public Address getAddress() {
        return copyAddress(address);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void assignOwner(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setStatus(BusinessStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPending() {
        return status == BusinessStatus.PENDING;
    }

    public boolean isActive() {
        return status == BusinessStatus.ACTIVE;
    }

    public boolean isBlocked() {
        return status == BusinessStatus.BLOCKED;
    }

    public boolean isRejected() {
        return status == BusinessStatus.REJECTED;
    }

    public boolean belongsTo(User user) {
        return user != null
                && ownerId != null
                && user.getId() != null
                && ownerId.equals(user.getId());
    }

    public boolean canBeManagedBy(User user) {
        return user != null && (belongsTo(user) || user.isAdmin());
    }

    public boolean canPublishOffers() {
        return status == BusinessStatus.ACTIVE;
    }

    public void approve() {
        require(isPending(), "Only pending businesses can be approved");
        status = BusinessStatus.ACTIVE;
    }

    public void reject() {
        status = BusinessStatus.REJECTED;
    }

    public void block() {
        status = BusinessStatus.BLOCKED;
    }

    public void reopenForReview() {
        status = BusinessStatus.PENDING;
    }

    public String normalizedName() {
        return normalizeName(name);
    }

    public void update(Business newData) {
        require(newData != null, "Business update data is required");
        applyProfile(newData.name, newData.description, newData.address);
    }

    public void prepareForCreation() {
        require(ownerId != null, "Owner is required");
        applyProfile(name, description, address);
        status = BusinessStatus.PENDING;

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private void applyProfile(String name, String description, Address address) {
        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
        this.address = normalizeAddress(address);
    }

    private String normalizeName(String name) {
        require(name != null && !name.isBlank(), "Business name is required");
        return name.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalizedDescription = description.trim();
        return normalizedDescription.isBlank() ? null : normalizedDescription;
    }

    private Address normalizeAddress(Address address) {
        require(address != null, "Address is required");

        Address normalizedAddress = copyAddress(address);
        normalizedAddress.prepareForCreation();
        return normalizedAddress;
    }

    private static Address copyAddress(Address address) {
        return address == null ? null : address.copy();
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}
