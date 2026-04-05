package ie.com.rag.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for login request containing user credentials
 */
public record LoginRequestDTO(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password
) {
}
