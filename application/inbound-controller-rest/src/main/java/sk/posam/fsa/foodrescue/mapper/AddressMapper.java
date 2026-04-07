package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.models.entities.Address;
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
                dto.getCountry()
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
        return dto;
    }
}