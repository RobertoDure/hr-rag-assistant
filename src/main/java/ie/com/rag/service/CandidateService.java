package ie.com.rag.service;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.entity.Candidate;
import ie.com.rag.exception.CandidateNotFoundException;
import ie.com.rag.exception.CandidateSaveException;
import ie.com.rag.exception.CandidateValidationException;
import ie.com.rag.exception.CandidateValidationException.ValidationError;
import ie.com.rag.mapper.CandidateMapperInterface;
import ie.com.rag.repository.CandidateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ie.com.rag.utils.TextUtils.sanitizeTextContent;

/**
 * Service class for managing candidates using modern Java and Spring Boot practices.
 * Includes proper exception handling, validation, and transaction management.
 */
@Service
@Transactional(readOnly = true)
public class CandidateService {

    private static final Logger logger = LoggerFactory.getLogger(CandidateService.class);

    private final CandidateRepository candidateRepository;
    private final CandidateMapperInterface candidateMapper;

    public CandidateService(CandidateRepository candidateRepository,
                            CandidateMapperInterface candidateMapper) {
        this.candidateRepository = candidateRepository;
        this.candidateMapper = candidateMapper;
    }

    /**
     * Saves a new candidate with comprehensive validation and error handling.
     */
    @Transactional
    public CandidateDTO saveCandidate(String name, String email, String phone, String cvContent,
                                      String originalFileName, List<String> skills, String experience,
                                      String education, Integer yearsOfExperience) {

        // Validate inputs using modern validation approach
        validateCandidateInputs(name, email, phone, cvContent, originalFileName, yearsOfExperience);

        try {
            // Sanitize all text content before saving to database
            var sanitizedName = sanitizeTextContent(name);
            var sanitizedEmail = sanitizeTextContent(email);
            var sanitizedPhone = sanitizeTextContent(phone);
            var sanitizedCvContent = sanitizeTextContent(cvContent);
            var sanitizedOriginalFileName = sanitizeTextContent(originalFileName);
            var sanitizedExperience = Optional.ofNullable(experience)
                    .filter(StringUtils::hasText)
                    .map(ie.com.rag.utils.TextUtils::sanitizeTextContent)
                    .orElse(null);
            var sanitizedEducation = Optional.ofNullable(education)
                    .filter(StringUtils::hasText)
                    .map(ie.com.rag.utils.TextUtils::sanitizeTextContent)
                    .orElse(null);


            Candidate candidate = Candidate.builder()
                    .name(sanitizedName)
                    .email(sanitizedEmail)
                    .phone(sanitizedPhone)
                    .cvContent(sanitizedCvContent)
                    .originalFileName(sanitizedOriginalFileName)
                    .skills(Optional.ofNullable(skills).orElse(List.of()))
                    .experience(sanitizedExperience)
                    .education(sanitizedEducation)
                    .yearsOfExperience(yearsOfExperience)
                    .build();

            // Save using repository with proper exception handling
            var savedCandidate = candidateRepository.save(candidate);

            logger.info("Candidate saved successfully with ID: {} and email: {}",
                    savedCandidate.getId(), savedCandidate.getEmail());

            // Convert to DTO and return
            return candidateMapper.toDTO(savedCandidate);

        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation when saving candidate with email: {}", email, e);
            throw new CandidateSaveException(
                    "Failed to save candidate due to data integrity violation",
                    name, email, e
            );
        } catch (Exception e) {
            logger.error("Unexpected error saving candidate with email: {}", email, e);
            throw new CandidateSaveException(
                    "Failed to save candidate due to unexpected error",
                    name, email, e
            );
        }
    }

    /**
     * Retrieves all candidates ordered by creation date in descending order.
     * Uses modern Optional and stream patterns.
     */
    public List<CandidateDTO> getAllCandidates() {
        try {
            return candidateRepository.findAllOrderByCreatedAtDesc()
                    .stream()
                    .map(candidateMapper::toDTO)
                    .toList(); // Modern Java method instead of collect(Collectors.toList())

        } catch (Exception e) {
            logger.error("Error retrieving all candidates", e);
            throw new RuntimeException("Failed to retrieve candidates", e);
        }
    }

    /**
     * Retrieves a candidate by their ID using Optional-based approach.
     */
    public CandidateDTO getCandidateById(String candidateId) {
        if (!StringUtils.hasText(candidateId)) {
            throw new CandidateValidationException("Candidate ID cannot be null or empty");
        }

        try {
            return candidateRepository.findById(candidateId)
                    .map(candidateMapper::toDTO)
                    .orElseThrow(() -> new CandidateNotFoundException(candidateId));

        } catch (CandidateNotFoundException e) {
            throw e; // Re-throw custom exceptions
        } catch (Exception e) {
            logger.error("Error retrieving candidate with ID: {}", candidateId, e);
            throw new RuntimeException("Failed to retrieve candidate", e);
        }
    }

