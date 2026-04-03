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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static ie.com.rag.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CandidateRepository candidateRepository;
    private final JobAnalysisRepository jobAnalysisRepository;
    private final QAHistoryRepository qaHistoryRepository;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    private final CandidateService candidateService;

    /**
     * Retrieves aggregated metrics for the dashboard, including counts, analytics, activity, and growth.
     *
     * @return a map containing various dashboard metrics
     */
    public Map<String, Object> getDashboardMetrics() {
        final Map<String, Object> metrics = new HashMap<>();
        populateBasicCounts(metrics);
        populateAnalytics(metrics);
        populateRecentActivity(metrics);
        populateGrowthMetrics(metrics);
        log.info("Dashboard metrics generated successfully with {} data points", metrics.size());

        return metrics;
    }

    /**
     * Populates the metrics map with basic counts of entities in the system.
     *
     * @param metrics the map to populate with count data
     */
    private void populateBasicCounts(final Map<String, Object> metrics) {
        metrics.put("totalCandidates", candidateRepository.count());
        metrics.put("totalJobAnalyses", jobAnalysisRepository.count());
        metrics.put("totalDocuments", uploadedDocumentRepository.count());
        metrics.put("recentUploads", getRecentUploadsCount());
    }

    /**
     * Populates the metrics map with analytics data, such as skills and experience distribution.
     *
     * @param metrics the map to populate with analytics data
     */
    private void populateAnalytics(final Map<String, Object> metrics) {
        metrics.put("topSkills", getTopSkills());
        metrics.put("skillDistribution", getSkillDistribution());
        metrics.put("experienceDistribution", getExperienceDistribution());
        metrics.put("averageExperience", getAverageExperience());
    }

    /**
     * Populates the metrics map with recent activity data, such as recent candidates and job analyses.
     *
     * @param metrics the map to populate with recent activity data
     */
    private void populateRecentActivity(final Map<String, Object> metrics) {
        metrics.put("recentCandidates", getRecentCandidates());
        metrics.put("recentJobAnalyses", getRecentJobAnalyses());
    }

    /**
     * Populates the metrics map with growth trends over time.
     *
     * @param metrics the map to populate with growth metrics
     */
    private void populateGrowthMetrics(final Map<String, Object> metrics) {
        metrics.put("candidateGrowth", getCandidateGrowthMetrics());
        metrics.put("analysisGrowth", getAnalysisGrowthMetrics());
    }

    /**
     * Calculates the number of recently uploaded documents.
     *
     * @return the count of recent document uploads
     */
    private long getRecentUploadsCount() {
        final LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RECENT_UPLOADS_DAYS);
        return uploadedDocumentRepository.countByUploadTimestampAfter(cutoffDate);
    }

    /**
     * Retrieves the top skills across all candidates.
     *
     * @return a list of maps, each representing a top skill and its count
     */
    private List<Map<String, Object>> getTopSkills() {
        final List<Object[]> results = candidateRepository.findTopSkills(TOP_SKILLS_LIMIT);
        return results.stream()
                .map(this::mapSkillResult)
                .collect(Collectors.toList());
    }

    /**
     * Maps a query result representing a skill count to a map.
     *
     * @param result an array containing the skill name and count
     * @return a map with the mapped skill data
     */
    private Map<String, Object> mapSkillResult(final Object[] result) {
        final Map<String, Object> skill = new HashMap<>();
        skill.put("name", result[0]);
        skill.put("count", result[1]);
        return skill;
    }

    /**
     * Computes the distribution of candidates based on their number of skills.
     *
     * @return a map categorizing candidate skill counts
     */
    private Map<String, Integer> getSkillDistribution() {
        final List<CandidateDTO> candidates = candidateService.getAllCandidates();

        return candidates.stream()
                .filter(candidate -> candidate.skills() != null)
                .collect(Collectors.groupingBy(
                    this::categorizeSkillCount,
                    Collectors.reducing(0, unused -> 1, Integer::sum)
                ));
    }

    /**
     * Categorizes a candidate into a bucket based on their number of skills.
     *
     * @param candidate the candidate whose skills are counted
     * @return a string representing the skill count category
     */
    private String categorizeSkillCount(final CandidateDTO candidate) {
        final int skillCount = candidate.skills().size();
        if (skillCount <= 3) {
            return SKILLS_1_3;
        }
        if (skillCount <= 7) {
            return SKILLS_4_7;
        }
        if (skillCount <= 10) {
            return SKILLS_8_10;
        }
        return SKILLS_10_PLUS;
    }

    /**
     * Retrieves the distribution of experience levels across all candidates.
     *
     * @return a map describing the experience distribution
     */
    private Map<String, Integer> getExperienceDistribution() {
        final List<Object[]> results = candidateRepository.findExperienceDistribution();
        return results.stream()
                .collect(Collectors.toMap(
                    result -> (String) result[0],
                    result -> ((Number) result[1]).intValue(),
                    Integer::sum
                ));
    }

    /**
     * Computes the average years of experience of all candidates.
     *
     * @return the average years of experience
     */
    private Double getAverageExperience() {
        return Optional.ofNullable(candidateRepository.findAverageYearsOfExperience())
                .orElse(0.0);
    }

    /**
     * Retrieves a list of recently added candidates for the dashboard.
     *
     * @return a list of maps representing recent candidates
     */
    private List<Map<String, Object>> getRecentCandidates() {
        final List<Candidate> candidates = candidateRepository.findTopNOrderByCreatedAtDesc(DEFAULT_RECENT_ITEMS_LIMIT);
        return candidates.stream()
                .map(this::mapCandidateData)
                .collect(Collectors.toList());
    }

    /**
     * Maps a Candidate entity to a simplified map structure for the dashboard.
     *
     * @param candidate the Candidate entity
     * @return a map containing candidate data
     */
    private Map<String, Object> mapCandidateData(final Candidate candidate) {
        final Map<String, Object> data = new HashMap<>();
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
     * Retrieves a list of recent job analyses.
     *
     * @return a list of maps representing recent job analyses
     */
    private List<Map<String, Object>> getRecentJobAnalyses() {
        final List<JobAnalysis> analyses = jobAnalysisRepository.findTopNOrderByCreatedAtDesc(DEFAULT_RECENT_ITEMS_LIMIT);
        return analyses.stream()
                .map(this::mapJobAnalysisData)
                .collect(Collectors.toList());
    }

    /**
     * Maps a JobAnalysis entity to a simplified map structure.
     *
     * @param analysis the JobAnalysis entity
     * @return a map containing analysis data
     */
    private Map<String, Object> mapJobAnalysisData(final JobAnalysis analysis) {
        final Map<String, Object> data = new HashMap<>();
        data.put("id", analysis.getId());
        data.put("jobTitle", analysis.getJobTitle());
        data.put("candidatesAnalyzed", analysis.getTotalCandidatesAnalyzed());
        data.put("createdAt", analysis.getCreatedAt());
        return data;
    }

    /**
     * Calculates candidate growth metrics over a recent period.
     *
     * @return a map of candidate growth trends
     */
    private Map<String, Object> getCandidateGrowthMetrics() {
        final LocalDateTime cutoffDate = LocalDateTime.now().minusDays(GROWTH_METRICS_DAYS);
        final List<Object[]> dailyCounts = candidateRepository.findDailyCountsSince(cutoffDate);

        final List<Map<String, Object>> dailyData = dailyCounts.stream()
                .map(this::mapDailyCountResult)
                .collect(Collectors.toList());

        final Map<String, Object> growth = new HashMap<>();
        growth.put("dailyData", dailyData);
        growth.put("thisMonth", getTotalCandidatesThisMonth());
        growth.put("lastMonth", getTotalCandidatesLastMonth());
        growth.put("periodDays", GROWTH_METRICS_DAYS);

        return growth;
    }

    /**
     * Calculates job analysis growth metrics over a recent period.
     *
     * @return a map of job analysis growth trends
     */
    private Map<String, Object> getAnalysisGrowthMetrics() {
        final LocalDateTime cutoffDate = LocalDateTime.now().minusDays(GROWTH_METRICS_DAYS);
        final List<Object[]> dailyCounts = jobAnalysisRepository.findDailyCountsSince(cutoffDate);

        final List<Map<String, Object>> dailyData = dailyCounts.stream()
                .map(this::mapDailyCountResult)
                .collect(Collectors.toList());

        final Map<String, Object> growth = new HashMap<>();
        growth.put("dailyData", dailyData);
        growth.put("periodDays", GROWTH_METRICS_DAYS);

        return growth;
    }

    /**
     * Maps a daily count result from repository aggregations to a map.
     *
     * @param result an array containing the date and the count
     * @return a map representing daily count data
     */
    private Map<String, Object> mapDailyCountResult(final Object[] result) {
        final Map<String, Object> data = new HashMap<>();
        data.put("date", result[0].toString());
        data.put("count", ((Number) result[1]).intValue());
        return data;
    }

    /**
     * Calculates the total number of candidates added in the current month.
     *
     * @return the candidate count for this month
     */
    private long getTotalCandidatesThisMonth() {
        final LocalDateTime startOfMonth = getStartOfCurrentMonth();
        return candidateRepository.countByCreatedAtAfter(startOfMonth);
    }

    /**
     * Calculates the total number of candidates added in the previous month.
     *
     * @return the candidate count for last month
     */
    private long getTotalCandidatesLastMonth() {
        final LocalDateTime startOfLastMonth = getStartOfLastMonth();
        final LocalDateTime startOfThisMonth = getStartOfCurrentMonth();
        return candidateRepository.countByCreatedAtBetween(startOfLastMonth, startOfThisMonth);
    }

    /**
     * Returns the start timestamp of the current month.
     *
     * @return the start of the current month
     */
    private LocalDateTime getStartOfCurrentMonth() {
        return LocalDateTime.now()
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);
    }

    /**
     * Returns the start timestamp of the previous month.
     *
     * @return the start of the previous month
     */
    private LocalDateTime getStartOfLastMonth() {
        return LocalDateTime.now()
                .minusMonths(1)
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);
    }

    /**
     * Saves a Question & Answer history record.
     *
     * @param question the user's question
     * @param answer   the corresponding answer
     */
    public void saveQAHistory(final String question, final String answer) {
        validateQAInput(question, answer);

        final QAHistory qaHistory = createQAHistory(question, answer);
        qaHistoryRepository.save(qaHistory);

        final String truncatedQuestion = question.length() > 50 ? question.substring(0, 50) + "..." : question;
        log.debug("Saved QA history entry for question: {}", truncatedQuestion);
    }

    /**
     * Validates input fields for a QA history record.
     *
     * @param question the question
     * @param answer   the answer
     */
    private void validateQAInput(final String question, final String answer) {
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("Question cannot be null or empty");
        }
        if (!StringUtils.hasText(answer)) {
            throw new IllegalArgumentException("Answer cannot be null or empty");
        }
    }

    /**
     * Constructs a QAHistory entity.
     *
     * @param question the mapped question
     * @param answer   the mapped answer
     * @return a newly populated QAHistory object
     */
    private QAHistory createQAHistory(final String question, final String answer) {
        final QAHistory qaHistory = new QAHistory();
        qaHistory.setQuestion(question.trim());
        qaHistory.setAnswer(answer.trim());
        qaHistory.setTimestamp(LocalDateTime.now());
        return qaHistory;
    }

    /**
     * Retrieves a summary of the most recent QA history entries.
     *
     * @return a list of QA history DTOs
     */
    public List<QAHistoryDTO> getQAHistory() {
        final List<QAHistory> histories = qaHistoryRepository.findTopNOrderByTimestampDesc(QA_HISTORY_LIMIT);
        return histories.stream()
                .map(this::mapToQAHistoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Maps a QAHistory entity to a QAHistoryDTO.
     *
     * @param history the QAHistory entity
     * @return the corresponding DTO
     */
    private QAHistoryDTO mapToQAHistoryDTO(final QAHistory history) {
        return new QAHistoryDTO(
                UUID.fromString(history.getId()),
                history.getQuestion(),
                history.getAnswer(),
                history.getTimestamp()
        );
    }

    /**
     * Retrieves recently uploaded document metadata.
     *
     * @return a list of recent Document DTOs
     */
    public List<UploadedDocumentDTO> getUploadedDocuments() {
        final List<UploadedDocument> documents = uploadedDocumentRepository.findAll();
        return documents.stream()
                .sorted(Comparator.comparing(UploadedDocument::getUploadedAt).reversed())
                .limit(UPLOADED_DOCUMENTS_LIMIT)
                .map(this::mapToUploadedDocumentDTO)
                .collect(Collectors.toList());
    }

    /**
     * Maps an UploadedDocument entity to its DTO counterpart.
     *
     * @param doc the UploadedDocument entity
     * @return the corresponding DTO
     */
    private UploadedDocumentDTO mapToUploadedDocumentDTO(final UploadedDocument doc) {
        return new UploadedDocumentDTO(
                UUID.fromString(doc.getId()),
                doc.getFilename(),
                doc.getFileSize(),
                doc.getContentType(),
                doc.getUploadedAt()
        );
    }

    /**
     * Logs and saves metadata for an uploaded document.
     *
     * @param filename    the name of the file
     * @param size        the file size in bytes
     * @param contentType the file's MIME type
     */
    public void saveUploadedDocumentInfo(final String filename, final long size, final String contentType) {
        validateUploadedDocumentInput(filename, size, contentType);

        final UploadedDocument document = createUploadedDocument(filename, size, contentType);
        uploadedDocumentRepository.save(document);
        log.info("Saved uploaded document info: {} ({} bytes, {})", filename, size, contentType);
    }

    /**
     * Validates input details for an uploaded document.
     *
     * @param filename    the name of the file
     * @param size        the size of the file
     * @param contentType the MIME type of the file
     */
    private void validateUploadedDocumentInput(final String filename, final long size, final String contentType) {
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        if (size < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
        if (!StringUtils.hasText(contentType)) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }
    }

    /**
     * Creates an UploadedDocument entity.
     *
     * @param filename    the name of the file
     * @param size        the size of the file
     * @param contentType the MIME type of the file
     * @return the populated UploadedDocument entity
     */
    private UploadedDocument createUploadedDocument(final String filename, final long size, final String contentType) {
        final UploadedDocument document = new UploadedDocument();
        document.setFilename(filename.trim());
        document.setFileSize(size);
        document.setContentType(contentType.trim());
        document.setUploadedAt(LocalDateTime.now());
        return document;
    }
}
