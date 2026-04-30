package sk.posam.fsa.foodrescue.domain.shared;

import java.util.List;

public final class ValidationException extends FoodRescueException {

    public ValidationException(String message) {
        super(
                Type.VALIDATION,
                "Validation failed",
                List.of(message)
        );
    }
}

