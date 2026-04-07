package sk.posam.fsa.foodrescue.domain.exceptions;

import java.util.List;

public class FoodRescueException extends RuntimeException {

    public enum Type {
        VALIDATION,
        NOT_FOUND,
        UNAUTHORIZED,
        FORBIDDEN,
        CONFLICT
    }

    private final Type type;
    private final List<String> details;

    public FoodRescueException(Type type, String message) {
        super(message);
        this.type = type == null ? Type.VALIDATION : type;
        this.details = List.of();
    }

    public FoodRescueException(Type type, String message, List<String> details) {
        super(message);
        this.type = type == null ? Type.VALIDATION : type;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public Type getType() {
        return type;
    }

    public List<String> getDetails() {
        return details;
    }
}