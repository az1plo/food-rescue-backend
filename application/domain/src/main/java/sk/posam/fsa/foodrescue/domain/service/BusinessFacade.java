package sk.posam.fsa.foodrescue.domain.service;

import sk.posam.fsa.foodrescue.domain.models.entities.Business;
import sk.posam.fsa.foodrescue.domain.models.entities.User;

public interface BusinessFacade {

    void create(User currentUser, Business business);

    Business get(User currentUser, Long id);

    Business update(User currentUser, Long id, Business business);

    void delete(User currentUser, Long id);
}