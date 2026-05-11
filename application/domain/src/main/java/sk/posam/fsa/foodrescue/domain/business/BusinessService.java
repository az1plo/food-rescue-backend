package sk.posam.fsa.foodrescue.domain.business;

import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.shared.ValidationException;
import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.business.BusinessStatus;
import sk.posam.fsa.foodrescue.domain.shared.AddressCoordinatesProvider;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;

import java.util.List;

public class BusinessService implements BusinessFacade {

    private static final int MAX_ICON_BYTES = 4 * 1024 * 1024;

    private final BusinessRepository businessRepository;
    private final AddressCoordinatesProvider addressCoordinatesProvider;
    private final BusinessIconStorage businessIconStorage;

    public BusinessService(BusinessRepository businessRepository,
                           AddressCoordinatesProvider addressCoordinatesProvider,
                           BusinessIconStorage businessIconStorage) {
        this.businessRepository = businessRepository;
        this.addressCoordinatesProvider = addressCoordinatesProvider;
        this.businessIconStorage = businessIconStorage;
    }

    @Override
    public Business create(User currentUser, Business business) {
        if (business == null) {
            throw new ValidationException("Business must not be null");
        }

        if (!currentUser.isActive()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "Only active users can create a business"
            );
        }

        business.assignOwner(currentUser.getId());
        populateBusinessCoordinates(business);
        business.prepareForCreation();

        if (businessRepository.existsByOwnerIdAndName(currentUser.getId(), business.getName())) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Business with the same name already exists for this owner"
            );
        }

        return businessRepository.save(business);
    }

    @Override
    public List<Business> getBusinesses(User currentUser) {
        if (currentUser.isAdmin()) {
            return businessRepository.findAll();
        }
        return businessRepository.findAllByOwnerId(currentUser.getId());
    }

    @Override
    public List<Business> getPendingBusinesses(User currentUser) {
        ensureAdmin(currentUser, "Only admins can review pending businesses");
        return businessRepository.findAllByStatus(BusinessStatus.PENDING);
    }

    @Override
    public Business get(User currentUser, Long id) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Business with id=" + id + " was not found"
                ));

        if (!business.canBeManagedBy(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You do not have access to business with id=" + id
            );
        }

        return business;
    }

    @Override
    public Business approve(User currentUser, Long id) {
        ensureAdmin(currentUser, "Only admins can approve businesses");

        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Business with id=" + id + " was not found"
                ));

        business.approve();
        return businessRepository.save(business);
    }

    @Override
    public Business update(User currentUser, Long id, Business businessData) {
        if (businessData == null) {
            throw new ValidationException("Business update data must not be null");
        }

        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Business with id=" + id + " was not found"
                ));

        if (!currentUser.isActive()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "Only active users can update a business"
            );
        }

        if (!business.belongsTo(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You are not allowed to update this business"
            );
        }

        String requestedName = businessData.normalizedName();

        if (businessRepository.existsByOwnerIdAndName(currentUser.getId(), requestedName)
                && !business.getName().equals(requestedName)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Business with the same name already exists for this owner"
            );
        }

        populateBusinessCoordinates(businessData);
        business.update(businessData);

        return businessRepository.save(business);
    }

    @Override
    public Business uploadIcon(User currentUser, Long businessId, BusinessIconUpload upload) {
        ensureActiveUser(currentUser, "Only active users can upload a business icon");
        if (businessId == null) {
            throw new ValidationException("Business is required");
        }

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Business with id=" + businessId + " was not found"
                ));

        if (!business.canBeManagedBy(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You are not allowed to update this business icon"
            );
        }

        BusinessIconUpload normalizedUpload = normalizeIconUpload(upload);
        String previousIconId = extractManagedIconId(business.getIconUrl());
        StoredBusinessIcon storedIcon = businessIconStorage.store(normalizedUpload);

        business.setIconUrl(storedIcon.imageUrl());
        Business savedBusiness = businessRepository.save(business);

        if (previousIconId != null && !previousIconId.equals(storedIcon.imageId())) {
            businessIconStorage.delete(previousIconId);
        }

        return savedBusiness;
    }

    @Override
    public StoredBusinessIconContent getIcon(String iconId) {
        return businessIconStorage.read(iconId);
    }

    @Override
    public void delete(User currentUser, Long id) {

        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Business with id=" + id + " was not found"
                ));

        if (!currentUser.isActive()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "Only active users can delete a business"
            );
        }

        if (!business.belongsTo(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You are not allowed to delete this business"
            );
        }

        businessRepository.delete(business);
    }

    private void ensureActiveUser(User currentUser, String message) {
        if (currentUser == null || !currentUser.isActive()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    message
            );
        }
    }

    private void ensureAdmin(User currentUser, String message) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    message
            );
        }
    }

    private void populateBusinessCoordinates(Business business) {
        if (business == null) {
            return;
        }

        var resolvedAddress = addressCoordinatesProvider.populateCoordinates(business.getAddress());
        if (resolvedAddress == null) {
            return;
        }

        business.setAddressCoordinates(resolvedAddress.getLatitude(), resolvedAddress.getLongitude());
    }

    private BusinessIconUpload normalizeIconUpload(BusinessIconUpload upload) {
        if (upload == null) {
            throw new ValidationException("Business icon upload is required");
        }

        String fileName = normalizeText(upload.fileName());
        String contentType = normalizeText(upload.contentType());
        byte[] bytes = upload.copyBytes();

        if (fileName == null) {
            throw new ValidationException("Business icon file name is required");
        }
        if (contentType == null) {
            throw new ValidationException("Business icon content type is required");
        }
        if (bytes.length == 0) {
            throw new ValidationException("Business icon content must not be empty");
        }
        if (bytes.length > MAX_ICON_BYTES) {
            throw new ValidationException("Business icon is too large");
        }
        if (!isSupportedIconType(contentType)) {
            throw new ValidationException("Business icon content type is not supported");
        }

        return new BusinessIconUpload(fileName, contentType, bytes);
    }

    private String extractManagedIconId(String iconUrl) {
        String normalizedUrl = normalizeText(iconUrl);
        String prefix = "/business-icons/";

        if (normalizedUrl == null || !normalizedUrl.startsWith(prefix)) {
            return null;
        }

        return normalizeText(normalizedUrl.substring(prefix.length()));
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isBlank() ? null : normalizedValue;
    }

    private boolean isSupportedIconType(String contentType) {
        return "image/jpeg".equalsIgnoreCase(contentType)
                || "image/png".equalsIgnoreCase(contentType)
                || "image/webp".equalsIgnoreCase(contentType);
    }
}


