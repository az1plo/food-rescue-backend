package sk.posam.fsa.foodrescue.domain.support;

import sk.posam.fsa.foodrescue.domain.shared.ValidationException;

import java.time.LocalDateTime;

public class SupportConversationMessage {

    private Long id;
    private Long conversationId;
    private SupportConversationMessageRole role;
    private String content;
    private LocalDateTime createdAt;

    public SupportConversationMessage() {
    }

    public static SupportConversationMessage userMessage(Long conversationId, String content) {
        return message(conversationId, SupportConversationMessageRole.USER, content);
    }

    public static SupportConversationMessage assistantMessage(Long conversationId, String content) {
        return message(conversationId, SupportConversationMessageRole.ASSISTANT, content);
    }

    public static SupportConversationMessage toolMessage(Long conversationId, String content) {
        return message(conversationId, SupportConversationMessageRole.TOOL, content);
    }

    public static SupportConversationMessage systemMessage(Long conversationId, String content) {
        return message(conversationId, SupportConversationMessageRole.SYSTEM, content);
    }

    private static SupportConversationMessage message(Long conversationId,
                                                      SupportConversationMessageRole role,
                                                      String content) {
        SupportConversationMessage message = new SupportConversationMessage();
        message.conversationId = conversationId;
        message.role = role;
        message.content = content;
        return message;
    }

    public Long getId() {
        return id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public SupportConversationMessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public void setRole(SupportConversationMessageRole role) {
        this.role = role;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void prepareForCreation() {
        require(conversationId != null, "Support conversation id is required");
        require(role != null, "Support message role is required");
        require(content != null && !content.isBlank(), "Support message content is required");

        content = content.trim();
        require(content.length() <= 4000, "Support message content is too long");

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
