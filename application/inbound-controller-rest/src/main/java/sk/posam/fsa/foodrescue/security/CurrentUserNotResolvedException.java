package sk.posam.fsa.foodrescue.security;

import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;

public class CurrentUserNotResolvedException extends FoodRescueException {
    public CurrentUserNotResolvedException(String message) {
        super(Type.UNAUTHORIZED, message);
    }
}

