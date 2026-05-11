package sk.posam.fsa.foodrescue.domain.order;

import sk.posam.fsa.foodrescue.domain.user.User;

import java.util.List;

public interface OrderFacade {

    List<OrderDetailsView> getOrders(User currentUser, Long businessId);

    OrderDetailsView get(User currentUser, Long id);

    OrderDetailsView create(User currentUser, Long offerId, Integer quantity, String cardHolderName, String cardLast4);

    OrderPickupPass getPickupPass(User currentUser, Long id);

    OrderDetailsView confirmPickup(User currentUser, Long id, String pickupToken);

    OrderDetailsView submitReview(User currentUser, Long id, Integer rating, String comment);
}
