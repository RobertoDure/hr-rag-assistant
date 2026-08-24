package ie.com.rag.service;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.dto.JobAnalysisRequestDTO;
import ie.com.rag.dto.JobAnalysisResponseDTO;
import ie.com.rag.dto.RankedCandidateDTO;
import ie.com.rag.entity.Candidate;
import ie.com.rag.entity.CandidateRanking;
import ie.com.rag.entity.JobAnalysis;
import ie.com.rag.exception.ResourceNotFoundException;
import ie.com.rag.repository.CandidateRankingRepository;
import ie.com.rag.repository.CandidateRepository;
import ie.com.rag.repository.JobAnalysisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobAnalysisServiceTest {

    private static final String ANALYSIS_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Mock
    private JobAnalysisRepository jobAnalysisRepository;

    @Mock
    private CandidateRankingRepository candidateRankingRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private ChatModel chatModel;

    @Mock
    private CandidateService candidateService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private JobAnalysisService jobAnalysisService;

    private CandidateDTO buildCandidate(final String id, final String name, final List<String> skills,
                                        final Integer years, final String education, final String cvContent) {
        return new CandidateDTO(
                UUID.fromString(id), name, name.toLowerCase() + "@example.com", "12345",
                cvContent, "cv.pdf", skills, "experience", education, years,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private JobAnalysisRequestDTO buildJobRequest() {
        return new JobAnalysisRequestDTO(
                "Backend Engineer",
                "Java Spring SQL",
                List.of("Java", "Spring", "SQL"),
                List.of("AWS", "Docker"),
                "Senior",
                "bachelor",
                3,
                8
        );
    }

    private void stubTransactionTemplateToRunCallback() {
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    final TransactionCallback<?> callback =
                            (TransactionCallback<?>) invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });
    }

    // ------------------------------------------------------------------
    // analyzeJob
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should analyze job, rank candidates and persist results")
    void analyzeJob_success_ranksAndPersists() {
        // Given
        final CandidateDTO candidateA = buildCandidate(
                "11111111-1111-1111-1111-111111111111", "Alice",
                List.of("Java", "Spring", "SQL"), 5, "BSc Computer Science",
                "Java Spring SQL PostgreSQL microservices");
        final CandidateDTO candidateB = buildCandidate(
                "22222222-2222-2222-2222-222222222222", "Bob",
                List.of("Python"), 1, "High school", "Python scripts");

        when(candidateService.getAllCandidates()).thenReturn(List.of(candidateA, candidateB));
        when(chatModel.call(anyString())).thenReturn("Alice is the best fit");

        stubTransactionTemplateToRunCallback();
        when(jobAnalysisRepository.save(any(JobAnalysis.class))).thenAnswer(invocation -> {
            final JobAnalysis saved = invocation.getArgument(0);
            saved.setId(ANALYSIS_ID);
            return saved;
        });
        when(candidateRankingRepository.saveAll(any())).thenReturn(List.of());

        // When
        final JobAnalysisResponseDTO response = jobAnalysisService.analyzeJob(buildJobRequest());

        // Then
        assertThat(response.getId()).isEqualTo(UUID.fromString(ANALYSIS_ID));
        assertThat(response.getJobTitle()).isEqualTo("Backend Engineer");
        assertThat(response.getTotalCandidatesAnalyzed()).isEqualTo(2);
        assertThat(response.getTopCandidateRecommendation()).isEqualTo("Alice is the best fit");

        assertThat(response.getRankedCandidates()).hasSize(2);
        final RankedCandidateDTO top = response.getRankedCandidates().get(0);
        assertThat(top.getName()).isEqualTo("Alice");
        assertThat(top.getRankingPosition()).isEqualTo(1);
        assertThat(top.getMatchScore()).isEqualTo(78.0);
        assertThat(top.getKeyHighlights()).contains("Key skills: Java, Spring, SQL");

        final RankedCandidateDTO second = response.getRankedCandidates().get(1);
        assertThat(second.getName()).isEqualTo("Bob");
        assertThat(second.getRankingPosition()).isEqualTo(2);
        assertThat(second.getMatchScore()).isEqualTo(34.0);

        verify(jobAnalysisRepository).save(any(JobAnalysis.class));
        verify(candidateRankingRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should use fallback recommendation when the chat model fails")
    void analyzeJob_chatModelFails_usesFallbackRecommendation() {
        // Given
        final CandidateDTO candidateA = buildCandidate(
                "11111111-1111-1111-1111-111111111111", "Alice",
                List.of("Java"), 5, null, "Java");
        when(candidateService.getAllCandidates()).thenReturn(List.of(candidateA));
        when(chatModel.call(anyString())).thenThrow(new RuntimeException("AI unavailable"));

        stubTransactionTemplateToRunCallback();
        when(jobAnalysisRepository.save(any(JobAnalysis.class))).thenAnswer(invocation -> {
            final JobAnalysis saved = invocation.getArgument(0);
            saved.setId(ANALYSIS_ID);
            return saved;
        });
        when(candidateRankingRepository.saveAll(any())).thenReturn(List.of());

        // When
        final JobAnalysisResponseDTO response = jobAnalysisService.analyzeJob(buildJobRequest());

        // Then
        assertThat(response.getTopCandidateRecommendation())
                .isEqualTo("Alice is the top candidate with a 55.3% match score based on the analysis criteria.");
    }

    @Test
    @DisplayName("Should handle analysis with no candidates")
    void analyzeJob_noCandidates_returnsEmptyRecommendation() {
        // Given
        when(candidateService.getAllCandidates()).thenReturn(List.of());

        stubTransactionTemplateToRunCallback();
        when(jobAnalysisRepository.save(any(JobAnalysis.class))).thenAnswer(invocation -> {
            final JobAnalysis saved = invocation.getArgument(0);
            saved.setId(ANALYSIS_ID);
            return saved;
        });

        // When
        final JobAnalysisResponseDTO response = jobAnalysisService.analyzeJob(buildJobRequest());

        // Then
        assertThat(response.getRankedCandidates()).isEmpty();
        assertThat(response.getTopCandidateRecommendation()).isEqualTo("No candidates found in the database.");
        assertThat(response.getTotalCandidatesAnalyzed()).isZero();
        verify(chatModel, never()).call(anyString());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null request")
    void analyzeJob_nullRequest_throws() {
        assertThatThrownBy(() -> jobAnalysisService.analyzeJob(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Should throw when job title is blank")
    void analyzeJob_blankTitle_throws() {
        final JobAnalysisRequestDTO request = new JobAnalysisRequestDTO(
                "  ", "desc", null, null, null, null, null, null);
        assertThatThrownBy(() -> jobAnalysisService.analyzeJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job title is required");
    }

    @Test
    @DisplayName("Should throw when job description is blank")
    void analyzeJob_blankDescription_throws() {
        final JobAnalysisRequestDTO request = new JobAnalysisRequestDTO(
                "title", "  ", null, null, null, null, null, null);
        assertThatThrownBy(() -> jobAnalysisService.analyzeJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job description is required");
    }

    // ------------------------------------------------------------------
    // getAllAnalyses / getAnalysisById / deleteAnalysis
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should return paginated analyses")
    void getAllAnalyses_returnsPage() {
        // Given
        final PageRequest pageable = PageRequest.of(0, 10);
        final JobAnalysis analysis = new JobAnalysis();
        analysis.setId(ANALYSIS_ID);
        analysis.setJobTitle("Backend Engineer");
        analysis.setJobDescription("desc");
        analysis.setTotalCandidatesAnalyzed(3);
        analysis.setCreatedAt(LocalDateTime.now());

        when(jobAnalysisRepository.findAllByOrderByCreatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(analysis), pageable, 1));

        // When
        final Page<JobAnalysisResponseDTO> result = jobAnalysisService.getAllAnalyses(pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getJobTitle()).isEqualTo("Backend Engineer");
        assertThat(result.getContent().get(0).getTotalCandidatesAnalyzed()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should return full analysis with ranked candidates")
    void getAnalysisById_found_returnsFullDto() {
        // Given
        final JobAnalysis analysis = new JobAnalysis();
        analysis.setId(ANALYSIS_ID);
        analysis.setJobTitle("Backend Engineer");
        analysis.setJobDescription("desc");
        analysis.setTotalCandidatesAnalyzed(1);

        final UUID candidateId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        final CandidateRanking ranking = new CandidateRanking();
        ranking.setJobAnalysisId(ANALYSIS_ID);
        ranking.setCandidateId(candidateId);
        ranking.setMatchScore(78.0);
        ranking.setRankingPosition(1);
        ranking.setKeyHighlights(List.of("Key skills: Java"));

        final Candidate candidate = new Candidate();
        candidate.setId(candidateId.toString());
        candidate.setName("Alice");
        candidate.setEmail("alice@example.com");
        candidate.setPhone("12345");

        when(jobAnalysisRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(analysis));
        when(candidateRankingRepository.findByJobAnalysisIdOrderByRankingPosition(ANALYSIS_ID))
                .thenReturn(List.of(ranking));
        when(candidateRepository.findById(candidateId.toString())).thenReturn(Optional.of(candidate));

        // When
        final JobAnalysisResponseDTO result = jobAnalysisService.getAnalysisById(ANALYSIS_ID);

        // Then
        assertThat(result.getJobTitle()).isEqualTo("Backend Engineer");
        assertThat(result.getRankedCandidates()).hasSize(1);
        assertThat(result.getRankedCandidates().get(0).getName()).isEqualTo("Alice");
        assertThat(result.getRankedCandidates().get(0).getMatchScore()).isEqualTo(78.0);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when analysis is missing")
    void getAnalysisById_notFound_throws() {
        // Given
        when(jobAnalysisRepository.findById("missing")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> jobAnalysisService.getAnalysisById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should delete analysis and its rankings")
    void deleteAnalysis_existingAnalysis_deletes() {
        // Given
        when(jobAnalysisRepository.existsById(ANALYSIS_ID)).thenReturn(true);

        // When
        jobAnalysisService.deleteAnalysis(ANALYSIS_ID);

        // Then
        verify(candidateRankingRepository).deleteByJobAnalysisId(ANALYSIS_ID);
        verify(jobAnalysisRepository).deleteById(ANALYSIS_ID);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting a missing analysis")
    void deleteAnalysis_missingAnalysis_throws() {
        // Given
        when(jobAnalysisRepository.existsById("missing")).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> jobAnalysisService.deleteAnalysis("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(jobAnalysisRepository, never()).deleteById(any());
    }

    // ------------------------------------------------------------------
    // private ranking helpers exercised through analyzeJob
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should cap experience score at 100 when no bounds requested")
    void analyzeJob_noExperienceBounds_fullExperienceScore() {
        // Given
        final CandidateDTO candidate = buildCandidate(
                "11111111-1111-1111-1111-111111111111", "Alice",
                List.of("Java"), 5, "BSc", "Java developer");
        when(candidateService.getAllCandidates()).thenReturn(List.of(candidate));
        when(chatModel.call(anyString())).thenReturn("ok");

        stubTransactionTemplateToRunCallback();
        when(jobAnalysisRepository.save(any(JobAnalysis.class))).thenAnswer(invocation -> {
            final JobAnalysis saved = invocation.getArgument(0);
            saved.setId(ANALYSIS_ID);
            return saved;
        });
        when(candidateRankingRepository.saveAll(any())).thenReturn(List.of());

        final JobAnalysisRequestDTO request = new JobAnalysisRequestDTO(
                "Title", "Java", List.of("Java"), null, null, null, null, null);

        // When
        final JobAnalysisResponseDTO response = jobAnalysisService.analyzeJob(request);

        // Then
        // skills: 1/1*70 = 70; experience: 100 (no bounds); education: 100 (no requirement);
        // content: "java" matched 1/1 -> 100
        // total: 70*.4 + 100*.3 + 100*.2 + 100*.1 = 28 + 30 + 20 + 10 = 88
        assertThat(response.getRankedCandidates().get(0).getMatchScore()).isEqualTo(88.0);
    }

    @Test
    @DisplayName("Should map rankings to candidates when building full response")
    void getAnalysisById_unknownCandidate_usesUnknownName() {
        // Given
        final JobAnalysis analysis = new JobAnalysis();
        analysis.setId(ANALYSIS_ID);
        analysis.setJobTitle("Title");
        analysis.setJobDescription("desc");
        analysis.setTotalCandidatesAnalyzed(1);

        final UUID candidateId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        final CandidateRanking ranking = new CandidateRanking();
        ranking.setJobAnalysisId(ANALYSIS_ID);
        ranking.setCandidateId(candidateId);
        ranking.setMatchScore(50.0);
        ranking.setRankingPosition(1);
        ranking.setKeyHighlights(List.of());

        when(jobAnalysisRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(analysis));
        when(candidateRankingRepository.findByJobAnalysisIdOrderByRankingPosition(ANALYSIS_ID))
                .thenReturn(List.of(ranking));
        when(candidateRepository.findById(candidateId.toString())).thenReturn(Optional.empty());

        // When
        final JobAnalysisResponseDTO result = jobAnalysisService.getAnalysisById(ANALYSIS_ID);

        // Then
        assertThat(result.getRankedCandidates().get(0).getName()).isEqualTo("Unknown");
    }
}
