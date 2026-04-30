package sk.posam.fsa.foodrescue.domain.reservation;

import sk.posam.fsa.foodrescue.domain.reservation.Reservation;
import sk.posam.fsa.foodrescue.domain.reservation.ReservationStatus;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);

    List<Reservation> findAllByUserId(Long userId);

    List<Reservation> findAllByOfferId(Long offerId);

    List<Reservation> findAllByOfferIds(List<Long> offerIds);

    List<Reservation> findAllByStatus(ReservationStatus status);

    Optional<Reservation> findActiveByOfferId(Long offerId);

    void delete(Reservation reservation);
}


