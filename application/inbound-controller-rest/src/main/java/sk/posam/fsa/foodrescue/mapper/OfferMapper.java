package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.offer.Offer;
import sk.posam.fsa.foodrescue.rest.dto.CreateOfferRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferStatusDto;
import sk.posam.fsa.foodrescue.rest.dto.UpdateOfferRequestDto;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OfferMapper {

    private final OfferItemMapper offerItemMapper;
    private final PickupLocationMapper pickupLocationMapper;
    private final PickupTimeWindowMapper pickupTimeWindowMapper;

    public OfferMapper(OfferItemMapper offerItemMapper,
                       PickupLocationMapper pickupLocationMapper,
                       PickupTimeWindowMapper pickupTimeWindowMapper) {
        this.offerItemMapper = offerItemMapper;
        this.pickupLocationMapper = pickupLocationMapper;
        this.pickupTimeWindowMapper = pickupTimeWindowMapper;
    }

    public OfferResponseDto toDto(Offer entity) {
        if (entity == null) {
            return null;
        }

        OfferResponseDto dto = new OfferResponseDto();
        dto.setId(entity.getId());
        dto.setBusinessId(entity.getBusinessId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setImageUrl(entity.getImageUrl());
        dto.setPrice(toDtoPrice(entity.getPrice()));
        dto.setOriginalPrice(toDtoPrice(entity.getOriginalPrice()));
        dto.setQuantityAvailable(entity.getQuantityAvailable());
        dto.setStatus(
                entity.getStatus() != null
                        ? OfferStatusDto.valueOf(entity.getStatus().name())
                        : null
        );
        dto.setItems(entity.getItems().stream()
                .map(offerItemMapper::toDto)
                .toList());
        dto.setPickupLocation(pickupLocationMapper.toDto(entity.getPickupLocation()));
        dto.setPickupTimeWindow(pickupTimeWindowMapper.toDto(entity.getPickupTimeWindow()));
        dto.setCreatedAt(
                entity.getCreatedAt() != null
                        ? ApiDateTimeMapper.toUtcOffsetDateTime(entity.getCreatedAt())
                        : null
        );
        return dto;
    }

    public List<OfferResponseDto> toDtos(List<Offer> entities) {
        return entities == null ? List.of() : entities.stream()
                .map(this::toDto)
                .toList();
    }

    public Offer toEntity(CreateOfferRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Offer.fromDraft(
                dto.getBusinessId(),
                dto.getTitle(),
                dto.getDescription(),
                dto.getImageUrl(),
                toDomainPrice(dto.getPrice()),
                toDomainPrice(dto.getOriginalPrice()),
                dto.getQuantityAvailable(),
                dto.getItems() == null ? List.of() : dto.getItems().stream()
                        .map(offerItemMapper::toEntity)
                        .toList(),
                pickupLocationMapper.toEntity(dto.getPickupLocation()),
                pickupTimeWindowMapper.toEntity(dto.getPickupTimeWindow())
        );
    }

    public Offer toEntity(UpdateOfferRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Offer.fromDraft(
                null,
                dto.getTitle(),
                dto.getDescription(),
                dto.getImageUrl(),
                toDomainPrice(dto.getPrice()),
                toDomainPrice(dto.getOriginalPrice()),
                dto.getQuantityAvailable(),
                dto.getItems() == null ? List.of() : dto.getItems().stream()
                        .map(offerItemMapper::toEntity)
                        .toList(),
                pickupLocationMapper.toEntity(dto.getPickupLocation()),
                pickupTimeWindowMapper.toEntity(dto.getPickupTimeWindow())
        );
    }

    private Double toDtoPrice(BigDecimal price) {
        return price == null ? null : price.doubleValue();
    }

    private BigDecimal toDomainPrice(Double price) {
        return price == null ? null : BigDecimal.valueOf(price);
    }
}

