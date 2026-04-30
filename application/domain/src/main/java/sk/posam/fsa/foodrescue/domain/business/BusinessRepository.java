package sk.posam.fsa.foodrescue.domain.business;

import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.business.BusinessStatus;

import java.util.List;
import java.util.Optional;

public interface BusinessRepository {

    Business save(Business business);

    List<Business> findAll();

    List<Business> findAllByStatus(BusinessStatus status);

    List<Business> findAllByOwnerId(Long ownerId);

    Optional<Business> findById(Long id);

    Optional<Business> findByOwnerIdAndName(Long ownerId, String name);

    boolean existsByOwnerIdAndName(Long ownerId, String name);

    void delete(Business business);
}


