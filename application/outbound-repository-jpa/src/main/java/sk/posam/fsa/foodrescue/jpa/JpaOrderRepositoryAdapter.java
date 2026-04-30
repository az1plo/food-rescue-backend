package sk.posam.fsa.foodrescue.jpa;

import org.springframework.stereotype.Repository;
import sk.posam.fsa.foodrescue.domain.order.Order;
import sk.posam.fsa.foodrescue.domain.order.OrderRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final OrderSpringDataRepository orderSpringDataRepository;

    public JpaOrderRepositoryAdapter(OrderSpringDataRepository orderSpringDataRepository) {
        this.orderSpringDataRepository = orderSpringDataRepository;
    }

    @Override
    public Order save(Order order) {
        return orderSpringDataRepository.save(order);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderSpringDataRepository.findById(id);
    }

    @Override
    public List<Order> findAllByUserId(Long userId) {
        return orderSpringDataRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Order> findAllByBusinessId(Long businessId) {
        return orderSpringDataRepository.findAllByBusinessIdOrderByCreatedAtDesc(businessId);
    }
}
