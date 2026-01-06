package ie.com.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for authentication error responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthErrorResponseDTO {

    private String message;
    private int status;
    private String error;
    private LocalDateTime timestamp;
    private String path;

    public AuthErrorResponseDTO(String message, int status, String error) {
        this.message = message;
        this.status = status;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }
}

