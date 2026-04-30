package sk.posam.fsa.foodrescue.domain.notification;

import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.notification.Notification;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.notification.NotificationRepository;

import java.util.List;

public class NotificationService implements NotificationFacade {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Notification> getNotifications(User currentUser) {
        return notificationRepository.findAllByUserId(currentUser.getId());
    }

    @Override
    public Notification get(User currentUser, Long id) {
        Notification notification = resolveNotification(id);
        ensureOwner(currentUser, notification);
        return notification;
    }

    @Override
    public Notification markAsRead(User currentUser, Long id) {
        Notification notification = resolveNotification(id);
        ensureOwner(currentUser, notification);
        notification.markAsRead();
        return notificationRepository.save(notification);
    }

    private Notification resolveNotification(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Notification with id=" + id + " was not found"
                ));
    }

    private void ensureOwner(User currentUser, Notification notification) {
        if (!notification.belongsTo(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You do not have access to notification with id=" + notification.getId()
            );
        }
    }
}


