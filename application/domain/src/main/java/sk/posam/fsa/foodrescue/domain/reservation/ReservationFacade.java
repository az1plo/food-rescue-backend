package sk.posam.fsa.foodrescue.domain.reservation;

import sk.posam.fsa.foodrescue.domain.reservation.Reservation;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.util.List;

public interface ReservationFacade {

    List<Reservation> getReservations(User currentUser, Long offerId);

    Reservation get(User currentUser, Long id);

    Reservation create(User currentUser, Long offerId, Integer quantity);

    Reservation cancel(User currentUser, Long id);

    Reservation confirmPickup(User currentUser, Long id);
}


