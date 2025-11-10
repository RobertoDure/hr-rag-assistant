package ie.com.rag.controller;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.service.CandidateService;
import ie.com.rag.service.RagDocumentService;
import ie.com.rag.service.RagUploaderService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/rag")
public class RagUploaderController {

    private static final Logger logger = LoggerFactory.getLogger(RagUploaderController.class);

    private final RagUploaderService ragUploaderService;
    private final RagDocumentService ragDocumentService;
    private final CandidateService candidateService;

    public RagUploaderController(RagUploaderService ragUploaderService, RagDocumentService ragDocumentService,
                                 CandidateService candidateService) {
        this.ragUploaderService = ragUploaderService;
        this.ragDocumentService = ragDocumentService;
        this.candidateService = candidateService;
    }

    /**
     * Endpoint to upload a candidate CV with metadata for processing.
     * The file is expected to be sent as a multipart/form-data request along with candidate details.
     *
     * @param file the CV file to be uploaded
     * @param name the candidate's name
     * @param email the candidate's email
     * @param phone the candidate's phone (optional)
     * @return ResponseEntity with candidate data or error message
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file,
                                          @RequestParam("name") String name,
                                          @RequestParam("email") String email,
                                          @RequestParam(value = "phone", required = false) String phone) {
        try {
            // Validate input
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is required");
            }

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Candidate name is required");
            }

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Candidate email is required");
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.contains("pdf") && !contentType.contains("text") &&
                !contentType.contains("document") && !contentType.contains("msword"))) {
                return ResponseEntity.badRequest().body("Only PDF, DOC, DOCX, and text files are supported");
            }

            // Check file size (50MB limit)
            if (file.getSize() > 50 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("File size must be less than 50MB");
            }

            logger.info("Processing CV upload for candidate: {} ({})", name, email);

            // Process the CV through the RAG service
            CandidateDTO candidate = ragUploaderService.processCV(file, name, email, phone);
            ragDocumentService.processAndStoreFile(file);

            return ResponseEntity.ok(
                "CV uploaded successfully for candidate: " + name + " (" + email + ")"
            );

        } catch (Exception e) {
            logger.error("Unexpected error when uploading CV for candidate {} ({}): {}", name, email, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("An error occurred while processing the CV: " + e.getMessage());
        }
    }

    /**
     * Get all candidates
     */
    @GetMapping("/candidates")
    public ResponseEntity<?> getAllCandidates() {
        try {
            return ResponseEntity.ok(candidateService.getAllCandidates());
        } catch (Exception e) {
            logger.error("Error retrieving candidates: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error retrieving candidates: " + e.getMessage());
        }
    }

    /**
     * Get candidate by ID
     */
    @GetMapping("/candidates/{id}")
    public ResponseEntity<?> getCandidateById(@PathVariable String id) {
        try {
            CandidateDTO candidate = candidateService.getCandidateById(id);
            return ResponseEntity.ok(candidate);
        } catch (Exception e) {
            logger.error("Error retrieving candidate with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Upload service is running");
    }
}
