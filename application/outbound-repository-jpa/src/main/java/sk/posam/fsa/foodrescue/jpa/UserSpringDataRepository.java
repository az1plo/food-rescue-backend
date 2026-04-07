package sk.posam.fsa.foodrescue.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.posam.fsa.foodrescue.domain.models.entities.User;

import java.util.Optional;

interface UserSpringDataRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
