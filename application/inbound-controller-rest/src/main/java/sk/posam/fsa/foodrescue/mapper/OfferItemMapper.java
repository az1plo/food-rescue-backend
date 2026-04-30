package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.offer.OfferItem;
import sk.posam.fsa.foodrescue.rest.dto.OfferItemDto;

@Component
public class OfferItemMapper {

    public OfferItem toEntity(OfferItemDto dto) {
        if (dto == null) {
            return null;
        }

        return OfferItem.of(dto.getName(), dto.getQuantity());
    }

    public OfferItemDto toDto(OfferItem entity) {
        if (entity == null) {
            return null;
        }

        OfferItemDto dto = new OfferItemDto();
        dto.setName(entity.getName());
        dto.setQuantity(entity.getQuantity());
        return dto;
    }
}

