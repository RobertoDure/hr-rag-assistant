package ie.com.rag.controller;

import ie.com.rag.dto.AskRequestDTO;
import ie.com.rag.dto.QAHistoryDTO;
import ie.com.rag.dto.UploadedDocumentDTO;
import ie.com.rag.service.DashboardService;
import ie.com.rag.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Tag(name = "RAG", description = "Retrieval-Augmented Generation question answering and document APIs")
@SecurityRequirement(name = "bearerAuth")
public class RagController {

    private final RagService ragService;
    private final DashboardService dashboardService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'USER')")
    @Operation(summary = "Ask a question", description = "Answer a question using the RAG system over uploaded documents")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question answered successfully"),
            @ApiResponse(responseCode = "400", description = "Question must not be blank"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<String> ask(@Valid @RequestBody final AskRequestDTO request) {
        final String answer = ragService.ask(request.question());
        return ResponseEntity.ok(answer);
    }

    @GetMapping("/qa-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'USER')")
    @Operation(summary = "Get QA history", description = "Retrieve the question and answer history")
    @ApiResponse(responseCode = "200", description = "History retrieved successfully")
    public ResponseEntity<List<QAHistoryDTO>> getQAHistory() {
        return ResponseEntity.ok(dashboardService.getQAHistory());
    }

    @GetMapping("/uploaded-documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'USER')")
    @Operation(summary = "Get uploaded documents", description = "Retrieve the list of uploaded documents")
    @ApiResponse(responseCode = "200", description = "Documents retrieved successfully")
    public ResponseEntity<List<UploadedDocumentDTO>> getUploadedDocuments() {
        return ResponseEntity.ok(dashboardService.getUploadedDocuments());
    }
}

