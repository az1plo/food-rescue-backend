package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.marketplace.MarketplaceBusinessView;
import sk.posam.fsa.foodrescue.domain.marketplace.MarketplaceOfferCriteria;
import sk.posam.fsa.foodrescue.domain.marketplace.MarketplaceOfferSort;
import sk.posam.fsa.foodrescue.domain.marketplace.MarketplaceOfferView;
import sk.posam.fsa.foodrescue.rest.dto.MarketplaceBusinessSummaryDto;
import sk.posam.fsa.foodrescue.rest.dto.MarketplaceOfferResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.MarketplaceOfferSortDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferStatusDto;

import java.util.List;

@Component
public class MarketplaceOfferMapper {

    private final AddressMapper addressMapper;
    private final PickupLocationMapper pickupLocationMapper;
    private final PickupTimeWindowMapper pickupTimeWindowMapper;

    public MarketplaceOfferMapper(AddressMapper addressMapper,
                                  PickupLocationMapper pickupLocationMapper,
                                  PickupTimeWindowMapper pickupTimeWindowMapper) {
        this.addressMapper = addressMapper;
        this.pickupLocationMapper = pickupLocationMapper;
        this.pickupTimeWindowMapper = pickupTimeWindowMapper;
    }

    public List<MarketplaceOfferResponseDto> toDtos(List<MarketplaceOfferView> offers) {
        return offers == null ? List.of() : offers.stream()
                .map(this::toDto)
                .toList();
    }

    public MarketplaceOfferResponseDto toDto(MarketplaceOfferView offer) {
        if (offer == null) {
            return null;
        }

        MarketplaceOfferResponseDto dto = new MarketplaceOfferResponseDto();
        dto.setId(offer.id());
        dto.setTitle(offer.title());
        dto.setDescription(offer.description());
        dto.setImageUrl(offer.imageUrl());
        dto.setPrice(offer.price());
        dto.setOriginalPrice(offer.originalPrice());
        dto.setQuantityAvailable(offer.quantityAvailable());
        dto.setStatus(offer.status() == null ? null : OfferStatusDto.valueOf(offer.status().name()));
        dto.setBadgeText(offer.badgeText());
        dto.setDistanceMeters(offer.distanceMeters());
        dto.setCanReserve(offer.canReserve());
        dto.setPickupLocation(pickupLocationMapper.toDto(offer.pickupLocation()));
        dto.setPickupTimeWindow(pickupTimeWindowMapper.toDto(offer.pickupTimeWindow()));
        dto.setBusiness(toBusinessDto(offer.business()));
        return dto;
    }

    public MarketplaceOfferCriteria toCriteria(String q,
                                               Double viewerLat,
                                               Double viewerLng,
                                               MarketplaceOfferSortDto sort,
                                               Boolean includeUnavailable) {
        return new MarketplaceOfferCriteria(
                q,
                viewerLat,
                viewerLng,
                toDomainSort(sort),
                Boolean.TRUE.equals(includeUnavailable)
        );
    }

    public MarketplaceOfferSort toDomainSort(MarketplaceOfferSortDto dto) {
        if (dto == null) {
            return MarketplaceOfferSort.DISTANCE;
        }

        return MarketplaceOfferSort.valueOf(dto.name());
    }

    private MarketplaceBusinessSummaryDto toBusinessDto(MarketplaceBusinessView business) {
        if (business == null) {
            return null;
        }

        MarketplaceBusinessSummaryDto dto = new MarketplaceBusinessSummaryDto();
        dto.setId(business.id());
        dto.setName(business.name());
        dto.setDescription(business.description());
        dto.setAddress(addressMapper.toDto(business.address()));
        dto.setRatingAverage(business.ratingAverage());
        dto.setRatingCount(business.ratingCount());
        dto.setAvailableOfferCount(business.availableOfferCount());
        dto.setUnavailableOfferCount(business.unavailableOfferCount());
        return dto;
    }
}
