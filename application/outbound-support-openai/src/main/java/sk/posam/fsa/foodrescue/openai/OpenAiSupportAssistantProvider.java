package sk.posam.fsa.foodrescue.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.support.SupportAssistantPrompt;
import sk.posam.fsa.foodrescue.domain.support.SupportAssistantProvider;
import sk.posam.fsa.foodrescue.domain.support.SupportAssistantReply;
import sk.posam.fsa.foodrescue.domain.support.SupportAssistantTool;
import sk.posam.fsa.foodrescue.domain.support.SupportAssistantToolContext;
import sk.posam.fsa.foodrescue.domain.support.SupportAssistantToolDefinition;
import sk.posam.fsa.foodrescue.domain.support.SupportConversationMessage;
import sk.posam.fsa.foodrescue.domain.support.SupportConversationMessageRole;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OpenAiSupportAssistantProvider implements SupportAssistantProvider {

    private static final String ASSISTANT_NAME = "Mika";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties properties;
    private final Map<String, SupportAssistantTool> toolsByName;

    public OpenAiSupportAssistantProvider(OpenAiProperties properties,
                                          List<SupportAssistantTool> tools) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(resolveTimeoutSeconds(properties)))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.properties = properties;
        this.toolsByName = tools == null
                ? Map.of()
                : tools.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        tool -> tool.definition().name(),
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @Override
    public SupportAssistantReply reply(SupportAssistantPrompt prompt) {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenAI support provider is not configured");
        }

        List<Object> input = new ArrayList<>(buildConversationInput(prompt));
        SupportAssistantToolContext toolContext = new SupportAssistantToolContext(
                prompt.conversationId(),
                prompt.sourcePage(),
                prompt.locale(),
                prompt.currentUser()
        );

        int round = 0;
        while (true) {
            JsonNode response = sendResponseRequest(prompt, input);
            List<JsonNode> outputItems = readOutputItems(response);
            List<JsonNode> functionCalls = outputItems.stream()
                    .filter(item -> "function_call".equals(item.path("type").asText()))
                    .toList();

            if (!functionCalls.isEmpty() && round < resolveMaxToolRounds(properties)) {
                for (JsonNode outputItem : outputItems) {
                    input.add(objectMapper.convertValue(outputItem, Object.class));
                }

                for (JsonNode functionCall : functionCalls) {
                    input.add(buildFunctionCallOutput(functionCall, toolContext));
                }

                round++;
                continue;
            }

            StructuredSupportReply structuredReply = parseStructuredReply(response);
            return new SupportAssistantReply(
                    structuredReply.assistantName(),
                    structuredReply.message(),
                    structuredReply.suggestions()
            );
        }
    }

    private JsonNode sendResponseRequest(SupportAssistantPrompt prompt, List<Object> input) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", trimToNull(properties.getModel()) == null ? "gpt-4.1-mini" : properties.getModel().trim());
        payload.put("instructions", buildInstructions(prompt));
        payload.put("input", input);
        payload.put("store", false);

        if (!toolsByName.isEmpty()) {
            payload.put("tools", toolsByName.values().stream()
                    .map(this::toOpenAiToolDefinition)
                    .toList());
            payload.put("tool_choice", "auto");
        }

        if (properties.getMaxOutputTokens() != null && properties.getMaxOutputTokens() > 0) {
            payload.put("max_output_tokens", properties.getMaxOutputTokens());
        }

        if (properties.getTemperature() != null) {
            payload.put("temperature", properties.getTemperature());
        }

        payload.put("text", Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "name", "support_assistant_reply",
                        "strict", true,
                        "schema", structuredReplySchema()
                )
        ));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(properties.getBaseUrl())))
                .timeout(Duration.ofSeconds(resolveTimeoutSeconds(properties)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey().trim())
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(payload)));

        String projectId = trimToNull(properties.getProjectId());
        if (projectId != null) {
            requestBuilder.header("OpenAI-Project", projectId);
        }

        String organizationId = trimToNull(properties.getOrganizationId());
        if (organizationId != null) {
            requestBuilder.header("OpenAI-Organization", organizationId);
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("OpenAI support request failed", ex);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI support request failed with status " + response.statusCode() + ": " + response.body());
        }

        return readJson(response.body());
    }

    private List<Object> buildConversationInput(SupportAssistantPrompt prompt) {
        if (prompt == null || prompt.history() == null || prompt.history().isEmpty()) {
            return prompt == null || trimToNull(prompt.message()) == null
                    ? List.of()
                    : List.<Object>of(Map.of("role", "user", "content", prompt.message().trim()));
        }

        List<Object> input = prompt.history().stream()
                .filter(Objects::nonNull)
                .filter(message -> message.getRole() == SupportConversationMessageRole.USER
                        || message.getRole() == SupportConversationMessageRole.ASSISTANT)
                .map(message -> (Object) toInputMessage(message))
                .toList();

        if (input.isEmpty() && trimToNull(prompt.message()) != null) {
            return List.<Object>of(Map.of("role", "user", "content", prompt.message().trim()));
        }

        return input;
    }

    private Map<String, Object> toInputMessage(SupportConversationMessage message) {
        return Map.of(
                "role", message.getRole() == SupportConversationMessageRole.ASSISTANT ? "assistant" : "user",
                "content", message.getContent()
        );
    }

    private Map<String, Object> toOpenAiToolDefinition(SupportAssistantTool tool) {
        SupportAssistantToolDefinition definition = tool.definition();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "function");
        payload.put("name", definition.name());
        payload.put("description", definition.description());
        payload.put("parameters", definition.parametersSchema());
        return payload;
    }

    private Map<String, Object> buildFunctionCallOutput(JsonNode functionCall, SupportAssistantToolContext context) {
        String toolName = functionCall.path("name").asText(null);
        String callId = functionCall.path("call_id").asText(null);
        String argumentsRaw = functionCall.path("arguments").asText("{}");

        SupportAssistantTool tool = toolsByName.get(toolName);
        Object toolOutput = tool == null
                ? Map.of("error", "Unknown tool: " + toolName)
                : tool.execute(readArguments(argumentsRaw), context);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("type", "function_call_output");
        output.put("call_id", callId);
        output.put("output", writeJson(toolOutput));
        return output;
    }

    private Map<String, Object> readArguments(String argumentsRaw) {
        if (argumentsRaw == null || argumentsRaw.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(argumentsRaw, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private StructuredSupportReply parseStructuredReply(JsonNode response) {
        String outputText = extractOutputText(response);
        if (outputText == null || outputText.isBlank()) {
            throw new IllegalStateException("OpenAI support response did not contain output text");
        }

        JsonNode jsonNode = readJson(outputText);
        String assistantName = trimToNull(jsonNode.path("assistantName").asText(null));
        String message = trimToNull(jsonNode.path("message").asText(null));
        List<String> suggestions = new ArrayList<>();
        JsonNode suggestionsNode = jsonNode.path("suggestions");
        if (suggestionsNode.isArray()) {
            for (JsonNode suggestionNode : suggestionsNode) {
                String suggestion = trimToNull(suggestionNode.asText(null));
                if (suggestion != null) {
                    suggestions.add(suggestion);
                }
            }
        }

        if (message == null) {
            throw new IllegalStateException("OpenAI support response did not include a message");
        }

        return new StructuredSupportReply(
                assistantName == null ? ASSISTANT_NAME : assistantName,
                message,
                suggestions.stream().distinct().limit(4).toList()
        );
    }

    private String extractOutputText(JsonNode response) {
        String topLevelOutputText = trimToNull(response.path("output_text").asText(null));
        if (topLevelOutputText != null) {
            return topLevelOutputText;
        }

        for (JsonNode outputItem : readOutputItems(response)) {
            JsonNode content = outputItem.path("content");
            if (content.isArray()) {
                for (JsonNode contentItem : content) {
                    String text = trimToNull(contentItem.path("text").asText(null));
                    if (text != null) {
                        return text;
                    }
                }
            }
        }

        return null;
    }

    private List<JsonNode> readOutputItems(JsonNode response) {
        JsonNode outputNode = response.path("output");
        if (!outputNode.isArray()) {
            return List.of();
        }

        List<JsonNode> items = new ArrayList<>();
        outputNode.forEach(items::add);
        return items;
    }

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse OpenAI JSON response", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize OpenAI support payload", ex);
        }
    }

    private Map<String, Object> structuredReplySchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "assistantName", Map.of(
                                "type", "string",
                                "description", "The assistant display name."
                        ),
                        "message", Map.of(
                                "type", "string",
                                "description", "The final user-facing support reply."
                        ),
                        "suggestions", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "maxItems", 4
                        )
                ),
                "required", List.of("assistantName", "message", "suggestions"),
                "additionalProperties", false
        );
    }

    private String buildInstructions(SupportAssistantPrompt prompt) {
        StringBuilder instructions = new StringBuilder();
        instructions.append("You are Mika, the AI support assistant for the Savr platform. ")
                .append("Help users with marketplace browsing, reservations, pickup windows, business publishing, pricing, and account questions. ")
                .append("Use the available tools whenever the user asks about a specific offer, business, or their own reservations. ")
                .append("Do not invent live data, ids, pricing, statuses, pickup times, or reservation state. ")
                .append("If the tools do not provide enough information, say that clearly and offer the next best step. ")
                .append("Keep replies concise, practical, and friendly. ")
                .append("Reply in the user's language when it is clear from the conversation.");

        if (prompt != null) {
            String sourcePage = trimToNull(prompt.sourcePage());
            if (sourcePage != null) {
                instructions.append(" Current page: ").append(sourcePage).append(".");
            }

            String locale = trimToNull(prompt.locale());
            if (locale != null) {
                instructions.append(" Preferred locale: ").append(locale).append(".");
            }

            User currentUser = prompt.currentUser();
            if (currentUser != null) {
                instructions.append(" Signed-in user context: firstName=")
                        .append(Optional.ofNullable(trimToNull(currentUser.getFirstName())).orElse("unknown"))
                        .append(", role=")
                        .append(currentUser.getRole() == null ? "unknown" : currentUser.getRole().name())
                        .append(".");
            } else {
                instructions.append(" The user is currently anonymous unless tool results say otherwise.");
            }
        }

        return instructions.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isBlank() ? null : normalizedValue;
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }

    private int resolveTimeoutSeconds(OpenAiProperties properties) {
        Integer timeoutSeconds = properties.getTimeoutSeconds();
        return timeoutSeconds == null || timeoutSeconds <= 0 ? 30 : timeoutSeconds;
    }

    private int resolveMaxToolRounds(OpenAiProperties properties) {
        Integer maxToolRounds = properties.getMaxToolRounds();
        return maxToolRounds == null || maxToolRounds < 0 ? 0 : maxToolRounds;
    }

    private record StructuredSupportReply(
            String assistantName,
            String message,
            List<String> suggestions
    ) {
    }
}
