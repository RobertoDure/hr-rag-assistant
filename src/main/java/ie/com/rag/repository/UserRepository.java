package ie.com.rag.repository;

import ie.com.rag.entity.SystemUser;
import ie.com.rag.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity operations
 * Provides database access methods for user management and authentication
 */
@Repository
public interface UserRepository extends JpaRepository<SystemUser, String> {

    /**
     * Find user by username (used for authentication)
     * @param username The username to search for
     * @return Optional containing the user if found
     */
    Optional<SystemUser> findByUsername(String username);

    /**
     * Find user by email
     * @param email The email address to search for
     * @return Optional containing the user if found
     */
    Optional<SystemUser> findByEmail(String email);

    /**
     * Check if username exists
     * @param username The username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     * @param email The email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find all enabled users
     * @return List of enabled users
     */
    @Query("SELECT u FROM SystemUser u WHERE u.enabled = true ORDER BY u.createdAt DESC")
    List<SystemUser> findAllEnabledUsers();

    /**
     * Find users by role
     * @param role The user role to filter by
     * @return List of users with the specified role
     */
    @Query("SELECT u FROM SystemUser u WHERE u.role = :role ORDER BY u.createdAt DESC")
    List<SystemUser> findByRole(@Param("role") UserRole role);

    /**
     * Count users by role
     * @param role The user role to count
     * @return Number of users with the specified role
     */
    long countByRole(UserRole role);
}

