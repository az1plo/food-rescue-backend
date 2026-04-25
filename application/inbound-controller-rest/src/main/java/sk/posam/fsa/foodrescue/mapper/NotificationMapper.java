package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.models.entities.Notification;
import sk.posam.fsa.foodrescue.rest.dto.NotificationResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.NotificationTypeDto;

import java.time.ZoneOffset;
import java.util.List;

@Component
public class NotificationMapper {

    public NotificationResponseDto toDto(Notification entity) {
        if (entity == null) {
            return null;
        }

        NotificationResponseDto dto = new NotificationResponseDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setType(
                entity.getType() != null
                        ? NotificationTypeDto.valueOf(entity.getType().name())
                        : null
        );
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setCreatedAt(
                entity.getCreatedAt() != null
                        ? entity.getCreatedAt().atOffset(ZoneOffset.UTC)
                        : null
        );
        dto.setReadAt(
                entity.getReadAt() != null
                        ? entity.getReadAt().atOffset(ZoneOffset.UTC)
                        : null
        );
        return dto;
    }

    public List<NotificationResponseDto> toDtos(List<Notification> entities) {
        return entities == null ? List.of() : entities.stream()
                .map(this::toDto)
                .toList();
    }
}
