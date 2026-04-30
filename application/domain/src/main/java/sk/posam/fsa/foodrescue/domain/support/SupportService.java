package sk.posam.fsa.foodrescue.domain.support;

import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SupportService implements SupportFacade {

    private static final int MESSAGE_MAX_LENGTH = 2000;
    private static final int SOURCE_PAGE_MAX_LENGTH = 255;
    private static final int LOCALE_MAX_LENGTH = 32;
    private static final int HISTORY_LIMIT = 12;
    private static final int SUGGESTION_LIMIT = 4;
    private static final String DEFAULT_ASSISTANT_NAME = "Mika";

    private final SupportAssistantProvider supportAssistantProvider;
    private final SupportConversationRepository supportConversationRepository;

    public SupportService(SupportAssistantProvider supportAssistantProvider,
                          SupportConversationRepository supportConversationRepository) {
        this.supportAssistantProvider = supportAssistantProvider;
        this.supportConversationRepository = supportConversationRepository;
    }

    @Override
    public SupportChatResponse reply(User currentUser, SupportChatRequest request) {
        SupportChatRequest normalizedRequest = normalizeRequest(request);
        SupportConversation conversation = loadOrCreateConversation(currentUser, normalizedRequest);
        persistUserMessage(conversation, normalizedRequest.message());
        List<SupportConversationMessage> history = supportConversationRepository.findLatestMessages(conversation.getId(), HISTORY_LIMIT);

        SupportAssistantReply assistantReply = supportAssistantProvider.reply(
                new SupportAssistantPrompt(
                        conversation.getPublicId(),
                        normalizedRequest.message(),
                        conversation.getSourcePage(),
                        conversation.getLocale(),
                        history,
                        currentUser
                )
        );

        String assistantName = normalizeAssistantName(assistantReply.assistantName());
        String assistantMessage = normalizeAssistantMessage(assistantReply.message());
        List<String> suggestions = normalizeSuggestions(assistantReply.suggestions());
        persistAssistantMessage(conversation, assistantMessage);

        return new SupportChatResponse(
                conversation.getPublicId(),
                assistantName,
                assistantMessage,
                LocalDateTime.now(),
                suggestions
        );
    }

    private SupportChatRequest normalizeRequest(SupportChatRequest request) {
        if (request == null) {
            throw validationException("Support chat request must be provided");
        }

        return new SupportChatRequest(
                normalizeConversationId(request.conversationId()),
                normalizeMessage(request.message()),
                normalizeOptional(request.sourcePage(), SOURCE_PAGE_MAX_LENGTH),
                normalizeOptional(request.locale(), LOCALE_MAX_LENGTH),
                normalizeHistory(request.history())
        );
    }

    private String normalizeConversationId(String conversationId) {
        String normalizedConversationId = trimToNull(conversationId);
        return normalizedConversationId == null ? UUID.randomUUID().toString() : normalizedConversationId;
    }

    private String normalizeMessage(String message) {
        String normalizedMessage = trimToNull(message);
        if (normalizedMessage == null) {
            throw validationException("Support chat message must not be empty");
        }

        if (normalizedMessage.length() > MESSAGE_MAX_LENGTH) {
            throw validationException("Support chat message is too long");
        }

        return normalizedMessage;
    }

    private String normalizeOptional(String value, int maxLength) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            return null;
        }

        return normalizedValue.length() > maxLength
                ? normalizedValue.substring(0, maxLength)
                : normalizedValue;
    }

    private List<SupportChatHistoryMessage> normalizeHistory(List<SupportChatHistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        return history.stream()
                .filter(message -> message != null && message.role() != null && trimToNull(message.content()) != null)
                .map(message -> new SupportChatHistoryMessage(
                        message.role(),
                        normalizeOptional(message.content(), MESSAGE_MAX_LENGTH)
                ))
                .limit(HISTORY_LIMIT)
                .toList();
    }

    private SupportConversation loadOrCreateConversation(User currentUser, SupportChatRequest request) {
        SupportConversation conversation = supportConversationRepository.findConversationByPublicId(request.conversationId())
                .orElseGet(() -> SupportConversation.start(
                        request.conversationId(),
                        currentUser == null ? null : currentUser.getId(),
                        request.sourcePage(),
                        request.locale()
                ));

        conversation.refreshMetadata(
                currentUser == null ? null : currentUser.getId(),
                request.sourcePage(),
                request.locale()
        );
        conversation.touch();
        conversation.prepareForCreation();
        return supportConversationRepository.saveConversation(conversation);
    }

    private void persistUserMessage(SupportConversation conversation, String message) {
        SupportConversationMessage userMessage = SupportConversationMessage.userMessage(conversation.getId(), message);
        userMessage.prepareForCreation();
        supportConversationRepository.saveMessage(userMessage);
    }

    private void persistAssistantMessage(SupportConversation conversation, String message) {
        conversation.touch();
        supportConversationRepository.saveConversation(conversation);

        SupportConversationMessage assistantMessage = SupportConversationMessage.assistantMessage(conversation.getId(), message);
        assistantMessage.prepareForCreation();
        supportConversationRepository.saveMessage(assistantMessage);
    }

    private String normalizeAssistantName(String assistantName) {
        String normalizedAssistantName = trimToNull(assistantName);
        return normalizedAssistantName == null ? DEFAULT_ASSISTANT_NAME : normalizedAssistantName;
    }

    private String normalizeAssistantMessage(String message) {
        String normalizedMessage = trimToNull(message);
        if (normalizedMessage == null) {
            throw validationException("Support assistant did not provide a response");
        }

        return normalizedMessage;
    }

    private List<String> normalizeSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of();
        }

        return suggestions.stream()
                .map(this::trimToNull)
                .filter(suggestion -> suggestion != null)
                .distinct()
                .limit(SUGGESTION_LIMIT)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private FoodRescueException validationException(String message) {
        return new FoodRescueException(FoodRescueException.Type.VALIDATION, message);
    }
}
