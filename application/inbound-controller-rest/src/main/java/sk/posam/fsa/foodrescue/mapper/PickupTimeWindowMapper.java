package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.models.valueobjects.PickupTimeWindow;
import sk.posam.fsa.foodrescue.rest.dto.PickupTimeWindowDto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class PickupTimeWindowMapper {

    public PickupTimeWindow toEntity(PickupTimeWindowDto dto) {
        if (dto == null) {
            return null;
        }

        return PickupTimeWindow.of(
                toLocalDateTime(dto.getFrom()),
                toLocalDateTime(dto.getTo())
        );
    }

    public PickupTimeWindowDto toDto(PickupTimeWindow entity) {
        if (entity == null) {
            return null;
        }

        PickupTimeWindowDto dto = new PickupTimeWindowDto();
        dto.setFrom(entity.getFrom() != null ? entity.getFrom().atOffset(ZoneOffset.UTC) : null);
        dto.setTo(entity.getTo() != null ? entity.getTo().atOffset(ZoneOffset.UTC) : null);
        return dto;
    }

    private java.time.LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
