package sk.posam.fsa.foodrescue.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.posam.fsa.foodrescue.domain.models.entities.Business;

import java.util.Optional;

interface BusinessSpringDataRepository extends JpaRepository<Business, Long> {
    Boolean existsByOwnerIdAndName(Long ownerId, String name);

    Optional<Business> findByOwnerIdAndName(Long ownerId, String name);
}
