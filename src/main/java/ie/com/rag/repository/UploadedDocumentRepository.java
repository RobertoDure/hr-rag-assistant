package ie.com.rag.repository;

import ie.com.rag.entity.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, String> {

    @Query("SELECT COUNT(u) FROM UploadedDocument u WHERE u.uploadedAt >= :startDate")
    long countByUploadTimestampAfter(@Param("startDate") LocalDateTime startDate);
}
