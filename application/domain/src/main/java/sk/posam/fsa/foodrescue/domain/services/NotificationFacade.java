package sk.posam.fsa.foodrescue.domain.services;

import sk.posam.fsa.foodrescue.domain.models.entities.Notification;
import sk.posam.fsa.foodrescue.domain.models.entities.User;

import java.util.List;

public interface NotificationFacade {

    List<Notification> getNotifications(User currentUser);

    Notification get(User currentUser, Long id);

    Notification markAsRead(User currentUser, Long id);
}
