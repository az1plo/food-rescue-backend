package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.models.entities.Business;
import sk.posam.fsa.foodrescue.rest.dto.BusinessResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.BusinessStatusDto;
import sk.posam.fsa.foodrescue.rest.dto.CreateBusinessRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.UpdateBusinessRequestDto;

import java.time.ZoneOffset;

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
                        ? entity.getCreatedAt().atOffset(ZoneOffset.UTC)
                        : null
        );
        return dto;
    }

    public Business toEntity(CreateBusinessRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Business entity = new Business();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setAddress(addressMapper.toEntity(dto.getAddress()));
        return entity;
    }

    public Business toEntity(UpdateBusinessRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Business entity = new Business();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setAddress(addressMapper.toEntity(dto.getAddress()));
        return entity;
    }
}
