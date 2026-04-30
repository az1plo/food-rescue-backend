package sk.posam.fsa.foodrescue.domain.support;

public record SupportChatHistoryMessage(
        SupportChatHistoryMessageRole role,
        String content
) {
}
