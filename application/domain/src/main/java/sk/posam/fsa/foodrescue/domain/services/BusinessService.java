package sk.posam.fsa.foodrescue.domain.services;

import sk.posam.fsa.foodrescue.domain.exceptions.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.models.entities.Business;
import sk.posam.fsa.foodrescue.domain.models.entities.User;
import sk.posam.fsa.foodrescue.domain.models.enums.UserRole;
import sk.posam.fsa.foodrescue.domain.repositories.BusinessRepository;

public class BusinessService implements BusinessFacade {

    private final BusinessRepository businessRepository;

    public BusinessService(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @Override
    public void create(User currentUser, Business business) {

        if (!currentUser.isActive()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "Only active users can create a business"
            );
        }

        if (businessRepository.existsByOwnerIdAndName(currentUser.getId(), business.getName())) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Business with the same name already exists for this owner"
            );
        }

        business.setOwnerId(currentUser.getId());
        business.prepareForCreation();

        businessRepository.save(business);
    }

    @Override
    public Business get(User currentUser, Long id) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Business with id=" + id + " was not found"
                ));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOwner = business.getOwnerId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You do not have access to business with id=" + id
            );
        }

        return business;
    }

    @Override
    public Business update(User currentUser, Long id, Business businessData) {

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

        if (!business.getOwnerId().equals(currentUser.getId())) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You are not allowed to update this business"
            );
        }

        if (businessRepository.existsByOwnerIdAndName(currentUser.getId(), businessData.getName())
                && !business.getName().equals(businessData.getName())) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "Business with the same name already exists for this owner"
            );
        }

        business.setName(businessData.getName());
        business.setDescription(businessData.getDescription());
        business.setAddress(businessData.getAddress());

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

        if (!business.getOwnerId().equals(currentUser.getId())) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You are not allowed to delete this business"
            );
        }

        businessRepository.delete(business);
    }
}