    /**
     * Deletes a candidate by their ID with proper validation.
     */
    @Transactional
    public void deleteCandidate(String candidateId) {
        if (!StringUtils.hasText(candidateId)) {
            throw new CandidateValidationException("Candidate ID cannot be null or empty");
        }

        try {
            // Check existence first with more descriptive error
            if (!candidateRepository.existsById(candidateId)) {
                throw new CandidateNotFoundException(candidateId);
            }

            candidateRepository.deleteById(candidateId);
            logger.info("Candidate deleted successfully with ID: {}", candidateId);

        } catch (CandidateNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting candidate with ID: {}", candidateId, e);
            throw new RuntimeException("Failed to delete candidate", e);
        }
    }

    /**
     * Finds a candidate by their email address using Optional pattern.
     */
    public CandidateDTO findByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new CandidateValidationException("Email cannot be null or empty");
        }

        try {
            return candidateRepository.findByEmail(email)
                    .map(candidateMapper::toDTO)
                    .orElseThrow(() -> CandidateNotFoundException.byEmail(email));

        } catch (CandidateNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error retrieving candidate with email: {}", email, e);
            throw new RuntimeException("Failed to retrieve candidate by email", e);
        }
    }

    /**
     * Finds candidates by their years of experience with validation.
     */
    public List<CandidateDTO> findByYearsOfExperience(Integer minYears, Integer maxYears) {
        // Validate input parameters
        if (minYears != null && minYears < 0) {
            throw new CandidateValidationException("Minimum years of experience cannot be negative");
        }
        if (maxYears != null && maxYears < 0) {
            throw new CandidateValidationException("Maximum years of experience cannot be negative");
        }
        if (minYears != null && maxYears != null && minYears > maxYears) {
            throw new CandidateValidationException("Minimum years cannot be greater than maximum years");
        }

        try {
            return candidateRepository.findByYearsOfExperienceBetween(
                            Optional.ofNullable(minYears).orElse(0),
                            Optional.ofNullable(maxYears).orElse(Integer.MAX_VALUE)
                    )
                    .stream()
                    .map(candidateMapper::toDTO)
                    .toList();

        } catch (Exception e) {
            logger.error("Error retrieving candidates by years of experience: min={}, max={}",
                    minYears, maxYears, e);
            throw new RuntimeException("Failed to retrieve candidates by years of experience", e);
        }
    }

    /**
     * Searches candidates by their name with improved validation.
     */
    public List<CandidateDTO> searchByName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new CandidateValidationException("Search name cannot be null or empty");
        }

        try {
            return candidateRepository.findByNameContainingIgnoreCase(name.trim())
                    .stream()
                    .map(candidateMapper::toDTO)
                    .toList();

        } catch (Exception e) {
            logger.error("Error searching candidates by name: {}", name, e);
            throw new RuntimeException("Failed to search candidates by name", e);
        }
    }

    /**
     * Validates candidate inputs using modern validation approach with specific error messages.
     */
    private void validateCandidateInputs(String name, String email, String phone,
                                         String cvContent, String originalFileName,
                                         Integer yearsOfExperience) {
        var errors = new ArrayList<ValidationError>();

        if (!StringUtils.hasText(name)) {
            errors.add(ValidationError.of("name", "Name is required"));
        }

        if (!StringUtils.hasText(email)) {
            errors.add(ValidationError.of("email", "Email is required"));
        } else if (!isValidEmail(email)) {
            errors.add(ValidationError.of("email", "Email format is invalid", email));
        }

        if (!StringUtils.hasText(cvContent)) {
            errors.add(ValidationError.of("cvContent", "CV content is required"));
        }

        if (!StringUtils.hasText(originalFileName)) {
            errors.add(ValidationError.of("originalFileName", "Original file name is required"));
        }

        if (yearsOfExperience != null && yearsOfExperience < 0) {
            errors.add(ValidationError.of("yearsOfExperience",
                    "Years of experience cannot be negative", yearsOfExperience));
        }

        if (!errors.isEmpty()) {
            throw new CandidateValidationException("Candidate validation failed", errors);
        }
    }

    /**
     * Basic email validation - could be enhanced with more sophisticated patterns.
     */
    private boolean isValidEmail(String email) {
        return email != null &&
                email.contains("@") &&
                email.contains(".") &&
                email.length() > 5;
    }
}
