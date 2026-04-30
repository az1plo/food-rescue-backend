package sk.posam.fsa.foodrescue.domain.user;

import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.shared.ValidationException;

import java.util.UUID;

public class UserService implements UserFacade {

    private final UserRepository userRepository;
    private final UserIdentityProvider userIdentityProvider;

    public UserService(UserRepository userRepository,
                       UserIdentityProvider userIdentityProvider) {
        this.userRepository = userRepository;
        this.userIdentityProvider = userIdentityProvider;
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
        provisionUser(user, false);
    }

    @Override
    public void register(User user) {
        provisionUser(user, true);
    }

    @Override
    public User provisionExternalIdentity(User user) {
        if (user == null) {
            throw new ValidationException("User must not be null");
        }

        User existingUser = userRepository.findByEmail(user.getEmail()).orElse(null);
        if (existingUser != null) {
            return existingUser;
        }

        user.setPassword(generateExternalIdentityPassword());

        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }

        user.prepareForCreation();
        userRepository.create(user);
        return user;
    }

    private void provisionUser(User user, boolean forceRegularUserRole) {
        if (user == null) {
            throw new ValidationException("User must not be null");
        }

        if (forceRegularUserRole) {
            user.setRole(UserRole.USER);
        }

        user.prepareForCreation();

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "User with given email already exists"
            );
        }

        userIdentityProvider.create(user);

        try {
            userRepository.create(user);
        } catch (RuntimeException ex) {
            userIdentityProvider.deleteByEmail(user.getEmail());
            throw ex;
        }
    }

    private String generateExternalIdentityPassword() {
        return "EXTERNAL-AUTH-" + UUID.randomUUID();
    }
}


