package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.reservation.PickupConfirmation;
import sk.posam.fsa.foodrescue.rest.dto.PickupConfirmationDto;

@Component
public class PickupConfirmationMapper {

    public PickupConfirmationDto toDto(PickupConfirmation entity) {
        if (entity == null) {
            return null;
        }

        PickupConfirmationDto dto = new PickupConfirmationDto();
        dto.setConfirmedByUserId(entity.getConfirmedByUserId());
        dto.setConfirmedAt(
                entity.getConfirmedAt() != null
                        ? ApiDateTimeMapper.toUtcOffsetDateTime(entity.getConfirmedAt())
                        : null
        );
        return dto;
    }
}

