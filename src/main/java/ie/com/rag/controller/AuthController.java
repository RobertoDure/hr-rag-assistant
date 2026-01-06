package ie.com.rag.controller;

import ie.com.rag.dto.AuthErrorResponseDTO;
import ie.com.rag.dto.LoginRequestDTO;
import ie.com.rag.dto.LoginResponseDTO;
import ie.com.rag.security.JwtTokenProvider;
import ie.com.rag.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * REST Controller for authentication operations
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * Authenticate user and generate JWT token
     *
     * @param loginRequest Login credentials
     * @return JWT token and user information
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with username and password")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully authenticated",
            content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = AuthErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(schema = @Schema(implementation = AuthErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            log.info("Login attempt for user: {}", loginRequest.getUsername());
            log.debug("Password length: {}", loginRequest.getPassword() != null ? loginRequest.getPassword().length() : 0);

            // Debug: Check if user exists in database
            userRepository.findByUsername(loginRequest.getUsername()).ifPresent(user -> {
                log.debug("User exists in DB - Username: {}, Enabled: {}, Role: {}",
                    user.getUsername(), user.getEnabled(), user.getRole());
                log.debug("Account status - NonExpired: {}, NonLocked: {}, CredsNonExpired: {}",
                    user.getAccountNonExpired(), user.getAccountNonLocked(), user.getCredentialsNonExpired());
            });

            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            // Generate JWT token
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtTokenProvider.generateToken(userDetails);

            log.info("User {} successfully authenticated", loginRequest.getUsername());

            // Return response with token
            LoginResponseDTO response = LoginResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .username(userDetails.getUsername())
                .expiresIn(jwtExpiration)
                .build();

            return ResponseEntity.ok(response);

        } catch (DisabledException e) {
            log.warn("Account disabled for user: {}", loginRequest.getUsername());

            AuthErrorResponseDTO error = AuthErrorResponseDTO.builder()
                .message("Account is disabled")
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (LockedException e) {
            log.warn("Account locked for user: {}", loginRequest.getUsername());

            AuthErrorResponseDTO error = AuthErrorResponseDTO.builder()
                .message("Account is locked")
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {} - Bad credentials", loginRequest.getUsername());
            log.error("BadCredentialsException details: ", e);

            AuthErrorResponseDTO error = AuthErrorResponseDTO.builder()
                .message("Invalid username or password")
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {} - {}", loginRequest.getUsername(), e.getClass().getSimpleName());
            log.error("AuthenticationException details: ", e);

            AuthErrorResponseDTO error = AuthErrorResponseDTO.builder()
                .message("Authentication failed: " + e.getMessage())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (Exception e) {
            log.error("Error during login for user: {}", loginRequest.getUsername(), e);

            AuthErrorResponseDTO error = AuthErrorResponseDTO.builder()
                .message("An error occurred during authentication")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Verify token validity
     *
     * @param token JWT token
     * @return Token validation status
     */
    @GetMapping("/verify")
    @Operation(summary = "Verify token", description = "Check if JWT token is valid and not expired")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String username = jwtTokenProvider.extractUsername(token);

            return ResponseEntity.ok()
                .body(new java.util.HashMap<String, Object>() {{
                    put("valid", true);
                    put("username", username);
                }});

        } catch (Exception e) {
            log.warn("Invalid token verification attempt", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new java.util.HashMap<String, Object>() {{
                    put("valid", false);
                    put("message", "Invalid or expired token");
                }});
        }
    }
}

