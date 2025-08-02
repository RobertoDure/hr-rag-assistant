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
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import static ie.com.rag.Constants.*;

/**
 * Service class responsible for providing dashboard metrics and analytics.
 * Handles candidate statistics, job analysis data, and QA history.
 */
@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final CandidateRepository candidateRepository;
    private final JobAnalysisRepository jobAnalysisRepository;
    private final QAHistoryRepository qaHistoryRepository;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    private final CandidateService candidateService;

    public DashboardService(CandidateRepository candidateRepository,
                           JobAnalysisRepository jobAnalysisRepository,
                           QAHistoryRepository qaHistoryRepository,
                           UploadedDocumentRepository uploadedDocumentRepository,
                           CandidateService candidateService) {
        this.candidateRepository = candidateRepository;
        this.jobAnalysisRepository = jobAnalysisRepository;
        this.qaHistoryRepository = qaHistoryRepository;
        this.uploadedDocumentRepository = uploadedDocumentRepository;
        this.candidateService = candidateService;
    }

    /**
     * Retrieves comprehensive dashboard metrics including counts, analytics, and recent activity.
     *
     * @return Map containing all dashboard metrics
     */
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // Basic counts - can be executed in parallel for better performance
            populateBasicCounts(metrics);

            // Analytics data
            populateAnalytics(metrics);

            // Recent activity data
            populateRecentActivity(metrics);

            // Growth metrics
            populateGrowthMetrics(metrics);

            logger.info("Dashboard metrics generated successfully with {} data points", metrics.size());

        } catch (Exception e) {
            logger.error("Error generating dashboard metrics: {}", e.getMessage(), e);
            metrics.clear();
            metrics.put("error", "Failed to load dashboard metrics");
            metrics.put("errorMessage", e.getMessage());
        }

        return metrics;
    }

    /**
     * Populates basic count metrics.
     */
    private void populateBasicCounts(Map<String, Object> metrics) {
        metrics.put("totalCandidates", candidateRepository.count());
        metrics.put("totalJobAnalyses", jobAnalysisRepository.count());
        metrics.put("totalDocuments", uploadedDocumentRepository.count());
        metrics.put("recentUploads", getRecentUploadsCount());
    }

    /**
     * Populates analytics metrics including skills and experience data.
     */
    private void populateAnalytics(Map<String, Object> metrics) {
        metrics.put("topSkills", getTopSkills());
        metrics.put("skillDistribution", getSkillDistribution());
        metrics.put("experienceDistribution", getExperienceDistribution());
        metrics.put("averageExperience", getAverageExperience());
    }

    /**
     * Populates recent activity metrics.
     */
    private void populateRecentActivity(Map<String, Object> metrics) {
        metrics.put("recentCandidates", getRecentCandidates());
        metrics.put("recentJobAnalyses", getRecentJobAnalyses());
    }

    /**
     * Populates growth metrics for trending analysis.
     */
    private void populateGrowthMetrics(Map<String, Object> metrics) {
        metrics.put("candidateGrowth", getCandidateGrowthMetrics());
        metrics.put("analysisGrowth", getAnalysisGrowthMetrics());
    }

    private long getRecentUploadsCount() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RECENT_UPLOADS_DAYS);
        return candidateRepository.countByCreatedAtAfter(cutoffDate);
    }

    /**
     * Retrieves top skills with their occurrence counts.
     *
     * @return List of skill data maps containing name and count
     */
    private List<Map<String, Object>> getTopSkills() {
        List<Object[]> results = candidateRepository.findTopSkills(TOP_SKILLS_LIMIT);
        return results.stream()
                .map(this::mapSkillResult)
                .collect(Collectors.toList());
    }

    /**
     * Maps database result to skill data map.
     */
    private Map<String, Object> mapSkillResult(Object[] result) {
        Map<String, Object> skill = new HashMap<>();
        skill.put("name", result[0]);
        skill.put("count", result[1]);
        return skill;
    }

    /**
     * Calculates skill distribution across candidates.
     * Uses efficient stream processing for better performance.
     *
     * @return Map of skill ranges to candidate counts
     */
    private Map<String, Integer> getSkillDistribution() {
        List<CandidateDTO> candidates = candidateService.getAllCandidates();

        return candidates.stream()
                .filter(candidate -> candidate.getSkills() != null)
                .collect(Collectors.groupingBy(
                    this::categorizeSkillCount,
                    Collectors.reducing(0, unused -> 1, Integer::sum)
                ));
    }

    /**
     * Categorizes skill count into predefined ranges.
     */
    private String categorizeSkillCount(CandidateDTO candidate) {
        int skillCount = candidate.getSkills().size();
        if (skillCount <= 3) return SKILLS_1_3;
        if (skillCount <= 7) return SKILLS_4_7;
        if (skillCount <= 10) return SKILLS_8_10;
        return SKILLS_10_PLUS;
    }

    /**
     * Retrieves experience distribution from repository.
     *
     * @return Map of experience ranges to candidate counts
     */
    private Map<String, Integer> getExperienceDistribution() {
        List<Object[]> results = candidateRepository.findExperienceDistribution();
        return results.stream()
                .collect(Collectors.toMap(
                    result -> (String) result[0],
                    result -> ((Number) result[1]).intValue(),
                    Integer::sum // Handle duplicates by summing values
                ));
    }

    /**
     * Calculates average years of experience across all candidates.
     *
     * @return Average experience or 0.0 if no data available
     */
    private Double getAverageExperience() {
        return Optional.ofNullable(candidateRepository.findAverageYearsOfExperience())
                .orElse(0.0);
    }

    /**
     * Retrieves recent candidates with essential information.
     *
     * @return List of recent candidate data maps
     */
    private List<Map<String, Object>> getRecentCandidates() {
        List<Candidate> candidates = candidateRepository.findTopNOrderByCreatedAtDesc(DEFAULT_RECENT_ITEMS_LIMIT);
        return candidates.stream()
                .map(this::mapCandidateData)
                .collect(Collectors.toList());
    }

    /**
     * Maps candidate entity to data map for API response.
     */
    private Map<String, Object> mapCandidateData(Candidate candidate) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", candidate.getId());
        data.put("name", candidate.getName());
        data.put("email", candidate.getEmail());
        data.put("createdAt", candidate.getCreatedAt());
        data.put("skillCount", Optional.ofNullable(candidate.getSkills())
                .map(List::size)
                .orElse(0));
        data.put("yearsOfExperience", candidate.getYearsOfExperience());
        return data;
    }

    /**
     * Retrieves recent job analyses with essential information.
     *
     * @return List of recent job analysis data maps
     */
    private List<Map<String, Object>> getRecentJobAnalyses() {
        List<JobAnalysis> analyses = jobAnalysisRepository.findTopNOrderByCreatedAtDesc(DEFAULT_RECENT_ITEMS_LIMIT);
        return analyses.stream()
                .map(this::mapJobAnalysisData)
                .collect(Collectors.toList());
    }

    /**
     * Maps job analysis entity to data map for API response.
     */
    private Map<String, Object> mapJobAnalysisData(JobAnalysis analysis) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", analysis.getId());
        data.put("jobTitle", analysis.getJobTitle());
        data.put("candidatesAnalyzed", analysis.getTotalCandidatesAnalyzed());
        data.put("createdAt", analysis.getCreatedAt());
        return data;
    }

    /**
     * Calculates candidate growth metrics for the specified period.
     *
     * @return Map containing daily data and monthly totals
     */
    private Map<String, Object> getCandidateGrowthMetrics() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(GROWTH_METRICS_DAYS);
        List<Object[]> dailyCounts = candidateRepository.findDailyCountsSince(cutoffDate);

        List<Map<String, Object>> dailyData = dailyCounts.stream()
                .map(this::mapDailyCountResult)
                .collect(Collectors.toList());

        Map<String, Object> growth = new HashMap<>();
        growth.put("dailyData", dailyData);
        growth.put("thisMonth", getTotalCandidatesThisMonth());
        growth.put("lastMonth", getTotalCandidatesLastMonth());
        growth.put("periodDays", GROWTH_METRICS_DAYS);

        return growth;
    }

    /**
     * Calculates job analysis growth metrics for the specified period.
     *
     * @return Map containing daily analysis data
     */
    private Map<String, Object> getAnalysisGrowthMetrics() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(GROWTH_METRICS_DAYS);
        List<Object[]> dailyCounts = jobAnalysisRepository.findDailyCountsSince(cutoffDate);

        List<Map<String, Object>> dailyData = dailyCounts.stream()
                .map(this::mapDailyCountResult)
                .collect(Collectors.toList());

        Map<String, Object> growth = new HashMap<>();
        growth.put("dailyData", dailyData);
        growth.put("periodDays", GROWTH_METRICS_DAYS);

        return growth;
    }

    /**
     * Maps daily count database result to data map.
     */
    private Map<String, Object> mapDailyCountResult(Object[] result) {
        Map<String, Object> data = new HashMap<>();
        data.put("date", result[0].toString());
        data.put("count", ((Number) result[1]).intValue());
        return data;
    }

    /**
     * Calculates total candidates created this month.
     */
    private long getTotalCandidatesThisMonth() {
        LocalDateTime startOfMonth = getStartOfCurrentMonth();
        return candidateRepository.countByCreatedAtAfter(startOfMonth);
    }

    /**
     * Calculates total candidates created last month.
     */
    private long getTotalCandidatesLastMonth() {
        LocalDateTime startOfLastMonth = getStartOfLastMonth();
        LocalDateTime startOfThisMonth = getStartOfCurrentMonth();
        return candidateRepository.countByCreatedAtBetween(startOfLastMonth, startOfThisMonth);
    }

    /**
     * Gets the start of the current month.
     */
    private LocalDateTime getStartOfCurrentMonth() {
        return LocalDateTime.now()
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);
    }

    /**
     * Gets the start of the last month.
     */
    private LocalDateTime getStartOfLastMonth() {
        return LocalDateTime.now()
                .minusMonths(1)
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);
    }

    // --- QA History Methods ---

    /**
     * Saves a question-answer pair to the history.
     *
     * @param question The question asked
     * @param answer The corresponding answer
     * @throws IllegalArgumentException if question or answer is null/empty
     */
    public void saveQAHistory(String question, String answer) {
        validateQAInput(question, answer);

        try {
            QAHistory qaHistory = createQAHistory(question, answer);
            qaHistoryRepository.save(qaHistory);
            logger.debug("Saved QA history entry for question: {}",
                    question.length() > 50 ? question.substring(0, 50) + "..." : question);
        } catch (Exception e) {
            logger.error("Failed to save QA history: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save QA history", e);
        }
    }

    /**
     * Validates QA input parameters.
     */
    private void validateQAInput(String question, String answer) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Question cannot be null or empty");
        }
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalArgumentException("Answer cannot be null or empty");
        }
    }

    /**
     * Creates a new QAHistory entity.
     */
    private QAHistory createQAHistory(String question, String answer) {
        QAHistory qaHistory = new QAHistory();
        qaHistory.setQuestion(question.trim());
        qaHistory.setAnswer(answer.trim());
        qaHistory.setTimestamp(LocalDateTime.now());
        return qaHistory;
    }

    /**
     * Retrieves QA history with pagination support.
     *
     * @return List of QA history DTOs ordered by timestamp (most recent first)
     */
    public List<QAHistoryDTO> getQAHistory() {
        try {
            List<QAHistory> histories = qaHistoryRepository.findTopNOrderByTimestampDesc(QA_HISTORY_LIMIT);
            return histories.stream()
                    .map(this::mapToQAHistoryDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to retrieve QA history: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Maps QAHistory entity to DTO.
     */
    private QAHistoryDTO mapToQAHistoryDTO(QAHistory history) {
        return new QAHistoryDTO(
                UUID.fromString(history.getId()),
                history.getQuestion(),
                history.getAnswer(),
                history.getTimestamp()
        );
    }

    // --- Uploaded Documents Methods ---

    /**
     * Retrieves uploaded documents with pagination and sorting.
     *
     * @return List of uploaded document DTOs ordered by upload date (most recent first)
     */
    public List<UploadedDocumentDTO> getUploadedDocuments() {
        try {
            List<UploadedDocument> documents = uploadedDocumentRepository.findAll();
            return documents.stream()
                    .sorted(Comparator.comparing(UploadedDocument::getUploadedAt).reversed())
                    .limit(UPLOADED_DOCUMENTS_LIMIT)
                    .map(this::mapToUploadedDocumentDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to retrieve uploaded documents: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Maps UploadedDocument entity to DTO.
     */
    private UploadedDocumentDTO mapToUploadedDocumentDTO(UploadedDocument doc) {
        return new UploadedDocumentDTO(
                UUID.fromString(doc.getId()),
                doc.getFilename(),
                doc.getFileSize(),
                doc.getContentType(),
                doc.getUploadedAt()
        );
    }

    /**
     * Saves uploaded document information to the database.
     *
     * @param filename The name of the uploaded file
     * @param size The size of the file in bytes
     * @param contentType The MIME type of the file
     * @throws IllegalArgumentException if parameters are invalid
     */
    public void saveUploadedDocumentInfo(String filename, long size, String contentType) {
        validateUploadedDocumentInput(filename, size, contentType);

        try {
            UploadedDocument document = createUploadedDocument(filename, size, contentType);
            uploadedDocumentRepository.save(document);
            logger.info("Saved uploaded document info: {} ({} bytes, {})", filename, size, contentType);
        } catch (Exception e) {
            logger.error("Failed to save uploaded document info for {}: {}", filename, e.getMessage(), e);
            throw new RuntimeException("Failed to save uploaded document info", e);
        }
    }

    /**
     * Validates uploaded document input parameters.
     */
    private void validateUploadedDocumentInput(String filename, long size, String contentType) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        if (size < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }
    }

    /**
     * Creates a new UploadedDocument entity.
     */
    private UploadedDocument createUploadedDocument(String filename, long size, String contentType) {
        UploadedDocument document = new UploadedDocument();
        document.setFilename(filename.trim());
        document.setFileSize(size);
        document.setContentType(contentType.trim());
        document.setUploadedAt(LocalDateTime.now());
        return document;
    }
}
