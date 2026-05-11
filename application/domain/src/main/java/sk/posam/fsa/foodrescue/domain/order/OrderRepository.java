package sk.posam.fsa.foodrescue.domain.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    List<Order> findAllByStatus(OrderStatus status);

    List<Order> findAllByUserId(Long userId);

    List<Order> findAllByBusinessId(Long businessId);
}
