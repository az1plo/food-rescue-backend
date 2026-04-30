package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.offer.PickupLocation;
import sk.posam.fsa.foodrescue.rest.dto.PickupLocationDto;

@Component
public class PickupLocationMapper {

    private final AddressMapper addressMapper;

    public PickupLocationMapper(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public PickupLocation toEntity(PickupLocationDto dto) {
        if (dto == null) {
            return null;
        }

        return PickupLocation.of(
                addressMapper.toEntity(dto.getAddress()),
                dto.getNote()
        );
    }

    public PickupLocationDto toDto(PickupLocation entity) {
        if (entity == null) {
            return null;
        }

        PickupLocationDto dto = new PickupLocationDto();
        dto.setAddress(addressMapper.toDto(entity.getAddress()));
        dto.setNote(entity.getNote());
        return dto;
    }
}

