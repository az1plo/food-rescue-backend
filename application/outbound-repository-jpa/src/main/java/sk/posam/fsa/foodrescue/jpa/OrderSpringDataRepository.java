package sk.posam.fsa.foodrescue.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.posam.fsa.foodrescue.domain.order.Order;
import sk.posam.fsa.foodrescue.domain.order.OrderStatus;

import java.util.List;

interface OrderSpringDataRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllByBusinessIdOrderByCreatedAtDesc(Long businessId);
}
