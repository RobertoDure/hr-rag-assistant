package ie.com.rag.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateNotFoundExceptionTest {

    @Test
    void constructorWithId_setsMessageAndCandidateId() {
        // Arrange / Act
        CandidateNotFoundException exception = new CandidateNotFoundException("42");

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Candidate not found with ID: 42");
        assertThat(exception.getCandidateId()).isEqualTo("42");
        assertThat(exception.getEmail()).isNull();
    }

    @Test
    void constructorWithMessageAndId_setsCustomMessage() {
        // Arrange / Act
        CandidateNotFoundException exception = new CandidateNotFoundException("Custom message", "99");

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Custom message");
        assertThat(exception.getCandidateId()).isEqualTo("99");
        assertThat(exception.getEmail()).isNull();
    }

    @Test
    void byEmail_setsEmailAndMessage() {
        // Arrange / Act
        CandidateNotFoundException exception = CandidateNotFoundException.byEmail("user@example.com");

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Candidate not found with email: user@example.com");
        assertThat(exception.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void isRuntimeException() {
        // Arrange / Act
        CandidateNotFoundException exception = new CandidateNotFoundException("1");

        // Assert
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
