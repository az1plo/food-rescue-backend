package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.rest.dto.BusinessResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.BusinessStatusDto;
import sk.posam.fsa.foodrescue.rest.dto.CreateBusinessRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.UpdateBusinessRequestDto;

@Component
public class BusinessMapper {

    private final AddressMapper addressMapper;

    public BusinessMapper(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public BusinessResponseDto toDto(Business entity) {
        if (entity == null) {
            return null;
        }

        BusinessResponseDto dto = new BusinessResponseDto();
        dto.setId(entity.getId());
        dto.setOwnerId(entity.getOwnerId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStatus(
                entity.getStatus() != null
                        ? BusinessStatusDto.valueOf(entity.getStatus().name())
                        : null
        );
        dto.setAddress(addressMapper.toDto(entity.getAddress()));
        dto.setCreatedAt(
                entity.getCreatedAt() != null
                        ? ApiDateTimeMapper.toUtcOffsetDateTime(entity.getCreatedAt())
                        : null
        );
        return dto;
    }

    public Business toEntity(CreateBusinessRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Business.fromProfile(
                dto.getName(),
                dto.getDescription(),
                addressMapper.toEntity(dto.getAddress())
        );
    }

    public Business toEntity(UpdateBusinessRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Business.fromProfile(
                dto.getName(),
                dto.getDescription(),
                addressMapper.toEntity(dto.getAddress())
        );
    }
}

