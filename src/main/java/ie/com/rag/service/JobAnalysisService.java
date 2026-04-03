package ie.com.rag.service;

import ie.com.rag.dto.*;
import ie.com.rag.entity.Candidate;
import ie.com.rag.entity.CandidateRanking;
import ie.com.rag.entity.JobAnalysis;
import ie.com.rag.exception.ResourceNotFoundException;
import ie.com.rag.repository.CandidateRankingRepository;
import ie.com.rag.repository.CandidateRepository;
import ie.com.rag.repository.JobAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobAnalysisService {

    private static final double MAX_MATCH_SCORE = 100.0;
    private static final double SKILLS_WEIGHT = 0.4;
    private static final double EXPERIENCE_WEIGHT = 0.3;
    private static final double EDUCATION_WEIGHT = 0.2;
    private static final double CONTENT_WEIGHT = 0.1;

    private final JobAnalysisRepository jobAnalysisRepository;
    private final CandidateRankingRepository candidateRankingRepository;
    private final CandidateRepository candidateRepository;
    private final ChatModel chatModel;
    private final CandidateService candidateService;
    private final TransactionTemplate transactionTemplate;

    /**
     * Analyzes a job requirement against all available candidates and provides a ranked recommendation.
     *
     * @param request the details of the job to analyze
     * @return a report containing the rankings and recommendation
     */
    public JobAnalysisResponseDTO analyzeJob(final JobAnalysisRequestDTO request) {
        validateRequest(request);

        final List<CandidateDTO> candidates = candidateService.getAllCandidates();
        final List<RankedCandidateDTO> rankedCandidates = rankCandidates(request, candidates);
        final String recommendation = generateRecommendation(request, rankedCandidates);

        final UUID analysisId = transactionTemplate.execute(
                status -> persistAnalysisWithRankings(request, rankedCandidates, recommendation, candidates.size())
        );
        if (analysisId == null) {
            throw new IllegalStateException("Failed to persist job analysis");
        }

        final JobAnalysisResponseDTO response = new JobAnalysisResponseDTO();
        response.setId(analysisId);
        response.setJobTitle(request.jobTitle());
        response.setJobDescription(request.jobDescription());
        response.setRequiredSkills(request.requiredSkills());
        response.setPreferredSkills(request.preferredSkills());
        response.setExperienceLevel(request.experienceLevel());
        response.setEducationRequirement(request.educationRequirement());
        response.setMinYearsExperience(request.minYearsExperience());
        response.setMaxYearsExperience(request.maxYearsExperience());
        response.setTotalCandidatesAnalyzed(candidates.size());
        response.setTopCandidateRecommendation(recommendation);
        response.setRankedCandidates(rankedCandidates);
        response.setCreatedAt(LocalDateTime.now());

        log.info("Job analysis completed for: {}, analyzed {} candidates", request.jobTitle(), candidates.size());

        return response;
    }

    /**
     * Validates a job analysis request.
     *
     * @param request the request to validate
     */
    private void validateRequest(final JobAnalysisRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Job analysis request cannot be null");
        }
        if (!StringUtils.hasText(request.jobTitle())) {
            throw new IllegalArgumentException("Job title is required");
        }
        if (!StringUtils.hasText(request.jobDescription())) {
            throw new IllegalArgumentException("Job description is required");
        }
    }

    /**
     * Persists the job analysis report along with its associated candidate rankings.
     *
     * @param request           the original request describing the job
     * @param rankedCandidates  the analyzed and ranked candidates
     * @param recommendation    the AI-generated candidate recommendation
     * @param totalCandidates   the total number of assessed candidates
     * @return the UUID of the saved analysis
     */
    private UUID persistAnalysisWithRankings(
            final JobAnalysisRequestDTO request,
            final List<RankedCandidateDTO> rankedCandidates,
            final String recommendation,
            final int totalCandidates
    ) {
        final UUID analysisId = saveJobAnalysis(request, totalCandidates, recommendation);
        saveCandidateRankings(analysisId, rankedCandidates);
        return analysisId;
    }

    /**
     * Computes the match score and ranks a list of candidates against the job request.
     *
     * @param jobRequest the job description criteria
     * @param candidates the list of candidates to rank
     * @return a sorted list of ranked candidates based on match score
     */
    private List<RankedCandidateDTO> rankCandidates(
            final JobAnalysisRequestDTO jobRequest,
            final List<CandidateDTO> candidates
    ) {
        final List<RankedCandidateDTO> rankedList = new ArrayList<>();

        for (final CandidateDTO candidate : candidates) {
            final double matchScore = calculateMatchScore(jobRequest, candidate);
            final List<String> highlights = generateKeyHighlights(jobRequest, candidate);

            final RankedCandidateDTO rankedCandidate = new RankedCandidateDTO(
                    candidate.id(),
                    candidate.name(),
                    candidate.email(),
                    candidate.phone(),
                    matchScore,
                    0,
                    highlights
            );

            rankedList.add(rankedCandidate);
        }

        rankedList.sort(Comparator.comparingDouble(RankedCandidateDTO::getMatchScore).reversed());

        for (int i = 0; i < rankedList.size(); i++) {
            rankedList.get(i).setRankingPosition(i + 1);
        }

        return rankedList;
    }

    /**
     * Calculates the overall match score between a job request and a specific candidate.
     *
     * @param jobRequest the job evaluation criteria
     * @param candidate  the candidate being scored
     * @return a combined score capped at MAX_MATCH_SCORE
     */
    private double calculateMatchScore(final JobAnalysisRequestDTO jobRequest, final CandidateDTO candidate) {
        double score = 0.0;

        final double skillsScore = calculateSkillsMatch(jobRequest, candidate);
        score += skillsScore * SKILLS_WEIGHT;

        final double experienceScore = calculateExperienceMatch(jobRequest, candidate);
        score += experienceScore * EXPERIENCE_WEIGHT;

        final double educationScore = calculateEducationMatch(jobRequest, candidate);
        score += educationScore * EDUCATION_WEIGHT;

        final double contentScore = calculateContentRelevance(jobRequest, candidate);
        score += contentScore * CONTENT_WEIGHT;

        return Math.min(score, MAX_MATCH_SCORE);
    }

    /**
     * Evaluates a candidate's skills against required and preferred job skills.
     *
     * @param jobRequest the job criteria containing skill requirements
     * @param candidate  the candidate to evaluate
     * @return a score representing skill matching efficiency
     */
    private double calculateSkillsMatch(final JobAnalysisRequestDTO jobRequest, final CandidateDTO candidate) {
        if (candidate.skills() == null || candidate.skills().isEmpty()) {
            return 0.0;
        }

        final List<String> candidateSkills = candidate.skills().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        double requiredScore = 0.0;
        double preferredScore = 0.0;

        if (jobRequest.requiredSkills() != null && !jobRequest.requiredSkills().isEmpty()) {
            final long matchedRequired = jobRequest.requiredSkills().stream()
                    .mapToLong(skill -> candidateSkills.contains(skill.toLowerCase()) ? 1 : 0)
                    .sum();
            requiredScore = (double) matchedRequired / jobRequest.requiredSkills().size() * 70.0;
        }

        if (jobRequest.preferredSkills() != null && !jobRequest.preferredSkills().isEmpty()) {
            final long matchedPreferred = jobRequest.preferredSkills().stream()
                    .mapToLong(skill -> candidateSkills.contains(skill.toLowerCase()) ? 1 : 0)
                    .sum();
            preferredScore = (double) matchedPreferred / jobRequest.preferredSkills().size() * 30.0;
        }

        return requiredScore + preferredScore;
    }

    /**
     * Compares a candidate's years of experience with requested minimums and maximums.
     *
     * @param jobRequest the job request with experience bounds
     * @param candidate  the candidate structure containing years of experience
     * @return an experience match score up to 100.0
     */
    private double calculateExperienceMatch(final JobAnalysisRequestDTO jobRequest, final CandidateDTO candidate) {
        final Integer candidateYears = candidate.yearsOfExperience();
        if (candidateYears == null) {
            return 50.0;
        }

        final Integer minYears = jobRequest.minYearsExperience();
        final Integer maxYears = jobRequest.maxYearsExperience();

        if (minYears == null && maxYears == null) {
            return 100.0;
        }

        if (minYears != null && candidateYears < minYears) {
            return Math.max(0.0, 100.0 - (minYears - candidateYears) * 10.0);
        }

        if (maxYears != null && candidateYears > maxYears) {
            return Math.max(50.0, 100.0 - (candidateYears - maxYears) * 5.0);
        }

        return 100.0;
    }

    /**
     * Evaluates education details against job requirements.
     *
     * @param jobRequest the job definition providing education criteria
     * @param candidate  the evaluated candidate
     * @return an education score up to 100.0
     */
    private double calculateEducationMatch(final JobAnalysisRequestDTO jobRequest, final CandidateDTO candidate) {
        if (jobRequest.educationRequirement() == null || jobRequest.educationRequirement().isEmpty()) {
            return 100.0;
        }

        if (candidate.education() == null || candidate.education().isEmpty()) {
            return 30.0;
        }

        final String candidateEducation = candidate.education().toLowerCase();
        final String requiredEducation = jobRequest.educationRequirement().toLowerCase();

        if (candidateEducation.contains(requiredEducation)) {
            return 100.0;
        }

        if (requiredEducation.contains("bachelor") && candidateEducation.contains("master")) {
            return 100.0;
        }

        if (requiredEducation.contains("master") && candidateEducation.contains("bachelor")) {
            return 70.0;
        }

        return 50.0;
    }

    /**
     * Generates a concise summary highlighting strengths of the candidate pertinent to the role.
     *
     * @param jobRequest the analysis criteria
     * @param candidate  the considered candidate
     * @return a concise list of text blurbs summarizing match characteristics
     */
    private List<String> generateKeyHighlights(final JobAnalysisRequestDTO jobRequest, final CandidateDTO candidate) {
        final List<String> highlights = new ArrayList<>();

        if (candidate.skills() != null && jobRequest.requiredSkills() != null) {
            final List<String> matchingSkills = candidate.skills().stream()
                    .filter(skill -> jobRequest.requiredSkills().stream()
                            .anyMatch(reqSkill -> reqSkill.equalsIgnoreCase(skill)))
                    .limit(3)
                    .collect(Collectors.toList());

            if (!matchingSkills.isEmpty()) {
                highlights.add("Key skills: " + String.join(", ", matchingSkills));
            }
        }

        if (candidate.yearsOfExperience() != null) {
            highlights.add(candidate.yearsOfExperience() + " years of experience");
        }

        if (candidate.education() != null && !candidate.education().isEmpty()) {
            final String education = candidate.education().length() > 50
                    ? candidate.education().substring(0, 50) + "..."
                    : candidate.education();
            highlights.add("Education: " + education);
        }

        return highlights;
    }

    /**
     * Requests an AI-generated analysis and recommendation tailored to the top candidate.
     *
     * @param request          the job description and requisites
     * @param rankedCandidates the sorted candidates based on scoring
     * @return a text-based recommendation describing the highest ranked match
     */
    private String generateRecommendation(
            final JobAnalysisRequestDTO request,
            final List<RankedCandidateDTO> rankedCandidates
    ) {
        if (rankedCandidates.isEmpty()) {
            return "No candidates found in the database.";
        }

        final RankedCandidateDTO topCandidate = rankedCandidates.get(0);

        final String prompt = String.format("""
            Based on the job analysis for "%s", the top candidate is %s with a match score of %.1f%%.
            
            Job Requirements:
            - Title: %s
            - Required Skills: %s
            - Experience Level: %s
            - Education: %s
            
            Top Candidate Highlights:
            - %s
            
            Please provide a brief professional recommendation (2-3 sentences) about this candidate for this role.
            """,
            request.jobTitle(),
            topCandidate.getName(),
            topCandidate.getMatchScore(),
            request.jobTitle(),
            request.requiredSkills() != null ? String.join(", ", request.requiredSkills()) : "Not specified",
            request.experienceLevel() != null ? request.experienceLevel() : "Not specified",
            request.educationRequirement() != null ? request.educationRequirement() : "Not specified",
            topCandidate.getKeyHighlights() != null ? String.join("; ", topCandidate.getKeyHighlights()) : "No highlights"
        );

        try {
            return chatModel.call(prompt);
        } catch (final RuntimeException e) {
            log.warn("Failed to generate AI recommendation: {}", e.getMessage());
            return String.format("%s is the top candidate with a %.1f%% match score based on the analysis criteria.",
                topCandidate.getName(), topCandidate.getMatchScore());
        }
    }

    /**
     * Saves a snapshot of a successful job analysis.
     *
     * @param request         the job parameters specifying the analysis
     * @param totalCandidates the number of candidates considered
     * @param recommendation  the final recommendation summary
     * @return the unique UUID of the stored analysis
     */
    private UUID saveJobAnalysis(
            final JobAnalysisRequestDTO request,
            final int totalCandidates,
            final String recommendation
    ) {
        final JobAnalysis jobAnalysis = new JobAnalysis();
        jobAnalysis.setJobTitle(request.jobTitle());
        jobAnalysis.setJobDescription(request.jobDescription());
        jobAnalysis.setRequiredSkills(request.requiredSkills());
        jobAnalysis.setPreferredSkills(request.preferredSkills());
        jobAnalysis.setExperienceLevel(request.experienceLevel());
        jobAnalysis.setEducationRequirement(request.educationRequirement());
        jobAnalysis.setMinYearsExperience(request.minYearsExperience());
        jobAnalysis.setMaxYearsExperience(request.maxYearsExperience());
        jobAnalysis.setTotalCandidatesAnalyzed(totalCandidates);
        jobAnalysis.setTopCandidateRecommendation(recommendation);

        try {
            final JobAnalysis savedAnalysis = jobAnalysisRepository.save(jobAnalysis);
            return UUID.fromString(savedAnalysis.getId());
        } catch (final RuntimeException e) {
            log.error("Error saving job analysis for title {}: {}", request.jobTitle(), e.getMessage(), e);
            throw new IllegalStateException("Failed to save job analysis", e);
        }
    }

    /**
     * Persists multiple candidate rankings tied to a specific analysis.
     *
     * @param analysisId       the ID of the containing JobAnalysis record
     * @param rankedCandidates the ordered list of evaluated candidates
     */
    private void saveCandidateRankings(final UUID analysisId, final List<RankedCandidateDTO> rankedCandidates) {
        final List<CandidateRanking> rankingEntities = rankedCandidates.stream()
                .map(candidate -> mapToCandidateRanking(analysisId, candidate))
                .toList();
        candidateRankingRepository.saveAll(rankingEntities);
    }

    /**
     * Converts a ranked candidate DTO into its persistent entity format.
     *
     * @param analysisId the relation parent ID
     * @param candidate  the computed ranking metrics representation
     * @return an entity mapped to the database schema
     */
    private CandidateRanking mapToCandidateRanking(final UUID analysisId, final RankedCandidateDTO candidate) {
        final CandidateRanking candidateRanking = new CandidateRanking();
        candidateRanking.setJobAnalysisId(analysisId.toString());
        candidateRanking.setCandidateId(candidate.getId());
        candidateRanking.setMatchScore(candidate.getMatchScore());
        candidateRanking.setRankingPosition(candidate.getRankingPosition());
        candidateRanking.setKeyHighlights(candidate.getKeyHighlights());
        return candidateRanking;
    }

    /**
     * Evaluates textual similarities and keyword matches between a curriculum and job description.
     *
     * @param jobRequest the job detailing responsibilities
     * @param candidate  the candidate housing the CV copy
     * @return a mapped relativity percent score
     */
    private double calculateContentRelevance(final JobAnalysisRequestDTO jobRequest, final CandidateDTO candidate) {
        if (candidate.cvContent() == null || candidate.cvContent().isEmpty()) {
            return 0.0;
        }
        if (!StringUtils.hasText(jobRequest.jobDescription())) {
            return 0.0;
        }

        final String cvContent = candidate.cvContent().toLowerCase();
        final String jobDescription = jobRequest.jobDescription().toLowerCase();
        final String[] jobKeywords = jobDescription.split("\\s+");
        final long validKeywordCount = Arrays.stream(jobKeywords)
                .filter(keyword -> keyword.length() > 3)
                .count();
        if (validKeywordCount == 0) {
            return 0.0;
        }

        final long matchCount = Arrays.stream(jobKeywords)
                .filter(keyword -> keyword.length() > 3)
                .mapToLong(keyword -> cvContent.contains(keyword) ? 1 : 0)
                .sum();

        return Math.min(100.0, (double) matchCount / validKeywordCount * 200.0);
    }

    @Transactional(readOnly = true)
    public Page<JobAnalysisResponseDTO> getAllAnalyses(final Pageable pageable) {
        return jobAnalysisRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toSummaryResponseDTO);
    }

    @Transactional(readOnly = true)
    public JobAnalysisResponseDTO getAnalysisById(final String id) {
        final JobAnalysis analysis = jobAnalysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobAnalysis", "id", id));

        final List<CandidateRanking> rankings =
                candidateRankingRepository.findByJobAnalysisIdOrderByRankingPosition(id);

        final List<RankedCandidateDTO> rankedCandidates = rankings.stream()
                .map(this::toRankedCandidateDTO)
                .toList();

        return toFullResponseDTO(analysis, rankedCandidates);
    }

    @Transactional
    public void deleteAnalysis(final String id) {
        if (!jobAnalysisRepository.existsById(id)) {
            throw new ResourceNotFoundException("JobAnalysis", "id", id);
        }
        candidateRankingRepository.deleteByJobAnalysisId(id);
        jobAnalysisRepository.deleteById(id);
        log.info("[RagWiser/JobAnalysisService] - deleteAnalysis: deleted analysis id: {}", id);
    }

    private JobAnalysisResponseDTO toSummaryResponseDTO(final JobAnalysis analysis) {
        final JobAnalysisResponseDTO dto = new JobAnalysisResponseDTO();
        dto.setId(UUID.fromString(analysis.getId()));
        dto.setJobTitle(analysis.getJobTitle());
        dto.setJobDescription(analysis.getJobDescription());
        dto.setRequiredSkills(analysis.getRequiredSkills());
        dto.setPreferredSkills(analysis.getPreferredSkills());
        dto.setExperienceLevel(analysis.getExperienceLevel());
        dto.setEducationRequirement(analysis.getEducationRequirement());
        dto.setMinYearsExperience(analysis.getMinYearsExperience());
        dto.setMaxYearsExperience(analysis.getMaxYearsExperience());
        dto.setTotalCandidatesAnalyzed(analysis.getTotalCandidatesAnalyzed());
        dto.setTopCandidateRecommendation(analysis.getTopCandidateRecommendation());
        dto.setCreatedAt(analysis.getCreatedAt());
        return dto;
    }

    private JobAnalysisResponseDTO toFullResponseDTO(final JobAnalysis analysis, final List<RankedCandidateDTO> rankedCandidates) {
        final JobAnalysisResponseDTO dto = toSummaryResponseDTO(analysis);
        dto.setRankedCandidates(rankedCandidates);
        return dto;
    }

    private RankedCandidateDTO toRankedCandidateDTO(final CandidateRanking ranking) {
        final String candidateIdStr = ranking.getCandidateId().toString();
        final Candidate candidate = candidateRepository.findById(candidateIdStr).orElse(null);
        final String name = candidate != null ? candidate.getName() : "Unknown";
        final String email = candidate != null ? candidate.getEmail() : "";
        final String phone = candidate != null ? candidate.getPhone() : "";
        return new RankedCandidateDTO(
                ranking.getCandidateId(),
                name,
                email,
                phone,
                ranking.getMatchScore(),
                ranking.getRankingPosition(),
                ranking.getKeyHighlights()
        );
    }
}
