package ie.com.rag.service;

import ie.com.rag.dto.RegisterRequestDTO;
import ie.com.rag.dto.UpdateUserRequestDTO;
import ie.com.rag.dto.UserResponseDTO;
import ie.com.rag.entity.SystemUser;
import ie.com.rag.entity.UserRole;
import ie.com.rag.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Service class for user management operations
 * Handles user registration, updates, and retrieval with proper validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user
     * @param request Registration request containing user details
     * @return Created user response DTO
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public UserResponseDTO registerUser(final RegisterRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }

        log.info("Attempting to register new user: {}", request.username());

        // Validate username uniqueness
        if (userRepository.existsByUsername(request.username())) {
            log.warn("Username already exists: {}", request.username());
            throw new IllegalArgumentException("Username already exists: " + request.username());
        }

        // Validate email uniqueness
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Email already exists: {}", request.email());
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }

        // Determine role (default to USER if not provided)
        final String userRoleString = resolveUserRole(request.role());

        // Create user entity
        final SystemUser user = SystemUser.builder()
            .username(request.username())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .firstName(request.firstName())
            .lastName(request.lastName())
            .role(userRoleString)
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();

        // Save user
        final SystemUser savedUser = userRepository.save(user);
        log.info("Successfully registered user: {} with ID: {}", savedUser.getUsername(), savedUser.getId());

        return mapToUserResponseDTO(savedUser);
    }

    /**
     * Resolves a user role string into a valid system role name, defaulting to USER if unspecified or invalid.
     *
     * @param roleValue the requested role value
     * @return the string representation of the resolved role
     */
    private String resolveUserRole(final String roleValue) {
        if (!StringUtils.hasText(roleValue)) {
            return UserRole.USER.toString();
        }

        try {
            return UserRole.valueOf(roleValue.trim().toUpperCase(Locale.ROOT)).toString();
        } catch (final IllegalArgumentException e) {
            log.warn("Invalid role provided: {}, defaulting to USER", roleValue);
            return UserRole.USER.toString();
        }
    }

    /**
     * Get user by ID
     * @param id User ID
     * @return User response DTO
     * @throws IllegalArgumentException if user not found
     */
    public UserResponseDTO getUserById(final String id) {
        log.debug("Fetching user by ID: {}", id);
        final SystemUser user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        return mapToUserResponseDTO(user);
    }

    /**
     * Get user by username
     * @param username Username
     * @return User response DTO
     * @throws IllegalArgumentException if user not found
     */
    public UserResponseDTO getUserByUsername(final String username) {
        log.debug("Fetching user by username: {}", username);
        final SystemUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return mapToUserResponseDTO(user);
    }

    /**
     * Get all users
     * @return List of user response DTOs
     */
    public List<UserResponseDTO> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll()
            .stream()
            .map(this::mapToUserResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get all enabled users
     * @return List of enabled user response DTOs
     */
    public List<UserResponseDTO> getAllEnabledUsers() {
        log.debug("Fetching all enabled users");
        return userRepository.findAllEnabledUsers()
            .stream()
            .map(this::mapToUserResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get users by role
     * @param role User role
     * @return List of user response DTOs
     */
    public List<UserResponseDTO> getUsersByRole(final String role) {
        log.debug("Fetching users by role: {}", role);
        final UserRole userRole;
        try {
            userRole = UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        return userRepository.findByRole(userRole)
            .stream()
            .map(this::mapToUserResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Search users with pagination and filters
     * @param username Username filter
     * @param email Email filter
     * @param role Role filter
     * @param pageable Pagination information
     * @return Page of user response DTOs
     */
    public Page<UserResponseDTO> searchUsers(final String username, final String email, final String role, final Pageable pageable) {
        log.debug("Searching users with filters - username: {}, email: {}, role: {}", username, email, role);
        String validRole = null;
        if (StringUtils.hasText(role)) {
            try {
                validRole = UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT)).name();
            } catch (final IllegalArgumentException e) {
                // Return empty or throw, we'll throw to inform client
                throw new IllegalArgumentException("Invalid role: " + role);
            }
        }

        return userRepository.searchUsers(
            StringUtils.hasText(username) ? "%" + username.trim() + "%" : null,
            StringUtils.hasText(email) ? "%" + email.trim() + "%" : null,
            validRole,
            pageable
        ).map(this::mapToUserResponseDTO);
    }

    /**
     * Update user
     * @param id User ID
     * @param request Update request containing fields to update
     * @return Updated user response DTO
     * @throws IllegalArgumentException if user not found or validation fails
     */
    @Transactional
    public UserResponseDTO updateUser(final String id, final UpdateUserRequestDTO request) {
        log.info("Updating user with ID: {}", id);

        final SystemUser user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // Update email if provided and unique
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email already exists: " + request.email());
            }
            user.setEmail(request.email());
        }

        // Update password if provided
        if (request.password() != null && !request.password().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        // Update other fields if provided
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.role() != null) {
            try {
                user.setRole(UserRole.valueOf(request.role()).toString());
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.role());
            }
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.accountNonExpired() != null) {
            user.setAccountNonExpired(request.accountNonExpired());
        }
        if (request.accountNonLocked() != null) {
            user.setAccountNonLocked(request.accountNonLocked());
        }
        if (request.credentialsNonExpired() != null) {
            user.setCredentialsNonExpired(request.credentialsNonExpired());
        }

        final SystemUser updatedUser = userRepository.save(user);
        log.info("Successfully updated user: {}", updatedUser.getUsername());

        return mapToUserResponseDTO(updatedUser);
    }

    /**
     * Delete user
     * @param id User ID
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public void deleteUser(final String id) {
        log.info("Deleting user with ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }

        userRepository.deleteById(id);
        log.info("Successfully deleted user with ID: {}", id);
    }

    /**
     * Map User entity to UserResponseDTO
     * @param user User entity
     * @return UserResponseDTO
     */
    private UserResponseDTO mapToUserResponseDTO(final SystemUser user) {
        return UserResponseDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole())
            .enabled(user.getEnabled())
            .accountNonExpired(user.getAccountNonExpired())
            .accountNonLocked(user.getAccountNonLocked())
            .credentialsNonExpired(user.getCredentialsNonExpired())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}

