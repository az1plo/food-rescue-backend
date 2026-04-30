package sk.posam.fsa.foodrescue.domain.support;

public interface SupportAssistantProvider {

    SupportAssistantReply reply(SupportAssistantPrompt prompt);
}
