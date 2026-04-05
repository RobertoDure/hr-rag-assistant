package ie.com.rag.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO for authentication error responses
 */
@Builder
public record AuthErrorResponseDTO(
        String message,
        int status,
        String error,
        LocalDateTime timestamp,
        String path
) {
    public AuthErrorResponseDTO(String message, int status, String error) {
        this(message, status, error, LocalDateTime.now(), null);
    }
}
