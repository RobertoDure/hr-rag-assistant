package ie.com.rag.controller;

import ie.com.rag.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("POST /api/auth/login should return token and user details on success")
    void login_success_returnsToken() throws Exception {
        // Given
        final UserDetails userDetails = User.withUsername("admin")
                .password("pass")
                .authorities("ROLE_ADMIN")
                .build();
        final Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpiration()).thenReturn(86_400_000L);

        // When / Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwt-token")))
                .andExpect(jsonPath("$.type", is("Bearer")))
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andExpect(jsonPath("$.expiresIn", is(86_400_000)));
    }

    @Test
    @DisplayName("POST /api/auth/login should default role to USER when no authorities exist")
    void login_noAuthorities_defaultsRoleToUser() throws Exception {
        // Given
        final UserDetails userDetails = User.withUsername("basic")
                .password("pass")
                .authorities(Collections.emptyList())
                .build();
        final Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> Collections.emptyList());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpiration()).thenReturn(86_400_000L);

        // When / Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"basic\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    @DisplayName("GET /api/auth/verify should confirm a valid token")
    void verifyToken_validToken_returnsValid() throws Exception {
        // Given
        when(jwtTokenProvider.extractUsername("valid-token")).thenReturn("admin");

        // When / Then
        mockMvc.perform(get("/api/auth/verify").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.username", is("admin")));
    }

    @Test
    @DisplayName("GET /api/auth/verify should return 401 for an invalid token")
    void verifyToken_invalidToken_returnsUnauthorized() throws Exception {
        // Given
        when(jwtTokenProvider.extractUsername("bad-token")).thenThrow(new RuntimeException("expired"));

        // When / Then
        mockMvc.perform(get("/api/auth/verify").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid", is(false)));
    }
}
