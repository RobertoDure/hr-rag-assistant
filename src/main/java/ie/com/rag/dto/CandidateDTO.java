package ie.com.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDTO {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String cvContent;
    @JsonProperty("originalFileName")
    private String originalFileName;
    private List<String> skills;
    private String experience;
    private String education;
    private Integer yearsOfExperience;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
