package sk.posam.fsa.foodrescue.domain.models.entities;

import sk.posam.fsa.foodrescue.domain.exceptions.ValidationException;
import sk.posam.fsa.foodrescue.domain.models.enums.BusinessStatus;

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
        return address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(BusinessStatus status) {
        this.status = status;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPending() {
        return status == BusinessStatus.PENDING;
    }

    public void prepareForCreation() {
        require(name != null && !name.isBlank(), "Business name is required");
        require(ownerId != null, "Owner is required");
        require(address != null, "Address is required");

        name = name.trim();

        if (description != null && description.isBlank()) {
            description = null;
        }

        status = BusinessStatus.PENDING;

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}