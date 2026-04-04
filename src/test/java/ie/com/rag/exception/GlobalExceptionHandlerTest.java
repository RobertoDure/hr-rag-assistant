package ie.com.rag.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private WebRequest mockRequest(final String uri) {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=" + uri);
        return request;
    }

    @Test
    void handleResourceNotFoundException_returns404() {
        // Arrange
        ResourceNotFoundException ex = new ResourceNotFoundException("Candidate not found");
        WebRequest request = mockRequest("/api/candidates/1");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Candidate not found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/candidates/1");
    }

    @Test
    void handleBadCredentialsException_returns401WithMessage() {
        // Arrange
        BadCredentialsException ex = new BadCredentialsException("bad creds");
        WebRequest request = mockRequest("/api/auth/login");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleBadCredentialsException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid username or password");
    }

    @Test
    void handleAccessDeniedException_returns403() {
        // Arrange
        AccessDeniedException ex = new AccessDeniedException("denied");
        WebRequest request = mockRequest("/api/admin");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
    }

    @Test
    void handleMaxUploadSizeExceededException_returns413() {
        // Arrange
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(50 * 1024 * 1024L);
        WebRequest request = mockRequest("/api/upload");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceededException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("50MB");
    }

    @Test
    void handleIllegalArgumentException_returns400WithExceptionMessage() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("invalid input");
        WebRequest request = mockRequest("/api/candidates");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("invalid input");
    }

    @Test
    void handleDisabledException_returns401() {
        // Arrange
        DisabledException ex = new DisabledException("Account disabled");
        WebRequest request = mockRequest("/api/auth/login");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleDisabledException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Account is disabled");
    }

    @Test
    void handleLockedException_returns401() {
        // Arrange
        LockedException ex = new LockedException("Account locked");
        WebRequest request = mockRequest("/api/auth/login");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleLockedException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Account is locked");
    }

    @Test
    void handleAuthenticationException_returns401() {
        // Arrange
        AuthenticationException ex = new BadCredentialsException("auth failed");
        WebRequest request = mockRequest("/api/protected");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Authentication failed");
    }

    @Test
    void handleGlobalException_returns500() {
        // Arrange
        Exception ex = new Exception("unexpected");
        WebRequest request = mockRequest("/api/any");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
    }
}
