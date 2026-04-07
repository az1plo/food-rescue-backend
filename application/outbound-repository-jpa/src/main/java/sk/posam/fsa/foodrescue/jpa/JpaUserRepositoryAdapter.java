package sk.posam.fsa.foodrescue.jpa;

import org.springframework.stereotype.Repository;
import sk.posam.fsa.foodrescue.domain.repositories.UserRepository;
import sk.posam.fsa.foodrescue.domain.models.entities.User;

import java.util.Optional;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final UserSpringDataRepository userSpringDataRepository;

    public JpaUserRepositoryAdapter(UserSpringDataRepository userSpringDataRepository) {
        this.userSpringDataRepository = userSpringDataRepository;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userSpringDataRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userSpringDataRepository.findById(id);
    }

    @Override
    public void create(User user) {
        userSpringDataRepository.save(user);
    }
}
