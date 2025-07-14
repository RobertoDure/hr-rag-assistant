package ie.com.rag.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class QAHistoryDTO {
    private UUID id;
    private String question;
    private String answer;
    private LocalDateTime timestamp;

    // Constructors
    public QAHistoryDTO() {}

    public QAHistoryDTO(UUID id, String question, String answer, LocalDateTime timestamp) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
