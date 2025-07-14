package ie.com.rag.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    // Constructors
    public JobAnalysisResponseDTO() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }

    public List<String> getPreferredSkills() { return preferredSkills; }
    public void setPreferredSkills(List<String> preferredSkills) { this.preferredSkills = preferredSkills; }

    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }

    public String getEducationRequirement() { return educationRequirement; }
    public void setEducationRequirement(String educationRequirement) { this.educationRequirement = educationRequirement; }

    public Integer getMinYearsExperience() { return minYearsExperience; }
    public void setMinYearsExperience(Integer minYearsExperience) { this.minYearsExperience = minYearsExperience; }

    public Integer getMaxYearsExperience() { return maxYearsExperience; }
    public void setMaxYearsExperience(Integer maxYearsExperience) { this.maxYearsExperience = maxYearsExperience; }

    public Integer getTotalCandidatesAnalyzed() { return totalCandidatesAnalyzed; }
    public void setTotalCandidatesAnalyzed(Integer totalCandidatesAnalyzed) { this.totalCandidatesAnalyzed = totalCandidatesAnalyzed; }

    public String getTopCandidateRecommendation() { return topCandidateRecommendation; }
    public void setTopCandidateRecommendation(String topCandidateRecommendation) { this.topCandidateRecommendation = topCandidateRecommendation; }

    public List<RankedCandidateDTO> getRankedCandidates() { return rankedCandidates; }
    public void setRankedCandidates(List<RankedCandidateDTO> rankedCandidates) { this.rankedCandidates = rankedCandidates; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
