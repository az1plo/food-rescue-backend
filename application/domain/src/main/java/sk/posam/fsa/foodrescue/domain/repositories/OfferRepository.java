package sk.posam.fsa.foodrescue.domain.repositories;

import sk.posam.fsa.foodrescue.domain.models.entities.Offer;

import java.util.List;
import java.util.Optional;

public interface OfferRepository {

    Offer save(Offer offer);

    Optional<Offer> findById(Long id);

    List<Offer> findAllByBusinessId(Long businessId);

    List<Offer> findAllAvailable();

    void delete(Offer offer);
}
