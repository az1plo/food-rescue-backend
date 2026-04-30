package sk.posam.fsa.foodrescue.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.business.BusinessStatus;

import java.util.List;
import java.util.Optional;

interface BusinessSpringDataRepository extends JpaRepository<Business, Long> {
    List<Business> findAllByStatusOrderByCreatedAtAsc(BusinessStatus status);

    List<Business> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Boolean existsByOwnerIdAndName(Long ownerId, String name);

    Optional<Business> findByOwnerIdAndName(Long ownerId, String name);
}

