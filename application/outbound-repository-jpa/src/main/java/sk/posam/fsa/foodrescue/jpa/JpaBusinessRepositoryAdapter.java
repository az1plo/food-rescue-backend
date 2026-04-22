package sk.posam.fsa.foodrescue.jpa;

import org.springframework.stereotype.Repository;
import sk.posam.fsa.foodrescue.domain.models.entities.Business;
import sk.posam.fsa.foodrescue.domain.repositories.BusinessRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaBusinessRepositoryAdapter implements BusinessRepository {

    private final BusinessSpringDataRepository businessSpringDataRepository;

    public JpaBusinessRepositoryAdapter(BusinessSpringDataRepository businessSpringDataRepository) {
        this.businessSpringDataRepository = businessSpringDataRepository;
    }

    @Override
    public Business save(Business business) {
        return businessSpringDataRepository.save(business);
    }

    @Override
    public List<Business> findAllByOwnerId(Long ownerId) {
        return businessSpringDataRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Override
    public Optional<Business> findById(Long id) {
        return businessSpringDataRepository.findById(id);
    }

    @Override
    public Optional<Business> findByOwnerIdAndName(Long ownerId, String name) {
        return businessSpringDataRepository.findByOwnerIdAndName(ownerId, name);
    }

    @Override
    public boolean existsByOwnerIdAndName(Long ownerId, String name) {
        return businessSpringDataRepository.existsByOwnerIdAndName(ownerId, name);
    }

    @Override
    public void delete(Business business) {
        businessSpringDataRepository.delete(business);
    }
}
