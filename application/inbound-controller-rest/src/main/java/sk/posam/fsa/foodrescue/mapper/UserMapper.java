package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.models.entities.User;
import sk.posam.fsa.foodrescue.domain.models.enums.UserRole;
import sk.posam.fsa.foodrescue.rest.dto.CreateUserRequestDto;

@Component
public class UserMapper {

    public User toUser(CreateUserRequestDto dto) {
        if (dto == null) {
            return null;
        }

        User entity = new User();
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPassword(dto.getPassword());
        entity.setRole(dto.getRole() != null ? UserRole.valueOf(dto.getRole().getValue()) : null);

        return entity;
    }
}