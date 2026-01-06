package ie.com.rag.exception;

import lombok.Getter;

/**
 * Exception thrown when a candidate is not found in the system.
 * This is a specific exception for better error handling and debugging.
 */
public class CandidateNotFoundException extends RuntimeException {

    @Getter
    private final String candidateId;
    @Getter
    private final String email;

    public CandidateNotFoundException(String candidateId) {
        super("Candidate not found with ID: " + candidateId);
        this.candidateId = candidateId;
        this.email = null;
    }

    public CandidateNotFoundException(String message, String candidateId) {
        super(message);
        this.candidateId = candidateId;
        this.email = null;
    }

    public static CandidateNotFoundException byEmail(String email) {
        var exception = new CandidateNotFoundException("Candidate not found with email: " + email, null);
        return exception.withEmail(email);
    }

    private CandidateNotFoundException withEmail(String email) {
        return new CandidateNotFoundException("Candidate not found with email: " + email, null) {
            @Override
            public String getEmail() {
                return email;
            }
        };
    }
}
