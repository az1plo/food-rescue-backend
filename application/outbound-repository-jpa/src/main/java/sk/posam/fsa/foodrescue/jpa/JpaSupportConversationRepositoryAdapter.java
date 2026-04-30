package sk.posam.fsa.foodrescue.jpa;

import org.springframework.stereotype.Repository;
import sk.posam.fsa.foodrescue.domain.support.SupportConversation;
import sk.posam.fsa.foodrescue.domain.support.SupportConversationMessage;
import sk.posam.fsa.foodrescue.domain.support.SupportConversationRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaSupportConversationRepositoryAdapter implements SupportConversationRepository {

    private final SupportConversationSpringDataRepository supportConversationSpringDataRepository;
    private final SupportConversationMessageSpringDataRepository supportConversationMessageSpringDataRepository;

    public JpaSupportConversationRepositoryAdapter(SupportConversationSpringDataRepository supportConversationSpringDataRepository,
                                                   SupportConversationMessageSpringDataRepository supportConversationMessageSpringDataRepository) {
        this.supportConversationSpringDataRepository = supportConversationSpringDataRepository;
        this.supportConversationMessageSpringDataRepository = supportConversationMessageSpringDataRepository;
    }

    @Override
    public SupportConversation saveConversation(SupportConversation conversation) {
        return supportConversationSpringDataRepository.save(conversation);
    }

    @Override
    public SupportConversationMessage saveMessage(SupportConversationMessage message) {
        return supportConversationMessageSpringDataRepository.save(message);
    }

    @Override
    public Optional<SupportConversation> findConversationByPublicId(String publicId) {
        return supportConversationSpringDataRepository.findByPublicId(publicId);
    }

    @Override
    public List<SupportConversationMessage> findLatestMessages(Long conversationId, int limit) {
        if (conversationId == null || limit <= 0) {
            return List.of();
        }

        return supportConversationMessageSpringDataRepository.findAllByConversationIdOrderByCreatedAtDesc(conversationId).stream()
                .limit(limit)
                .sorted(Comparator.comparing(SupportConversationMessage::getCreatedAt))
                .toList();
    }
}
