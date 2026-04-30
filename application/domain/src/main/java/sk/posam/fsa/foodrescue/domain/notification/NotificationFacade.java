package sk.posam.fsa.foodrescue.domain.notification;

import sk.posam.fsa.foodrescue.domain.notification.Notification;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.util.List;

public interface NotificationFacade {

    List<Notification> getNotifications(User currentUser);

    Notification get(User currentUser, Long id);

    Notification markAsRead(User currentUser, Long id);
}


