package sk.posam.fsa.foodrescue.domain.support;

import java.util.Map;

public record SupportAssistantToolDefinition(
        String name,
        String description,
        Map<String, Object> parametersSchema
) {
}
