package ie.com.rag.service;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.entity.Candidate;
import ie.com.rag.mapper.CandidateMapperInterface;
import ie.com.rag.repository.CandidateRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static ie.com.rag.utils.TextUtils.sanitizeTextContent;

@Service
public class CandidateService {

    private static final Logger logger = LoggerFactory.getLogger(CandidateService.class);

    private final CandidateRepository candidateRepository;

    private final CandidateMapperInterface candidateMapper;


    public CandidateService(CandidateRepository candidateRepository, CandidateMapperInterface candidateMapper) {
        this.candidateRepository = candidateRepository;
        this.candidateMapper = candidateMapper;
    }

    public CandidateDTO saveCandidate(String name, String email, String phone, String cvContent,
                                      String originalFileName, List<String> skills, String experience,
                                      String education, Integer yearsOfExperience) {
        try {
            // Sanitize all text content before saving to database
            name = sanitizeTextContent(name);
            email = sanitizeTextContent(email);
            phone = sanitizeTextContent(phone);
            cvContent = sanitizeTextContent(cvContent);
            originalFileName = sanitizeTextContent(originalFileName);
            experience = sanitizeTextContent(experience);
            education = sanitizeTextContent(education);

            // Create and populate candidate entity
            Candidate candidate = new Candidate();
            candidate.setId(UUID.randomUUID().toString());
            candidate.setName(name);
            candidate.setEmail(email);
            candidate.setPhone(phone);
            candidate.setCvContent(cvContent);
            candidate.setOriginalFileName(originalFileName);
            candidate.setSkills(skills);
            candidate.setExperience(experience);
            candidate.setEducation(education);
            candidate.setYearsOfExperience(yearsOfExperience);

            // Save using repository
            Candidate savedCandidate = candidateRepository.save(candidate);

            logger.info("Candidate saved successfully with ID: {}", savedCandidate.getId());

            // Convert to DTO and return
            return CandidateMapperInterface.INSTANCE.toDTO(savedCandidate);

        } catch (Exception e) {
            logger.error("Error saving candidate: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save candidate", e);
        }
    }

    /**
     * Retrieves all candidates ordered by creation date in descending order.
     *
     * @return List of CandidateDTO objects
     */
    public List<CandidateDTO> getAllCandidates() {
        try {
            List<Candidate> candidates = candidateRepository.findAllOrderByCreatedAtDesc();
            return candidates.stream()
                    .map(candidateMapper::toDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error retrieving candidates: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve candidates", e);
        }
    }
    /**
     * Retrieves a candidate by their ID.
     *
     * @param candidateId The ID of the candidate to retrieve
     * @return CandidateDTO object
     */
    public CandidateDTO getCandidateById(String candidateId) {
        try {
            Candidate candidate = candidateRepository.findById(candidateId)
                    .orElseThrow(() -> new RuntimeException("Candidate not found with ID: " + candidateId));

            return candidateMapper.toDTO(candidate);

        } catch (Exception e) {
            logger.error("Error retrieving candidate with ID {}: {}", candidateId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve candidate", e);
        }
    }
    /**
     * Deletes a candidate by their ID.
     *
     * @param candidateId The ID of the candidate to delete
     */
    public void deleteCandidate(String candidateId) {
        try {
            if (!candidateRepository.existsById(candidateId)) {
                throw new RuntimeException("Candidate not found with ID: " + candidateId);
            }

            candidateRepository.deleteById(candidateId);
            logger.info("Candidate deleted successfully with ID: {}", candidateId);

        } catch (Exception e) {
            logger.error("Error deleting candidate with ID {}: {}", candidateId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete candidate", e);
        }
    }

    /**
     * Finds a candidate by their email address.
     *
     * @param email The email address of the candidate
     * @return CandidateDTO object
     */
    public CandidateDTO findByEmail(String email) {
        try {
            Candidate candidate = candidateRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Candidate not found with email: " + email));

            return candidateMapper.toDTO(candidate);

        } catch (Exception e) {
            logger.error("Error retrieving candidate with email {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve candidate by email", e);
        }
    }
   /**
     * Finds candidates by their years of experience.
     *
     * @param minYears Minimum years of experience
     * @param maxYears Maximum years of experience
     * @return List of CandidateDTO objects
     */
    public List<CandidateDTO> findByYearsOfExperience(Integer minYears, Integer maxYears) {
        try {
            List<Candidate> candidates = candidateRepository.findByYearsOfExperienceBetween(minYears, maxYears);
            return candidates.stream()
                    .map(candidateMapper::toDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error retrieving candidates by years of experience: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve candidates by years of experience", e);
        }
    }
    /**
     * Searches candidates by their name.
     *
     * @param name The name to search for
     * @return List of CandidateDTO objects matching the search criteria
     */
    public List<CandidateDTO> searchByName(String name) {
        try {
            List<Candidate> candidates = candidateRepository.findByNameContainingIgnoreCase(name);
            return candidates.stream()
                    .map(candidateMapper::toDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error searching candidates by name: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search candidates by name", e);
        }
    }
}
