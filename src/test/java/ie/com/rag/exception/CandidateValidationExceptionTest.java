package ie.com.rag.exception;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateValidationExceptionTest {

    @Test
    void constructorWithMessageOnly_hasEmptyErrors() {
        // Arrange / Act
        CandidateValidationException exception = new CandidateValidationException("Validation failed");

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Validation failed");
        assertThat(exception.getErrors()).isEmpty();
    }

    @Test
    void constructorWithErrors_storesImmutableCopy() {
        // Arrange
        List<CandidateValidationException.ValidationError> errors = List.of(
                CandidateValidationException.ValidationError.of("email", "must not be blank"),
                CandidateValidationException.ValidationError.of("name", "too short", "x")
        );

        // Act
        CandidateValidationException exception = new CandidateValidationException("Validation failed", errors);

        // Assert
        assertThat(exception.getErrors()).hasSize(2);
        assertThat(exception.getErrors().get(0).field()).isEqualTo("email");
        assertThat(exception.getErrors().get(0).message()).isEqualTo("must not be blank");
        assertThat(exception.getErrors().get(0).rejectedValue()).isNull();
        assertThat(exception.getErrors().get(1).rejectedValue()).isEqualTo("x");
    }

    @Test
    void validationError_ofWithTwoArgs_hasNullRejectedValue() {
        // Act
        CandidateValidationException.ValidationError error =
                CandidateValidationException.ValidationError.of("field", "message");

        // Assert
        assertThat(error.field()).isEqualTo("field");
        assertThat(error.message()).isEqualTo("message");
        assertThat(error.rejectedValue()).isNull();
    }

    @Test
    void validationError_ofWithThreeArgs_storesRejectedValue() {
        // Act
        CandidateValidationException.ValidationError error =
                CandidateValidationException.ValidationError.of("age", "must be positive", -1);

        // Assert
        assertThat(error.field()).isEqualTo("age");
        assertThat(error.rejectedValue()).isEqualTo(-1);
    }

    @Test
    void isRuntimeException() {
        // Arrange / Act
        CandidateValidationException exception = new CandidateValidationException("error");

        // Assert
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
