package sk.posam.fsa.foodrescue.domain.support;

import java.util.List;
import java.util.Optional;

public interface SupportConversationRepository {

    SupportConversation saveConversation(SupportConversation conversation);

    SupportConversationMessage saveMessage(SupportConversationMessage message);

    Optional<SupportConversation> findConversationByPublicId(String publicId);

    List<SupportConversationMessage> findLatestMessages(Long conversationId, int limit);
}
