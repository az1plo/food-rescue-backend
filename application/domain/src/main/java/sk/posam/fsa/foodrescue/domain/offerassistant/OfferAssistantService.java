package sk.posam.fsa.foodrescue.domain.offerassistant;

import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferCategory;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OfferAssistantService implements OfferAssistantFacade {

    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private static final int MAX_TEXT_LENGTH = 1000;
    private static final int MAX_DETECTED_ITEMS = 8;
    private static final int MAX_DETECTED_ITEM_NAME_LENGTH = 80;
    private static final Pattern DETECTED_ITEM_TRAILING_COUNT = Pattern.compile("^(.+?)\\s*[x×]\\s*(\\d{1,2})$");
    private static final Pattern DETECTED_ITEM_LEADING_COUNT = Pattern.compile("^(\\d{1,2})\\s*[x×]\\s*(.+)$");
    private static final List<String> PREPARED_DISH_KEYWORDS = List.of(
            "salad", "soup", "pasta", "pizza", "bowl", "sandwich", "burger", "wrap", "meal",
            "dish", "plate", "pasta", "noodle", "spaghetti", "lasagna", "poke", "sushi", "panini"
    );
    private static final List<String> SEPARATE_PRODUCT_KEYWORDS = List.of(
            "croissant", "donut", "doughnut", "bread roll", "bread", "baguette", "roll", "bun",
            "bagel", "muffin", "cookie", "pastry", "sandwich", "packaged", "box", "cup", "bottle"
    );
    private static final List<String> INGREDIENT_KEYWORDS = List.of(
            "lettuce", "cucumber", "tomato", "onion", "pepper", "paprika", "spinach", "greens",
            "carrot", "olive", "olives", "corn", "avocado", "cheese", "egg", "eggs", "beans",
            "croutons", "dressing", "sauce", "topping", "toppings"
    );
    private static final List<String> GENERIC_GROUP_ONLY_KEYWORDS = List.of(
            "prepared meal", "chef selection", "side dish", "mixed food selection", "fresh produce", "greens"
    );

    private final OfferAiProvider offerAiProvider;
    private final OfferImageStorage offerImageStorage;
    private final BusinessRepository businessRepository;

    public OfferAssistantService(OfferAiProvider offerAiProvider,
                                 OfferImageStorage offerImageStorage,
                                 BusinessRepository businessRepository) {
        this.offerAiProvider = offerAiProvider;
        this.offerImageStorage = offerImageStorage;
        this.businessRepository = businessRepository;
    }

    @Override
    public OfferDraftSuggestion createDraftFromImage(User currentUser, OfferDraftRequest request) {
        ensureActiveUser(currentUser, "Only active users can create AI offer drafts");
        if (request == null) {
            throw validationException("Offer draft request must be provided");
        }

        Business business = resolveManagedBusiness(currentUser, request.businessId());
        OfferImageUpload image = normalizeUpload(request.image(), "Offer draft image");
        OfferDraftSuggestion suggestion = offerAiProvider.suggestDraft(new OfferDraftRequest(
                business.getId(),
                business.getName(),
                image
        ));
        return normalizeDraftSuggestion(suggestion);
    }

    @Override
    public GeneratedOfferImage generateIllustrativeCover(User currentUser, OfferIllustrativeCoverRequest request) {
        ensureActiveUser(currentUser, "Only active users can generate illustrative offer covers");
        if (request == null) {
            throw validationException("Illustrative cover request must be provided");
        }

        resolveManagedBusiness(currentUser, request.businessId());
        GeneratedOfferImage generatedImage = offerAiProvider.generateIllustrativeCover(normalizeCoverRequest(request));
        return normalizeGeneratedImage(generatedImage);
    }

    @Override
    public StoredOfferImage uploadOfferImage(User currentUser,
                                             Long businessId,
                                             OfferImageUpload upload,
                                             boolean illustrativeImage) {
        ensureActiveUser(currentUser, "Only active users can upload offer images");
        resolveManagedBusiness(currentUser, businessId);
        return offerImageStorage.store(normalizeUpload(upload, "Offer image"), illustrativeImage);
    }

    @Override
    public StoredOfferImageContent getOfferImage(String imageId) {
        String normalizedImageId = trimToNull(imageId);
        if (normalizedImageId == null) {
            throw new FoodRescueException(FoodRescueException.Type.NOT_FOUND, "Offer image was not found");
        }
        return offerImageStorage.read(normalizedImageId);
    }

    private Business resolveManagedBusiness(User currentUser, Long businessId) {
        if (businessId == null) {
            throw validationException("Business is required");
        }

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new FoodRescueException(
                        FoodRescueException.Type.NOT_FOUND,
                        "Business with id=" + businessId + " was not found"
                ));

        if (!business.canBeManagedBy(currentUser)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.FORBIDDEN,
                    "You are not allowed to manage offer AI actions for this business"
            );
        }

        return business;
    }

    private void ensureActiveUser(User currentUser, String message) {
        if (currentUser == null || !currentUser.isActive()) {
            throw new FoodRescueException(FoodRescueException.Type.FORBIDDEN, message);
        }
    }

    private OfferImageUpload normalizeUpload(OfferImageUpload upload, String label) {
        if (upload == null) {
            throw validationException(label + " must be provided");
        }

        String fileName = trimToNull(upload.fileName());
        String contentType = trimToNull(upload.contentType());
        byte[] bytes = upload.copyBytes();

        if (fileName == null) {
            throw validationException(label + " file name is required");
        }
        if (contentType == null) {
            throw validationException(label + " content type is required");
        }
        if (bytes.length == 0) {
            throw validationException(label + " content must not be empty");
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw validationException(label + " is too large");
        }
        if (!isSupportedImageType(contentType)) {
            throw validationException(label + " content type is not supported");
        }

        return new OfferImageUpload(fileName, contentType, bytes);
    }

    private OfferDraftSuggestion normalizeDraftSuggestion(OfferDraftSuggestion suggestion) {
        if (suggestion == null) {
            throw validationException("AI draft suggestion was not returned");
        }

        String suggestedTitle = limitText(suggestion.suggestedTitle(), 160, "AI suggested title");
        String suggestedDescription = limitOptionalText(suggestion.suggestedDescription(), MAX_TEXT_LENGTH);
        OfferCategory suggestedCategory = suggestion.suggestedCategory() == null
                ? OfferCategory.OTHER
                : suggestion.suggestedCategory();

        return new OfferDraftSuggestion(
                normalizeDetectedItems(
                        suggestion.detectedItems(),
                        suggestedTitle,
                        suggestedDescription,
                        suggestedCategory
                ),
                suggestedTitle,
                suggestedDescription,
                suggestedCategory
        );
    }

    private OfferIllustrativeCoverRequest normalizeCoverRequest(OfferIllustrativeCoverRequest request) {
        String title = limitText(request.title(), 160, "Illustrative cover title");
        String description = limitOptionalText(request.description(), MAX_TEXT_LENGTH);
        java.util.List<String> detectedItems = request.detectedItems() == null ? java.util.List.of() : request.detectedItems().stream()
                .map(this::trimToNull)
                .filter(item -> item != null)
                .distinct()
                .limit(8)
                .toList();

        return new OfferIllustrativeCoverRequest(
                request.businessId(),
                title,
                description,
                request.category() == null ? sk.posam.fsa.foodrescue.domain.offer.OfferCategory.OTHER : request.category(),
                detectedItems
        );
    }

    private GeneratedOfferImage normalizeGeneratedImage(GeneratedOfferImage image) {
        if (image == null) {
            throw validationException("AI cover image was not returned");
        }

        String fileName = trimToNull(image.fileName());
        String contentType = trimToNull(image.contentType());
        String base64Data = trimToNull(image.base64Data());

        if (fileName == null || contentType == null || base64Data == null) {
            throw validationException("AI cover image response is incomplete");
        }
        if (!isSupportedImageType(contentType)) {
            throw validationException("AI cover image type is not supported");
        }

        return new GeneratedOfferImage(fileName, contentType, base64Data, true);
    }

    private boolean isSupportedImageType(String contentType) {
        return "image/jpeg".equalsIgnoreCase(contentType)
                || "image/png".equalsIgnoreCase(contentType)
                || "image/webp".equalsIgnoreCase(contentType)
                || "image/svg+xml".equalsIgnoreCase(contentType);
    }

    private String limitText(String value, int maxLength, String label) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            throw validationException(label + " is required");
        }
        return normalizedValue.length() <= maxLength
                ? normalizedValue
                : normalizedValue.substring(0, maxLength);
    }

    private String limitOptionalText(String value, int maxLength) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            return null;
        }
        return normalizedValue.length() <= maxLength
                ? normalizedValue
                : normalizedValue.substring(0, maxLength);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private List<String> normalizeDetectedItems(List<String> detectedItems,
                                                String suggestedTitle,
                                                String suggestedDescription,
                                                OfferCategory suggestedCategory) {
        List<DetectedItemCandidate> parsedDetectedItems = detectedItems == null ? List.of() : detectedItems.stream()
                .map(this::parseDetectedItemCandidate)
                .filter(item -> item != null)
                .limit(MAX_DETECTED_ITEMS)
                .toList();

        if (parsedDetectedItems.isEmpty()) {
            return List.of(formatDetectedItemLabel(buildGroupedSellableItemLabel(
                    suggestedTitle,
                    suggestedDescription,
                    suggestedCategory,
                    List.of()
            ), 1));
        }

        if (shouldGroupPreparedDish(parsedDetectedItems, suggestedTitle, suggestedDescription, suggestedCategory)) {
            return List.of(formatDetectedItemLabel(buildGroupedSellableItemLabel(
                    suggestedTitle,
                    suggestedDescription,
                    suggestedCategory,
                    parsedDetectedItems
            ), 1));
        }

        LinkedHashSet<String> normalizedLabels = new LinkedHashSet<>();
        for (DetectedItemCandidate item : parsedDetectedItems) {
            normalizedLabels.add(formatDetectedItemLabel(item.name(), item.quantity()));
        }
        return new ArrayList<>(normalizedLabels);
    }

    private DetectedItemCandidate parseDetectedItemCandidate(String value) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            return null;
        }

        Matcher trailingCountMatcher = DETECTED_ITEM_TRAILING_COUNT.matcher(normalizedValue);
        if (trailingCountMatcher.matches()) {
            return createDetectedItemCandidate(trailingCountMatcher.group(1), trailingCountMatcher.group(2));
        }

        Matcher leadingCountMatcher = DETECTED_ITEM_LEADING_COUNT.matcher(normalizedValue);
        if (leadingCountMatcher.matches()) {
            return createDetectedItemCandidate(leadingCountMatcher.group(2), leadingCountMatcher.group(1));
        }

        String normalizedName = normalizeDetectedItemName(normalizedValue);
        return normalizedName == null ? null : new DetectedItemCandidate(normalizedName, 1);
    }

    private DetectedItemCandidate createDetectedItemCandidate(String rawName, String rawQuantity) {
        String normalizedName = normalizeDetectedItemName(rawName);
        if (normalizedName == null) {
            return null;
        }

        int quantity = 1;
        try {
            quantity = Math.max(1, Math.min(99, Integer.parseInt(rawQuantity)));
        } catch (NumberFormatException ignored) {
            quantity = 1;
        }

        return new DetectedItemCandidate(normalizedName, quantity);
    }

    private String normalizeDetectedItemName(String rawName) {
        String normalizedName = trimToNull(rawName);
        if (normalizedName == null) {
            return null;
        }

        normalizedName = normalizedName
                .replace('×', 'x')
                .replaceAll("[,;:.]+$", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalizedName.isEmpty()) {
            return null;
        }

        if (normalizedName.length() > MAX_DETECTED_ITEM_NAME_LENGTH) {
            normalizedName = normalizedName.substring(0, MAX_DETECTED_ITEM_NAME_LENGTH).trim();
        }

        return Character.toUpperCase(normalizedName.charAt(0)) + normalizedName.substring(1);
    }

    private boolean shouldGroupPreparedDish(List<DetectedItemCandidate> detectedItems,
                                            String suggestedTitle,
                                            String suggestedDescription,
                                            OfferCategory suggestedCategory) {
        if (detectedItems.size() <= 1) {
            return false;
        }

        String combinedContext = ((suggestedTitle == null ? "" : suggestedTitle) + " "
                + (suggestedDescription == null ? "" : suggestedDescription)).toLowerCase(Locale.ROOT);
        boolean preparedDishContext = containsAnyKeyword(combinedContext, PREPARED_DISH_KEYWORDS);
        boolean separateProductContext = detectedItems.stream()
                .anyMatch(item -> item.quantity() > 1 || containsAnyKeyword(item.name().toLowerCase(Locale.ROOT), SEPARATE_PRODUCT_KEYWORDS));
        boolean ingredientOnlyContext = detectedItems.stream()
                .allMatch(item -> isLikelyIngredientItem(item.name()) || isGroupOnlyDetectedItem(item.name()));
        boolean readyMealContext = suggestedCategory == OfferCategory.READY_MEAL;
        boolean genericGroupedContext = detectedItems.stream()
                .anyMatch(item -> isGroupOnlyDetectedItem(item.name()));

        return preparedDishContext
                || ingredientOnlyContext
                || (readyMealContext && !separateProductContext)
                || (genericGroupedContext && !separateProductContext);
    }

    private String buildGroupedSellableItemLabel(String suggestedTitle,
                                                 String suggestedDescription,
                                                 OfferCategory suggestedCategory,
                                                 List<DetectedItemCandidate> detectedItems) {
        String combinedContext = ((suggestedTitle == null ? "" : suggestedTitle) + " "
                + (suggestedDescription == null ? "" : suggestedDescription)).toLowerCase(Locale.ROOT);
        boolean ingredientHeavy = detectedItems.stream().filter(item -> isLikelyIngredientItem(item.name())).count() >= 2;

        if (combinedContext.contains("salad") || (ingredientHeavy && suggestedCategory == OfferCategory.PRODUCE)) {
            return "Mixed salad bowl";
        }
        if (combinedContext.contains("soup")) {
            return "Soup portion";
        }
        if (containsAnyKeyword(combinedContext, List.of("pasta", "spaghetti", "noodle", "lasagna"))) {
            return "Pasta dish";
        }
        if (combinedContext.contains("pizza")) {
            return combinedContext.contains("slice") ? "Pizza slice" : "Pizza portion";
        }
        if (containsAnyKeyword(combinedContext, List.of("sandwich", "panini", "toastie"))) {
            return "Sandwich";
        }
        if (combinedContext.contains("burger")) {
            return "Burger";
        }
        if (combinedContext.contains("wrap")) {
            return "Wrap";
        }
        if (containsAnyKeyword(combinedContext, List.of("bowl", "poke"))) {
            return "Prepared bowl";
        }
        if (combinedContext.contains("sushi")) {
            return "Sushi portion";
        }
        if (suggestedCategory == OfferCategory.BAKERY) {
            return "Mixed bakery box";
        }
        if (suggestedCategory == OfferCategory.PRODUCE) {
            return ingredientHeavy ? "Mixed salad bowl" : "Fresh produce bundle";
        }
        if (suggestedCategory == OfferCategory.DESSERT) {
            return "Dessert portion";
        }
        if (suggestedCategory == OfferCategory.READY_MEAL) {
            return "Prepared meal portion";
        }
        if (suggestedCategory == OfferCategory.GROCERY) {
            return "Grocery selection";
        }
        return "Assorted meal box";
    }

    private boolean isLikelyIngredientItem(String value) {
        return containsAnyKeyword(value.toLowerCase(Locale.ROOT), INGREDIENT_KEYWORDS);
    }

    private boolean isGroupOnlyDetectedItem(String value) {
        return containsAnyKeyword(value.toLowerCase(Locale.ROOT), GENERIC_GROUP_ONLY_KEYWORDS);
    }

    private boolean containsAnyKeyword(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String formatDetectedItemLabel(String name, int quantity) {
        return quantity <= 1 ? name : name + " x" + quantity;
    }

    private FoodRescueException validationException(String message) {
        return new FoodRescueException(FoodRescueException.Type.VALIDATION, message);
    }

    private record DetectedItemCandidate(String name, int quantity) {
    }
}
