package ie.com.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO for user response data
 * Excludes sensitive information like password
 */
@Builder
public record UserResponseDTO(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String role,
        Boolean enabled,
        @JsonProperty("accountNonExpired") Boolean accountNonExpired,
        @JsonProperty("accountNonLocked") Boolean accountNonLocked,
        @JsonProperty("credentialsNonExpired") Boolean credentialsNonExpired,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
