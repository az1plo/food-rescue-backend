package sk.posam.fsa.foodrescue.domain.support;

import java.util.List;

public record SupportAssistantReply(
        String assistantName,
        String message,
        List<String> suggestions
) {
}
