package sk.posam.fsa.foodrescue.transactional;

import org.springframework.transaction.annotation.Transactional;
import sk.posam.fsa.foodrescue.domain.order.OrderDetailsView;
import sk.posam.fsa.foodrescue.domain.order.OrderFacade;
import sk.posam.fsa.foodrescue.domain.order.OrderPickupPass;
import sk.posam.fsa.foodrescue.domain.order.OrderService;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.util.List;

public class TransactionalOrderFacade implements OrderFacade {

    private final OrderService orderService;

    public TransactionalOrderFacade(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    @Transactional
    public List<OrderDetailsView> getOrders(User currentUser, Long businessId) {
        return orderService.getOrders(currentUser, businessId);
    }

    @Override
    @Transactional
    public OrderDetailsView get(User currentUser, Long id) {
        return orderService.get(currentUser, id);
    }

    @Override
    @Transactional
    public OrderDetailsView create(User currentUser, Long offerId, Integer quantity, String cardHolderName, String cardLast4) {
        return orderService.create(currentUser, offerId, quantity, cardHolderName, cardLast4);
    }

    @Override
    @Transactional
    public OrderPickupPass getPickupPass(User currentUser, Long id) {
        return orderService.getPickupPass(currentUser, id);
    }

    @Override
    @Transactional
    public OrderDetailsView confirmPickup(User currentUser, Long id, String pickupToken) {
        return orderService.confirmPickup(currentUser, id, pickupToken);
    }

    @Override
    @Transactional
    public OrderDetailsView submitReview(User currentUser, Long id, Integer rating, String comment) {
        return orderService.submitReview(currentUser, id, rating, comment);
    }

    @Transactional
    public int settleExpiredNoShows() {
        return orderService.settleExpiredNoShows();
    }
}
