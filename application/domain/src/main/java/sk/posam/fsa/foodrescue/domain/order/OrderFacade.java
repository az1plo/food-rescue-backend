package sk.posam.fsa.foodrescue.domain.order;

import sk.posam.fsa.foodrescue.domain.user.User;

import java.util.List;

public interface OrderFacade {

    List<Order> getOrders(User currentUser, Long businessId);

    Order get(User currentUser, Long id);

    Order create(User currentUser, Long offerId, Integer quantity, String cardHolderName, String cardLast4);

    OrderPickupPass getPickupPass(User currentUser, Long id);

    Order confirmPickup(User currentUser, Long id, String pickupToken);
}
