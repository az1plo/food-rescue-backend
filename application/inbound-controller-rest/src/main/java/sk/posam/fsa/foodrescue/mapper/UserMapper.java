package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.user.UserRole;
import sk.posam.fsa.foodrescue.rest.dto.CreateUserRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.RegisterUserRequestDto;

@Component
public class UserMapper {

    public User toUser(CreateUserRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return User.create(
                dto.getFirstName(),
                dto.getLastName(),
                dto.getEmail(),
                dto.getPassword(),
                dto.getRole() != null ? UserRole.valueOf(dto.getRole().getValue()) : null
        );
    }

    public User toUser(RegisterUserRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return User.create(
                dto.getFirstName(),
                dto.getLastName(),
                dto.getEmail(),
                dto.getPassword(),
                UserRole.USER
        );
    }
}

