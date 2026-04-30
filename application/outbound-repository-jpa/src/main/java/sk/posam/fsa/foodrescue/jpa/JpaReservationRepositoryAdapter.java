package sk.posam.fsa.foodrescue.jpa;

import org.springframework.stereotype.Repository;
import sk.posam.fsa.foodrescue.domain.reservation.Reservation;
import sk.posam.fsa.foodrescue.domain.reservation.ReservationStatus;
import sk.posam.fsa.foodrescue.domain.reservation.ReservationRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaReservationRepositoryAdapter implements ReservationRepository {

    private final ReservationSpringDataRepository reservationSpringDataRepository;

    public JpaReservationRepositoryAdapter(ReservationSpringDataRepository reservationSpringDataRepository) {
        this.reservationSpringDataRepository = reservationSpringDataRepository;
    }

    @Override
    public Reservation save(Reservation reservation) {
        return reservationSpringDataRepository.save(reservation);
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return reservationSpringDataRepository.findById(id);
    }

    @Override
    public List<Reservation> findAllByUserId(Long userId) {
        return reservationSpringDataRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Reservation> findAllByOfferId(Long offerId) {
        return reservationSpringDataRepository.findAllByOfferIdOrderByCreatedAtDesc(offerId);
    }

    @Override
    public List<Reservation> findAllByOfferIds(List<Long> offerIds) {
        return offerIds == null || offerIds.isEmpty()
                ? List.of()
                : reservationSpringDataRepository.findAllByOfferIdInOrderByCreatedAtDesc(offerIds);
    }

    @Override
    public List<Reservation> findAllByStatus(ReservationStatus status) {
        return reservationSpringDataRepository.findAllByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    public Optional<Reservation> findActiveByOfferId(Long offerId) {
        return reservationSpringDataRepository.findByOfferIdAndStatus(offerId, ReservationStatus.ACTIVE);
    }

    @Override
    public void delete(Reservation reservation) {
        reservationSpringDataRepository.delete(reservation);
    }
}

