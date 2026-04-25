package sk.posam.fsa.foodrescue.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.posam.fsa.foodrescue.domain.models.entities.Reservation;
import sk.posam.fsa.foodrescue.domain.models.enums.ReservationStatus;

import java.util.List;
import java.util.Optional;

interface ReservationSpringDataRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Reservation> findAllByOfferIdOrderByCreatedAtDesc(Long offerId);

    List<Reservation> findAllByStatusOrderByCreatedAtDesc(ReservationStatus status);

    Optional<Reservation> findByOfferIdAndStatus(Long offerId, ReservationStatus status);
}
