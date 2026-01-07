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

import java.util.List;
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
    public UserResponseDTO registerUser(RegisterRequestDTO request) {
        log.info("Attempting to register new user: {}", request.getUsername());

        // Validate username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Validate email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Determine role (default to USER if not provided)
        String userRoleString = null;
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                userRoleString = UserRole.valueOf(request.getRole()).toString();
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role provided: {}, defaulting to USER", request.getRole());
            }
        }

        // Create user entity
        SystemUser user = SystemUser.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .role(userRoleString)
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();

        // Save user
        SystemUser savedUser = userRepository.save(user);
        log.info("Successfully registered user: {} with ID: {}", savedUser.getUsername(), savedUser.getId());

        return mapToUserResponseDTO(savedUser);
    }

    /**
     * Get user by ID
     * @param id User ID
     * @return User response DTO
     * @throws IllegalArgumentException if user not found
     */
    public UserResponseDTO getUserById(String id) {
        log.debug("Fetching user by ID: {}", id);
        SystemUser user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        return mapToUserResponseDTO(user);
    }

    /**
     * Get user by username
     * @param username Username
     * @return User response DTO
     * @throws IllegalArgumentException if user not found
     */
    public UserResponseDTO getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        SystemUser user = userRepository.findByUsername(username)
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
    public List<UserResponseDTO> getUsersByRole(String role) {
        log.debug("Fetching users by role: {}", role);
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        return userRepository.findByRole(userRole)
            .stream()
            .map(this::mapToUserResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Update user
     * @param id User ID
     * @param request Update request containing fields to update
     * @return Updated user response DTO
     * @throws IllegalArgumentException if user not found or validation fails
     */
    @Transactional
    public UserResponseDTO updateUser(String id, UpdateUserRequestDTO request) {
        log.info("Updating user with ID: {}", id);

        SystemUser user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // Update email if provided and unique
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Update other fields if provided
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getRole() != null) {
            try {
                user.setRole(UserRole.valueOf(request.getRole()).toString());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getAccountNonExpired() != null) {
            user.setAccountNonExpired(request.getAccountNonExpired());
        }
        if (request.getAccountNonLocked() != null) {
            user.setAccountNonLocked(request.getAccountNonLocked());
        }
        if (request.getCredentialsNonExpired() != null) {
            user.setCredentialsNonExpired(request.getCredentialsNonExpired());
        }

        SystemUser updatedUser = userRepository.save(user);
        log.info("Successfully updated user: {}", updatedUser.getUsername());

        return mapToUserResponseDTO(updatedUser);
    }

    /**
     * Delete user
     * @param id User ID
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public void deleteUser(String id) {
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
    private UserResponseDTO mapToUserResponseDTO(SystemUser user) {
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

