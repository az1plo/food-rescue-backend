package sk.posam.fsa.foodrescue.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.domain.offer.OfferStatus;

import java.util.List;

interface OfferSpringDataRepository extends JpaRepository<Offer, Long> {

    default List<Offer> findAllOrdered() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    List<Offer> findAllByBusinessIdOrderByCreatedAtDesc(Long businessId);

    List<Offer> findAllByStatusOrderByCreatedAtDesc(OfferStatus status);
}

