package ie.com.rag.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record JobAnalysisRequestDTO(
        @NotBlank(message = "Job title must not be blank")
        String jobTitle,
        @NotBlank(message = "Job description must not be blank")
        String jobDescription,
        List<String> requiredSkills,
        List<String> preferredSkills,
        String experienceLevel,
        String educationRequirement,
        Integer minYearsExperience,
        Integer maxYearsExperience
) {
}

