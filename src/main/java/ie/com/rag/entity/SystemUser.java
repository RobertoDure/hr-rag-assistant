package ie.com.rag.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity for database authentication
 * Represents a user account in the system with authentication and authorization details
 */
@Entity
@Table(name = "users")
@Getter @Setter @ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemUser {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password; // BCrypt hashed password

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "account_non_expired", nullable = false)
    @Builder.Default
    private Boolean accountNonExpired = true;

    @Column(name = "account_non_locked", nullable = false)
    @Builder.Default
    private Boolean accountNonLocked = true;

    @Column(name = "credentials_non_expired", nullable = false)
    @Builder.Default
    private Boolean credentialsNonExpired = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Generate UUID before persisting if not set
     */
    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        // Ensure boolean fields are never null
        if (enabled == null) {
            enabled = true;
        }
        if (accountNonExpired == null) {
            accountNonExpired = true;
        }
        if (accountNonLocked == null) {
            accountNonLocked = true;
        }
        if (credentialsNonExpired == null) {
            credentialsNonExpired = true;
        }
    }

    /**
     * Null-safe getter for enabled status
     */
    public Boolean getEnabled() {
        return enabled != null ? enabled : true;
    }

    /**
     * Null-safe getter for account non-expired status
     */
    public Boolean getAccountNonExpired() {
        return accountNonExpired != null ? accountNonExpired : true;
    }

    /**
     * Null-safe getter for account non-locked status
     */
    public Boolean getAccountNonLocked() {
        return accountNonLocked != null ? accountNonLocked : true;
    }

    /**
     * Null-safe getter for credentials non-expired status
     */
    public Boolean getCredentialsNonExpired() {
        return credentialsNonExpired != null ? credentialsNonExpired : true;
    }

}

