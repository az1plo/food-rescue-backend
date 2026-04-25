package sk.posam.fsa.foodrescue.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.posam.fsa.foodrescue.domain.models.entities.Offer;
import sk.posam.fsa.foodrescue.domain.models.enums.OfferStatus;

import java.util.List;

interface OfferSpringDataRepository extends JpaRepository<Offer, Long> {

    List<Offer> findAllByBusinessIdOrderByCreatedAtDesc(Long businessId);

    List<Offer> findAllByStatusOrderByCreatedAtDesc(OfferStatus status);
}
