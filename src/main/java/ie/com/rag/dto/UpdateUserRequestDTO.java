package ie.com.rag.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * DTO for user update requests
 * All fields are optional - only provided fields will be updated
 */
@Builder
public record UpdateUserRequestDTO(
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit"
        )
        String password,

        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Pattern(regexp = "^(ADMIN|USER|HR_MANAGER)$", message = "Role must be ADMIN, USER, or HR_MANAGER")
        String role,

        Boolean enabled,
        Boolean accountNonExpired,
        Boolean accountNonLocked,
        Boolean credentialsNonExpired
) {
}
