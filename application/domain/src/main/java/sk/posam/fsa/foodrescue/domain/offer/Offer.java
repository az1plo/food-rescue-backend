package sk.posam.fsa.foodrescue.domain.offer;

import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.shared.Address;
import sk.posam.fsa.foodrescue.domain.shared.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public class Offer {

    private Long id;
    private Long businessId;
    private String title;
    private String description;
    private String imageUrl;
    private OfferCategory category;
    private boolean illustrativeImage;
    private List<AllergenCode> containsAllergens = new ArrayList<>();
    private List<AllergenCode> mayContainAllergens = new ArrayList<>();
    private String otherAllergenNote;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer quantityAvailable;
    private OfferStatus status;
    private List<OfferItem> items = new ArrayList<>();
    private PickupLocation pickupLocation;
    private PickupTimeWindow pickupTimeWindow;
    private boolean autoRepeatEnabled;
    private Integer autoRepeatQuantity;
    private LocalTime autoRepeatPickupStartTime;
    private LocalTime autoRepeatPickupEndTime;
    private String recurrenceKey;
    private LocalDateTime createdAt;

    public Offer() {
    }

    public static Offer fromDraft(Long businessId,
                                  String title,
                                  String description,
                                  String imageUrl,
                                  OfferCategory category,
                                  boolean illustrativeImage,
                                  List<AllergenCode> containsAllergens,
                                  List<AllergenCode> mayContainAllergens,
                                  String otherAllergenNote,
                                  BigDecimal price,
                                  BigDecimal originalPrice,
                                  Integer quantityAvailable,
                                  List<OfferItem> items,
                                  PickupLocation pickupLocation,
                                  PickupTimeWindow pickupTimeWindow) {
        return fromDraft(
                businessId,
                title,
                description,
                imageUrl,
                category,
                illustrativeImage,
                containsAllergens,
                mayContainAllergens,
                otherAllergenNote,
                price,
                originalPrice,
                quantityAvailable,
                items,
                pickupLocation,
                pickupTimeWindow,
                false,
                null,
                null,
                null,
                null
        );
    }

    public static Offer fromDraft(Long businessId,
                                  String title,
                                  String description,
                                  String imageUrl,
                                  OfferCategory category,
                                  boolean illustrativeImage,
                                  List<AllergenCode> containsAllergens,
                                  List<AllergenCode> mayContainAllergens,
                                  String otherAllergenNote,
                                  BigDecimal price,
                                  BigDecimal originalPrice,
                                  Integer quantityAvailable,
                                  List<OfferItem> items,
                                  PickupLocation pickupLocation,
                                  PickupTimeWindow pickupTimeWindow,
                                  boolean autoRepeatEnabled,
                                  String recurrenceKey,
                                  Integer autoRepeatQuantity,
                                  LocalTime autoRepeatPickupStartTime,
                                  LocalTime autoRepeatPickupEndTime) {
        Offer offer = new Offer();
        offer.businessId = businessId;
        offer.title = title;
        offer.description = description;
        offer.imageUrl = imageUrl;
        offer.category = category;
        offer.illustrativeImage = illustrativeImage;
        offer.containsAllergens = copyAllergens(containsAllergens);
        offer.mayContainAllergens = copyAllergens(mayContainAllergens);
        offer.otherAllergenNote = otherAllergenNote;
        offer.price = price;
        offer.originalPrice = originalPrice;
        offer.quantityAvailable = quantityAvailable;
        offer.items = copyItems(items);
        offer.pickupLocation = copyPickupLocation(pickupLocation);
        offer.pickupTimeWindow = copyPickupTimeWindow(pickupTimeWindow);
        offer.autoRepeatEnabled = autoRepeatEnabled;
        offer.recurrenceKey = normalizeRecurrenceKey(recurrenceKey);
        offer.autoRepeatQuantity = autoRepeatQuantity;
        offer.autoRepeatPickupStartTime = autoRepeatPickupStartTime;
        offer.autoRepeatPickupEndTime = autoRepeatPickupEndTime;
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

    public OfferCategory getCategory() {
        return category;
    }

    public boolean isIllustrativeImage() {
        return illustrativeImage;
    }

    public List<AllergenCode> getContainsAllergens() {
        return containsAllergens == null ? List.of() : List.copyOf(containsAllergens);
    }

    public List<AllergenCode> getMayContainAllergens() {
        return mayContainAllergens == null ? List.of() : List.copyOf(mayContainAllergens);
    }

    public String getOtherAllergenNote() {
        return otherAllergenNote;
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

    public boolean isAutoRepeatEnabled() {
        return autoRepeatEnabled;
    }

    public Integer getAutoRepeatQuantity() {
        return autoRepeatQuantity;
    }

    public LocalTime getAutoRepeatPickupStartTime() {
        return autoRepeatPickupStartTime;
    }

    public LocalTime getAutoRepeatPickupEndTime() {
        return autoRepeatPickupEndTime;
    }

    public String getRecurrenceKey() {
        return recurrenceKey;
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
        require(isManagedState(), "Only editable offers can be updated");

        applyDetails(
                newData.title,
                newData.description,
                newData.imageUrl,
                newData.category,
                newData.illustrativeImage,
                newData.containsAllergens,
                newData.mayContainAllergens,
                newData.otherAllergenNote,
                newData.price,
                newData.originalPrice,
                newData.quantityAvailable,
                newData.items,
                newData.pickupLocation,
                newData.pickupTimeWindow,
                newData.autoRepeatEnabled,
                newData.autoRepeatQuantity,
                newData.autoRepeatPickupStartTime,
                newData.autoRepeatPickupEndTime
        );

        if (autoRepeatEnabled && recurrenceKey == null) {
            recurrenceKey = UUID.randomUUID().toString();
        }
    }

    public String normalizedTitle() {
        return normalizeTitle(title);
    }

    public void prepareForCreation() {
        require(businessId != null, "Business is required");
        applyDetails(
                title,
                description,
                imageUrl,
                category,
                illustrativeImage,
                containsAllergens,
                mayContainAllergens,
                otherAllergenNote,
                price,
                originalPrice,
                quantityAvailable,
                items,
                pickupLocation,
                pickupTimeWindow,
                autoRepeatEnabled,
                autoRepeatQuantity,
                autoRepeatPickupStartTime,
                autoRepeatPickupEndTime
        );

        if (status == null || status == OfferStatus.DRAFT) {
            status = OfferStatus.AVAILABLE;
        }

        if (recurrenceKey == null && autoRepeatEnabled) {
            recurrenceKey = UUID.randomUUID().toString();
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void publish(Business business) {
        require(belongsTo(business), "Offer must belong to the selected business");
        require(business.canPublishOffers(), "Only active businesses can publish offers");
        require(isManagedState(), "Only editable offers can be published");

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

    public Offer createRepeatedOccurrenceFor(LocalDate occurrenceDate) {
        require(occurrenceDate != null, "Recurring offer date is required");
        require(recurrenceKey != null, "Recurring offer key is required");

        LocalTime pickupStartTime = autoRepeatPickupStartTime != null
                ? autoRepeatPickupStartTime
                : pickupTimeWindow.getFrom().toLocalTime();
        LocalTime pickupEndTime = autoRepeatPickupEndTime != null
                ? autoRepeatPickupEndTime
                : pickupTimeWindow.getTo().toLocalTime();
        LocalDateTime nextPickupFrom = LocalDateTime.of(occurrenceDate, pickupStartTime);
        LocalDateTime nextPickupTo = LocalDateTime.of(occurrenceDate, pickupEndTime);

        if (!nextPickupTo.isAfter(nextPickupFrom)) {
            nextPickupTo = nextPickupTo.plusDays(1);
        }

        Offer repeatedOffer = Offer.fromDraft(
                businessId,
                title,
                description,
                imageUrl,
                category,
                illustrativeImage,
                containsAllergens,
                mayContainAllergens,
                otherAllergenNote,
                price,
                originalPrice,
                autoRepeatQuantity != null ? autoRepeatQuantity : quantityAvailable,
                items,
                pickupLocation,
                PickupTimeWindow.of(nextPickupFrom, nextPickupTo),
                autoRepeatEnabled,
                recurrenceKey,
                autoRepeatQuantity,
                pickupStartTime,
                pickupEndTime
        );
        repeatedOffer.prepareForCreation();
        return repeatedOffer;
    }

    private void applyDetails(String title,
                              String description,
                              String imageUrl,
                              OfferCategory category,
                              boolean illustrativeImage,
                              List<AllergenCode> containsAllergens,
                              List<AllergenCode> mayContainAllergens,
                              String otherAllergenNote,
                              BigDecimal price,
                              BigDecimal originalPrice,
                              Integer quantityAvailable,
                              List<OfferItem> items,
                              PickupLocation pickupLocation,
                              PickupTimeWindow pickupTimeWindow,
                              boolean autoRepeatEnabled,
                              Integer autoRepeatQuantity,
                              LocalTime autoRepeatPickupStartTime,
                              LocalTime autoRepeatPickupEndTime) {
        this.title = normalizeTitle(title);
        this.description = normalizeDescription(description);
        this.imageUrl = normalizeImageUrl(imageUrl);
        this.category = normalizeCategory(category);
        this.illustrativeImage = illustrativeImage;
        this.containsAllergens = normalizeAllergens(containsAllergens, "containsAllergens");
        this.mayContainAllergens = normalizeAllergens(mayContainAllergens, "mayContainAllergens");
        validateAllergenCollections(this.containsAllergens, this.mayContainAllergens);
        this.otherAllergenNote = normalizeOtherAllergenNote(otherAllergenNote);
        this.price = normalizePrice(price);
        this.originalPrice = normalizeOriginalPrice(originalPrice, this.price);
        this.quantityAvailable = normalizeQuantity(quantityAvailable);
        this.items = normalizeItems(items);
        this.pickupLocation = normalizePickupLocation(pickupLocation);
        this.pickupTimeWindow = normalizePickupTimeWindow(pickupTimeWindow);
        this.autoRepeatEnabled = autoRepeatEnabled;
        this.autoRepeatQuantity = normalizeAutoRepeatQuantity(autoRepeatQuantity, this.quantityAvailable);
        this.autoRepeatPickupStartTime = normalizeAutoRepeatTime(autoRepeatPickupStartTime, this.pickupTimeWindow.getFrom().toLocalTime());
        this.autoRepeatPickupEndTime = normalizeAutoRepeatTime(autoRepeatPickupEndTime, this.pickupTimeWindow.getTo().toLocalTime());
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

    private OfferCategory normalizeCategory(OfferCategory category) {
        return category == null ? OfferCategory.OTHER : category;
    }

    private List<AllergenCode> normalizeAllergens(List<AllergenCode> allergens, String fieldName) {
        if (allergens == null || allergens.isEmpty()) {
            return new ArrayList<>();
        }

        LinkedHashSet<AllergenCode> normalizedAllergens = new LinkedHashSet<>();
        for (AllergenCode allergen : allergens) {
            require(allergen != null, fieldName + " contains an invalid allergen");
            normalizedAllergens.add(allergen);
        }

        if (normalizedAllergens.contains(AllergenCode.UNKNOWN) && normalizedAllergens.size() > 1) {
            throw new ValidationException(fieldName + " cannot combine UNKNOWN with other allergens");
        }

        return new ArrayList<>(normalizedAllergens);
    }

    private void validateAllergenCollections(List<AllergenCode> containsAllergens,
                                             List<AllergenCode> mayContainAllergens) {
        for (AllergenCode allergen : containsAllergens) {
            if (mayContainAllergens.contains(allergen)) {
                throw new ValidationException("The same allergen cannot be both confirmed and marked as may contain");
            }
        }
    }

    private String normalizeOtherAllergenNote(String otherAllergenNote) {
        if (otherAllergenNote == null) {
            return null;
        }

        String normalizedOtherAllergenNote = otherAllergenNote.trim();
        if (normalizedOtherAllergenNote.isBlank()) {
            return null;
        }

        require(normalizedOtherAllergenNote.length() <= 500, "Other allergen note is too long");
        return normalizedOtherAllergenNote;
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

    private Integer normalizeAutoRepeatQuantity(Integer autoRepeatQuantity, Integer fallbackQuantity) {
        Integer normalizedQuantity = autoRepeatQuantity == null ? fallbackQuantity : autoRepeatQuantity;
        require(normalizedQuantity != null && normalizedQuantity > 0, "Auto-repeat quantity must be greater than zero");
        return normalizedQuantity;
    }

    private LocalTime normalizeAutoRepeatTime(LocalTime autoRepeatTime, LocalTime fallbackTime) {
        LocalTime normalizedTime = autoRepeatTime == null ? fallbackTime : autoRepeatTime;
        require(normalizedTime != null, "Auto-repeat pickup time is required");
        return normalizedTime;
    }

    private static List<OfferItem> copyItems(List<OfferItem> items) {
        if (items == null) {
            return new ArrayList<>();
        }

        return items.stream()
                .map(item -> item == null ? null : item.copy())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static List<AllergenCode> copyAllergens(List<AllergenCode> allergens) {
        if (allergens == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(allergens);
    }

    private static PickupLocation copyPickupLocation(PickupLocation pickupLocation) {
        return pickupLocation == null ? null : pickupLocation.copy();
    }

    private static PickupTimeWindow copyPickupTimeWindow(PickupTimeWindow pickupTimeWindow) {
        return pickupTimeWindow == null ? null : pickupTimeWindow.copy();
    }

    private static String normalizeRecurrenceKey(String recurrenceKey) {
        if (recurrenceKey == null) {
            return null;
        }

        String normalizedRecurrenceKey = recurrenceKey.trim();
        return normalizedRecurrenceKey.isBlank() ? null : normalizedRecurrenceKey;
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}
