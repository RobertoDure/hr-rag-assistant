package ie.com.rag.service;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.dto.QAHistoryDTO;
import ie.com.rag.dto.UploadedDocumentDTO;
import ie.com.rag.entity.Candidate;
import ie.com.rag.entity.JobAnalysis;
import ie.com.rag.entity.QAHistory;
import ie.com.rag.entity.UploadedDocument;
import ie.com.rag.repository.CandidateRepository;
import ie.com.rag.repository.JobAnalysisRepository;
import ie.com.rag.repository.QAHistoryRepository;
import ie.com.rag.repository.UploadedDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    private static final String UUID_1 = "123e4567-e89b-12d3-a456-426614174000";
    private static final String UUID_2 = "223e4567-e89b-12d3-a456-426614174000";

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private JobAnalysisRepository jobAnalysisRepository;

    @Mock
    private QAHistoryRepository qaHistoryRepository;

    @Mock
    private UploadedDocumentRepository uploadedDocumentRepository;

    @Mock
    private CandidateService candidateService;

    @InjectMocks
    private DashboardService dashboardService;

    /**
     * Stubs every repository aggregation used by {@code getDashboardMetrics} so that
     * partial tests do not trip over null returns from Mockito mocks.
     */
    private void stubEmptyDefaults() {
        when(candidateRepository.count()).thenReturn(0L);
        when(jobAnalysisRepository.count()).thenReturn(0L);
        when(uploadedDocumentRepository.count()).thenReturn(0L);
        when(uploadedDocumentRepository.countByUploadTimestampAfter(any())).thenReturn(0L);
        when(candidateRepository.findTopSkills(10)).thenReturn(List.of());
        when(candidateService.getAllCandidates()).thenReturn(List.of());
        when(candidateRepository.findExperienceDistribution()).thenReturn(List.of());
        when(candidateRepository.findAverageYearsOfExperience()).thenReturn(0.0);
        when(candidateRepository.findTopNOrderByCreatedAtDesc(5)).thenReturn(List.of());
        when(jobAnalysisRepository.findTopNOrderByCreatedAtDesc(5)).thenReturn(List.of());
        when(candidateRepository.findDailyCountsSince(any())).thenReturn(List.of());
        when(candidateRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        when(candidateRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(jobAnalysisRepository.findDailyCountsSince(any())).thenReturn(List.of());
    }

    private CandidateDTO buildCandidate(final List<String> skills, final Integer years) {
        return new CandidateDTO(
                UUID.randomUUID(), "Alice", "alice@example.com", "123",
                "cv", "cv.pdf", skills, "exp", "edu", years,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Should return all dashboard metric sections")
    void getDashboardMetrics_returnsAllSections() {
        // Given
        stubEmptyDefaults();
        when(candidateRepository.count()).thenReturn(10L);
        when(jobAnalysisRepository.count()).thenReturn(5L);
        when(uploadedDocumentRepository.count()).thenReturn(3L);
        when(uploadedDocumentRepository.countByUploadTimestampAfter(any())).thenReturn(2L);
        when(candidateRepository.findAverageYearsOfExperience()).thenReturn(4.5);

        // When
        final Map<String, Object> metrics = dashboardService.getDashboardMetrics();

        // Then
        assertThat(metrics).containsKeys(
                "totalCandidates", "totalJobAnalyses", "totalDocuments", "recentUploads",
                "topSkills", "skillDistribution", "experienceDistribution", "averageExperience",
                "recentCandidates", "recentJobAnalyses", "candidateGrowth", "analysisGrowth"
        );
        assertThat(metrics.get("totalCandidates")).isEqualTo(10L);
        assertThat(metrics.get("totalJobAnalyses")).isEqualTo(5L);
        assertThat(metrics.get("recentUploads")).isEqualTo(2L);
        assertThat(metrics.get("averageExperience")).isEqualTo(4.5);
    }

    @Test
    @DisplayName("Should compute skill and experience distributions")
    void getDashboardMetrics_computesDistributions() {
        // Given
        stubEmptyDefaults();
        when(candidateService.getAllCandidates()).thenReturn(List.of(
                buildCandidate(List.of("Java", "SQL"), 2),
                buildCandidate(List.of("Java", "Spring", "SQL", "AWS", "Docker"), 4),
                buildCandidate(List.of("a", "b", "c", "d", "e", "f", "g", "h"), 6),
                buildCandidate(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"), 12)
        ));
        when(candidateRepository.findExperienceDistribution()).thenReturn(List.of(
                new Object[]{"0-2", 1L},
                new Object[]{"3-5", 2L}
        ));

        // When
        final Map<String, Object> metrics = dashboardService.getDashboardMetrics();

        // Then
        @SuppressWarnings("unchecked")
        final Map<String, Integer> skillDistribution = (Map<String, Integer>) metrics.get("skillDistribution");
        assertThat(skillDistribution.get("1-3 skills")).isEqualTo(1);
        assertThat(skillDistribution.get("4-7 skills")).isEqualTo(1);
        assertThat(skillDistribution.get("8-10 skills")).isEqualTo(1);
        assertThat(skillDistribution.get("10+ skills")).isEqualTo(1);

        @SuppressWarnings("unchecked")
        final Map<String, Integer> experienceDistribution = (Map<String, Integer>) metrics.get("experienceDistribution");
        assertThat(experienceDistribution).containsEntry("0-2", 1).containsEntry("3-5", 2);
    }

    @Test
    @DisplayName("Should default average experience to 0.0 when repository returns null")
    void getDashboardMetrics_nullAverageExperience_defaultsToZero() {
        // Given
        stubEmptyDefaults();
        when(candidateRepository.findAverageYearsOfExperience()).thenReturn(null);

        // When
        final Map<String, Object> metrics = dashboardService.getDashboardMetrics();

        // Then
        assertThat(metrics.get("averageExperience")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should include growth metrics with daily data")
    void getDashboardMetrics_includesGrowthMetrics() {
        // Given
        stubEmptyDefaults();
        when(candidateRepository.findDailyCountsSince(any())).thenReturn(List.of(
                new Object[]{LocalDate.of(2024, 1, 1), 3L},
                new Object[]{LocalDate.of(2024, 1, 2), 5L}
        ));
        when(candidateRepository.countByCreatedAtAfter(any())).thenReturn(4L);
        when(candidateRepository.countByCreatedAtBetween(any(), any())).thenReturn(6L);

        // When
        final Map<String, Object> metrics = dashboardService.getDashboardMetrics();

        // Then
        @SuppressWarnings("unchecked")
        final Map<String, Object> candidateGrowth = (Map<String, Object>) metrics.get("candidateGrowth");
        assertThat(candidateGrowth.get("thisMonth")).isEqualTo(4L);
        assertThat(candidateGrowth.get("lastMonth")).isEqualTo(6L);
        assertThat(candidateGrowth.get("periodDays")).isEqualTo(30);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> dailyData = (List<Map<String, Object>>) candidateGrowth.get("dailyData");
        assertThat(dailyData).hasSize(2);
        assertThat(dailyData.get(0)).containsEntry("count", 3);
    }

    @Test
    @DisplayName("Should map recent candidates with skill count")
    void getDashboardMetrics_recentCandidates_includeSkillCount() {
        // Given
        stubEmptyDefaults();
        final Candidate candidate = new Candidate();
        candidate.setId(UUID_1);
        candidate.setName("Alice");
        candidate.setEmail("alice@example.com");
        candidate.setSkills(List.of("Java", "SQL", "Spring"));
        candidate.setYearsOfExperience(4);
        candidate.setCreatedAt(LocalDateTime.now());
        when(candidateRepository.findTopNOrderByCreatedAtDesc(5)).thenReturn(List.of(candidate));

        // When
        final Map<String, Object> metrics = dashboardService.getDashboardMetrics();

        // Then
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> recentCandidates = (List<Map<String, Object>>) metrics.get("recentCandidates");
        assertThat(recentCandidates).hasSize(1);
        assertThat(recentCandidates.get(0)).containsEntry("skillCount", 3)
                .containsEntry("yearsOfExperience", 4);
    }

    // ------------------------------------------------------------------
    // QA history
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should save trimmed QA history entry")
    void saveQAHistory_validInput_saves() {
        // When
        dashboardService.saveQAHistory("  What is Spring?  ", "  Spring is a framework  ");

        // Then
        final ArgumentCaptor<QAHistory> captor = ArgumentCaptor.forClass(QAHistory.class);
        verify(qaHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getQuestion()).isEqualTo("What is Spring?");
        assertThat(captor.getValue().getAnswer()).isEqualTo("Spring is a framework");
        assertThat(captor.getValue().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should reject blank question")
    void saveQAHistory_blankQuestion_throws() {
        assertThatThrownBy(() -> dashboardService.saveQAHistory("  ", "answer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Question cannot be null or empty");
    }

    @Test
    @DisplayName("Should reject blank answer")
    void saveQAHistory_blankAnswer_throws() {
        assertThatThrownBy(() -> dashboardService.saveQAHistory("question", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Answer cannot be null or empty");
    }

    @Test
    @DisplayName("Should return QA history DTOs")
    void getQAHistory_returnsDtos() {
        // Given
        final QAHistory history = new QAHistory();
        history.setId(UUID_1);
        history.setQuestion("q1");
        history.setAnswer("a1");
        history.setTimestamp(LocalDateTime.of(2024, 1, 1, 10, 0));
        when(qaHistoryRepository.findTopNOrderByTimestampDesc(50)).thenReturn(List.of(history));

        // When
        final List<QAHistoryDTO> result = dashboardService.getQAHistory();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(UUID.fromString(UUID_1));
        assertThat(result.get(0).question()).isEqualTo("q1");
    }

    // ------------------------------------------------------------------
    // uploaded documents
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should return uploaded documents sorted by upload date descending")
    void getUploadedDocuments_returnsSortedAndLimited() {
        // Given
        final UploadedDocument older = new UploadedDocument();
        older.setId(UUID_1);
        older.setFilename("old.pdf");
        older.setContentType("application/pdf");
        older.setFileSize(100L);
        older.setUploadedAt(LocalDateTime.of(2024, 1, 1, 10, 0));

        final UploadedDocument newer = new UploadedDocument();
        newer.setId(UUID_2);
        newer.setFilename("new.pdf");
        newer.setContentType("application/pdf");
        newer.setFileSize(200L);
        newer.setUploadedAt(LocalDateTime.of(2024, 1, 2, 10, 0));

        when(uploadedDocumentRepository.findAll()).thenReturn(List.of(older, newer));

        // When
        final List<UploadedDocumentDTO> result = dashboardService.getUploadedDocuments();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).fileName()).isEqualTo("new.pdf");
        assertThat(result.get(1).fileName()).isEqualTo("old.pdf");
    }

    @Test
    @DisplayName("Should save uploaded document info")
    void saveUploadedDocumentInfo_validInput_saves() {
        // When
        dashboardService.saveUploadedDocumentInfo("  cv.pdf  ", 1024, "  application/pdf  ");

        // Then
        final ArgumentCaptor<UploadedDocument> captor = ArgumentCaptor.forClass(UploadedDocument.class);
        verify(uploadedDocumentRepository).save(captor.capture());
        assertThat(captor.getValue().getFilename()).isEqualTo("cv.pdf");
        assertThat(captor.getValue().getFileSize()).isEqualTo(1024L);
        assertThat(captor.getValue().getContentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Should reject blank filename")
    void saveUploadedDocumentInfo_blankFilename_throws() {
        assertThatThrownBy(() -> dashboardService.saveUploadedDocumentInfo("", 10, "text/plain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Filename cannot be null or empty");
    }

    @Test
    @DisplayName("Should reject negative file size")
    void saveUploadedDocumentInfo_negativeSize_throws() {
        assertThatThrownBy(() -> dashboardService.saveUploadedDocumentInfo("cv.pdf", -1, "text/plain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size cannot be negative");
    }

    @Test
    @DisplayName("Should reject blank content type")
    void saveUploadedDocumentInfo_blankContentType_throws() {
        assertThatThrownBy(() -> dashboardService.saveUploadedDocumentInfo("cv.pdf", 10, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content type cannot be null or empty");
    }
}
