package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.support.SupportChatHistoryMessage;
import sk.posam.fsa.foodrescue.domain.support.SupportChatHistoryMessageRole;
import sk.posam.fsa.foodrescue.domain.support.SupportChatRequest;
import sk.posam.fsa.foodrescue.domain.support.SupportChatResponse;
import sk.posam.fsa.foodrescue.rest.dto.SupportChatHistoryMessageDto;
import sk.posam.fsa.foodrescue.rest.dto.SupportChatHistoryMessageRoleDto;
import sk.posam.fsa.foodrescue.rest.dto.SupportChatRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.SupportChatResponseDto;

import java.util.List;

@Component
public class SupportChatMapper {

    public SupportChatRequest toDomain(SupportChatRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return new SupportChatRequest(
                dto.getConversationId(),
                dto.getMessage(),
                dto.getSourcePage(),
                dto.getLocale(),
                dto.getHistory() == null ? List.of() : dto.getHistory().stream()
                        .map(this::toDomain)
                        .toList()
        );
    }

    public SupportChatResponseDto toDto(SupportChatResponse response) {
        if (response == null) {
            return null;
        }

        SupportChatResponseDto dto = new SupportChatResponseDto();
        dto.setConversationId(response.conversationId());
        dto.setAssistantName(response.assistantName());
        dto.setMessage(response.message());
        dto.setGeneratedAt(ApiDateTimeMapper.toUtcOffsetDateTime(response.generatedAt()));
        dto.setSuggestions(response.suggestions());
        return dto;
    }

    private SupportChatHistoryMessage toDomain(SupportChatHistoryMessageDto dto) {
        if (dto == null) {
            return null;
        }

        return new SupportChatHistoryMessage(
                dto.getRole() == null ? null : SupportChatHistoryMessageRole.valueOf(dto.getRole().name()),
                dto.getContent()
        );
    }
}
