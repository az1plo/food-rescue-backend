package sk.posam.fsa.foodrescue.domain.services;

import sk.posam.fsa.foodrescue.domain.exceptions.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.exceptions.ValidationException;
import sk.posam.fsa.foodrescue.domain.models.entities.User;
import sk.posam.fsa.foodrescue.domain.repositories.UserRepository;

public class UserService implements UserFacade {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User get(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User get(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public void create(User user) {
        if (user == null) {
            throw new ValidationException("User must not be null");
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "User with given email already exists"
            );
        }

        user.prepareForCreation();
        userRepository.create(user);
    }
}