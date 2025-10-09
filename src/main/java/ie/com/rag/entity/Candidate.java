package ie.com.rag.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candidate {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "cv_content", columnDefinition = "TEXT")
    private String cvContent;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "skills", columnDefinition = "TEXT[]")
    @Type(value = io.hypersistence.utils.hibernate.type.array.ListArrayType.class)
    private List<String> skills;

    @Column(name = "experience", columnDefinition = "TEXT")
    private String experience;

    @Column(name = "education", columnDefinition = "TEXT")
    private String education;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
