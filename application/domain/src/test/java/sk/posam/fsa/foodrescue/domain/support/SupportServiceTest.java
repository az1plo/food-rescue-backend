package sk.posam.fsa.foodrescue.domain.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.user.UserRole;
import sk.posam.fsa.foodrescue.domain.user.UserStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportServiceTest {

    @Mock
    private SupportAssistantProvider supportAssistantProvider;

    @Mock
    private SupportConversationRepository supportConversationRepository;

    @Test
    void replyCreatesConversationPersistsMessagesAndNormalizesAssistantReply() {
        User currentUser = activeUser(7L);
        SupportChatRequest request = new SupportChatRequest(
                "  ",
                "  Need help with pickup  ",
                "  /offers/7  ",
                "  sk-SK  ",
                List.of()
        );
        SupportConversationMessage priorMessage = SupportConversationMessage.assistantMessage(77L, "Earlier context");
        priorMessage.prepareForCreation();

        when(supportConversationRepository.findConversationByPublicId(any())).thenReturn(Optional.empty());
        when(supportConversationRepository.saveConversation(any(SupportConversation.class))).thenAnswer(invocation -> {
            SupportConversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) {
                conversation.setId(77L);
            }
            return conversation;
        });
        when(supportConversationRepository.findLatestMessages(77L, 12)).thenReturn(List.of(priorMessage));
        when(supportAssistantProvider.reply(any(SupportAssistantPrompt.class))).thenReturn(
                new SupportAssistantReply(
                        "   ",
                        "  Here is what to do next.  ",
                        Arrays.asList(" Track order ", null, "Track order", "", "Pickup tips", "Business help", "More")
                )
        );

        SupportService service = new SupportService(supportAssistantProvider, supportConversationRepository);

        SupportChatResponse response = service.reply(currentUser, request);

        assertNotNull(response.conversationId());
        assertFalse(response.conversationId().isBlank());
        assertEquals("Mika", response.assistantName());
        assertEquals("Here is what to do next.", response.message());
        assertEquals(List.of("Track order", "Pickup tips", "Business help", "More"), response.suggestions());
        assertNotNull(response.generatedAt());

        ArgumentCaptor<SupportAssistantPrompt> promptCaptor = ArgumentCaptor.forClass(SupportAssistantPrompt.class);
        verify(supportAssistantProvider).reply(promptCaptor.capture());

        SupportAssistantPrompt prompt = promptCaptor.getValue();
        assertEquals(response.conversationId(), prompt.conversationId());
        assertEquals("Need help with pickup", prompt.message());
        assertEquals("/offers/7", prompt.sourcePage());
        assertEquals("sk-SK", prompt.locale());
        assertEquals(List.of(priorMessage), prompt.history());
        assertEquals(currentUser, prompt.currentUser());

        ArgumentCaptor<SupportConversationMessage> messageCaptor = ArgumentCaptor.forClass(SupportConversationMessage.class);
        verify(supportConversationRepository, times(2)).saveMessage(messageCaptor.capture());

        List<SupportConversationMessage> savedMessages = messageCaptor.getAllValues();
        assertEquals(SupportConversationMessageRole.USER, savedMessages.get(0).getRole());
        assertEquals("Need help with pickup", savedMessages.get(0).getContent());
        assertEquals(77L, savedMessages.get(0).getConversationId());
        assertEquals(SupportConversationMessageRole.ASSISTANT, savedMessages.get(1).getRole());
        assertEquals("Here is what to do next.", savedMessages.get(1).getContent());
        assertEquals(77L, savedMessages.get(1).getConversationId());

        verify(supportConversationRepository, times(2)).saveConversation(any(SupportConversation.class));
        verify(supportConversationRepository).findLatestMessages(77L, 12);
    }

    @Test
    void replyRefreshesExistingConversationMetadata() {
        User currentUser = activeUser(12L);
        SupportConversation existingConversation = SupportConversation.start("conv-42", null, "/old-page", "en");
        existingConversation.setId(42L);
        existingConversation.prepareForCreation();
        SupportChatRequest request = new SupportChatRequest(
                " conv-42 ",
                "Hi there",
                " /orders/42 ",
                " en-GB ",
                List.of()
        );

        when(supportConversationRepository.findConversationByPublicId("conv-42")).thenReturn(Optional.of(existingConversation));
        when(supportConversationRepository.saveConversation(any(SupportConversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(supportConversationRepository.findLatestMessages(42L, 12)).thenReturn(List.of());
        when(supportAssistantProvider.reply(any(SupportAssistantPrompt.class))).thenReturn(
                new SupportAssistantReply("Mika", "Hello!", List.of())
        );

        SupportService service = new SupportService(supportAssistantProvider, supportConversationRepository);

        SupportChatResponse response = service.reply(currentUser, request);

        assertEquals("conv-42", response.conversationId());
        assertEquals(12L, existingConversation.getUserId());
        assertEquals("/orders/42", existingConversation.getSourcePage());
        assertEquals("en-GB", existingConversation.getLocale());
    }

    @Test
    void replyRejectsBlankMessage() {
        SupportChatRequest request = new SupportChatRequest(
                null,
                "   ",
                null,
                null,
                List.of()
        );

        SupportService service = new SupportService(supportAssistantProvider, supportConversationRepository);

        FoodRescueException exception = assertThrows(
                FoodRescueException.class,
                () -> service.reply(activeUser(7L), request)
        );

        assertEquals(FoodRescueException.Type.VALIDATION, exception.getType());
        assertTrue(exception.getMessage().contains("must not be empty"));
    }

    private User activeUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
