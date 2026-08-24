package ie.com.rag.service;

import ie.com.rag.dto.CandidateDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagUploaderServiceTest {

    @Mock
    private CandidateService candidateService;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private RagDocumentService ragDocumentService;

    @Mock
    private NLPSkillExtractorService nlpSkillExtractorService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private RagUploaderService ragUploaderService;

    private CandidateDTO buildCandidateDto(final String name, final String email) {
        return new CandidateDTO(
                UUID.randomUUID(), name, email, "123", "cv content",
                "cv.txt", List.of("Java"), "exp", "edu", 5,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private void stubTransactionToRunCallback() {
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    final TransactionCallback<?> callback =
                            (TransactionCallback<?>) invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });
    }

    private MockMultipartFile buildTextFile(final String content) {
        return new MockMultipartFile(
                "file", "cv.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------
    // processCV - happy path
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should process a text CV end to end and return the saved candidate")
    void processCV_success_returnsSavedCandidate() {
        // Given
        final String content = "John Doe\n5 years of experience\nEducation: BSc Computer Science\nSkills: Java, Spring";
        final MockMultipartFile file = buildTextFile(content);
        final CandidateDTO expected = buildCandidateDto("John Doe", "john@example.com");

        when(nlpSkillExtractorService.extractSkills(anyString())).thenReturn(List.of("Java", "Spring"));
        stubTransactionToRunCallback();
        when(candidateService.saveCandidate(anyString(), anyString(), any(), anyString(), anyString(),
                any(), anyString(), anyString(), any()))
                .thenReturn(expected);

        // When
        final CandidateDTO result = ragUploaderService.processCV(file, "John Doe", "john@example.com", "123");

        // Then
        assertThat(result).isSameAs(expected);

        verify(candidateService).saveCandidate(
                eq("John Doe"), eq("john@example.com"), eq("123"),
                contains("John Doe"), eq("cv.txt"),
                eq(List.of("Java", "Spring")), anyString(), eq("BSc Computer Science"), eq(5));
        verify(dashboardService).saveUploadedDocumentInfo("cv.txt", content.getBytes(StandardCharsets.UTF_8).length, "text/plain");
        verify(ragDocumentService).processDocument(anyString(), eq("cv.txt"));
    }

    @Test
    @DisplayName("Should use octet-stream content type when none is provided")
    void processCV_missingContentType_defaultsToOctetStream() {
        // Given
        final MockMultipartFile file = new MockMultipartFile(
                "file", "cv.txt", null, "Java developer".getBytes(StandardCharsets.UTF_8));
        final CandidateDTO expected = buildCandidateDto("John", "john@example.com");

        when(nlpSkillExtractorService.extractSkills(anyString())).thenReturn(List.of("Java"));
        stubTransactionToRunCallback();
        when(candidateService.saveCandidate(anyString(), anyString(), any(), anyString(), anyString(),
                any(), anyString(), anyString(), any()))
                .thenReturn(expected);

        // When
        ragUploaderService.processCV(file, "John", "john@example.com", null);

        // Then
        verify(dashboardService).saveUploadedDocumentInfo("cv.txt", 14L, "application/octet-stream");
    }

    @Test
    @DisplayName("Should fall back to static skill list when NLP returns nothing")
    void processCV_nlpEmpty_usesStaticFallbackSkills() {
        // Given
        final MockMultipartFile file = buildTextFile("Java and Python experience with Agile");
        final CandidateDTO expected = buildCandidateDto("John", "john@example.com");

        when(nlpSkillExtractorService.extractSkills(anyString())).thenReturn(List.of());
        stubTransactionToRunCallback();
        when(candidateService.saveCandidate(anyString(), anyString(), any(), anyString(), anyString(),
                any(), anyString(), anyString(), any()))
                .thenReturn(expected);

        // When
        ragUploaderService.processCV(file, "John", "john@example.com", null);

        // Then
        verify(candidateService).saveCandidate(eq("John"), eq("john@example.com"), eq(null), anyString(),
                eq("cv.txt"), eq(List.of("Java", "Python", "Agile")), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should still succeed when RAG processing of the CV fails")
    void processCV_ragFailure_doesNotFailUpload() {
        // Given
        final MockMultipartFile file = buildTextFile("Java developer");
        final CandidateDTO expected = buildCandidateDto("John", "john@example.com");

        when(nlpSkillExtractorService.extractSkills(anyString())).thenReturn(List.of("Java"));
        stubTransactionToRunCallback();
        when(candidateService.saveCandidate(anyString(), anyString(), any(), anyString(), anyString(),
                any(), anyString(), anyString(), any()))
                .thenReturn(expected);
        doThrow(new RuntimeException("vector store down"))
                .when(ragDocumentService).processDocument(anyString(), anyString());

        // When
        final CandidateDTO result = ragUploaderService.processCV(file, "John", "john@example.com", null);

        // Then
        assertThat(result).isSameAs(expected);
    }

    // ------------------------------------------------------------------
    // processCV - validation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should reject a null file")
    void processCV_nullFile_throws() {
        assertThatThrownBy(() -> ragUploaderService.processCV(null, "John", "john@example.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is required");
    }

    @Test
    @DisplayName("Should reject an empty file")
    void processCV_emptyFile_throws() {
        final MockMultipartFile empty = new MockMultipartFile("file", "cv.txt", "text/plain", new byte[0]);
        assertThatThrownBy(() -> ragUploaderService.processCV(empty, "John", "john@example.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is required");
    }

    @Test
    @DisplayName("Should reject a blank candidate name")
    void processCV_blankName_throws() {
        assertThatThrownBy(() -> ragUploaderService.processCV(buildTextFile("cv"), "  ", "john@example.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Candidate name is required");
    }

    @Test
    @DisplayName("Should reject a blank email")
    void processCV_blankEmail_throws() {
        assertThatThrownBy(() -> ragUploaderService.processCV(buildTextFile("cv"), "John", "  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Candidate email is required");
    }

    @Test
    @DisplayName("Should reject a missing original filename")
    void processCV_missingFilename_throws() {
        final MockMultipartFile noName = new MockMultipartFile("file", null, "text/plain", "cv".getBytes());
        assertThatThrownBy(() -> ragUploaderService.processCV(noName, "John", "john@example.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Uploaded filename is required");
    }

    @Test
    @DisplayName("Should throw when the transaction returns no candidate")
    void processCV_transactionNull_throwsIllegalState() {
        // Given
        final MockMultipartFile file = buildTextFile("Java developer");
        when(nlpSkillExtractorService.extractSkills(anyString())).thenReturn(List.of("Java"));
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> ragUploaderService.processCV(file, "John", "john@example.com", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to persist uploaded CV data");
        verify(ragDocumentService, never()).processDocument(anyString(), anyString());
    }
}
