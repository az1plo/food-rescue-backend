package sk.posam.fsa.foodrescue.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.offer.OfferCategory;
import sk.posam.fsa.foodrescue.domain.offerassistant.GeneratedOfferImage;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferAiProvider;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferDraftRequest;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferDraftSuggestion;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferIllustrativeCoverRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class OpenAiOfferAssistantProvider implements OfferAiProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OfferOpenAiProperties properties;

    public OpenAiOfferAssistantProvider(OfferOpenAiProperties properties) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(resolveTimeoutSeconds(properties)))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.properties = properties;
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @Override
    public OfferDraftSuggestion suggestDraft(OfferDraftRequest request) {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenAI offer assistant is not configured");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", trimToNull(properties.getDraftModel()) == null ? "gpt-4.1-mini" : properties.getDraftModel().trim());
        payload.put("input", List.of(Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "input_text", "text", buildDraftInstructions(request)),
                        Map.of("type", "input_image", "image_url", toDataUrl(request))
                )
        )));
        payload.put("store", false);
        payload.put("max_output_tokens", properties.getMaxOutputTokens());
        payload.put("temperature", properties.getTemperature());
        payload.put("text", Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "name", "offer_draft_suggestion",
                        "strict", true,
                        "schema", draftSuggestionSchema()
                )
        ));

        JsonNode response = sendJsonRequest(trimTrailingSlash(properties.getResponsesBaseUrl()), payload);
        JsonNode parsedResponse = readJson(extractOutputText(response));

        OfferCategory suggestedCategory = parseCategory(parsedResponse.path("suggestedCategory").asText(null));
        List<String> detectedItems = parsedResponse.path("detectedItems").isArray()
                ? java.util.stream.StreamSupport.stream(parsedResponse.path("detectedItems").spliterator(), false)
                .map(node -> trimToNull(node.asText(null)))
                .filter(value -> value != null)
                .distinct()
                .limit(8)
                .toList()
                : List.of();

        return new OfferDraftSuggestion(
                detectedItems,
                trimToNull(parsedResponse.path("suggestedTitle").asText(null)),
                trimToNull(parsedResponse.path("suggestedDescription").asText(null)),
                suggestedCategory == null ? OfferCategory.OTHER : suggestedCategory
        );
    }

    @Override
    public GeneratedOfferImage generateIllustrativeCover(OfferIllustrativeCoverRequest request) {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenAI offer assistant is not configured");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", trimToNull(properties.getImageModel()) == null ? "gpt-image-1" : properties.getImageModel().trim());
        payload.put("prompt", buildCoverPrompt(request));
        payload.put("size", trimToNull(properties.getImageSize()) == null ? "1024x1024" : properties.getImageSize().trim());
        payload.put("quality", trimToNull(properties.getImageQuality()) == null ? "medium" : properties.getImageQuality().trim());

        JsonNode response = sendJsonRequest(trimTrailingSlash(properties.getImagesBaseUrl()), payload);
        JsonNode dataNode = response.path("data");
        if (!dataNode.isArray() || dataNode.isEmpty()) {
            throw new IllegalStateException("OpenAI cover image response did not include image data");
        }

        String imageBase64 = trimToNull(dataNode.get(0).path("b64_json").asText(null));
        if (imageBase64 == null) {
            throw new IllegalStateException("OpenAI cover image response did not include base64 content");
        }

        return new GeneratedOfferImage(
                "offer-cover-" + java.util.UUID.randomUUID().toString().replace("-", "") + ".png",
                "image/png",
                imageBase64,
                true
        );
    }

    private JsonNode sendJsonRequest(String url, Map<String, Object> payload) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
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
            throw new IllegalStateException("OpenAI offer assistant request failed", ex);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI offer assistant request failed with status " + response.statusCode() + ": " + response.body());
        }

        return readJson(response.body());
    }

    private String buildDraftInstructions(OfferDraftRequest request) {
        String businessName = trimToNull(request.businessName());
        return "You are preparing an offer draft for the Savr food rescue marketplace. "
                + "Analyze the uploaded food photo and return only visible-food suggestions as JSON. "
                + "Do not invent price, quantity, pickup windows, allergens, or certification claims. "
                + "Keep the copy short, customer-friendly, and realistic for a rescue offer. "
                + "Return detectedItems as sellable offer items only, never as a list of ingredients. "
                + "If the image shows a prepared dish such as a salad, soup, pasta, pizza, bowl, sandwich, wrap, sushi, or similar meal, "
                + "return one grouped sellable item only, for example 'Mixed salad bowl', 'Pasta dish', 'Pizza portion', 'Prepared meal portion', or 'Sandwich'. "
                + "Do not return visible ingredients such as lettuce, cucumber, or tomato as separate detectedItems. "
                + "Visible components may appear only in suggestedDescription using cautious wording such as 'with visible lettuce, cucumber and tomato' or 'appears to include'. "
                + "Do not state exact ingredients as confirmed facts. "
                + "If the image shows separate individual products such as croissants, donuts, bread rolls, packaged items, or clearly multiple sandwiches, "
                + "return separate detectedItems with approximate visible counts in the string format 'Croissant x3' or 'Sandwich x2'. "
                + "If you are unsure whether the photo shows a prepared dish or separate products, prefer one grouped sellable item such as "
                + "'Mixed salad bowl', 'Mixed bakery box', 'Assorted meal box', or 'Prepared meal portion'. "
                + "Never use ingredient counts or visible counts as the final offer quantity; the business will confirm quantity manually. "
                + "Choose the best category from the provided enum only. "
                + "Business name context: " + (businessName == null ? "unknown" : businessName) + ".";
    }

    private String buildCoverPrompt(OfferIllustrativeCoverRequest request) {
        String title = trimToNull(request.title()) == null ? "Rescue offer" : request.title().trim();
        String description = trimToNull(request.description());
        String items = request.detectedItems() == null || request.detectedItems().isEmpty()
                ? "mixed food selection"
                : String.join(", ", request.detectedItems());
        String category = request.category() == null ? OfferCategory.OTHER.name() : request.category().name();

        return "Create an illustrative, polished marketplace cover image for a food rescue offer. "
                + "This must be illustrative only, not a literal guarantee of exact contents. "
                + "No price text, no pickup text, no brand logos, no packaging labels, no allergens claims. "
                + "Use a warm editorial food styling approach with a clean composition suitable for a card thumbnail. "
                + "Offer title: " + title + ". "
                + "Category: " + category + ". "
                + "Visible or suggested items: " + items + ". "
                + (description == null ? "" : "Description context: " + description + ". ");
    }

    private String toDataUrl(OfferDraftRequest request) {
        String contentType = request.image() == null ? null : trimToNull(request.image().contentType());
        byte[] bytes = request.image() == null ? new byte[0] : request.image().copyBytes();
        return "data:" + (contentType == null ? "image/png" : contentType) + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private Map<String, Object> draftSuggestionSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "detectedItems", Map.of(
                                "type", "array",
                                "description", "Sellable detected offer items only. Prepared dishes must be grouped into one item. Separate individual products may include approximate visible counts like 'Croissant x3'. Never list ingredients separately.",
                                "items", Map.of(
                                        "type", "string",
                                        "maxLength", 80
                                ),
                                "maxItems", 8
                        ),
                        "suggestedTitle", Map.of(
                                "type", "string"
                        ),
                        "suggestedDescription", Map.of(
                                "type", List.of("string", "null")
                        ),
                        "suggestedCategory", Map.of(
                                "type", "string",
                                "enum", List.of(
                                        "BAKERY",
                                        "READY_MEAL",
                                        "PRODUCE",
                                        "GROCERY",
                                        "DESSERT",
                                        "BEVERAGE",
                                        "MIXED",
                                        "OTHER"
                                )
                        )
                ),
                "required", List.of("detectedItems", "suggestedTitle", "suggestedDescription", "suggestedCategory"),
                "additionalProperties", false
        );
    }

    private String extractOutputText(JsonNode response) {
        String topLevelOutputText = trimToNull(response.path("output_text").asText(null));
        if (topLevelOutputText != null) {
            return topLevelOutputText;
        }

        JsonNode outputNode = response.path("output");
        if (!outputNode.isArray()) {
            throw new IllegalStateException("OpenAI draft response did not contain output text");
        }

        for (JsonNode outputItem : outputNode) {
            JsonNode content = outputItem.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                String text = trimToNull(contentItem.path("text").asText(null));
                if (text != null) {
                    return text;
                }
            }
        }

        throw new IllegalStateException("OpenAI draft response did not contain output text");
    }

    private OfferCategory parseCategory(String rawCategory) {
        String normalizedCategory = trimToNull(rawCategory);
        if (normalizedCategory == null) {
            return OfferCategory.OTHER;
        }

        try {
            return OfferCategory.valueOf(normalizedCategory.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return OfferCategory.OTHER;
        }
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
            throw new IllegalStateException("Unable to serialize OpenAI offer assistant payload", ex);
        }
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

    private int resolveTimeoutSeconds(OfferOpenAiProperties properties) {
        Integer timeoutSeconds = properties.getTimeoutSeconds();
        return timeoutSeconds == null || timeoutSeconds <= 0 ? 45 : timeoutSeconds;
    }
}
