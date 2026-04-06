package ie.com.rag.repository;

import ie.com.rag.entity.QAHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QAHistoryRepository extends JpaRepository<QAHistory, String> {

    /**
     * Retrieve the most recent N QA history records ordered by timestamp descending
     * @param limit The number of records to retrieve
     * @return List of QAHistory records
     */
    @Query(nativeQuery = true)
    List<QAHistory> findTopNOrderByTimestampDesc(@Param("limit") int limit);

    /**
     * Retrieve all QA history records ordered by timestamp descending
     * @return List of QAHistory records
     */
    @Query
    List<QAHistory> findAllOrderByTimestampDesc();
}
