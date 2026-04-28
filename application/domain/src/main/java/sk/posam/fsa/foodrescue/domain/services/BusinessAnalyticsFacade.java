package sk.posam.fsa.foodrescue.domain.services;

import sk.posam.fsa.foodrescue.domain.models.entities.User;
import sk.posam.fsa.foodrescue.domain.models.valueobjects.BusinessAnalyticsSnapshot;

public interface BusinessAnalyticsFacade {

    BusinessAnalyticsSnapshot getAnalytics(User currentUser, Long businessId);
}
