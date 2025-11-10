package ie.com.rag.controller;

import ie.com.rag.dto.*;
import ie.com.rag.service.CandidateService;
import ie.com.rag.service.JobAnalysisService;
import ie.com.rag.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr")
public class HRController {

    private final CandidateService candidateService;
    private final JobAnalysisService jobAnalysisService;
    private final DashboardService dashboardService;
    private static final Logger logger = LoggerFactory.getLogger(HRController.class);

    public HRController(CandidateService candidateService, JobAnalysisService jobAnalysisService, DashboardService dashboardService) {
        this.candidateService = candidateService;
        this.jobAnalysisService = jobAnalysisService;
        this.dashboardService = dashboardService;
    }

    /**
     * Get all candidates
     */
    @GetMapping("/candidates")
    public ResponseEntity<List<CandidateDTO>> getAllCandidates() {
        try {
            List<CandidateDTO> candidates = candidateService.getAllCandidates();
            return ResponseEntity.ok(candidates);
        } catch (Exception e) {
            logger.error("Error retrieving candidates: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get candidate by ID
     */
    @GetMapping("/candidates/{id}")
    public ResponseEntity<CandidateDTO> getCandidateById(@PathVariable UUID id) {
        try {
            CandidateDTO candidate = candidateService.getCandidateById(id.toString());
            return ResponseEntity.ok(candidate);
        } catch (Exception e) {
            logger.error("Error retrieving candidate {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Delete candidate by ID
     */
    @DeleteMapping("/candidates/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable UUID id) {
        try {
            candidateService.deleteCandidate(id.toString());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting candidate {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Unexpected error deleting candidate {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Analyze job requirements against candidates
     */
    @PostMapping("/analyze")
    public ResponseEntity<JobAnalysisResponseDTO> analyzeJob(@RequestBody JobAnalysisRequestDTO request) {
        try {
            // Validate required fields
            if (request.getJobTitle() == null || request.getJobTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            if (request.getJobDescription() == null || request.getJobDescription().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            JobAnalysisResponseDTO result = jobAnalysisService.analyzeJob(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error analyzing job: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get comprehensive dashboard metrics including candidate statistics,
     * skill analytics, experience distribution, and recent activity
     *
     * @return ResponseEntity with dashboard metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getDashboardMetrics() {
        try {
            Map<String, Object> metrics = dashboardService.getDashboardMetrics();
            logger.info("Dashboard metrics retrieved successfully");
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Error retrieving dashboard metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve dashboard metrics"));
        }
    }

    /**
     * Health check endpoint for the dashboard
     *
     * @return ResponseEntity with health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> getDashboardHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "HR Dashboard",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}
