package ie.com.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QAHistoryDTO {

    private UUID id;
    private String question;
    private String answer;
    private LocalDateTime timestamp;
}
