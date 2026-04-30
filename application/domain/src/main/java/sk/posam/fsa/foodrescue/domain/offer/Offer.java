package sk.posam.fsa.foodrescue.domain.offer;

import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.shared.ValidationException;
import sk.posam.fsa.foodrescue.domain.shared.Address;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Offer {

    private Long id;
    private Long businessId;
    private String title;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer quantityAvailable;
    private OfferStatus status;
    private List<OfferItem> items = new ArrayList<>();
    private PickupLocation pickupLocation;
    private PickupTimeWindow pickupTimeWindow;
    private LocalDateTime createdAt;

    public Offer() {
    }

    public static Offer fromDraft(Long businessId,
                                  String title,
                                  String description,
                                  String imageUrl,
                                  BigDecimal price,
                                  BigDecimal originalPrice,
                                  Integer quantityAvailable,
                                  List<OfferItem> items,
                                  PickupLocation pickupLocation,
                                  PickupTimeWindow pickupTimeWindow) {
        Offer offer = new Offer();
        offer.businessId = businessId;
        offer.title = title;
        offer.description = description;
        offer.imageUrl = imageUrl;
        offer.price = price;
        offer.originalPrice = originalPrice;
        offer.quantityAvailable = quantityAvailable;
        offer.items = copyItems(items);
        offer.pickupLocation = copyPickupLocation(pickupLocation);
        offer.pickupTimeWindow = copyPickupTimeWindow(pickupTimeWindow);
        return offer;
    }

    public Long getId() {
        return id;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public Integer getQuantityAvailable() {
        return quantityAvailable;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public List<OfferItem> getItems() {
        return items == null ? List.of() : items.stream()
                .map(OfferItem::copy)
                .toList();
    }

    public PickupLocation getPickupLocation() {
        return copyPickupLocation(pickupLocation);
    }

    public PickupTimeWindow getPickupTimeWindow() {
        return copyPickupTimeWindow(pickupTimeWindow);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void assignBusiness(Long businessId) {
        this.businessId = businessId;
    }

    public void setStatus(OfferStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setPickupLocationCoordinates(Double latitude, Double longitude) {
        if (pickupLocation == null || pickupLocation.getAddress() == null) {
            return;
        }

        Address address = pickupLocation.getAddress();
        address.setLatitude(latitude);
        address.setLongitude(longitude);
        pickupLocation = PickupLocation.of(address, pickupLocation.getNote());
    }

    public boolean isDraft() {
        return status == OfferStatus.DRAFT;
    }

    public boolean isAvailable() {
        return status == OfferStatus.AVAILABLE;
    }

    public boolean isManagedState() {
        return status == OfferStatus.DRAFT || status == OfferStatus.AVAILABLE;
    }

    public boolean belongsTo(Business business) {
        return business != null
                && businessId != null
                && business.getId() != null
                && businessId.equals(business.getId());
    }

    public void update(Offer newData) {
        require(newData != null, "Offer update data is required");
        require(isManagedState(), "Only draft or available offers can be updated");

        applyDetails(
                newData.title,
                newData.description,
                newData.imageUrl,
                newData.price,
                newData.originalPrice,
                newData.quantityAvailable,
                newData.items,
                newData.pickupLocation,
                newData.pickupTimeWindow
        );
    }

    public String normalizedTitle() {
        return normalizeTitle(title);
    }

    public void prepareForCreation() {
        require(businessId != null, "Business is required");
        applyDetails(title, description, imageUrl, price, originalPrice, quantityAvailable, items, pickupLocation, pickupTimeWindow);

        if (status == null) {
            status = OfferStatus.DRAFT;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void publish(Business business) {
        require(belongsTo(business), "Offer must belong to the selected business");
        require(business.canPublishOffers(), "Only active businesses can publish offers");
        require(status == OfferStatus.DRAFT, "Only draft offers can be published");

        status = OfferStatus.AVAILABLE;
    }

    public void markReserved() {
        require(status == OfferStatus.AVAILABLE, "Only available offers can be reserved");
        status = OfferStatus.RESERVED;
    }

    public void reserveQuantity(int quantity) {
        require(status == OfferStatus.AVAILABLE, "Only available offers can be reserved");
        require(quantity > 0, "Reserved quantity must be greater than zero");
        require(quantityAvailable >= quantity, "Reserved quantity exceeds current availability");

        quantityAvailable -= quantity;
        status = quantityAvailable == 0 ? OfferStatus.SOLD_OUT : OfferStatus.AVAILABLE;
    }

    public void reopenAfterReservationCancellation() {
        require(status == OfferStatus.RESERVED, "Only reserved offers can be reopened");
        status = OfferStatus.AVAILABLE;
    }

    public void restoreQuantity(int quantity) {
        require(quantity > 0, "Restored quantity must be greater than zero");
        require(status == OfferStatus.AVAILABLE || status == OfferStatus.SOLD_OUT, "Only active catalog offers can be restored");

        quantityAvailable += quantity;
        status = OfferStatus.AVAILABLE;
    }

    public void markPickedUp() {
        require(status == OfferStatus.RESERVED, "Only reserved offers can be marked as picked up");
        status = OfferStatus.PICKED_UP;
    }

    public void markSoldOut() {
        require(status == OfferStatus.AVAILABLE, "Only available offers can be marked as sold out");
        status = OfferStatus.SOLD_OUT;
    }

    public void expire() {
        require(status == OfferStatus.AVAILABLE || status == OfferStatus.RESERVED,
                "Only active offers can expire");
        status = OfferStatus.EXPIRED;
    }

    public void cancel() {
        require(status != OfferStatus.PICKED_UP, "Picked up offers cannot be cancelled");
        require(status != OfferStatus.EXPIRED, "Expired offers cannot be cancelled");
        require(status != OfferStatus.SOLD_OUT, "Sold out offers cannot be cancelled");

        status = OfferStatus.CANCELLED;
    }

    private void applyDetails(String title,
                              String description,
                              String imageUrl,
                              BigDecimal price,
                              BigDecimal originalPrice,
                              Integer quantityAvailable,
                              List<OfferItem> items,
                              PickupLocation pickupLocation,
                              PickupTimeWindow pickupTimeWindow) {
        this.title = normalizeTitle(title);
        this.description = normalizeDescription(description);
        this.imageUrl = normalizeImageUrl(imageUrl);
        this.price = normalizePrice(price);
        this.originalPrice = normalizeOriginalPrice(originalPrice, this.price);
        this.quantityAvailable = normalizeQuantity(quantityAvailable);
        this.items = normalizeItems(items);
        this.pickupLocation = normalizePickupLocation(pickupLocation);
        this.pickupTimeWindow = normalizePickupTimeWindow(pickupTimeWindow);
    }

    private String normalizeTitle(String title) {
        require(title != null && !title.isBlank(), "Offer title is required");
        return title.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalizedDescription = description.trim();
        return normalizedDescription.isBlank() ? null : normalizedDescription;
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        String normalizedImageUrl = imageUrl.trim();
        if (normalizedImageUrl.isBlank()) {
            return null;
        }

        require(normalizedImageUrl.length() <= 1024, "Offer image URL is too long");
        return normalizedImageUrl;
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        require(price != null && price.compareTo(BigDecimal.ZERO) >= 0, "Offer price must not be negative");
        return price;
    }

    private BigDecimal normalizeOriginalPrice(BigDecimal originalPrice, BigDecimal currentPrice) {
        if (originalPrice == null) {
            return null;
        }

        require(originalPrice.compareTo(BigDecimal.ZERO) >= 0, "Offer original price must not be negative");
        require(currentPrice == null || originalPrice.compareTo(currentPrice) >= 0,
                "Offer original price must be greater than or equal to the current price");
        return originalPrice;
    }

    private Integer normalizeQuantity(Integer quantityAvailable) {
        require(quantityAvailable != null && quantityAvailable > 0, "Offer quantity must be greater than zero");
        return quantityAvailable;
    }

    private List<OfferItem> normalizeItems(List<OfferItem> items) {
        require(items != null && !items.isEmpty(), "At least one offer item is required");

        List<OfferItem> normalizedItems = new ArrayList<>();
        for (OfferItem item : items) {
            require(item != null, "Offer item must not be null");
            OfferItem normalizedItem = item.copy();
            normalizedItem.prepareForCreation();
            normalizedItems.add(normalizedItem);
        }
        return normalizedItems;
    }

    private PickupLocation normalizePickupLocation(PickupLocation pickupLocation) {
        require(pickupLocation != null, "Pickup location is required");

        PickupLocation normalizedPickupLocation = pickupLocation.copy();
        normalizedPickupLocation.prepareForCreation();
        return normalizedPickupLocation;
    }

    private PickupTimeWindow normalizePickupTimeWindow(PickupTimeWindow pickupTimeWindow) {
        require(pickupTimeWindow != null, "Pickup time window is required");

        PickupTimeWindow normalizedPickupTimeWindow = pickupTimeWindow.copy();
        normalizedPickupTimeWindow.prepareForCreation();
        return normalizedPickupTimeWindow;
    }

    private static List<OfferItem> copyItems(List<OfferItem> items) {
        if (items == null) {
            return new ArrayList<>();
        }

        return items.stream()
                .map(item -> item == null ? null : item.copy())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static PickupLocation copyPickupLocation(PickupLocation pickupLocation) {
        return pickupLocation == null ? null : pickupLocation.copy();
    }

    private static PickupTimeWindow copyPickupTimeWindow(PickupTimeWindow pickupTimeWindow) {
        return pickupTimeWindow == null ? null : pickupTimeWindow.copy();
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}

