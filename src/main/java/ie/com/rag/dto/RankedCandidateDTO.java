package ie.com.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankedCandidateDTO {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private Double matchScore;
    private Integer rankingPosition;
    private List<String> keyHighlights;
}
