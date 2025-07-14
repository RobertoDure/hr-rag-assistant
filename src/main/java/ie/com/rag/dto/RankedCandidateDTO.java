package ie.com.rag.dto;

import java.util.List;
import java.util.UUID;

public class RankedCandidateDTO {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private Double matchScore;
    private Integer rankingPosition;
    private List<String> keyHighlights;

    // Constructors
    public RankedCandidateDTO() {}

    public RankedCandidateDTO(UUID id, String name, String email, String phone,
                             Double matchScore, Integer rankingPosition, List<String> keyHighlights) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.matchScore = matchScore;
        this.rankingPosition = rankingPosition;
        this.keyHighlights = keyHighlights;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }

    public Integer getRankingPosition() { return rankingPosition; }
    public void setRankingPosition(Integer rankingPosition) { this.rankingPosition = rankingPosition; }

    public List<String> getKeyHighlights() { return keyHighlights; }
    public void setKeyHighlights(List<String> keyHighlights) { this.keyHighlights = keyHighlights; }
}
