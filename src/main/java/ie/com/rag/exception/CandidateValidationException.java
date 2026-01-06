package ie.com.rag.exception;

import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when candidate input validation fails.
 * Uses modern Java features like records for validation error details.
 */
public class CandidateValidationException extends RuntimeException {

    @Getter
    private final List<ValidationError> errors;

    public CandidateValidationException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = List.copyOf(errors); // Immutable copy
    }

    public CandidateValidationException(String message) {
        super(message);
        this.errors = List.of();
    }

    /**
     * Record for validation error details - modern Java approach
     */
    public record ValidationError(String field, String message, Object rejectedValue) {

        public static ValidationError of(String field, String message) {
            return new ValidationError(field, message, null);
        }

        public static ValidationError of(String field, String message, Object rejectedValue) {
            return new ValidationError(field, message, rejectedValue);
        }
    }
}
