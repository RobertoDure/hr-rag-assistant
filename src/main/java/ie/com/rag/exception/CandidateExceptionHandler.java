package ie.com.rag.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for candidate-related exceptions.
 * Uses modern Spring Boot exception handling patterns.
 */
@ControllerAdvice
public class CandidateExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CandidateExceptionHandler.class);

    /**
     * Handles candidate not found exceptions.
     */
    @ExceptionHandler(CandidateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCandidateNotFoundException(
            CandidateNotFoundException ex, WebRequest request) {

        logger.warn("Candidate not found: {}", ex.getMessage());

        var errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Candidate Not Found")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles candidate validation exceptions with detailed field errors.
     */
    @ExceptionHandler(CandidateValidationException.class)
    public ResponseEntity<ErrorResponse> handleCandidateValidationException(
            CandidateValidationException ex, WebRequest request) {

        logger.warn("Candidate validation failed: {}", ex.getMessage());

        var fieldErrors = ex.getErrors().stream()
                .collect(Collectors.toMap(
                    CandidateValidationException.ValidationError::field,
                    CandidateValidationException.ValidationError::message
                ));

        var errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles candidate save exceptions.
     */
    @ExceptionHandler(CandidateSaveException.class)
    public ResponseEntity<ErrorResponse> handleCandidateSaveException(
            CandidateSaveException ex, WebRequest request) {

        logger.error("Failed to save candidate: {}", ex.getMessage(), ex);

        var errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Save Operation Failed")
                .message("Failed to save candidate. Please try again.")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles general runtime exceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        logger.error("Unexpected runtime exception: {}", ex.getMessage(), ex);

        var errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Modern error response record using Java records feature.
     */
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private LocalDateTime timestamp;
            private int status;
            private String error;
            private String message;
            private String path;
            private Map<String, String> fieldErrors;

            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder status(int status) {
                this.status = status;
                return this;
            }

            public Builder error(String error) {
                this.error = error;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder path(String path) {
                this.path = path;
                return this;
            }

            public Builder fieldErrors(Map<String, String> fieldErrors) {
                this.fieldErrors = fieldErrors;
                return this;
            }

            public ErrorResponse build() {
                return new ErrorResponse(timestamp, status, error, message, path, fieldErrors);
            }
        }
    }
}
