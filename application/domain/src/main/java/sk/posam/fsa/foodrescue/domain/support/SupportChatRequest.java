package sk.posam.fsa.foodrescue.domain.support;

import java.util.List;

public record SupportChatRequest(
        String conversationId,
        String message,
        String sourcePage,
        String locale,
        List<SupportChatHistoryMessage> history
) {
}
