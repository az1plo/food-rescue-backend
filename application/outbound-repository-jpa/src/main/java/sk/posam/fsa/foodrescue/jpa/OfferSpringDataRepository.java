package sk.posam.fsa.foodrescue.jpa;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.offer.OfferStatus;

import java.util.List;
import java.util.Optional;

interface OfferSpringDataRepository extends JpaRepository<Offer, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select offer from Offer offer where offer.id = :id")
    Optional<Offer> findByIdForUpdate(@Param("id") Long id);

    default List<Offer> findAllOrdered() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    List<Offer> findAllByBusinessIdOrderByCreatedAtDesc(Long businessId);

    List<Offer> findAllByStatusOrderByCreatedAtDesc(OfferStatus status);
}

