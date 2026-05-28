package sk.posam.fsa.foodrescue.domain.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityProvider userIdentityProvider;

    @Test
    void registerForcesRegularUserRoleBeforeProvisioning() {
        User user = draftUser("owner@savr.test");
        user.setRole(UserRole.ADMIN);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        UserService service = new UserService(userRepository, userIdentityProvider);

        service.register(user);

        assertEquals(UserRole.USER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userIdentityProvider).create(user);
        verify(userRepository).create(user);
    }

    @Test
    void createRejectsDuplicateEmailBeforeProvisioningExternalIdentity() {
        User user = draftUser("duplicate@savr.test");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(existingUser(8L, user.getEmail())));

        UserService service = new UserService(userRepository, userIdentityProvider);

        FoodRescueException exception = assertThrows(
                FoodRescueException.class,
                () -> service.create(user)
        );

        assertEquals(FoodRescueException.Type.CONFLICT, exception.getType());
        assertTrue(exception.getMessage().contains("already exists"));
        verify(userIdentityProvider, never()).create(user);
        verify(userRepository, never()).create(user);
    }

    @Test
    void createRollsBackExternalIdentityWhenRepositoryCreateFails() {
        User user = draftUser("rollback@savr.test");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        doThrow(new RuntimeException("db write failed")).when(userRepository).create(user);

        UserService service = new UserService(userRepository, userIdentityProvider);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.create(user)
        );

        assertEquals("db write failed", exception.getMessage());
        verify(userIdentityProvider).create(user);
        verify(userIdentityProvider).deleteByEmail(user.getEmail());
    }

    @Test
    void provisionExternalIdentityReturnsExistingUserWithoutCreatingDuplicate() {
        User existingUser = existingUser(5L, "existing@savr.test");
        User requestedUser = draftUser("existing@savr.test");

        when(userRepository.findByEmail(requestedUser.getEmail())).thenReturn(Optional.of(existingUser));

        UserService service = new UserService(userRepository, userIdentityProvider);

        User result = service.provisionExternalIdentity(requestedUser);

        assertSame(existingUser, result);
        verify(userRepository, never()).create(requestedUser);
        verify(userIdentityProvider, never()).create(requestedUser);
    }

    @Test
    void provisionExternalIdentityAssignsGeneratedPasswordAndDefaultUserRole() {
        User user = new User();
        user.setFirstName("Lina");
        user.setLastName("Rescue");
        user.setEmail("external@savr.test");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        UserService service = new UserService(userRepository, userIdentityProvider);

        User result = service.provisionExternalIdentity(user);

        assertSame(user, result);
        assertEquals(UserRole.USER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.getPassword().startsWith("EXTERNAL-AUTH-"));
        verify(userRepository).create(user);
        verify(userIdentityProvider, never()).create(user);
    }

    private User draftUser(String email) {
        User user = new User();
        user.setFirstName("Lina");
        user.setLastName("Rescue");
        user.setEmail(email);
        user.setPassword("StrongPassword123");
        user.setRole(UserRole.USER);
        return user;
    }

    private User existingUser(Long id, String email) {
        User user = draftUser(email);
        user.setId(id);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
