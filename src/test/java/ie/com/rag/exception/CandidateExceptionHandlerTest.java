package ie.com.rag.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateExceptionHandlerTest {

    private final CandidateExceptionHandler handler = new CandidateExceptionHandler();

    private WebRequest mockRequest(final String uri) {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=" + uri);
        return request;
    }

    @Test
    void handleCandidateNotFoundException_returns404() {
        // Arrange
        CandidateNotFoundException ex = new CandidateNotFoundException("42");
        WebRequest request = mockRequest("/api/candidates/42");

        // Act
        ResponseEntity<CandidateExceptionHandler.ErrorResponse> response =
                handler.handleCandidateNotFoundException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Candidate Not Found");
        assertThat(response.getBody().message()).contains("42");
        assertThat(response.getBody().path()).isEqualTo("/api/candidates/42");
    }

    @Test
    void handleCandidateValidationException_returns400WithFieldErrors() {
        // Arrange
        List<CandidateValidationException.ValidationError> errors = List.of(
                CandidateValidationException.ValidationError.of("email", "must not be blank"),
                CandidateValidationException.ValidationError.of("name", "too short")
        );
        CandidateValidationException ex = new CandidateValidationException("Validation failed", errors);
        WebRequest request = mockRequest("/api/candidates");

        // Act
        ResponseEntity<CandidateExceptionHandler.ErrorResponse> response =
                handler.handleCandidateValidationException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().fieldErrors()).containsKey("email");
        assertThat(response.getBody().fieldErrors()).containsKey("name");
    }

    @Test
    void handleCandidateSaveException_returns500() {
        // Arrange
        CandidateSaveException ex = new CandidateSaveException("DB failure", new RuntimeException("constraint"));
        WebRequest request = mockRequest("/api/candidates");

        // Act
        ResponseEntity<CandidateExceptionHandler.ErrorResponse> response =
                handler.handleCandidateSaveException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Save Operation Failed");
    }

    @Test
    void handleRuntimeException_returns500() {
        // Arrange
        RuntimeException ex = new RuntimeException("unexpected");
        WebRequest request = mockRequest("/api/any");

        // Act
        ResponseEntity<CandidateExceptionHandler.ErrorResponse> response =
                handler.handleRuntimeException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
    }
}
