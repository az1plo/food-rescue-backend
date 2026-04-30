package sk.posam.fsa.foodrescue.domain.notification;

import sk.posam.fsa.foodrescue.domain.notification.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    List<Notification> findAllByUserId(Long userId);

    List<Notification> findUnreadByUserId(Long userId);

    void delete(Notification notification);
}


