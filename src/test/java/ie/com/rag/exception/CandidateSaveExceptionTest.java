package ie.com.rag.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateSaveExceptionTest {

    @Test
    void constructorWithMessageAndCause_setsMessageAndCause() {
        // Arrange
        RuntimeException cause = new RuntimeException("DB error");

        // Act
        CandidateSaveException exception = new CandidateSaveException("Save failed", cause);

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Save failed");
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getCandidateName()).isNull();
        assertThat(exception.getCandidateEmail()).isNull();
    }

    @Test
    void constructorWithAllFields_storesNameAndEmail() {
        // Arrange
        RuntimeException cause = new RuntimeException("constraint violation");

        // Act
        CandidateSaveException exception = new CandidateSaveException(
                "Failed to save John", "John Doe", "john@example.com", cause);

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Failed to save John");
        assertThat(exception.getCandidateName()).isEqualTo("John Doe");
        assertThat(exception.getCandidateEmail()).isEqualTo("john@example.com");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void isRuntimeException() {
        // Arrange / Act
        CandidateSaveException exception = new CandidateSaveException("error", new RuntimeException());

        // Assert
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
