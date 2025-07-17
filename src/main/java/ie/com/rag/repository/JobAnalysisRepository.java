package ie.com.rag.repository;

import ie.com.rag.entity.JobAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobAnalysisRepository extends JpaRepository<JobAnalysis, String> {

    @Query("SELECT j FROM JobAnalysis j ORDER BY j.createdAt DESC")
    List<JobAnalysis> findAllOrderByCreatedAtDesc();

    @Query("SELECT COUNT(j) FROM JobAnalysis j WHERE j.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(j) FROM JobAnalysis j WHERE j.createdAt >= :startDate AND j.createdAt < :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT * FROM job_analyses ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<JobAnalysis> findTopNOrderByCreatedAtDesc(@Param("limit") int limit);

    @Query(value = """
        SELECT 
            DATE(created_at) as date,
            COUNT(*) as daily_count
        FROM job_analyses 
        WHERE created_at >= :startDate
        GROUP BY DATE(created_at)
        ORDER BY date
        """, nativeQuery = true)
    List<Object[]> findDailyCountsSince(@Param("startDate") LocalDateTime startDate);
}
