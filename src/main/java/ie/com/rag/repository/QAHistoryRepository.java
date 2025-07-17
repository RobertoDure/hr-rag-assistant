package ie.com.rag.repository;

import ie.com.rag.entity.QAHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QAHistoryRepository extends JpaRepository<QAHistory, String> {

    @Query(value = "SELECT * FROM qa_history ORDER BY timestamp DESC LIMIT :limit", nativeQuery = true)
    List<QAHistory> findTopNOrderByTimestampDesc(@Param("limit") int limit);

    @Query("SELECT q FROM QAHistory q ORDER BY q.timestamp DESC")
    List<QAHistory> findAllOrderByTimestampDesc();
}
