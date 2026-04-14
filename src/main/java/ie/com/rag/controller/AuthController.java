package ie.com.rag.controller;

import ie.com.rag.dto.LoginRequestDTO;
import ie.com.rag.dto.LoginResponseDTO;
import ie.com.rag.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with username and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account locked/disabled"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody final LoginRequestDTO loginRequest) {
        log.info("[RagWiser/AuthController] - login: attempt for user: {}", loginRequest.username());

        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        final String token = jwtTokenProvider.generateToken(userDetails);

        final String userRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("USER");

        log.info("[RagWiser/AuthController] - login: user {} successfully authenticated", loginRequest.username());

        final LoginResponseDTO response = LoginResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .username(userDetails.getUsername())
                .role(userRole)
                .expiresIn(jwtTokenProvider.getExpiration())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify token", description = "Check if JWT token is valid and not expired")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    public ResponseEntity<Map<String, Object>> verifyToken(
            @RequestHeader("Authorization") final String authHeader) {
        try {
            final String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            final String username = jwtTokenProvider.extractUsername(token);
            return ResponseEntity.ok(Map.of("valid", true, "username", username));
        } catch (Exception e) {
            log.warn("[RagWiser/AuthController] - verifyToken: invalid token verification attempt", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "Invalid or expired token"));
        }
    }
}


