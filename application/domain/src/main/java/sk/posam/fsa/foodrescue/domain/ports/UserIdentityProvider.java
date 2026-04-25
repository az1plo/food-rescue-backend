package sk.posam.fsa.foodrescue.domain.ports;

import sk.posam.fsa.foodrescue.domain.models.entities.User;

public interface UserIdentityProvider {

    void create(User user);

    void deleteByEmail(String email);
}
