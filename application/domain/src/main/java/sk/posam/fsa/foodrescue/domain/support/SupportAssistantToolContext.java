package sk.posam.fsa.foodrescue.domain.support;

import sk.posam.fsa.foodrescue.domain.user.User;

public record SupportAssistantToolContext(
        String conversationId,
        String sourcePage,
        String locale,
        User currentUser
) {
}
