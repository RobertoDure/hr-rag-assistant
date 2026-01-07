package ie.com.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user response data
 * Excludes sensitive information like password
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean enabled;

    @JsonProperty("accountNonExpired")
    private Boolean accountNonExpired;

    @JsonProperty("accountNonLocked")
    private Boolean accountNonLocked;

    @JsonProperty("credentialsNonExpired")
    private Boolean credentialsNonExpired;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

