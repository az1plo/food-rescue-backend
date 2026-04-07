package sk.posam.fsa.foodrescue.domain.repositories;

import sk.posam.fsa.foodrescue.domain.models.entities.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    void create(User user);
}
