package ie.com.rag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_analyses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobAnalysis {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "required_skills", columnDefinition = "TEXT[]")
    @Type(value = io.hypersistence.utils.hibernate.type.array.ListArrayType.class)
    private List<String> requiredSkills;

    @Column(name = "preferred_skills", columnDefinition = "TEXT[]")
    @Type(value = io.hypersistence.utils.hibernate.type.array.ListArrayType.class)
    private List<String> preferredSkills;

    @Column(name = "experience_level")
    private String experienceLevel;

    @Column(name = "education_requirement")
    private String educationRequirement;

    @Column(name = "min_years_experience")
    private Integer minYearsExperience;

    @Column(name = "max_years_experience")
    private Integer maxYearsExperience;

    @Column(name = "total_candidates_analyzed", nullable = false)
    private Integer totalCandidatesAnalyzed;

    @Column(name = "top_candidate_recommendation", columnDefinition = "TEXT")
    private String topCandidateRecommendation;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
