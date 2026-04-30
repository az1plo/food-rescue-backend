package sk.posam.fsa.foodrescue.domain.support;

import sk.posam.fsa.foodrescue.domain.user.User;

public interface SupportFacade {

    SupportChatResponse reply(User currentUser, SupportChatRequest request);
}
