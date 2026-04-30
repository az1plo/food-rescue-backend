package sk.posam.fsa.foodrescue.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.posam.fsa.foodrescue.domain.support.SupportConversationMessage;

import java.util.List;

interface SupportConversationMessageSpringDataRepository extends JpaRepository<SupportConversationMessage, Long> {

    List<SupportConversationMessage> findAllByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
