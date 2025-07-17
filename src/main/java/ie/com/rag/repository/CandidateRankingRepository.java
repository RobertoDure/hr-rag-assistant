package ie.com.rag.repository;

import ie.com.rag.entity.CandidateRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateRankingRepository extends JpaRepository<CandidateRanking, UUID> {

    @Query("SELECT cr FROM CandidateRanking cr WHERE cr.jobAnalysisId = :jobAnalysisId ORDER BY cr.rankingPosition ASC")
    List<CandidateRanking> findByJobAnalysisIdOrderByRankingPosition(@Param("jobAnalysisId") String jobAnalysisId);

    @Query("SELECT cr FROM CandidateRanking cr WHERE cr.candidateId = :candidateId ORDER BY cr.jobAnalysis.createdAt DESC")
    List<CandidateRanking> findByCandidateIdOrderByCreatedAtDesc(@Param("candidateId") UUID candidateId);

    void deleteByJobAnalysisId(String jobAnalysisId);
}
