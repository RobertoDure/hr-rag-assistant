package ie.com.rag.controller;

import ie.com.rag.dto.QAHistoryDTO;
import ie.com.rag.dto.UploadedDocumentDTO;
import ie.com.rag.service.DashboardService;
import ie.com.rag.service.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RagControllerTest {

    @Mock
    private RagService ragService;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private RagController ragController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ragController).build();
    }

    @Test
    @DisplayName("POST /api/rag should return the generated answer")
    void ask_returnsAnswer() throws Exception {
        // Given
        when(ragService.ask("What is Spring?")).thenReturn("Spring is a framework");

        // When / Then
        mockMvc.perform(post("/api/rag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is Spring?\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Spring is a framework"));
    }

    @Test
    @DisplayName("GET /api/rag/qa-history should return the QA history")
    void getQAHistory_returnsHistory() throws Exception {
        // Given
        when(dashboardService.getQAHistory()).thenReturn(List.of(
                new QAHistoryDTO(UUID.randomUUID(), "q1", "a1", LocalDateTime.now())));

        // When / Then
        mockMvc.perform(get("/api/rag/qa-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].question", is("q1")));
    }

    @Test
    @DisplayName("GET /api/rag/uploaded-documents should return uploaded documents")
    void getUploadedDocuments_returnsDocuments() throws Exception {
        // Given
        when(dashboardService.getUploadedDocuments()).thenReturn(List.of(
                new UploadedDocumentDTO(UUID.randomUUID(), "cv.pdf", 1024L, "application/pdf", LocalDateTime.now())));

        // When / Then
        mockMvc.perform(get("/api/rag/uploaded-documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fileName", is("cv.pdf")));
    }
}
