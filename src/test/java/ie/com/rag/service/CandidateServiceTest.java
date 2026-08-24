package ie.com.rag.service;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.entity.Candidate;
import ie.com.rag.exception.CandidateNotFoundException;
import ie.com.rag.exception.CandidateSaveException;
import ie.com.rag.exception.CandidateValidationException;
import ie.com.rag.exception.CandidateValidationException.ValidationError;
import ie.com.rag.mapper.CandidateMapperInterface;
import ie.com.rag.repository.CandidateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private CandidateMapperInterface candidateMapper;

    @InjectMocks
    private CandidateService candidateService;

    private CandidateDTO buildCandidateDTO(final UUID id, final String name, final String email) {
        return new CandidateDTO(
                id, name, email, "+353 87 123 4567", "CV content here",
                "cv.pdf", List.of("Java", "Spring"), "5 years", "BSc Computer Science",
                5, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    // ------------------------------------------------------------------
    // saveCandidate
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should save candidate successfully and return DTO")
    void saveCandidate_success_returnsDto() {
        // Given
        final CandidateDTO expected = buildCandidateDTO(UUID.randomUUID(), "John Doe", "john@example.com");
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(candidateMapper.toDTO(any(Candidate.class))).thenReturn(expected);

        // When
        final CandidateDTO result = candidateService.saveCandidate(
                "John Doe", "john@example.com", "+353 87 123 4567",
                "Experienced Java developer", "cv.pdf",
                List.of("Java", " Spring ", "Java"), "5 years", "BSc",
                5);

        // Then
        assertThat(result).isSameAs(expected);

        final ArgumentCaptor<Candidate> captor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository).save(captor.capture());
        final Candidate saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("John Doe");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getSkills()).containsExactly("Java", "Spring");
        assertThat(saved.getYearsOfExperience()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should sanitize control characters and trim skills before saving")
    void saveCandidate_sanitizesInputs() {
        // Given
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(candidateMapper.toDTO(any(Candidate.class))).thenReturn(buildCandidateDTO(UUID.randomUUID(), "x", "x@x.com"));

        // When
        candidateService.saveCandidate(
                "Jo\u0000hn\u0001Doe", "  john@example.com  ", null,
                "cv \u0000 content\r\nline2", "cv.pdf",
                List.of("  Java  ", "Java", " Python "), null, null, null);

        // Then
        final ArgumentCaptor<Candidate> captor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository).save(captor.capture());
        final Candidate saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("JohnDoe");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getPhone()).isNull();
        assertThat(saved.getExperience()).isNull();
        assertThat(saved.getCvContent()).isEqualTo("cv content\nline2");
        assertThat(saved.getSkills()).containsExactly("Java", "Python");
    }

    @Test
    @DisplayName("Should reject missing required fields with validation errors")
    void saveCandidate_missingFields_throwsValidationException() {
        // When / Then
        assertThatThrownBy(() -> candidateService.saveCandidate(
                null, null, null, null, null, null, null, null, null))
                .isInstanceOf(CandidateValidationException.class)
                .satisfies(ex -> {
                    final CandidateValidationException v = (CandidateValidationException) ex;
                    assertThat(v.getErrors()).extracting(ValidationError::field)
                            .contains("name", "email", "cvContent", "originalFileName");
                });
        verify(candidateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject an invalid email format")
    void saveCandidate_invalidEmail_throws() {
        assertThatThrownBy(() -> candidateService.saveCandidate(
                "John", "not-an-email", null, "cv", "cv.pdf", null, null, null, null))
                .isInstanceOf(CandidateValidationException.class)
                .satisfies(ex -> {
                    final CandidateValidationException v = (CandidateValidationException) ex;
                    assertThat(v.getErrors()).extracting(ValidationError::field).contains("email");
                });
    }

    @Test
    @DisplayName("Should reject negative years of experience")
    void saveCandidate_negativeExperience_throws() {
        assertThatThrownBy(() -> candidateService.saveCandidate(
                "John", "john@example.com", null, "cv", "cv.pdf", null, null, null, -3))
                .isInstanceOf(CandidateValidationException.class)
                .satisfies(ex -> {
                    final CandidateValidationException v = (CandidateValidationException) ex;
                    assertThat(v.getErrors()).extracting(ValidationError::field).contains("yearsOfExperience");
                });
    }

    @Test
    @DisplayName("Should reject blank skills entries")
    void saveCandidate_blankSkill_throws() {
        assertThatThrownBy(() -> candidateService.saveCandidate(
                "John", "john@example.com", null, "cv", "cv.pdf", List.of("Java", "  "), null, null, null))
                .isInstanceOf(CandidateValidationException.class)
                .satisfies(ex -> {
                    final CandidateValidationException v = (CandidateValidationException) ex;
                    assertThat(v.getErrors()).extracting(ValidationError::field).contains("skills");
                });
    }

    @Test
    @DisplayName("Should wrap data integrity violations in CandidateSaveException")
    void saveCandidate_dataIntegrityViolation_wrapsException() {
        // Given
        when(candidateRepository.save(any(Candidate.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        // When / Then
        assertThatThrownBy(() -> candidateService.saveCandidate(
                "John", "john@example.com", null, "cv", "cv.pdf", null, null, null, null))
                .isInstanceOf(CandidateSaveException.class)
                .hasMessageContaining("data integrity violation");
    }

    @Test
    @DisplayName("Should wrap unexpected runtime errors in CandidateSaveException")
    void saveCandidate_unexpectedError_wrapsException() {
        // Given
        when(candidateRepository.save(any(Candidate.class))).thenThrow(new IllegalStateException("boom"));

        // When / Then
        assertThatThrownBy(() -> candidateService.saveCandidate(
                "John", "john@example.com", null, "cv", "cv.pdf", null, null, null, null))
                .isInstanceOf(CandidateSaveException.class)
                .hasMessageContaining("unexpected error");
    }

    // ------------------------------------------------------------------
    // retrieval
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should return all candidates ordered by creation date descending")
    void getAllCandidates_returnsAll() {
        // Given
        when(candidateRepository.findAllOrderByCreatedAtDesc()).thenReturn(List.of(new Candidate(), new Candidate()));
        when(candidateMapper.toDTO(any(Candidate.class)))
                .thenReturn(buildCandidateDTO(UUID.randomUUID(), "a", "a@example.com"),
                        buildCandidateDTO(UUID.randomUUID(), "b", "b@example.com"));

        // When
        final List<CandidateDTO> result = candidateService.getAllCandidates();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("a");
    }

    @Test
    @DisplayName("Should return candidate by ID")
    void getCandidateById_found_returnsCandidate() {
        // Given
        final UUID candidateId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        final Candidate candidate = new Candidate();
        candidate.setId(candidateId.toString());
        when(candidateRepository.findById(candidateId.toString())).thenReturn(Optional.of(candidate));
        when(candidateMapper.toDTO(candidate)).thenReturn(buildCandidateDTO(candidateId, "John", "john@example.com"));

        // When
        final CandidateDTO result = candidateService.getCandidateById(candidateId.toString());

        // Then
        assertThat(result.email()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should throw CandidateValidationException for blank ID")
    void getCandidateById_blankId_throws() {
        assertThatThrownBy(() -> candidateService.getCandidateById("  "))
                .isInstanceOf(CandidateValidationException.class)
                .hasMessageContaining("Candidate ID cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw CandidateNotFoundException when candidate is missing")
    void getCandidateById_notFound_throws() {
        // Given
        when(candidateRepository.findById("missing")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> candidateService.getCandidateById("missing"))
                .isInstanceOf(CandidateNotFoundException.class)
                .hasMessageContaining("missing");
    }

    // ------------------------------------------------------------------
    // deleteCandidate
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should delete an existing candidate")
    void deleteCandidate_existingCandidate_deletes() {
        // Given
        when(candidateRepository.existsById("c-1")).thenReturn(true);

        // When
        candidateService.deleteCandidate("c-1");

        // Then
        verify(candidateRepository).deleteById("c-1");
    }

    @Test
    @DisplayName("Should throw CandidateNotFoundException when deleting a missing candidate")
    void deleteCandidate_missingCandidate_throws() {
        // Given
        when(candidateRepository.existsById("missing")).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> candidateService.deleteCandidate("missing"))
                .isInstanceOf(CandidateNotFoundException.class);
        verify(candidateRepository, never()).deleteById(any());
    }

    // ------------------------------------------------------------------
    // findByEmail
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should find candidate by email")
    void findByEmail_validEmail_returnsCandidate() {
        // Given
        final Candidate candidate = new Candidate();
        candidate.setEmail("john@example.com");
        when(candidateRepository.findByEmail("john@example.com")).thenReturn(Optional.of(candidate));
        when(candidateMapper.toDTO(candidate)).thenReturn(buildCandidateDTO(UUID.randomUUID(), "John", "john@example.com"));

        // When
        final CandidateDTO result = candidateService.findByEmail("  john@example.com  ");

        // Then
        assertThat(result.email()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should throw for blank email")
    void findByEmail_blankEmail_throws() {
        assertThatThrownBy(() -> candidateService.findByEmail(""))
                .isInstanceOf(CandidateValidationException.class)
                .hasMessageContaining("Email cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw for invalid email format")
    void findByEmail_invalidEmail_throws() {
        assertThatThrownBy(() -> candidateService.findByEmail("not-an-email"))
                .isInstanceOf(CandidateValidationException.class)
                .hasMessageContaining("Email format is invalid");
    }

    @Test
    @DisplayName("Should throw CandidateNotFoundException when email is unknown")
    void findByEmail_unknownEmail_throws() {
        // Given
        when(candidateRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> candidateService.findByEmail("nobody@example.com"))
                .isInstanceOf(CandidateNotFoundException.class)
                .hasMessageContaining("nobody@example.com");
    }

    // ------------------------------------------------------------------
    // findByYearsOfExperience
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should find candidates within experience range")
    void findByYearsOfExperience_validRange_returnsCandidates() {
        // Given
        when(candidateRepository.findByYearsOfExperienceBetween(2, 8)).thenReturn(List.of(new Candidate()));
        when(candidateMapper.toDTO(any(Candidate.class)))
                .thenReturn(buildCandidateDTO(UUID.randomUUID(), "a", "a@example.com"));

        // When
        final List<CandidateDTO> result = candidateService.findByYearsOfExperience(2, 8);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should default null bounds to 0 and MAX_VALUE")
    void findByYearsOfExperience_nullBounds_useDefaults() {
        // Given
        when(candidateRepository.findByYearsOfExperienceBetween(0, Integer.MAX_VALUE)).thenReturn(List.of());

        // When
        candidateService.findByYearsOfExperience(null, null);

        // Then
        verify(candidateRepository).findByYearsOfExperienceBetween(0, Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("Should throw for negative minimum years")
    void findByYearsOfExperience_negativeMin_throws() {
        assertThatThrownBy(() -> candidateService.findByYearsOfExperience(-1, 5))
                .isInstanceOf(CandidateValidationException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("Should throw when min exceeds max")
    void findByYearsOfExperience_invertedRange_throws() {
        assertThatThrownBy(() -> candidateService.findByYearsOfExperience(10, 2))
                .isInstanceOf(CandidateValidationException.class)
                .hasMessageContaining("cannot be greater than maximum");
    }

    // ------------------------------------------------------------------
    // searchByName
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should search candidates by name ignoring case")
    void searchByName_validName_returnsCandidates() {
        // Given
        when(candidateRepository.findByNameContainingIgnoreCase("John")).thenReturn(List.of(new Candidate()));
        when(candidateMapper.toDTO(any(Candidate.class)))
                .thenReturn(buildCandidateDTO(UUID.randomUUID(), "John", "john@example.com"));

        // When
        final List<CandidateDTO> result = candidateService.searchByName("  John  ");

        // Then
        assertThat(result).hasSize(1);
        verify(candidateRepository).findByNameContainingIgnoreCase("John");
    }

    @Test
    @DisplayName("Should throw for blank search name")
    void searchByName_blankName_throws() {
        assertThatThrownBy(() -> candidateService.searchByName("  "))
                .isInstanceOf(CandidateValidationException.class)
                .hasMessageContaining("Search name cannot be null or empty");
    }
}
