package sk.posam.fsa.foodrescue.jpa;

import org.springframework.stereotype.Repository;
import sk.posam.fsa.foodrescue.domain.models.entities.Offer;
import sk.posam.fsa.foodrescue.domain.models.enums.OfferStatus;
import sk.posam.fsa.foodrescue.domain.repositories.OfferRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaOfferRepositoryAdapter implements OfferRepository {

    private final OfferSpringDataRepository offerSpringDataRepository;

    public JpaOfferRepositoryAdapter(OfferSpringDataRepository offerSpringDataRepository) {
        this.offerSpringDataRepository = offerSpringDataRepository;
    }

    @Override
    public Offer save(Offer offer) {
        return offerSpringDataRepository.save(offer);
    }

    @Override
    public Optional<Offer> findById(Long id) {
        return offerSpringDataRepository.findById(id);
    }

    @Override
    public List<Offer> findAllByBusinessId(Long businessId) {
        return offerSpringDataRepository.findAllByBusinessIdOrderByCreatedAtDesc(businessId);
    }

    @Override
    public List<Offer> findAllAvailable() {
        return offerSpringDataRepository.findAllByStatusOrderByCreatedAtDesc(OfferStatus.AVAILABLE);
    }

    @Override
    public void delete(Offer offer) {
        offerSpringDataRepository.delete(offer);
    }
}
