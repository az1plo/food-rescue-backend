package sk.posam.fsa.foodrescue.domain.support;

public class FallbackSupportAssistantProvider implements SupportAssistantProvider {

    private final SupportAssistantProvider primary;
    private final SupportAssistantProvider fallback;

    public FallbackSupportAssistantProvider(SupportAssistantProvider primary, SupportAssistantProvider fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public SupportAssistantReply reply(SupportAssistantPrompt prompt) {
        try {
            return primary.reply(prompt);
        } catch (RuntimeException ex) {
            return fallback.reply(prompt);
        }
    }
}
