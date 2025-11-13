package ie.com.rag.service;

import ie.com.rag.dto.*;
import ie.com.rag.entity.JobAnalysis;
import ie.com.rag.entity.CandidateRanking;
import ie.com.rag.repository.JobAnalysisRepository;
import ie.com.rag.repository.CandidateRankingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobAnalysisService {

    private final JobAnalysisRepository jobAnalysisRepository;
    private final CandidateRankingRepository candidateRankingRepository;
    private final ChatModel chatModel;
    private final CandidateService candidateService;
    private static final Logger logger = LoggerFactory.getLogger(JobAnalysisService.class);

    public JobAnalysisService(JobAnalysisRepository jobAnalysisRepository,
                             CandidateRankingRepository candidateRankingRepository,
                             ChatModel chatModel,
                             CandidateService candidateService) {
        this.jobAnalysisRepository = jobAnalysisRepository;
        this.candidateRankingRepository = candidateRankingRepository;
        this.chatModel = chatModel;
        this.candidateService = candidateService;
    }

    @Transactional
    public JobAnalysisResponseDTO analyzeJob(JobAnalysisRequestDTO request) {
        // Get all candidates
        List<CandidateDTO> candidates = candidateService.getAllCandidates();

        // Analyze and rank candidates
        List<RankedCandidateDTO> rankedCandidates = rankCandidates(request, candidates);

        // Generate AI recommendation
        String recommendation = generateRecommendation(request, rankedCandidates);

        // Save job analysis to database
        UUID analysisId = saveJobAnalysis(request, candidates.size(), recommendation);

        // Save candidate rankings
        saveCandidateRankings(analysisId, rankedCandidates);

        // Create response
        JobAnalysisResponseDTO response = new JobAnalysisResponseDTO();
        response.setId(analysisId);
        response.setJobTitle(request.getJobTitle());
        response.setJobDescription(request.getJobDescription());
        response.setRequiredSkills(request.getRequiredSkills());
        response.setPreferredSkills(request.getPreferredSkills());
        response.setExperienceLevel(request.getExperienceLevel());
        response.setEducationRequirement(request.getEducationRequirement());
        response.setMinYearsExperience(request.getMinYearsExperience());
        response.setMaxYearsExperience(request.getMaxYearsExperience());
        response.setTotalCandidatesAnalyzed(candidates.size());
        response.setTopCandidateRecommendation(recommendation);
        response.setRankedCandidates(rankedCandidates);
        response.setCreatedAt(LocalDateTime.now());

        logger.info("Job analysis completed for: {}, analyzed {} candidates",
                   request.getJobTitle(), candidates.size());

        return response;
    }

    private List<RankedCandidateDTO> rankCandidates(JobAnalysisRequestDTO jobRequest, List<CandidateDTO> candidates) {
        List<RankedCandidateDTO> rankedList = new ArrayList<>();

        for (CandidateDTO candidate : candidates) {
            double matchScore = calculateMatchScore(jobRequest, candidate);
            List<String> highlights = generateKeyHighlights(jobRequest, candidate);

            RankedCandidateDTO rankedCandidate = new RankedCandidateDTO(
                candidate.getId(),
                candidate.getName(),
                candidate.getEmail(),
                candidate.getPhone(),
                matchScore,
                0, // Will be set after sorting
                highlights
            );

            rankedList.add(rankedCandidate);
        }

        // Sort by match score descending
        rankedList.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));

        // Set ranking positions
        for (int i = 0; i < rankedList.size(); i++) {
            rankedList.get(i).setRankingPosition(i + 1);
        }

        return rankedList;
    }

    private double calculateMatchScore(JobAnalysisRequestDTO jobRequest, CandidateDTO candidate) {
        double score = 0.0;
        double maxScore = 100.0;

        // Skills matching (40% weight)
        double skillsScore = calculateSkillsMatch(jobRequest, candidate);
        score += skillsScore * 0.4;

        // Experience level matching (30% weight)
        double experienceScore = calculateExperienceMatch(jobRequest, candidate);
        score += experienceScore * 0.3;

        // Education matching (20% weight)
        double educationScore = calculateEducationMatch(jobRequest, candidate);
        score += educationScore * 0.2;

        // CV content relevance (10% weight)
        double contentScore = calculateContentRelevance(jobRequest, candidate);
        score += contentScore * 0.1;

        return Math.min(score, maxScore);
    }

    private double calculateSkillsMatch(JobAnalysisRequestDTO jobRequest, CandidateDTO candidate) {
        if (candidate.getSkills() == null || candidate.getSkills().isEmpty()) {
            return 0.0;
        }

        List<String> candidateSkills = candidate.getSkills().stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());

        double requiredScore = 0.0;
        double preferredScore = 0.0;

        // Check required skills (70% of skills score)
        if (jobRequest.getRequiredSkills() != null && !jobRequest.getRequiredSkills().isEmpty()) {
            long matchedRequired = jobRequest.getRequiredSkills().stream()
                .mapToLong(skill -> candidateSkills.contains(skill.toLowerCase()) ? 1 : 0)
                .sum();
            requiredScore = (double) matchedRequired / jobRequest.getRequiredSkills().size() * 70.0;
        }

        // Check preferred skills (30% of skills score)
        if (jobRequest.getPreferredSkills() != null && !jobRequest.getPreferredSkills().isEmpty()) {
            long matchedPreferred = jobRequest.getPreferredSkills().stream()
                .mapToLong(skill -> candidateSkills.contains(skill.toLowerCase()) ? 1 : 0)
                .sum();
            preferredScore = (double) matchedPreferred / jobRequest.getPreferredSkills().size() * 30.0;
        }

        return requiredScore + preferredScore;
    }

    private double calculateExperienceMatch(JobAnalysisRequestDTO jobRequest, CandidateDTO candidate) {
        Integer candidateYears = candidate.getYearsOfExperience();
        if (candidateYears == null) {
            return 50.0; // Neutral score if no experience data
        }

        Integer minYears = jobRequest.getMinYearsExperience();
        Integer maxYears = jobRequest.getMaxYearsExperience();

        if (minYears == null && maxYears == null) {
            return 100.0; // No experience requirements
        }

        if (minYears != null && candidateYears < minYears) {
            return Math.max(0.0, 100.0 - (minYears - candidateYears) * 10.0);
        }

        if (maxYears != null && candidateYears > maxYears) {
            return Math.max(50.0, 100.0 - (candidateYears - maxYears) * 5.0);
        }

        return 100.0; // Perfect match
    }

    private double calculateEducationMatch(JobAnalysisRequestDTO jobRequest, CandidateDTO candidate) {
        if (jobRequest.getEducationRequirement() == null || jobRequest.getEducationRequirement().isEmpty()) {
            return 100.0; // No education requirements
        }

        if (candidate.getEducation() == null || candidate.getEducation().isEmpty()) {
            return 30.0; // Low score for missing education info
        }

        String candidateEducation = candidate.getEducation().toLowerCase();
        String requiredEducation = jobRequest.getEducationRequirement().toLowerCase();

        if (candidateEducation.contains(requiredEducation)) {
            return 100.0;
        }

        // Partial matching for education levels
        if (requiredEducation.contains("bachelor") && candidateEducation.contains("master")) {
            return 100.0; // Higher education satisfies lower requirement
        }

        if (requiredEducation.contains("master") && candidateEducation.contains("bachelor")) {
            return 70.0; // Lower education partially satisfies
        }

        return 50.0; // Basic score for any education mentioned
    }

    private double calculateContentRelevance(JobAnalysisRequestDTO jobRequest, CandidateDTO candidate) {
        if (candidate.getCvContent() == null || candidate.getCvContent().isEmpty()) {
            return 0.0;
        }

        String cvContent = candidate.getCvContent().toLowerCase();
        String jobDescription = jobRequest.getJobDescription().toLowerCase();

        // Simple keyword matching
        String[] jobKeywords = jobDescription.split("\\s+");
        long matchCount = Arrays.stream(jobKeywords)
            .filter(keyword -> keyword.length() > 3)
            .mapToLong(keyword -> cvContent.contains(keyword) ? 1 : 0)
            .sum();

        return Math.min(100.0, (double) matchCount / jobKeywords.length * 200.0);
    }

    private List<String> generateKeyHighlights(JobAnalysisRequestDTO jobRequest, CandidateDTO candidate) {
        List<String> highlights = new ArrayList<>();

        // Skill highlights
        if (candidate.getSkills() != null && jobRequest.getRequiredSkills() != null) {
            List<String> matchingSkills = candidate.getSkills().stream()
                .filter(skill -> jobRequest.getRequiredSkills().stream()
                    .anyMatch(reqSkill -> reqSkill.equalsIgnoreCase(skill)))
                .limit(3)
                .collect(Collectors.toList());

            if (!matchingSkills.isEmpty()) {
                highlights.add("Key skills: " + String.join(", ", matchingSkills));
            }
        }

        // Experience highlight
        if (candidate.getYearsOfExperience() != null) {
            highlights.add(candidate.getYearsOfExperience() + " years of experience");
        }

        // Education highlight
        if (candidate.getEducation() != null && !candidate.getEducation().isEmpty()) {
            String education = candidate.getEducation().length() > 50
                ? candidate.getEducation().substring(0, 50) + "..."
                : candidate.getEducation();
            highlights.add("Education: " + education);
        }

        return highlights;
    }

    private String generateRecommendation(JobAnalysisRequestDTO request, List<RankedCandidateDTO> rankedCandidates) {
        if (rankedCandidates.isEmpty()) {
            return "No candidates found in the database.";
        }

        RankedCandidateDTO topCandidate = rankedCandidates.get(0);

        String prompt = String.format("""
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
            request.getJobTitle(),
            topCandidate.getName(),
            topCandidate.getMatchScore(),
            request.getJobTitle(),
            request.getRequiredSkills() != null ? String.join(", ", request.getRequiredSkills()) : "Not specified",
            request.getExperienceLevel() != null ? request.getExperienceLevel() : "Not specified",
            request.getEducationRequirement() != null ? request.getEducationRequirement() : "Not specified",
            topCandidate.getKeyHighlights() != null ? String.join("; ", topCandidate.getKeyHighlights()) : "No highlights"
        );

        try {
            return chatModel.call(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate AI recommendation: {}", e.getMessage());
            return String.format("%s is the top candidate with a %.1f%% match score based on the analysis criteria.",
                topCandidate.getName(), topCandidate.getMatchScore());
        }
    }

    private UUID saveJobAnalysis(JobAnalysisRequestDTO request, int totalCandidates, String recommendation) {
        JobAnalysis jobAnalysis = new JobAnalysis();
        jobAnalysis.setJobTitle(request.getJobTitle());
        jobAnalysis.setJobDescription(request.getJobDescription());
        jobAnalysis.setRequiredSkills(request.getRequiredSkills());
        jobAnalysis.setPreferredSkills(request.getPreferredSkills());
        jobAnalysis.setExperienceLevel(request.getExperienceLevel());
        jobAnalysis.setEducationRequirement(request.getEducationRequirement());
        jobAnalysis.setMinYearsExperience(request.getMinYearsExperience());
        jobAnalysis.setMaxYearsExperience(request.getMaxYearsExperience());
        jobAnalysis.setTotalCandidatesAnalyzed(totalCandidates);
        jobAnalysis.setTopCandidateRecommendation(recommendation);

        try {
            JobAnalysis savedAnalysis = jobAnalysisRepository.save(jobAnalysis);
            return UUID.fromString(savedAnalysis.getId());
        } catch (Exception e) {
            logger.error("Error saving job analysis: {}", e.getMessage());
            throw new RuntimeException("Failed to save job analysis", e);
        }
    }

    private void saveCandidateRankings(UUID analysisId, List<RankedCandidateDTO> rankedCandidates) {
        for (RankedCandidateDTO candidate : rankedCandidates) {
            try {
                CandidateRanking candidateRanking = new CandidateRanking();
                candidateRanking.setJobAnalysisId(analysisId.toString());
                candidateRanking.setCandidateId(candidate.getId());
                candidateRanking.setMatchScore(candidate.getMatchScore());
                candidateRanking.setRankingPosition(candidate.getRankingPosition());
                candidateRanking.setKeyHighlights(candidate.getKeyHighlights());

                candidateRankingRepository.save(candidateRanking);
            } catch (Exception e) {
                logger.warn("Failed to save ranking for candidate {}: {}", candidate.getId(), e.getMessage());
            }
        }
    }
}
