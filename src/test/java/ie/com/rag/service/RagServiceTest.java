package ie.com.rag.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private RagService ragService;

    private ChatResponse chatResponse(final String content) {
        final Generation generation = new Generation(content);
        return new ChatResponse(List.of(generation));
    }

    @Test
    @DisplayName("Should answer a question using retrieved documents and save QA history")
    void ask_returnsAnswerAndSavesHistory() {
        // Given
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("Spring Boot is a Java framework")));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("Spring Boot is a Java framework."));

        // When
        final String answer = ragService.ask("What is Spring Boot?");

        // Then
        assertThat(answer).isEqualTo("Spring Boot is a Java framework.");
        verify(dashboardService).saveQAHistory("What is Spring Boot?", "Spring Boot is a Java framework.");
    }

    @Test
    @DisplayName("Should pass retrieved document content into the prompt as context")
    void ask_documentsIncludedInPrompt() {
        // Given
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(
                        new Document("Document one content"),
                        new Document("Document two content")
                ));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("answer"));

        // When
        ragService.ask("question");

        // Then
        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should truncate document context to the token budget")
    void ask_oversizedContext_truncated() {
        // Given
        // MAX_CONTEXT_TOKENS=5000, CHARS_PER_TOKEN=4 -> 20000 chars budget
        final String hugeContent = "a".repeat(30_000);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document(hugeContent)));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("answer"));

        // When
        ragService.ask("question");

        // Then
        final ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        assertThat(promptCaptor.getValue().getContents())
                .contains("[Document truncated to fit context limit]");
    }

    @Test
    @DisplayName("Should still answer when no documents are found")
    void ask_noDocuments_answersFromModel() {
        // Given
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("I don't know."));

        // When
        final String answer = ragService.ask("unknown topic");

        // Then
        assertThat(answer).isEqualTo("I don't know.");
        verify(dashboardService).saveQAHistory(anyString(), anyString());
    }
}
