package sk.posam.fsa.foodrescue.domain.business;

import sk.posam.fsa.foodrescue.domain.shared.ValidationException;
import sk.posam.fsa.foodrescue.domain.business.BusinessStatus;
import sk.posam.fsa.foodrescue.domain.shared.Address;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.time.LocalDateTime;

public class Business {

    private Long id;
    private Long ownerId;
    private String name;
    private String description;
    private String iconUrl;
    private BusinessStatus status;
    private Address address;
    private Double ratingAverage;
    private Integer ratingCount;
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

    public String getIconUrl() {
        return iconUrl;
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

    public Double getRatingAverage() {
        return ratingAverage;
    }

    public Integer getRatingCount() {
        return ratingCount == null ? 0 : ratingCount;
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

    public void setRatingAverage(Double ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = normalizeOptionalUrl(iconUrl);
    }

    public void setAddressCoordinates(Double latitude, Double longitude) {
        if (address == null) {
            return;
        }

        address.setLatitude(latitude);
        address.setLongitude(longitude);
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

    public void registerRating(Integer rating) {
        require(rating != null && rating >= 0 && rating <= 5, "Rating must be between 0 and 5");

        int currentCount = getRatingCount();
        double currentAverage = ratingAverage == null || currentCount <= 0 ? 0.0 : ratingAverage;
        double nextAverage = ((currentAverage * currentCount) + rating) / (currentCount + 1);

        ratingCount = currentCount + 1;
        ratingAverage = roundToOneDecimal(nextAverage);
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
        ratingAverage = null;
        ratingCount = 0;

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

    private String normalizeOptionalUrl(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isBlank() ? null : normalizedValue;
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

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}
