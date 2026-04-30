package sk.posam.fsa.foodrescue.domain.support;

import java.util.Map;

public interface SupportAssistantTool {

    SupportAssistantToolDefinition definition();

    Object execute(Map<String, Object> arguments, SupportAssistantToolContext context);
}
