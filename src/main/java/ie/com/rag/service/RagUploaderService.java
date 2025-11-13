package ie.com.rag.service;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.utils.TextUtils;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class RagUploaderService {

    private static final Logger logger = LoggerFactory.getLogger(RagUploaderService.class);

    private final CandidateService candidateService;

    private final DashboardService dashboardService;

    private final RagDocumentService ragDocumentService;

    private final NLPSkillExtractorService nlpSkillExtractorService;

    public RagUploaderService(CandidateService candidateService, DashboardService dashboardService,
                            RagDocumentService ragDocumentService, NLPSkillExtractorService nlpSkillExtractorService) {
        this.candidateService = candidateService;
        this.dashboardService = dashboardService;
        this.ragDocumentService = ragDocumentService;
        this.nlpSkillExtractorService = nlpSkillExtractorService;
    }

    /**
     * Process uploaded CV file and extract candidate information
     */
    public CandidateDTO processCV(MultipartFile file, String name, String email, String phone) {
        try {
            logger.info("Processing CV upload for candidate: {} ({})", name, email);

            // Extract text content from the file
            String cvContent = extractTextFromFile(file);
            logger.debug("Extracted CV content length: {} characters", cvContent.length());

            // Save document info to dashboard
            dashboardService.saveUploadedDocumentInfo(
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
            );

            // Extract structured information from CV content using RAG service
            List<String> skills = extractSkills(cvContent);
            String experience = extractExperience(cvContent);
            String education = extractEducation(cvContent);
            Integer yearsOfExperience = extractYearsOfExperience(cvContent);

            logger.debug("Extracted skills: {}", skills);
            logger.debug("Extracted experience: {}", experience);
            logger.debug("Extracted education: {}", education);
            logger.debug("Extracted years of experience: {}", yearsOfExperience);

            // Process the CV content through RAG service for chunking and storage
            try {
                ragDocumentService.processDocument(cvContent, file.getOriginalFilename());
                logger.info("CV content processed through RAG service successfully");
            } catch (Exception e) {
                logger.warn("Failed to process CV through RAG service: {}", e.getMessage());
                // Continue processing even if RAG service fails
            }

            // Save candidate to database
            CandidateDTO candidate = candidateService.saveCandidate(
                name, email, phone, cvContent, file.getOriginalFilename(),
                skills, experience, education, yearsOfExperience
            );

            logger.info("CV processing completed successfully for candidate ID: {}", candidate.getId());
            return candidate;

        } catch (Exception e) {
            logger.error("Error processing CV for candidate {} ({}): {}", name, email, e.getMessage(), e);
            throw new RuntimeException("Failed to process CV: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text content from uploaded file
     */
    private String extractTextFromFile(MultipartFile file) throws IOException {
        try {
            String contentType = file.getContentType();
            String content;

            if (contentType != null && contentType.contains("text")) {
                // Handle text files
                content = new String(file.getBytes(), "UTF-8");
                logger.info("Processing text file: {}", file.getOriginalFilename());
            } else if (contentType != null && contentType.contains("pdf")) {
                // For PDF files, use Spring AI PDF reader
                try {
                    ByteArrayResource resource = new ByteArrayResource(file.getBytes());
                    PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
                    List<Document> documents = pdfReader.get();

                    content = documents.stream()
                                .map(Document::getContent)
                                .collect(Collectors.joining("\n"));

                    logger.info("PDF file processed successfully: {} pages extracted from {}",
                              documents.size(), file.getOriginalFilename());
                } catch (Exception e) {
                    logger.error("Failed to parse PDF file {}: {}", file.getOriginalFilename(), e.getMessage());
                    // Fallback to treating as text (though this will likely fail)
                    content = new String(file.getBytes(), "UTF-8");
                }
            } else if (contentType != null && (contentType.contains("word") || contentType.contains("docx"))) {
                // For Word documents - currently not supported, treat as text
                logger.warn("Word document processing not fully implemented for {}, treating as text", file.getOriginalFilename());
                content = new String(file.getBytes(), "UTF-8");
            } else {
                // Default: treat as text
                content = new String(file.getBytes(), "UTF-8");
                logger.warn("Unknown file type {} for {}, treating as text", contentType, file.getOriginalFilename());
            }

            // Clean the content to remove null bytes and other problematic characters
            if (content != null) {
                content = TextUtils.sanitizeTextContent(content);
                logger.debug("Extracted content preview (first 200 chars): {}",
                           content.length() > 200 ? content.substring(0, 200) + "..." : content);
            }

            return content;

        } catch (IOException e) {
            logger.error("Error extracting text from file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw e;
        }
    }

    /**
     * Extract skills from CV content using advanced NLP
     */
    private List<String> extractSkills(String cvContent) {
        // Use the NLPSkillExtractorService to extract skills
        List<String> extractedSkills = nlpSkillExtractorService.extractSkills(cvContent);

        if (extractedSkills != null && !extractedSkills.isEmpty()) {
            logger.info("Extracted skills using NLP: {}", extractedSkills);
            return extractedSkills;
        }

        // Fallback to simple pattern matching if NLP extraction fails
        String[] commonSkills = {
            "Java", "Python", "JavaScript", "React", "Angular", "Spring", "Node.js",
            "SQL", "PostgreSQL", "MySQL", "MongoDB", "Docker", "Kubernetes",
            "AWS", "Azure", "Git", "Jenkins", "CI/CD", "Agile", "Scrum",
            "HTML", "CSS", "REST API", "Microservices", "Leadership", "Communication"
        };
        logger.error("Extracted skills using NLP failed : {}", Arrays.toString(commonSkills));
        return Arrays.stream(commonSkills)
            .filter(skill -> cvContent.toLowerCase().contains(skill.toLowerCase()))
            .toList();
    }

    /**
     * Extract work experience section from CV
     */
    private String extractExperience(String cvContent) {
        // Simple experience extraction - look for experience-related sections
        Pattern experiencePattern = Pattern.compile(
            "(?i)(experience|work history|employment|career)(.*?)(?=education|skills|references|$)",
            Pattern.DOTALL
        );

        Matcher matcher = experiencePattern.matcher(cvContent);
        if (matcher.find()) {
            return matcher.group(2).trim().substring(0, Math.min(1000, matcher.group(2).trim().length()));
        }

        return "Experience details not clearly identified";
    }

    /**
     * Extract education section from CV
     */
    private String extractEducation(String cvContent) {
        // Improved education extraction with multiple strategies

        // Strategy 1: Look for common education section headers
        Pattern educationHeaderPattern = Pattern.compile(
            "(?i)\\b(education|academic|qualifications?|degrees?|diplomas?|certifications?|training|schooling|university|college)\\b[\\s:]*\\n?(.*?)(?=\\n\\s*\\b(experience|work|employment|skills|references|achievements|projects|languages)\\b|$)",
            Pattern.DOTALL
        );

        Matcher headerMatcher = educationHeaderPattern.matcher(cvContent);
        if (headerMatcher.find()) {
            String educationContent = headerMatcher.group(2).trim();
            if (educationContent.length() > 10) { // Ensure we have meaningful content
                return educationContent.substring(0, Math.min(1000, educationContent.length()));
            }
        }

        // Strategy 2: Look for degree patterns (Bachelor's, Master's, PhD, etc.)
        Pattern degreePattern = Pattern.compile(
            "(?i)\\b(bachelor'?s?|master'?s?|phd|doctorate|associate|diploma|certificate|b\\.?[a-z]{1,4}|m\\.?[a-z]{1,4}|ph\\.?d\\.?)\\s+(of|in|degree)\\s+[a-zA-Z\\s,]+",
            Pattern.CASE_INSENSITIVE
        );

        Matcher degreeMatcher = degreePattern.matcher(cvContent);
        StringBuilder educationInfo = new StringBuilder();

        while (degreeMatcher.find()) {
            if (educationInfo.length() > 0) {
                educationInfo.append("; ");
            }
            educationInfo.append(degreeMatcher.group().trim());
        }

        if (educationInfo.length() > 0) {
            return educationInfo.toString();
        }

        // Strategy 3: Look for university/college names
        Pattern institutionPattern = Pattern.compile(
            "(?i)\\b(university|college|institute|academy|school)\\s+of\\s+[a-zA-Z\\s,]+|[a-zA-Z\\s,]+\\s+(university|college|institute)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher institutionMatcher = institutionPattern.matcher(cvContent);
        StringBuilder institutionInfo = new StringBuilder();

        while (institutionMatcher.find()) {
            if (institutionInfo.length() > 0) {
                institutionInfo.append("; ");
            }
            institutionInfo.append(institutionMatcher.group().trim());
        }

        if (institutionInfo.length() > 0) {
            return institutionInfo.toString();
        }

        return "Education details not clearly identified";
    }

    /**
     * Extract years of experience from CV content
     */
    private Integer extractYearsOfExperience(String cvContent) {
        // Look for patterns like "5 years", "10+ years", etc.
        Pattern yearsPattern = Pattern.compile("(\\d+)\\+?\\s*years?\\s*(?:of\\s*)?(?:experience|work)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = yearsPattern.matcher(cvContent);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse years of experience: {}", matcher.group(1));
            }
        }

        return null; // Unknown years of experience
    }
}
