package ie.com.rag.controller;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.service.CandidateService;
import ie.com.rag.service.RagDocumentService;
import ie.com.rag.service.RagUploaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RagUploaderControllerTest {

    @Mock
    private RagUploaderService ragUploaderService;

    @Mock
    private RagDocumentService ragDocumentService;

    @Mock
    private CandidateService candidateService;

    @InjectMocks
    private RagUploaderController ragUploaderController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ragUploaderController).build();
    }

    @Test
    @DisplayName("POST /api/rag/upload should process a text CV and return 201")
    void uploadDocument_validTextFile_returnsCreated() throws Exception {
        // Given
        final CandidateDTO candidate = new CandidateDTO(
                UUID.randomUUID(), "Alice", "alice@example.com", "123",
                "cv content", "cv.txt", List.of("Java"), "exp", "edu", 5,
                LocalDateTime.now(), LocalDateTime.now());
        when(ragUploaderService.processCV(any(), any(), any(), any())).thenReturn(candidate);

        final MockMultipartFile file =
                new MockMultipartFile("file", "cv.txt", "text/plain", "Java developer".getBytes());

        // When / Then
        mockMvc.perform(multipart("/api/rag/upload")
                        .file(file)
                        .param("name", "Alice")
                        .param("email", "alice@example.com")
                        .param("phone", "123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Alice")))
                .andExpect(jsonPath("$.email", is("alice@example.com")));

        verify(ragUploaderService).processCV(eq(file), eq("Alice"), eq("alice@example.com"), eq("123"));
        verify(ragDocumentService).processAndStoreFile(file);
    }

    @Test
    @DisplayName("POST /api/rag/upload should return 400 for an empty file")
    void uploadDocument_emptyFile_returnsBadRequest() throws Exception {
        // Given
        final MockMultipartFile empty =
                new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        // When / Then
        mockMvc.perform(multipart("/api/rag/upload")
                        .file(empty)
                        .param("name", "Alice")
                        .param("email", "alice@example.com"))
                .andExpect(status().isBadRequest());

        verify(ragUploaderService, never()).processCV(any(), any(), any(), any());
        verify(ragDocumentService, never()).processAndStoreFile(any());
    }

    @Test
    @DisplayName("POST /api/rag/upload should return 415 for an unsupported content type")
    void uploadDocument_unsupportedType_returnsUnsupportedMediaType() throws Exception {
        // Given
        final MockMultipartFile png =
                new MockMultipartFile("file", "image.png", "image/png", new byte[]{1, 2, 3});

        // When / Then
        mockMvc.perform(multipart("/api/rag/upload")
                        .file(png)
                        .param("name", "Alice")
                        .param("email", "alice@example.com"))
                .andExpect(status().isUnsupportedMediaType());

        verify(ragUploaderService, never()).processCV(any(), any(), any(), any());
        verify(ragDocumentService, never()).processAndStoreFile(any());
    }

    @Test
    @DisplayName("POST /api/rag/upload should return 400 when the file part is missing")
    void uploadDocument_missingFile_returnsBadRequest() throws Exception {
        // When / Then
        mockMvc.perform(multipart("/api/rag/upload")
                        .param("name", "Alice")
                        .param("email", "alice@example.com"))
                .andExpect(status().isBadRequest());
    }
}
