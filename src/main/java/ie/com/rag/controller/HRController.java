package ie.com.rag.controller;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.dto.JobAnalysisRequestDTO;
import ie.com.rag.dto.JobAnalysisResponseDTO;
import ie.com.rag.service.CandidateService;
import ie.com.rag.service.DashboardService;
import ie.com.rag.service.JobAnalysisService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
@Tag(name = "HR Management", description = "HR candidate and job analysis management APIs")
@SecurityRequirement(name = "bearerAuth")
public class HRController {

    private final CandidateService candidateService;
    private final JobAnalysisService jobAnalysisService;
    private final DashboardService dashboardService;

    @GetMapping("/candidates")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'USER')")
    @Operation(summary = "Get all candidates", description = "Retrieve all candidates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Candidates retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<CandidateDTO>> getAllCandidates() {
        return ResponseEntity.ok(candidateService.getAllCandidates());
    }

    @GetMapping("/candidates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'USER')")
    @Operation(summary = "Get candidate by ID", description = "Retrieve a candidate by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Candidate found"),
            @ApiResponse(responseCode = "404", description = "Candidate not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<CandidateDTO> getCandidateById(@PathVariable final UUID id) {
        return ResponseEntity.ok(candidateService.getCandidateById(id.toString()));
    }

    @DeleteMapping("/candidates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'USER')")
    @Operation(summary = "Delete candidate", description = "Delete a candidate by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Candidate deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Candidate not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteCandidate(@PathVariable final UUID id) {
        candidateService.deleteCandidate(id.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'USER')")
    @Operation(summary = "Analyze job requirements", description = "Analyze job requirements against available candidates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analysis completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<JobAnalysisResponseDTO> analyzeJob(@Valid @RequestBody final JobAnalysisRequestDTO request) {
        return ResponseEntity.ok(jobAnalysisService.analyzeJob(request));
    }

    @GetMapping("/analyses")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'USER')")
    @Operation(summary = "Get all job analyses", description = "Retrieve paginated job analyses ordered by creation date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analyses retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<JobAnalysisResponseDTO>> getAllAnalyses(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "10") final int size) {
        return ResponseEntity.ok(jobAnalysisService.getAllAnalyses(PageRequest.of(page, size)));
    }

    @GetMapping("/analyses/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'USER')")
    @Operation(summary = "Get job analysis by ID", description = "Retrieve a job analysis with full ranked candidates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analysis retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<JobAnalysisResponseDTO> getAnalysisById(@PathVariable final String id) {
        return ResponseEntity.ok(jobAnalysisService.getAnalysisById(id));
    }

    @DeleteMapping("/analyses/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'USER')")
    @Operation(summary = "Delete job analysis", description = "Delete a job analysis and its candidate rankings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Analysis deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteAnalysis(@PathVariable final String id) {
        jobAnalysisService.deleteAnalysis(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'USER')")
    @Operation(summary = "Get dashboard metrics", description = "Retrieve comprehensive dashboard metrics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getDashboardMetrics() {
        final Map<String, Object> metrics = dashboardService.getDashboardMetrics();
        log.info("[RagWiser/HRController] - getDashboardMetrics: metrics retrieved successfully");
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check the health of the HR dashboard service")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, String>> getDashboardHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "HR Dashboard",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}

