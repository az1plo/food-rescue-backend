package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.offer.AllergenCode;
import sk.posam.fsa.foodrescue.rest.dto.AllergenCodeDto;

import java.util.List;

@Component
public class AllergenCodeMapper {

    public List<AllergenCode> toEntities(List<AllergenCodeDto> dtos) {
        return dtos == null ? List.of() : dtos.stream()
                .map(this::toEntity)
                .toList();
    }

    public List<AllergenCodeDto> toDtos(List<AllergenCode> entities) {
        return entities == null ? List.of() : entities.stream()
                .map(this::toDto)
                .toList();
    }

    public AllergenCode toEntity(AllergenCodeDto dto) {
        return dto == null ? null : AllergenCode.valueOf(dto.name());
    }

    public AllergenCodeDto toDto(AllergenCode entity) {
        return entity == null ? null : AllergenCodeDto.valueOf(entity.name());
    }
}
