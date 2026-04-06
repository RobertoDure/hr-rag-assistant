package ie.com.rag.repository;

import ie.com.rag.entity.JobAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobAnalysisRepository extends JpaRepository<JobAnalysis, String> {

    /**
     * Retrieve all JobAnalysis records ordered by creation date descending
     * @return List of JobAnalysis records
     */
    @Query
    List<JobAnalysis> findAllOrderByCreatedAtDesc();

    Page<JobAnalysis> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Count JobAnalysis records created after a specific date
     * @param startDate The date to filter records
     * @return Count of JobAnalysis records created after the specified date
     */
    @Query
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

    /**
     * Count JobAnalysis records created between two dates
     * @param startDate The start date of the range
     * @param endDate The end date of the range
     * @return Count of JobAnalysis records created within the specified date range
     */
    @Query
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Retrieve the most recent N JobAnalysis records ordered by creation date descending
     * @param limit The number of records to retrieve
     * @return List of JobAnalysis records
     */
    @Query(nativeQuery = true)
    List<JobAnalysis> findTopNOrderByCreatedAtDesc(@Param("limit") int limit);

    /**
     * Retrieve daily counts of JobAnalysis records created since a specific date
     * @param startDate The date to filter records
     * @return List of Object arrays containing date and count
     */
    @Query(nativeQuery = true)
    List<Object[]> findDailyCountsSince(@Param("startDate") LocalDateTime startDate);
}
