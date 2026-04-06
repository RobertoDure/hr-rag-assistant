package ie.com.rag.repository;

import ie.com.rag.entity.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, String> {

    /**
     * Count documents uploaded after a specific date
     * @param startDate The date to filter uploads
     * @return Count of documents uploaded after the specified date
     */
    @Query
    long countByUploadTimestampAfter(@Param("startDate") LocalDateTime startDate);
}
