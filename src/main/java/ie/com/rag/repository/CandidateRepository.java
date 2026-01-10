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

    /**
     * Retrieve all Candidate records ordered by creation date descending
     * @return List of Candidate records
     */
    @Query("SELECT c FROM Candidate c ORDER BY c.createdAt DESC")
    List<Candidate> findAllOrderByCreatedAtDesc();

    /**
     * Find candidates by years of experience range
     * @param minYears Minimum years of experience
     * @param maxYears Maximum years of experience
     * @return List of candidates within the specified experience range
     */
    @Query("SELECT c FROM Candidate c WHERE c.yearsOfExperience >= :minYears AND c.yearsOfExperience <= :maxYears")
    List<Candidate> findByYearsOfExperienceBetween(@Param("minYears") Integer minYears, @Param("maxYears") Integer maxYears);

    /**
     * Find candidates whose names contain the specified substring (case insensitive)
     * @param name Substring to search for in candidate names
     * @return List of candidates whose names contain the specified substring
     */
    @Query("SELECT c FROM Candidate c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Candidate> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Count Candidate records created after a specific date
     * @param startDate The date to filter records
     * @return Count of Candidate records created after the specified date
     */
    @Query("SELECT COUNT(c) FROM Candidate c WHERE c.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

    /**
     * Count Candidate records created between two dates
     * @param startDate The start date of the range
     * @param endDate The end date of the range
     * @return Count of Candidate records created within the specified date range
     */
    @Query("SELECT COUNT(c) FROM Candidate c WHERE c.createdAt >= :startDate AND c.createdAt < :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate the average years of experience among all candidates
     * @return Average years of experience
     */
    @Query("SELECT AVG(c.yearsOfExperience) FROM Candidate c WHERE c.yearsOfExperience IS NOT NULL")
    Double findAverageYearsOfExperience();

    /**
     * Retrieve the top N most common skills among candidates
     * @param limit The number of top skills to retrieve
     * @return List of Object arrays containing skill and its count
     */
    @Query(value = """
        SELECT skill, COUNT(*) as count
        FROM candidates, unnest(skills) as skill
        WHERE skills IS NOT NULL
        GROUP BY skill
        ORDER BY count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopSkills(@Param("limit") int limit);

    /**
     * Retrieve the most recent N Candidate records ordered by creation date descending
     * @param limit The number of records to retrieve
     * @return List of Candidate records
     */
    @Query(value = "SELECT * FROM candidates ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Candidate> findTopNOrderByCreatedAtDesc(@Param("limit") int limit);

    /**
     * Retrieve distribution of candidates by years of experience ranges
     * @return List of Object arrays containing experience range and its count
     */
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

    /**
     * Retrieve daily counts of Candidate records created since a specific date
     * @param startDate The date to filter records
     * @return List of Object arrays containing date and count
     */
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
