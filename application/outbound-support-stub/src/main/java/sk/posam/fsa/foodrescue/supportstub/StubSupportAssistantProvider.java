package sk.posam.fsa.foodrescue.supportstub;

import sk.posam.fsa.foodrescue.domain.support.SupportAssistantPrompt;
import sk.posam.fsa.foodrescue.domain.support.SupportAssistantProvider;
import sk.posam.fsa.foodrescue.domain.support.SupportAssistantReply;

import java.util.List;
import java.util.Locale;

public class StubSupportAssistantProvider implements SupportAssistantProvider {

    private static final String ASSISTANT_NAME = "Mika";

    @Override
    public SupportAssistantReply reply(SupportAssistantPrompt prompt) {
        String normalizedMessage = prompt.message().toLowerCase(Locale.ROOT);
        String firstName = prompt.currentUser() == null ? null : prompt.currentUser().getFirstName();
        String greetingPrefix = firstName == null || firstName.isBlank()
                ? ""
                : firstName.trim() + ", ";

        if (containsAny(normalizedMessage, "reserve", "reservation", "book", "buy", "order", "pay")) {
            return new SupportAssistantReply(
                    ASSISTANT_NAME,
                    greetingPrefix + "open the offer details page, add the offer to your cart, and complete payment to create a pickup order. If you are not signed in, the app will prompt you to log in before checkout.",
                    List.of("What does unavailable mean?", "How does pickup work?", "Where do I see my paid orders?")
            );
        }

        if (containsAny(normalizedMessage, "pickup", "collect", "time")) {
            return new SupportAssistantReply(
                    ASSISTANT_NAME,
                    greetingPrefix + "pickup windows show when the business expects the rescue bag to be collected. If the time has passed, the offer will eventually become unavailable or expired in the marketplace.",
                    List.of("How do I pay for an offer?", "Why is an offer expired?", "How do businesses set pickup times?")
            );
        }

        if (containsAny(normalizedMessage, "business", "publish", "post", "offer")) {
            return new SupportAssistantReply(
                    ASSISTANT_NAME,
                    greetingPrefix + "businesses can onboard through the public business flow, then manage offers in the workspace. They can define pricing, original price, pickup location, pickup window, and what is inside each rescue offer.",
                    List.of("How does the business workspace work?", "Can a business edit an offer later?", "How are discounts shown?")
            );
        }

        if (containsAny(normalizedMessage, "map", "distance", "location", "city")) {
            return new SupportAssistantReply(
                    ASSISTANT_NAME,
                    greetingPrefix + "the marketplace can use your current location or a chosen city to scope the map, sort by distance, and narrow results to a radius like 30 km. That keeps the browsing experience focused and easier to compare.",
                    List.of("How do I use my location?", "Why do I only see nearby offers?", "How does map mode work?")
            );
        }

        if (containsAny(normalizedMessage, "price", "discount", "save")) {
            return new SupportAssistantReply(
                    ASSISTANT_NAME,
                    greetingPrefix + "when a business provides both the current rescue price and the original price, the marketplace can show the crossed original price together with the savings percentage on the card and the detail page.",
                    List.of("Why is there a crossed price?", "How are discounts calculated?", "What does original price mean?")
            );
        }

        return new SupportAssistantReply(
                ASSISTANT_NAME,
                greetingPrefix + "I can help with marketplace browsing, orders, pickup windows, business publishing, pricing, and distance-based map filtering. Ask me a specific question and I will point you in the right direction.",
                List.of("How do I pay for an offer?", "How does pickup work?", "How can a business publish offers?")
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
}
