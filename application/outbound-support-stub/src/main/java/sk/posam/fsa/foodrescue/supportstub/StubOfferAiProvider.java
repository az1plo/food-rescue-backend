package sk.posam.fsa.foodrescue.supportstub;

import sk.posam.fsa.foodrescue.domain.offer.OfferCategory;
import sk.posam.fsa.foodrescue.domain.offerassistant.GeneratedOfferImage;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferAiProvider;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferDraftRequest;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferDraftSuggestion;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferIllustrativeCoverRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class StubOfferAiProvider implements OfferAiProvider {

    @Override
    public OfferDraftSuggestion suggestDraft(OfferDraftRequest request) {
        String normalizedFileName = request.image() == null || request.image().fileName() == null
                ? ""
                : request.image().fileName().toLowerCase(Locale.ROOT);

        if (containsAny(normalizedFileName, "bread", "bakery", "croissant", "pastry")) {
            return new OfferDraftSuggestion(
                    List.of("Croissant x3", "Baguette x1", "Pastry x2"),
                    "Evening bakery rescue bag",
                    "A mixed bakery pickup with visible pastries and bread from the late-day selection.",
                    OfferCategory.BAKERY
            );
        }

        if (containsAny(normalizedFileName, "salad", "bowl")) {
            return new OfferDraftSuggestion(
                    List.of("Mixed salad bowl"),
                    "Fresh salad rescue bowl",
                    "A prepared salad that appears to include visible greens and vegetables in the bowl.",
                    OfferCategory.READY_MEAL
            );
        }

        if (containsAny(normalizedFileName, "fruit", "produce", "veggie")) {
            return new OfferDraftSuggestion(
                    List.of("Fresh produce bundle"),
                    "Fresh produce rescue bundle",
                    "A fresh assortment built around the visible produce in the photo.",
                    OfferCategory.PRODUCE
            );
        }

        if (containsAny(normalizedFileName, "sandwiches")) {
            return new OfferDraftSuggestion(
                    List.of("Sandwich x2"),
                    "Sandwich rescue offer",
                    "A sandwich offer that appears to include multiple prepared portions in the photo.",
                    OfferCategory.READY_MEAL
            );
        }

        if (containsAny(normalizedFileName, "sandwich", "panini")) {
            return new OfferDraftSuggestion(
                    List.of("Sandwich"),
                    "Sandwich rescue offer",
                    "A prepared sandwich offer that appears to include a filled sandwich in the photo.",
                    OfferCategory.READY_MEAL
            );
        }

        if (containsAny(normalizedFileName, "pasta", "spaghetti", "noodle")) {
            return new OfferDraftSuggestion(
                    List.of("Pasta dish"),
                    "Pasta rescue portion",
                    "A prepared pasta portion that appears to include visible sauce and toppings.",
                    OfferCategory.READY_MEAL
            );
        }

        if (containsAny(normalizedFileName, "pizza")) {
            return new OfferDraftSuggestion(
                    List.of("Pizza portion"),
                    "Pizza rescue portion",
                    "A prepared pizza portion that appears to include visible toppings.",
                    OfferCategory.READY_MEAL
            );
        }

        if (containsAny(normalizedFileName, "sushi")) {
            return new OfferDraftSuggestion(
                    List.of("Sushi portion"),
                    "Sushi rescue portion",
                    "A prepared sushi portion that appears to include a visible assortment in the photo.",
                    OfferCategory.READY_MEAL
            );
        }

        if (containsAny(normalizedFileName, "meal", "lunch", "dinner", "rice")) {
            return new OfferDraftSuggestion(
                    List.of("Prepared meal portion"),
                    "Ready meal rescue offer",
                    "A prepared meal pickup inspired by the visible dishes in the uploaded food photo.",
                    OfferCategory.READY_MEAL
            );
        }

        return new OfferDraftSuggestion(
                List.of("Assorted meal box"),
                "Mixed rescue food offer",
                "A mixed food pickup based on the visible items in the uploaded photo.",
                OfferCategory.MIXED
        );
    }

    @Override
    public GeneratedOfferImage generateIllustrativeCover(OfferIllustrativeCoverRequest request) {
        OfferCategory category = request.category() == null ? OfferCategory.OTHER : request.category();
        String title = request.title() == null || request.title().isBlank() ? "Rescue offer" : request.title().trim();
        String accent = accentFor(category);
        String secondaryAccent = secondaryAccentFor(category);
        String shortTitle = title.length() <= 34 ? title : title.substring(0, 34);
        String items = request.detectedItems() == null || request.detectedItems().isEmpty()
                ? "Illustrative cover"
                : String.join(" | ", request.detectedItems().stream().limit(3).toList());

        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
                  <defs>
                    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stop-color="%s"/>
                      <stop offset="100%" stop-color="%s"/>
                    </linearGradient>
                  </defs>
                  <rect width="1024" height="1024" rx="56" fill="url(#bg)"/>
                  <circle cx="220" cy="210" r="120" fill="rgba(255,255,255,0.18)"/>
                  <circle cx="790" cy="290" r="160" fill="rgba(255,255,255,0.14)"/>
                  <circle cx="700" cy="760" r="210" fill="rgba(255,255,255,0.12)"/>
                  <rect x="104" y="620" width="816" height="236" rx="36" fill="rgba(255,255,255,0.18)"/>
                  <text x="120" y="162" font-family="Arial, Helvetica, sans-serif" font-size="42" fill="#fff" opacity="0.88">Illustrative cover only</text>
                  <text x="120" y="760" font-family="Arial, Helvetica, sans-serif" font-size="72" font-weight="700" fill="#fff">%s</text>
                  <text x="120" y="822" font-family="Arial, Helvetica, sans-serif" font-size="34" fill="#fff" opacity="0.92">%s</text>
                </svg>
                """.formatted(accent, secondaryAccent, escapeXml(shortTitle), escapeXml(items));

        return new GeneratedOfferImage(
                "offer-cover-" + UUID.randomUUID().toString().replace("-", "") + ".svg",
                "image/svg+xml",
                Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8)),
                true
        );
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String accentFor(OfferCategory category) {
        return switch (category) {
            case BAKERY -> "#C96B2C";
            case READY_MEAL -> "#C24B3C";
            case PRODUCE -> "#3D8F57";
            case GROCERY -> "#4F6DB8";
            case DESSERT -> "#B04D7A";
            case BEVERAGE -> "#2D8A9F";
            case MIXED -> "#7A5BC2";
            case OTHER -> "#5E7F56";
        };
    }

    private String secondaryAccentFor(OfferCategory category) {
        return switch (category) {
            case BAKERY -> "#7B3F1D";
            case READY_MEAL -> "#6E2D27";
            case PRODUCE -> "#234F31";
            case GROCERY -> "#273A74";
            case DESSERT -> "#6E284B";
            case BEVERAGE -> "#1D4C58";
            case MIXED -> "#48366E";
            case OTHER -> "#31452A";
        };
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
