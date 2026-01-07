package ie.com.rag.controller;

import ie.com.rag.dto.RegisterRequestDTO;
import ie.com.rag.dto.UpdateUserRequestDTO;
import ie.com.rag.dto.UserResponseDTO;
import ie.com.rag.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for user management operations
 * Provides endpoints for user registration, retrieval, update, and deletion
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management APIs for admin operations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    /**
     * Register a new user (Admin only)
     * @param request Registration request
     * @return Created user details
     */
    @PostMapping("/register")
    //@PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register new user", description = "Create a new user account (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User successfully registered",
            content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input or username/email already exists"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Admin role required"
        )
    })
    public ResponseEntity<UserResponseDTO> registerUser(
        @Valid @RequestBody RegisterRequestDTO request
    ) {
        log.info("Received registration request for username: {}", request.getUsername());
        try {
            UserResponseDTO user = userService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            log.error("Registration failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get user by ID (Admin only)
     * @param id User ID
     * @return User details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Retrieve user details by ID (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Admin role required"
        )
    })
    public ResponseEntity<UserResponseDTO> getUserById(
        @Parameter(description = "User ID") @PathVariable String id
    ) {
        log.info("Fetching user by ID: {}", id);
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user by username (Admin only)
     * @param username Username
     * @return User details
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by username", description = "Retrieve user details by username (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Admin role required"
        )
    })
    public ResponseEntity<UserResponseDTO> getUserByUsername(
        @Parameter(description = "Username") @PathVariable String username
    ) {
        log.info("Fetching user by username: {}", username);
        UserResponseDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    /**
     * Get all users (Admin only)
     * @return List of all users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Retrieve all users (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Admin role required"
        )
    })
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        log.info("Fetching all users");
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get all enabled users (Admin only)
     * @return List of enabled users
     */
    @GetMapping("/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get enabled users", description = "Retrieve all enabled users (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Enabled users retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Admin role required"
        )
    })
    public ResponseEntity<List<UserResponseDTO>> getEnabledUsers() {
        log.info("Fetching enabled users");
        List<UserResponseDTO> users = userService.getAllEnabledUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get users by role (Admin only)
     * @param role User role (ADMIN, USER, HR_MANAGER)
     * @return List of users with specified role
     */
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get users by role", description = "Retrieve users by role (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid role"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Admin role required"
        )
    })
    public ResponseEntity<List<UserResponseDTO>> getUsersByRole(
        @Parameter(description = "User role (ADMIN, USER, HR_MANAGER)") @PathVariable String role
    ) {
        log.info("Fetching users by role: {}", role);
        List<UserResponseDTO> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    /**
     * Update user (Admin only)
     * @param id User ID
     * @param request Update request
     * @return Updated user details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "Update user details (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Admin role required"
        )
    })
    public ResponseEntity<UserResponseDTO> updateUser(
        @Parameter(description = "User ID") @PathVariable String id,
        @Valid @RequestBody UpdateUserRequestDTO request
    ) {
        log.info("Updating user with ID: {}", id);
        UserResponseDTO user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }

    /**
     * Delete user (Admin only)
     * @param id User ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Delete user account (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Admin role required"
        )
    })
    public ResponseEntity<Map<String, String>> deleteUser(
        @Parameter(description = "User ID") @PathVariable String id
    ) {
        log.info("Deleting user with ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of(
            "message", "User deleted successfully",
            "userId", id
        ));
    }
}

