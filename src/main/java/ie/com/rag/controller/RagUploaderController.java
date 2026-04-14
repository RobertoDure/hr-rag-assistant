package ie.com.rag.controller;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.service.CandidateService;
import ie.com.rag.service.RagDocumentService;
import ie.com.rag.service.RagUploaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Tag(name = "RAG Upload", description = "Candidate CV upload and processing APIs")
@SecurityRequirement(name = "bearerAuth")
public class RagUploaderController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private final RagUploaderService ragUploaderService;
    private final RagDocumentService ragDocumentService;
    private final CandidateService candidateService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'USER')")
    @Operation(summary = "Upload candidate CV", description = "Upload and process a candidate CV through the RAG system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "CV uploaded and processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or missing required fields"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<CandidateDTO> uploadDocument(
            @RequestParam("file") final MultipartFile file,
            @RequestParam("name") final String name,
            @RequestParam("email") final String email,
            @RequestParam(value = "phone", required = false) final String phone) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        final String contentType = file.getContentType();
        final boolean isAllowedType = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);
        if (!isAllowedType) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        log.info("[RagWiser/RagUploaderController] - uploadDocument: processing CV for candidate: {} ({})", name, email);

        final CandidateDTO candidate = ragUploaderService.processCV(file, name, email, phone);
        ragDocumentService.processAndStoreFile(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(candidate);
    }
}

