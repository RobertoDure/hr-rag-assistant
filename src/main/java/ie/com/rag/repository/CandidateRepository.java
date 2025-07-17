package ie.com.rag.repository;

import ie.com.rag.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, String> {

    Optional<Candidate> findByEmail(String email);

    @Query("SELECT c FROM Candidate c ORDER BY c.createdAt DESC")
    List<Candidate> findAllOrderByCreatedAtDesc();

    @Query("SELECT c FROM Candidate c WHERE c.yearsOfExperience >= :minYears AND c.yearsOfExperience <= :maxYears")
    List<Candidate> findByYearsOfExperienceBetween(@Param("minYears") Integer minYears, @Param("maxYears") Integer maxYears);

    @Query("SELECT c FROM Candidate c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Candidate> findByNameContainingIgnoreCase(@Param("name") String name);

    // Dashboard specific queries
    @Query("SELECT COUNT(c) FROM Candidate c WHERE c.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(c) FROM Candidate c WHERE c.createdAt >= :startDate AND c.createdAt < :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT AVG(c.yearsOfExperience) FROM Candidate c WHERE c.yearsOfExperience IS NOT NULL")
    Double findAverageYearsOfExperience();

    @Query(value = """
        SELECT skill, COUNT(*) as count
        FROM candidates, unnest(skills) as skill
        WHERE skills IS NOT NULL
        GROUP BY skill
        ORDER BY count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopSkills(@Param("limit") int limit);

    @Query(value = "SELECT * FROM candidates ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Candidate> findTopNOrderByCreatedAtDesc(@Param("limit") int limit);

    @Query(value = """
        SELECT 
            CASE 
                WHEN years_of_experience IS NULL THEN 'Not specified'
                WHEN years_of_experience < 2 THEN 'Entry level (0-1 years)'
                WHEN years_of_experience < 5 THEN 'Junior (2-4 years)'
                WHEN years_of_experience < 10 THEN 'Mid-level (5-9 years)'
                WHEN years_of_experience < 15 THEN 'Senior (10-14 years)'
                ELSE 'Expert (15+ years)'
            END as experience_range,
            COUNT(*) as count
        FROM candidates
        GROUP BY experience_range
        ORDER BY 
            CASE 
                WHEN years_of_experience IS NULL THEN 0
                ELSE AVG(years_of_experience)
            END
        """, nativeQuery = true)
    List<Object[]> findExperienceDistribution();

    @Query(value = """
        SELECT 
            DATE(created_at) as date,
            COUNT(*) as daily_count
        FROM candidates 
        WHERE created_at >= :startDate
        GROUP BY DATE(created_at)
        ORDER BY date
        """, nativeQuery = true)
    List<Object[]> findDailyCountsSince(@Param("startDate") LocalDateTime startDate);
}
