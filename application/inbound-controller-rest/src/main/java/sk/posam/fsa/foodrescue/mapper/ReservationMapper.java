package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.models.entities.Reservation;
import sk.posam.fsa.foodrescue.rest.dto.CreateReservationRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.ReservationResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.ReservationStatusDto;

import java.time.ZoneOffset;
import java.util.List;

@Component
public class ReservationMapper {

    private final PickupConfirmationMapper pickupConfirmationMapper;

    public ReservationMapper(PickupConfirmationMapper pickupConfirmationMapper) {
        this.pickupConfirmationMapper = pickupConfirmationMapper;
    }

    public ReservationResponseDto toDto(Reservation entity) {
        if (entity == null) {
            return null;
        }

        ReservationResponseDto dto = new ReservationResponseDto();
        dto.setId(entity.getId());
        dto.setOfferId(entity.getOfferId());
        dto.setUserId(entity.getUserId());
        dto.setStatus(
                entity.getStatus() != null
                        ? ReservationStatusDto.valueOf(entity.getStatus().name())
                        : null
        );
        dto.setCreatedAt(
                entity.getCreatedAt() != null
                        ? entity.getCreatedAt().atOffset(ZoneOffset.UTC)
                        : null
        );
        dto.setCancelledAt(
                entity.getCancelledAt() != null
                        ? entity.getCancelledAt().atOffset(ZoneOffset.UTC)
                        : null
        );
        dto.setPickupConfirmation(pickupConfirmationMapper.toDto(entity.getPickupConfirmation()));
        return dto;
    }

    public List<ReservationResponseDto> toDtos(List<Reservation> entities) {
        return entities == null ? List.of() : entities.stream()
                .map(this::toDto)
                .toList();
    }

    public Long toOfferId(CreateReservationRequestDto dto) {
        return dto == null ? null : dto.getOfferId();
    }
}
