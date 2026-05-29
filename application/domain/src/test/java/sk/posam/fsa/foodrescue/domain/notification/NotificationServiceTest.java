package sk.posam.fsa.foodrescue.domain.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.user.UserRole;
import sk.posam.fsa.foodrescue.domain.user.UserStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    void getNotificationsReturnsCurrentUsersInbox() {
        User currentUser = activeUser(7L);
        List<Notification> inbox = List.of(notification(10L, 7L), notification(11L, 7L));

        when(notificationRepository.findAllByUserId(currentUser.getId())).thenReturn(inbox);

        NotificationService service = new NotificationService(notificationRepository);

        List<Notification> result = service.getNotifications(currentUser);

        assertEquals(inbox, result);
        verify(notificationRepository).findAllByUserId(currentUser.getId());
    }

    @Test
    void getNotificationsRejectsInactiveUser() {
        User currentUser = activeUser(7L);
        currentUser.block();

        NotificationService service = new NotificationService(notificationRepository);

        FoodRescueException exception = assertThrows(
                FoodRescueException.class,
                () -> service.getNotifications(currentUser)
        );

        assertEquals(FoodRescueException.Type.FORBIDDEN, exception.getType());
        assertTrue(exception.getMessage().contains("Only active users"));
        verify(notificationRepository, never()).findAllByUserId(currentUser.getId());
    }

    @Test
    void clearNotificationsDeletesCurrentUsersInbox() {
        User currentUser = activeUser(7L);
        List<Notification> inbox = List.of(notification(10L, 7L), notification(11L, 7L));

        when(notificationRepository.findAllByUserId(currentUser.getId())).thenReturn(inbox);

        NotificationService service = new NotificationService(notificationRepository);

        service.clearNotifications(currentUser);

        verify(notificationRepository).findAllByUserId(currentUser.getId());
        verify(notificationRepository).delete(inbox.get(0));
        verify(notificationRepository).delete(inbox.get(1));
    }

    @Test
    void markAsReadPersistsOwnedNotification() {
        User currentUser = activeUser(7L);
        Notification notification = notification(10L, 7L);

        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationService service = new NotificationService(notificationRepository);

        Notification result = service.markAsRead(currentUser, notification.getId());

        assertTrue(result.isRead());
        assertNotNull(result.getReadAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    void getRejectsForeignNotification() {
        User currentUser = activeUser(7L);
        Notification notification = notification(10L, 9L);

        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        NotificationService service = new NotificationService(notificationRepository);

        FoodRescueException exception = assertThrows(
                FoodRescueException.class,
                () -> service.get(currentUser, notification.getId())
        );

        assertEquals(FoodRescueException.Type.FORBIDDEN, exception.getType());
        assertTrue(exception.getMessage().contains("do not have access"));
        verify(notificationRepository, never()).save(notification);
    }

    @Test
    void markAsReadRejectsMissingNotification() {
        User currentUser = activeUser(7L);

        when(notificationRepository.findById(404L)).thenReturn(Optional.empty());

        NotificationService service = new NotificationService(notificationRepository);

        FoodRescueException exception = assertThrows(
                FoodRescueException.class,
                () -> service.markAsRead(currentUser, 404L)
        );

        assertEquals(FoodRescueException.Type.NOT_FOUND, exception.getType());
        assertTrue(exception.getMessage().contains("was not found"));
    }

    private Notification notification(Long id, Long userId) {
        Notification notification = Notification.create(
                userId,
                NotificationType.RESERVATION_STATUS_CHANGED,
                "Pickup reminder",
                "Your rescue bag is ready."
        );
        notification.setId(id);
        notification.prepareForCreation();
        return notification;
    }

    private User activeUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
