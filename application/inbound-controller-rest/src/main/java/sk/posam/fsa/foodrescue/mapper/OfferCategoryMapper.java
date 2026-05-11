package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.offer.OfferCategory;
import sk.posam.fsa.foodrescue.rest.dto.OfferCategoryDto;

@Component
public class OfferCategoryMapper {

    public OfferCategory toEntity(OfferCategoryDto dto) {
        return dto == null ? OfferCategory.OTHER : OfferCategory.valueOf(dto.name());
    }

    public OfferCategoryDto toDto(OfferCategory entity) {
        OfferCategory normalizedEntity = entity == null ? OfferCategory.OTHER : entity;
        return OfferCategoryDto.valueOf(normalizedEntity.name());
    }
}
