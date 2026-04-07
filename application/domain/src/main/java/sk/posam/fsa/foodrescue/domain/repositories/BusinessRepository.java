package sk.posam.fsa.foodrescue.domain.repositories;

import sk.posam.fsa.foodrescue.domain.models.entities.Business;

import java.util.Optional;

public interface BusinessRepository {

    Business save(Business business);

    Optional<Business> findById(Long id);

    Optional<Business> findByOwnerIdAndName(Long ownerId, String name);

    boolean existsByOwnerIdAndName(Long ownerId, String name);

    void delete(Business business);
}
