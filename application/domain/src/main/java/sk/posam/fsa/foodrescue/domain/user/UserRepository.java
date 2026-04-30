package sk.posam.fsa.foodrescue.domain.user;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    void create(User user);
}


