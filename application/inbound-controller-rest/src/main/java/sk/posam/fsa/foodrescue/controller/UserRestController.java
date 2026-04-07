package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.service.UserFacade;
import sk.posam.fsa.foodrescue.mapper.UserMapper;
import sk.posam.fsa.foodrescue.domain.models.entities.User;
import sk.posam.fsa.foodrescue.rest.api.UsersApi;
import sk.posam.fsa.foodrescue.rest.dto.CreateUserRequestDto;

@RestController
public class UserRestController implements UsersApi {

    private final UserFacade userFacade;
    private final UserMapper userMapper;

    public UserRestController(UserFacade userFacade, UserMapper userMapper) {
        this.userFacade = userFacade;
        this.userMapper = userMapper;
    }

    @Override
    public ResponseEntity<Void> createUser(CreateUserRequestDto createUserRequestDto) {
        User user = userMapper.toUser(createUserRequestDto);
        userFacade.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
