package ie.com.rag.exception;

import lombok.Getter;

/**
 * Exception thrown when there's an error saving a candidate to the database.
 * This provides more specific error handling for data persistence issues.
 */
public class CandidateSaveException extends RuntimeException {
    @Getter
    private final String candidateName;
    @Getter
    private final String candidateEmail;

    public CandidateSaveException(String message, Throwable cause) {
        super(message, cause);
        this.candidateName = null;
        this.candidateEmail = null;
    }

    public CandidateSaveException(String message, String candidateName, String candidateEmail, Throwable cause) {
        super(message, cause);
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
    }
}
