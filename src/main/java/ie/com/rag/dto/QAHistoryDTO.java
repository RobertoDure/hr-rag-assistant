package ie.com.rag.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record QAHistoryDTO(
        UUID id,
        String question,
        String answer,
        LocalDateTime timestamp
) {
}
