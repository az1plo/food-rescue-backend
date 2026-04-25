package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.models.entities.Notification;
import sk.posam.fsa.foodrescue.domain.models.entities.User;
import sk.posam.fsa.foodrescue.domain.services.NotificationFacade;
import sk.posam.fsa.foodrescue.mapper.NotificationMapper;
import sk.posam.fsa.foodrescue.rest.api.NotificationsApi;
import sk.posam.fsa.foodrescue.rest.dto.NotificationResponseDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

import java.util.List;

@RestController
public class NotificationController implements NotificationsApi {

    private final NotificationFacade notificationFacade;
    private final NotificationMapper notificationMapper;
    private final CurrentUserDetailService currentUserDetailService;

    public NotificationController(NotificationFacade notificationFacade,
                                  NotificationMapper notificationMapper,
                                  CurrentUserDetailService currentUserDetailService) {
        this.notificationFacade = notificationFacade;
        this.notificationMapper = notificationMapper;
        this.currentUserDetailService = currentUserDetailService;
    }

    @Override
    public ResponseEntity<List<NotificationResponseDto>> getNotifications() {
        User user = currentUserDetailService.getFullCurrentUser();
        List<Notification> notifications = notificationFacade.getNotifications(user);
        return ResponseEntity.ok(notificationMapper.toDtos(notifications));
    }

    @Override
    public ResponseEntity<NotificationResponseDto> getNotification(Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        Notification notification = notificationFacade.get(user, id);
        return ResponseEntity.ok(notificationMapper.toDto(notification));
    }

    @Override
    public ResponseEntity<NotificationResponseDto> markNotificationAsRead(Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        Notification notification = notificationFacade.markAsRead(user, id);
        return ResponseEntity.ok(notificationMapper.toDto(notification));
    }
}
