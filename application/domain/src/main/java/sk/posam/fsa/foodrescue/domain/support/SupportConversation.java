package sk.posam.fsa.foodrescue.domain.support;

import sk.posam.fsa.foodrescue.domain.shared.ValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

public class SupportConversation {

    private Long id;
    private String publicId;
    private Long userId;
    private String sourcePage;
    private String locale;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SupportConversation() {
    }

    public static SupportConversation start(String publicId, Long userId, String sourcePage, String locale) {
        SupportConversation conversation = new SupportConversation();
        conversation.publicId = publicId;
        conversation.userId = userId;
        conversation.sourcePage = sourcePage;
        conversation.locale = locale;
        return conversation;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSourcePage() {
        return sourcePage;
    }

    public String getLocale() {
        return locale;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setSourcePage(String sourcePage) {
        this.sourcePage = sourcePage;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void refreshMetadata(Long userId, String sourcePage, String locale) {
        if (userId != null) {
            this.userId = userId;
        }

        if (sourcePage != null) {
            this.sourcePage = sourcePage;
        }

        if (locale != null) {
            this.locale = locale;
        }
    }

    public void touch() {
        updatedAt = LocalDateTime.now();
    }

    public void prepareForCreation() {
        if (publicId == null || publicId.isBlank()) {
            publicId = UUID.randomUUID().toString();
        } else {
            publicId = publicId.trim();
        }

        require(publicId.length() <= 64, "Support conversation id is too long");

        sourcePage = normalizeOptional(sourcePage, 255);
        locale = normalizeOptional(locale, 32);

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    private String normalizeOptional(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isBlank()) {
            return null;
        }

        return normalizedValue.length() > maxLength
                ? normalizedValue.substring(0, maxLength)
                : normalizedValue;
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}
