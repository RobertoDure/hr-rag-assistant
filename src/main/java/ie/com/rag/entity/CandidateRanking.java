package ie.com.rag.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidate_rankings")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidateRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_analysis_id", nullable = false)
    private String jobAnalysisId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "match_score", nullable = false)
    private Double matchScore;

    @Column(name = "ranking_position", nullable = false)
    private Integer rankingPosition;

    @Column(name = "key_highlights")
    @Type(io.hypersistence.utils.hibernate.type.array.ListArrayType.class)
    private List<String> keyHighlights;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_analysis_id", insertable = false, updatable = false)
    private JobAnalysis jobAnalysis;
}