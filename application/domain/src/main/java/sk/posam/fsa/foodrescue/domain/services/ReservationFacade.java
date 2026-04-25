package sk.posam.fsa.foodrescue.domain.services;

import sk.posam.fsa.foodrescue.domain.models.entities.Reservation;
import sk.posam.fsa.foodrescue.domain.models.entities.User;

import java.util.List;

public interface ReservationFacade {

    List<Reservation> getReservations(User currentUser, Long offerId);

    Reservation get(User currentUser, Long id);

    Reservation create(User currentUser, Long offerId);

    Reservation cancel(User currentUser, Long id);

    Reservation confirmPickup(User currentUser, Long id);
}
