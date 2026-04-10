package ie.com.rag.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    // Base64-encoded 512-bit key (sufficient for HS512)
    private static final String SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtbG9uZy1lbm91Z2g=";
    private static final long EXPIRATION = 86_400_000L;

    private JwtTokenProvider jwtTokenProvider;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secret", SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "expiration", EXPIRATION);

        userDetails = User.builder()
                .username("admin")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidToken() {
        // When
        String token = jwtTokenProvider.generateToken(userDetails);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsername() {
        // Given
        String token = jwtTokenProvider.generateToken(userDetails);

        // When
        String username = jwtTokenProvider.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("admin");
    }

    @Test
    @DisplayName("Should validate token successfully")
    void shouldValidateToken() {
        // Given
        String token = jwtTokenProvider.generateToken(userDetails);

        // When
        Boolean isValid = jwtTokenProvider.validateToken(token, userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should extract expiration date after now")
    void shouldExtractExpiration() {
        // Given
        String token = jwtTokenProvider.generateToken(userDetails);

        // When
        Date expiration = jwtTokenProvider.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("Should fail validation with wrong user")
    void shouldFailValidationWithWrongUser() {
        // Given
        String token = jwtTokenProvider.generateToken(userDetails);
        UserDetails wrongUser = User.builder()
                .username("wronguser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        // When
        Boolean isValid = jwtTokenProvider.validateToken(token, wrongUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should generate token with extra claims")
    void shouldGenerateTokenWithExtraClaims() {
        // Given
        Map<String, Object> claims = Map.of("role", "ADMIN");

        // When
        String token = jwtTokenProvider.generateToken(claims, userDetails);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("admin");
    }

    @Test
    @DisplayName("Should return configured expiration value")
    void shouldReturnExpiration() {
        assertThat(jwtTokenProvider.getExpiration()).isEqualTo(EXPIRATION);
    }
}

