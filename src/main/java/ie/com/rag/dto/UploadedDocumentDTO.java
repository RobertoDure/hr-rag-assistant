package ie.com.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadedDocumentDTO {

    private UUID id;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
}
