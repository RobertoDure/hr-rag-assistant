package ie.com.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobAnalysisResponseDTO {
    private UUID id;
    private String jobTitle;
    private String jobDescription;
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private String experienceLevel;
    private String educationRequirement;
    private Integer minYearsExperience;
    private Integer maxYearsExperience;
    private Integer totalCandidatesAnalyzed;
    private String topCandidateRecommendation;
    private List<RankedCandidateDTO> rankedCandidates;
    private LocalDateTime createdAt;
}
