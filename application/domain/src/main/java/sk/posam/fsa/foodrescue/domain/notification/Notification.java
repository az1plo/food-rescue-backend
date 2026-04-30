package sk.posam.fsa.foodrescue.domain.notification;

import sk.posam.fsa.foodrescue.domain.shared.ValidationException;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.time.LocalDateTime;

public class Notification {

    private Long id;
    private Long userId;
    private NotificationType type;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public Notification() {
    }

    public static Notification create(Long userId, NotificationType type, String title, String message) {
        Notification notification = new Notification();
        notification.userId = userId;
        notification.type = type;
        notification.title = title;
        notification.message = message;
        return notification;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markAsRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }

    public boolean belongsTo(User user) {
        return user != null
                && userId != null
                && user.getId() != null
                && userId.equals(user.getId());
    }

    public void prepareForCreation() {
        require(userId != null, "Notification user is required");
        require(type != null, "Notification type is required");
        require(title != null && !title.isBlank(), "Notification title is required");
        require(message != null && !message.isBlank(), "Notification message is required");

        title = title.trim();
        message = message.trim();

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}


