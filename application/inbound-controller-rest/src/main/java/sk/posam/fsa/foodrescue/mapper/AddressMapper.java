package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.shared.Address;
import sk.posam.fsa.foodrescue.rest.dto.AddressDto;

@Component
public class AddressMapper {

    public Address toEntity(AddressDto dto) {
        if (dto == null) {
            return null;
        }

        return new Address(
                dto.getStreet(),
                dto.getCity(),
                dto.getPostalCode(),
                dto.getCountry(),
                dto.getLatitude(),
                dto.getLongitude()
        );
    }

    public AddressDto toDto(Address entity) {
        if (entity == null) {
            return null;
        }

        AddressDto dto = new AddressDto();
        dto.setStreet(entity.getStreet());
        dto.setCity(entity.getCity());
        dto.setPostalCode(entity.getPostalCode());
        dto.setCountry(entity.getCountry());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        return dto;
    }
}

