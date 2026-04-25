package sk.posam.fsa.foodrescue.domain.services;

import sk.posam.fsa.foodrescue.domain.models.entities.Business;
import sk.posam.fsa.foodrescue.domain.models.entities.User;

import java.util.List;

public interface BusinessFacade {

    Business create(User currentUser, Business business);

    List<Business> getBusinesses(User currentUser);

    List<Business> getPendingBusinesses(User currentUser);

    Business get(User currentUser, Long id);

    Business approve(User currentUser, Long id);

    Business update(User currentUser, Long id, Business business);

    void delete(User currentUser, Long id);
}
