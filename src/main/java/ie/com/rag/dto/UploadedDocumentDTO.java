package ie.com.rag.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UploadedDocumentDTO(
        UUID id,
        String fileName,
        Long fileSize,
        String contentType,
        LocalDateTime uploadedAt
) {
}
