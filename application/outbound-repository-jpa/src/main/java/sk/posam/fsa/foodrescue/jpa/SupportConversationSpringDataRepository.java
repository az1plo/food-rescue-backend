package sk.posam.fsa.foodrescue.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.posam.fsa.foodrescue.domain.support.SupportConversation;

import java.util.Optional;

interface SupportConversationSpringDataRepository extends JpaRepository<SupportConversation, Long> {

    Optional<SupportConversation> findByPublicId(String publicId);
}
