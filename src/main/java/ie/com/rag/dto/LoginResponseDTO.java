package ie.com.rag.dto;

import lombok.Builder;

/**
 * DTO for login response containing JWT token and user information
 */
@Builder
public record LoginResponseDTO(
        String token,
        String type,
        String username,
        String role,
        Long expiresIn
) {
    public LoginResponseDTO {
        if (type == null) {
            type = "Bearer";
        }
    }

    public LoginResponseDTO(String token, String username, String role, Long expiresIn) {
        this(token, "Bearer", username, role, expiresIn);
    }
}
