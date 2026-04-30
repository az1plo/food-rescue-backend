package sk.posam.fsa.foodrescue.jpa;

import org.springframework.stereotype.Repository;
import sk.posam.fsa.foodrescue.domain.notification.Notification;
import sk.posam.fsa.foodrescue.domain.notification.NotificationRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaNotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationSpringDataRepository notificationSpringDataRepository;

    public JpaNotificationRepositoryAdapter(NotificationSpringDataRepository notificationSpringDataRepository) {
        this.notificationSpringDataRepository = notificationSpringDataRepository;
    }

    @Override
    public Notification save(Notification notification) {
        return notificationSpringDataRepository.save(notification);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return notificationSpringDataRepository.findById(id);
    }

    @Override
    public List<Notification> findAllByUserId(Long userId) {
        return notificationSpringDataRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Notification> findUnreadByUserId(Long userId) {
        return notificationSpringDataRepository.findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId);
    }

    @Override
    public void delete(Notification notification) {
        notificationSpringDataRepository.delete(notification);
    }
}

