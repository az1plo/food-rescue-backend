package sk.posam.fsa.foodrescue.domain.support;

import sk.posam.fsa.foodrescue.domain.user.User;

import java.util.List;

public record SupportAssistantPrompt(
        String conversationId,
        String message,
        String sourcePage,
        String locale,
        List<SupportConversationMessage> history,
        User currentUser
) {
}
