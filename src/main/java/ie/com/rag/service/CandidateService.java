package ie.com.rag.service;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.entity.Candidate;
import ie.com.rag.exception.CandidateNotFoundException;
import ie.com.rag.exception.CandidateSaveException;
import ie.com.rag.exception.CandidateValidationException;
import ie.com.rag.exception.CandidateValidationException.ValidationError;
import ie.com.rag.mapper.CandidateMapperInterface;
import ie.com.rag.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static ie.com.rag.utils.TextUtils.sanitizeTextContent;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidateService {

    private static final int MIN_YEARS_EXPERIENCE = 0;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final CandidateRepository candidateRepository;
    private final CandidateMapperInterface candidateMapper;

    /**
     * Saves a candidate with the provided details, including sanitization and validation.
     *
     * @param name              the candidate's name
     * @param email             the candidate's email address
     * @param phone             the candidate's phone number
     * @param cvContent         the content of the candidate's CV
     * @param originalFileName  the original file name of the uploaded CV
     * @param skills            the list of skills associated with the candidate
     * @param experience        the candidate's experience description
     * @param education         the candidate's education description
     * @param yearsOfExperience the number of years of experience
     * @return the saved candidate as a Data Transfer Object
     */
    @Transactional
    public CandidateDTO saveCandidate(final String name, final String email, final String phone,
                                      final String cvContent, final String originalFileName,
                                      final List<String> skills, final String experience,
                                      final String education, final Integer yearsOfExperience) {

        validateCandidateInputs(name, email, cvContent, originalFileName, yearsOfExperience, skills);

        try {
            final String sanitizedName = sanitizeTextContent(name);
            final String sanitizedEmail = sanitizeTextContent(email);
            final String sanitizedPhone = sanitizeNullableText(phone);
            final String sanitizedCvContent = sanitizeTextContent(cvContent);
            final String sanitizedOriginalFileName = sanitizeTextContent(originalFileName);
            final String sanitizedExperience = sanitizeNullableText(experience);
            final String sanitizedEducation = sanitizeNullableText(education);
            final List<String> sanitizedSkills = sanitizeSkills(skills);

            final Candidate candidate = Candidate.builder()
                    .name(sanitizedName)
                    .email(sanitizedEmail)
                    .phone(sanitizedPhone)
                    .cvContent(sanitizedCvContent)
                    .originalFileName(sanitizedOriginalFileName)
                    .skills(sanitizedSkills)
                    .experience(sanitizedExperience)
                    .education(sanitizedEducation)
                    .yearsOfExperience(yearsOfExperience)
                    .build();

            final Candidate savedCandidate = candidateRepository.save(candidate);

            log.info("Candidate saved successfully with ID: {} and email: {}",
                    savedCandidate.getId(), sanitizedEmail);

            return candidateMapper.toDTO(savedCandidate);

        } catch (final DataIntegrityViolationException e) {
            log.error("Data integrity violation when saving candidate with email: {}", email, e);
            throw new CandidateSaveException(
                    "Failed to save candidate due to data integrity violation",
                    name, email, e
            );
        } catch (final RuntimeException e) {
            log.error("Unexpected error saving candidate with email: {}", email, e);
            throw new CandidateSaveException(
                    "Failed to save candidate due to unexpected error",
                    name, email, e
            );
        }
    }

    /**
     * Retrieves all candidates, ordered by creation date in descending order.
     *
     * @return a list of all candidates as Data Transfer Objects
     */
    public List<CandidateDTO> getAllCandidates() {
        return candidateRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .map(candidateMapper::toDTO)
                .toList();
    }

    /**
     * Retrieves a candidate by their unique identifier.
     *
     * @param candidateId the unique identifier of the candidate
     * @return the candidate as a Data Transfer Object
     * @throws CandidateNotFoundException if the candidate is not found
     */
    public CandidateDTO getCandidateById(final String candidateId) {
        if (!StringUtils.hasText(candidateId)) {
            throw new CandidateValidationException("Candidate ID cannot be null or empty");
        }

        return candidateRepository.findById(candidateId)
                .map(candidateMapper::toDTO)
                .orElseThrow(() -> new CandidateNotFoundException(candidateId));
    }

    /**
     * Deletes a candidate by their unique identifier.
     *
     * @param candidateId the unique identifier of the candidate to delete
     * @throws CandidateNotFoundException if the candidate does not exist
     */
    @Transactional
    public void deleteCandidate(final String candidateId) {
        if (!StringUtils.hasText(candidateId)) {
            throw new CandidateValidationException("Candidate ID cannot be null or empty");
        }

        if (!candidateRepository.existsById(candidateId)) {
            throw new CandidateNotFoundException(candidateId);
        }

        candidateRepository.deleteById(candidateId);
        log.info("Candidate deleted successfully with ID: {}", candidateId);
    }

    /**
     * Finds a candidate by their email address.
     *
     * @param email the email address to search for
     * @return the candidate as a Data Transfer Object
     * @throws CandidateNotFoundException if no candidate is found with the given email
     */
    public CandidateDTO findByEmail(final String email) {
        if (!StringUtils.hasText(email)) {
            throw new CandidateValidationException("Email cannot be null or empty");
        }

        final String normalizedEmail = email.trim();
        if (!isValidEmail(normalizedEmail)) {
            throw new CandidateValidationException("Email format is invalid");
        }

        return candidateRepository.findByEmail(normalizedEmail)
                .map(candidateMapper::toDTO)
                .orElseThrow(() -> CandidateNotFoundException.byEmail(normalizedEmail));
    }

    /**
     * Finds candidates within a specified range of years of experience.
     *
     * @param minYears the minimum years of experience (inclusive)
     * @param maxYears the maximum years of experience (inclusive)
     * @return a list of candidates matching the experience criteria
     */
    public List<CandidateDTO> findByYearsOfExperience(final Integer minYears, final Integer maxYears) {
        if (minYears != null && minYears < MIN_YEARS_EXPERIENCE) {
            throw new CandidateValidationException("Minimum years of experience cannot be negative");
        }
        if (maxYears != null && maxYears < MIN_YEARS_EXPERIENCE) {
            throw new CandidateValidationException("Maximum years of experience cannot be negative");
        }

        final boolean isInvalidRange = minYears != null && maxYears != null && minYears > maxYears;
        if (isInvalidRange) {
            throw new CandidateValidationException("Minimum years cannot be greater than maximum years");
        }

        final int safeMinYears = Optional.ofNullable(minYears).orElse(MIN_YEARS_EXPERIENCE);
        final int safeMaxYears = Optional.ofNullable(maxYears).orElse(Integer.MAX_VALUE);
        return candidateRepository.findByYearsOfExperienceBetween(safeMinYears, safeMaxYears)
                .stream()
                .map(candidateMapper::toDTO)
                .toList();
    }

    /**
     * Searches for candidates by name, ignoring case.
     *
     * @param name the name or partial name to search for
     * @return a list of candidates whose matching the search criteria
     */
    public List<CandidateDTO> searchByName(final String name) {
        if (!StringUtils.hasText(name)) {
            throw new CandidateValidationException("Search name cannot be null or empty");
        }

        return candidateRepository.findByNameContainingIgnoreCase(name.trim())
                .stream()
                .map(candidateMapper::toDTO)
                .toList();
    }

    /**
     * Validates the input data for creating or updating a candidate.
     *
     * @param name              the candidate's name
     * @param email             the candidate's email address
     * @param cvContent         the content of the candidate's CV
     * @param originalFileName  the original file name
     * @param yearsOfExperience the candidate's years of experience
     * @param skills            the list of skills
     * @throws CandidateValidationException if any of the inputs are invalid
     */
    private void validateCandidateInputs(final String name, final String email,
                                         final String cvContent, final String originalFileName,
                                         final Integer yearsOfExperience, final List<String> skills) {
        final List<ValidationError> errors = new ArrayList<>();

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

        if (yearsOfExperience != null && yearsOfExperience < MIN_YEARS_EXPERIENCE) {
            errors.add(ValidationError.of("yearsOfExperience",
                    "Years of experience cannot be negative", yearsOfExperience));
        }

        if (skills != null && skills.stream().anyMatch(skill -> !StringUtils.hasText(skill))) {
            errors.add(ValidationError.of("skills", "Skills cannot contain null or blank values"));
        }

        if (!errors.isEmpty()) {
            throw new CandidateValidationException("Candidate validation failed", errors);
        }
    }

    /**
     * Sanitizes and deduplicates a list of skills.
     *
     * @param skills the list of skills to sanitize
     * @return a sanitized list of distinct skills
     */
    private List<String> sanitizeSkills(final List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }

        final List<String> sanitizedSkills = skills.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(ie.com.rag.utils.TextUtils::sanitizeTextContent)
                .distinct()
                .toList();

        return sanitizedSkills.isEmpty() ? List.of() : List.copyOf(sanitizedSkills);
    }

    /**
     * Sanitizes text input, returning null if the input is empty or blank.
     *
     * @param input the text to sanitize
     * @return the sanitized text, or null if the input is blank
     */
    private String sanitizeNullableText(final String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }

        return sanitizeTextContent(input);
    }

    /**
     * Validates an email address format.
     *
     * @param email the email address to validate
     * @return true if the email is valid, false otherwise
     */
    private boolean isValidEmail(final String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }

        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}
