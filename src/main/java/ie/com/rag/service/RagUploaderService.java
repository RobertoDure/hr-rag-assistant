package ie.com.rag.service;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.utils.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagUploaderService {

    private final CandidateService candidateService;
    private final DashboardService dashboardService;
    private final RagDocumentService ragDocumentService;
    private final NLPSkillExtractorService nlpSkillExtractorService;
    private final TransactionTemplate transactionTemplate;

    /**
     * Processes an uploaded Curriculum Vitae (CV) file and creates a new candidate record.
     *
     * @param file  the uploaded CV multipart file
     * @param name  the name of the candidate
     * @param email the email address of the candidate
     * @param phone the contact number of the candidate
     * @return the saved candidate data
     */
    public CandidateDTO processCV(final MultipartFile file, final String name, final String email, final String phone) {
        validateInput(file, name, email);
        log.info("Processing CV upload for candidate: {} ({})", name, email);

        final String originalFilename = resolveOriginalFilename(file);
        final String contentType = resolveContentType(file);

        final String cvContent = extractTextFromFile(file);
        log.debug("Extracted CV content length: {} characters", cvContent.length());

        final List<String> skills = extractSkills(cvContent);
        final String experience = extractExperience(cvContent);
        final String education = extractEducation(cvContent);
        final Integer yearsOfExperience = extractYearsOfExperience(cvContent);

        log.debug("Extracted skills: {}", skills);
        log.debug("Extracted experience: {}", experience);
        log.debug("Extracted education: {}", education);
        log.debug("Extracted years of experience: {}", yearsOfExperience);

        final CandidateDTO savedCandidate = transactionTemplate.execute(status -> {
            final CandidateDTO candidate = candidateService.saveCandidate(
                    name,
                    email,
                    phone,
                    cvContent,
                    originalFilename,
                    skills,
                    experience,
                    education,
                    yearsOfExperience
            );
            dashboardService.saveUploadedDocumentInfo(originalFilename, file.getSize(), contentType);
            return candidate;
        });

        if (savedCandidate == null) {
            throw new IllegalStateException("Failed to persist uploaded CV data");
        }

        try {
            ragDocumentService.processDocument(cvContent, originalFilename);
            log.info("CV content processed through RAG service successfully");
        } catch (final RuntimeException e) {
            log.warn("Failed to process CV through RAG service for candidate {}: {}", savedCandidate.id(), e.getMessage());
        }

        log.info("CV processing completed successfully for candidate ID: {}", savedCandidate.id());
        return savedCandidate;
    }

    /**
     * Validates the inputs required for processing a CV upload.
     *
     * @param file  the incoming file
     * @param name  the applicant's name
     * @param email the applicant's email
     */
    private void validateInput(final MultipartFile file, final String name, final String email) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Candidate name is required");
        }

        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Candidate email is required");
        }
    }

    /**
     * Extrapolates the original filename from the multipart structure.
     *
     * @param file the uploaded file
     * @return the resolved filename
     */
    private String resolveOriginalFilename(final MultipartFile file) {
        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new IllegalArgumentException("Uploaded filename is required");
        }

        return file.getOriginalFilename();
    }

    /**
     * Resolves the MIME content type from an incoming file upload payload.
     *
     * @param file the uploaded object
     * @return the MIME type, or a default octet-stream flag
     */
    private String resolveContentType(final MultipartFile file) {
        if (StringUtils.hasText(file.getContentType())) {
            return file.getContentType();
        }

        return "application/octet-stream";
    }

    /**
     * Extracts text content based on the identified content structure, utilizing simple formats or PDF structures.
     *
     * @param file the multipart input object
     * @return the extracted textual payload
     */
    private String extractTextFromFile(final MultipartFile file) {
        try {
            final String contentType = file.getContentType();
            String content;

            if (contentType != null && contentType.contains("text")) {
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
                log.info("Processing text file: {}", file.getOriginalFilename());
            } else if (contentType != null && contentType.contains("pdf")) {
                try {
                    final ByteArrayResource resource = new ByteArrayResource(file.getBytes());
                    final PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
                    final List<Document> documents = pdfReader.get();

                    content = documents.stream()
                            .map(Document::getContent)
                            .collect(Collectors.joining("\n"));

                    log.info("PDF file processed successfully: {} pages extracted from {}",
                            documents.size(), file.getOriginalFilename());
                } catch (final RuntimeException e) {
                    log.warn("Failed to parse PDF file {}, using UTF-8 fallback", file.getOriginalFilename());
                    content = new String(file.getBytes(), StandardCharsets.UTF_8);
                }
            } else if (contentType != null && (contentType.contains("word") || contentType.contains("docx"))) {
                log.warn("Word document parsing is not implemented for {}, using UTF-8 fallback", file.getOriginalFilename());
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
                log.warn("Unknown file type {} for {}, using UTF-8 fallback", contentType, file.getOriginalFilename());
            }

            if (content != null) {
                content = TextUtils.sanitizeTextContent(content);
                final String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                log.debug("Extracted content preview (first 200 chars): {}", preview);
            }

            return content;

        } catch (final IOException e) {
            log.error("Error extracting text from file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new IllegalStateException("Failed to extract text from uploaded file", e);
        }
    }

    /**
     * Retrieves an extrapolated skillset array utilizing the internal NLP sequence processor.
     *
     * @param cvContent the plaintext sequence representing a candidate's CV
     * @return a collection of identified skills
     */
    private List<String> extractSkills(final String cvContent) {
        if (!StringUtils.hasText(cvContent)) {
            return List.of();
        }

        final List<String> extractedSkills = nlpSkillExtractorService.extractSkills(cvContent);

        if (extractedSkills != null && !extractedSkills.isEmpty()) {
            log.info("Extracted skills using NLP: {}", extractedSkills);
            return extractedSkills;
        }

        final String[] commonSkills = {
            "Java", "Python", "JavaScript", "React", "Angular", "Spring", "Node.js",
            "SQL", "PostgreSQL", "MySQL", "MongoDB", "Docker", "Kubernetes",
            "AWS", "Azure", "Git", "Jenkins", "CI/CD", "Agile", "Scrum",
            "HTML", "CSS", "REST API", "Microservices", "Leadership", "Communication"
        };
        log.warn("NLP skill extraction returned no results, using static fallback list");
        return Arrays.stream(commonSkills)
            .filter(skill -> cvContent.toLowerCase().contains(skill.toLowerCase()))
            .toList();
    }

    /**
     * Extracts an extrapolated experience block natively from regex parsing heuristics.
     *
     * @param cvContent the plaintext sequence
     * @return the experience paragraph string
     */
    private String extractExperience(final String cvContent) {
        if (!StringUtils.hasText(cvContent)) {
            return "Experience details not clearly identified";
        }

        final Pattern experiencePattern = Pattern.compile(
            "(?i)(experience|work history|employment|career)(.*?)(?=education|skills|references|$)",
            Pattern.DOTALL
        );

        final Matcher matcher = experiencePattern.matcher(cvContent);
        if (matcher.find()) {
            return matcher.group(2).trim().substring(0, Math.min(1000, matcher.group(2).trim().length()));
        }

        return "Experience details not clearly identified";
    }

    /**
     * Determines educational credentials relying structurally on generic keyword anchors.
     *
     * @param cvContent the target extraction sequence
     * @return a consolidated representation of the applicant's academic history
     */
    private String extractEducation(final String cvContent) {
        if (!StringUtils.hasText(cvContent)) {
            return "Education details not clearly identified";
        }

        final Pattern educationHeaderPattern = Pattern.compile(
            "(?i)\\b(education|academic|qualifications?|degrees?|diplomas?|certifications?|training|schooling|university|college)\\b[\\s:]*\\n?(.*?)(?=\\n\\s*\\b(experience|work|employment|skills|references|achievements|projects|languages)\\b|$)",
            Pattern.DOTALL
        );

        final Matcher headerMatcher = educationHeaderPattern.matcher(cvContent);
        if (headerMatcher.find()) {
            final String educationContent = headerMatcher.group(2).trim();
            if (educationContent.length() > 10) {
                return educationContent.substring(0, Math.min(1000, educationContent.length()));
            }
        }

        final Pattern degreePattern = Pattern.compile(
            "(?i)\\b(bachelor'?s?|master'?s?|phd|doctorate|associate|diploma|certificate|b\\.?[a-z]{1,4}|m\\.?[a-z]{1,4}|ph\\.?d\\.?)\\s+(of|in|degree)\\s+[a-zA-Z\\s,]+",
            Pattern.CASE_INSENSITIVE
        );

        final Matcher degreeMatcher = degreePattern.matcher(cvContent);
        final StringBuilder educationInfo = new StringBuilder();

        while (degreeMatcher.find()) {
            if (educationInfo.length() > 0) {
                educationInfo.append("; ");
            }
            educationInfo.append(degreeMatcher.group().trim());
        }

        if (educationInfo.length() > 0) {
            return educationInfo.toString();
        }

        final Pattern institutionPattern = Pattern.compile(
            "(?i)\\b(university|college|institute|academy|school)\\s+of\\s+[a-zA-Z\\s,]+|[a-zA-Z\\s,]+\\s+(university|college|institute)",
            Pattern.CASE_INSENSITIVE
        );

        final Matcher institutionMatcher = institutionPattern.matcher(cvContent);
        final StringBuilder institutionInfo = new StringBuilder();

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
     * Intercepts quantifiable indicators identifying years of experience related conceptually.
     *
     * @param cvContent the analyzed text blob
     * @return an integer reflecting the extracted years
     */
    private Integer extractYearsOfExperience(final String cvContent) {
        if (!StringUtils.hasText(cvContent)) {
            return null;
        }

        final Pattern yearsPattern = Pattern.compile(
                "(\\d+)\\+?\\s*years?\\s*(?:of\\s*)?(?:experience|work)",
                Pattern.CASE_INSENSITIVE
        );
        final Matcher matcher = yearsPattern.matcher(cvContent);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (final NumberFormatException e) {
                log.warn("Could not parse years of experience value: {}", matcher.group(1));
            }
        }

        return null;
    }
}
