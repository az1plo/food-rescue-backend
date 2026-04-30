package sk.posam.fsa.foodrescue.domain.business;

import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.business.BusinessAnalyticsSnapshot;

public interface BusinessAnalyticsFacade {

    BusinessAnalyticsSnapshot getAnalytics(User currentUser, Long businessId);
}


