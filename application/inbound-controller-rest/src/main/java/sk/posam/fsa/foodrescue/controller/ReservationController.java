package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.reservation.Reservation;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.reservation.ReservationFacade;
import sk.posam.fsa.foodrescue.mapper.ReservationMapper;
import sk.posam.fsa.foodrescue.rest.api.ReservationsApi;
import sk.posam.fsa.foodrescue.rest.dto.CreateReservationRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.ReservationResponseDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

import java.net.URI;
import java.util.List;

@RestController
public class ReservationController implements ReservationsApi {

    private final ReservationFacade reservationFacade;
    private final ReservationMapper reservationMapper;
    private final CurrentUserDetailService currentUserDetailService;

    public ReservationController(ReservationFacade reservationFacade,
                                 ReservationMapper reservationMapper,
                                 CurrentUserDetailService currentUserDetailService) {
        this.reservationFacade = reservationFacade;
        this.reservationMapper = reservationMapper;
        this.currentUserDetailService = currentUserDetailService;
    }

    @Override
    public ResponseEntity<List<ReservationResponseDto>> getReservations(Long offerId) {
        User user = currentUserDetailService.getFullCurrentUser();
        List<Reservation> reservations = reservationFacade.getReservations(user, offerId);
        return ResponseEntity.ok(reservationMapper.toDtos(reservations));
    }

    @Override
    public ResponseEntity<Void> createReservation(CreateReservationRequestDto request) {
        User user = currentUserDetailService.getFullCurrentUser();
        Reservation reservation = reservationFacade.create(
                user,
                reservationMapper.toOfferId(request),
                reservationMapper.toQuantity(request)
        );
        return ResponseEntity.created(URI.create("/reservations/" + reservation.getId())).build();
    }

    @Override
    public ResponseEntity<ReservationResponseDto> getReservation(Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        Reservation reservation = reservationFacade.get(user, id);
        return ResponseEntity.ok(reservationMapper.toDto(reservation));
    }

    @Override
    public ResponseEntity<ReservationResponseDto> cancelReservation(Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        Reservation reservation = reservationFacade.cancel(user, id);
        return ResponseEntity.ok(reservationMapper.toDto(reservation));
    }

    @Override
    public ResponseEntity<ReservationResponseDto> confirmReservationPickup(Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        Reservation reservation = reservationFacade.confirmPickup(user, id);
        return ResponseEntity.ok(reservationMapper.toDto(reservation));
    }
}

