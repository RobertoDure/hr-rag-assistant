package ie.com.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CandidateDTO(
        UUID id,
        String name,
        String email,
        String phone,
        String cvContent,
        @JsonProperty("originalFileName") String originalFileName,
        List<String> skills,
        String experience,
        String education,
        Integer yearsOfExperience,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
