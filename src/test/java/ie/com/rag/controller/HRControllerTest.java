package ie.com.rag.controller;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.dto.JobAnalysisRequestDTO;
import ie.com.rag.dto.JobAnalysisResponseDTO;
import ie.com.rag.dto.RankedCandidateDTO;
import ie.com.rag.service.CandidateService;
import ie.com.rag.service.DashboardService;
import ie.com.rag.service.JobAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HRControllerTest {

    private static final String CANDIDATE_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String ANALYSIS_ID = "223e4567-e89b-12d3-a456-426614174000";

    @Mock
    private CandidateService candidateService;

    @Mock
    private JobAnalysisService jobAnalysisService;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private HRController hrController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(hrController).build();
    }

    @Test
    @DisplayName("GET /api/hr/candidates should return all candidates")
    void getAllCandidates_returnsList() throws Exception {
        // Given
        final CandidateDTO candidate = new CandidateDTO(
                UUID.fromString(CANDIDATE_ID), "Alice", "alice@example.com", "123",
                "cv", "cv.pdf", List.of("Java"), "exp", "edu", 5,
                LocalDateTime.now(), LocalDateTime.now());
        when(candidateService.getAllCandidates()).thenReturn(List.of(candidate));

        // When / Then
        mockMvc.perform(get("/api/hr/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Alice")))
                .andExpect(jsonPath("$[0].email", is("alice@example.com")));
    }

    @Test
    @DisplayName("GET /api/hr/candidates/{id} should return a candidate")
    void getCandidateById_returnsCandidate() throws Exception {
        // Given
        final CandidateDTO candidate = new CandidateDTO(
                UUID.fromString(CANDIDATE_ID), "Alice", "alice@example.com", "123",
                "cv", "cv.pdf", List.of("Java"), "exp", "edu", 5,
                LocalDateTime.now(), LocalDateTime.now());
        when(candidateService.getCandidateById(CANDIDATE_ID)).thenReturn(candidate);

        // When / Then
        mockMvc.perform(get("/api/hr/candidates/{id}", CANDIDATE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice")));
    }

    @Test
    @DisplayName("DELETE /api/hr/candidates/{id} should return 204")
    void deleteCandidate_returnsNoContent() throws Exception {
        // When / Then
        mockMvc.perform(delete("/api/hr/candidates/{id}", CANDIDATE_ID))
                .andExpect(status().isNoContent());
        verify(candidateService).deleteCandidate(CANDIDATE_ID);
    }

    @Test
    @DisplayName("POST /api/hr/analyze should return the analysis result")
    void analyzeJob_returnsAnalysis() throws Exception {
        // Given
        final JobAnalysisResponseDTO response = new JobAnalysisResponseDTO();
        response.setId(UUID.fromString(ANALYSIS_ID));
        response.setJobTitle("Backend Engineer");
        response.setTotalCandidatesAnalyzed(1);
        response.setRankedCandidates(List.of(
                new RankedCandidateDTO(UUID.fromString(CANDIDATE_ID), "Alice", "alice@example.com",
                        "123", 78.0, 1, List.of("Key skills: Java"))));
        when(jobAnalysisService.analyzeJob(any(JobAnalysisRequestDTO.class))).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/hr/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobTitle\":\"Backend Engineer\",\"jobDescription\":\"Java Spring\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle", is("Backend Engineer")))
                .andExpect(jsonPath("$.totalCandidatesAnalyzed", is(1)))
                .andExpect(jsonPath("$.rankedCandidates[0].name", is("Alice")));
    }

    @Test
    @DisplayName("GET /api/hr/analyses should return a paginated list")
    void getAllAnalyses_returnsPage() throws Exception {
        // Given
        final JobAnalysisResponseDTO analysis = new JobAnalysisResponseDTO();
        analysis.setId(UUID.fromString(ANALYSIS_ID));
        analysis.setJobTitle("Backend Engineer");
        when(jobAnalysisService.getAllAnalyses(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(analysis), PageRequest.of(0, 10), 1));

        // When / Then
        mockMvc.perform(get("/api/hr/analyses").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].jobTitle", is("Backend Engineer")));
    }

    @Test
    @DisplayName("GET /api/hr/analyses/{id} should return a full analysis")
    void getAnalysisById_returnsAnalysis() throws Exception {
        // Given
        final JobAnalysisResponseDTO analysis = new JobAnalysisResponseDTO();
        analysis.setId(UUID.fromString(ANALYSIS_ID));
        analysis.setJobTitle("Backend Engineer");
        when(jobAnalysisService.getAnalysisById(ANALYSIS_ID)).thenReturn(analysis);

        // When / Then
        mockMvc.perform(get("/api/hr/analyses/{id}", ANALYSIS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle", is("Backend Engineer")));
    }

    @Test
    @DisplayName("DELETE /api/hr/analyses/{id} should return 204")
    void deleteAnalysis_returnsNoContent() throws Exception {
        // When / Then
        mockMvc.perform(delete("/api/hr/analyses/{id}", ANALYSIS_ID))
                .andExpect(status().isNoContent());
        verify(jobAnalysisService).deleteAnalysis(ANALYSIS_ID);
    }

    @Test
    @DisplayName("GET /api/hr/metrics should return dashboard metrics")
    void getDashboardMetrics_returnsMetrics() throws Exception {
        // Given
        when(dashboardService.getDashboardMetrics())
                .thenReturn(Map.of("totalCandidates", 10, "totalJobAnalyses", 5));

        // When / Then
        mockMvc.perform(get("/api/hr/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCandidates", is(10)))
                .andExpect(jsonPath("$.totalJobAnalyses", is(5)));
    }

    @Test
    @DisplayName("GET /api/hr/health should report the service as healthy")
    void getDashboardHealth_returnsHealthy() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/hr/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("healthy")))
                .andExpect(jsonPath("$.service", is("HR Dashboard")));
    }
}
