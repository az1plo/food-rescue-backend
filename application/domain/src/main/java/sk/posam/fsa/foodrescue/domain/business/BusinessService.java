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

    private final BusinessRepository businessRepository;
    private final AddressCoordinatesProvider addressCoordinatesProvider;

    public BusinessService(BusinessRepository businessRepository,
                           AddressCoordinatesProvider addressCoordinatesProvider) {
        this.businessRepository = businessRepository;
        this.addressCoordinatesProvider = addressCoordinatesProvider;
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
}


