package sk.posam.fsa.foodrescue.domain.support;

import java.time.LocalDateTime;
import java.util.List;

public record SupportChatResponse(
        String conversationId,
        String assistantName,
        String message,
        LocalDateTime generatedAt,
        List<String> suggestions
) {
}
