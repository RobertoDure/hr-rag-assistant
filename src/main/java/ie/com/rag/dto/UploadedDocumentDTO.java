package ie.com.rag.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class UploadedDocumentDTO {
    private UUID id;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;

    // Constructors
    public UploadedDocumentDTO() {}

    public UploadedDocumentDTO(UUID id, String fileName, Long fileSize, String contentType, LocalDateTime uploadedAt) {
        this.id = id;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadedAt = uploadedAt;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
