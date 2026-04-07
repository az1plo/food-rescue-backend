package sk.posam.fsa.foodrescue.domain.service;

import sk.posam.fsa.foodrescue.domain.models.entities.User;

public interface UserFacade {

    User get(Long id);

    User get(String email);

    void create(User user);
}
