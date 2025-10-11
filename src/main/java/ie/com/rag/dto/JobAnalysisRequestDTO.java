package ie.com.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class
JobAnalysisRequestDTO {

    private String jobTitle;
    private String jobDescription;
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private String experienceLevel;
    private String educationRequirement;
    private Integer minYearsExperience;
    private Integer maxYearsExperience;
}
