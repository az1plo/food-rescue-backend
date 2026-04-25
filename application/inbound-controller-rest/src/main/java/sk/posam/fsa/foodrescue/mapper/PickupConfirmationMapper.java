package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.models.valueobjects.PickupConfirmation;
import sk.posam.fsa.foodrescue.rest.dto.PickupConfirmationDto;

import java.time.ZoneOffset;

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
                        ? entity.getConfirmedAt().atOffset(ZoneOffset.UTC)
                        : null
        );
        return dto;
    }
}
