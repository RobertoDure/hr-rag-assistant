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
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagDocumentServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private RagDocumentService ragDocumentService;

    private ChatResponse chatResponse(final String content) {
        final Generation generation = new Generation(content);
        return new ChatResponse(List.of(generation));
    }

    // ------------------------------------------------------------------
    // knowledgeRAG
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should reject a blank question")
    void knowledgeRAG_blankQuestion_throws() {
        assertThatThrownBy(() -> ragDocumentService.knowledgeRAG("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Question cannot be null or empty");
    }

    @Test
    @DisplayName("Should answer using retrieved document context")
    void knowledgeRAG_success_returnsAnswer() {
        // Given
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("Context about the topic", Map.of("filename", "doc.txt"))));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("Answer from context"));

        // When
        final String answer = ragDocumentService.knowledgeRAG("What is the topic?");

        // Then
        assertThat(answer).isEqualTo("Answer from context");
    }

    @Test
    @DisplayName("Should return empty context when no documents match")
    void knowledgeRAG_noDocuments_emptyContext() {
        // Given
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("I don't know"));

        // When
        final String answer = ragDocumentService.knowledgeRAG("unknown");

        // Then
        assertThat(answer).isEqualTo("I don't know");
    }

    // ------------------------------------------------------------------
    // processDocument
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should reject a blank filename")
    void processDocument_blankFilename_throws() {
        assertThatThrownBy(() -> ragDocumentService.processDocument("content", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Filename cannot be null or empty");
    }

    @Test
    @DisplayName("Should skip vector storage for empty content")
    void processDocument_emptyContent_skipsStorage() {
        // When
        ragDocumentService.processDocument("   \n  ", "file.txt");

        // Then
        verify(vectorStore, never()).add(any());
    }

    @Test
    @DisplayName("Should store short content as a single chunk")
    void processDocument_shortContent_singleChunk() {
        // When
        ragDocumentService.processDocument("Short document content here.", "cv.txt");

        // Then
        final ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        final List<Document> documents = captor.getValue();
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getContent()).isEqualTo("Short document content here.");
        assertThat(documents.get(0).getMetadata())
                .containsEntry("filename", "cv.txt")
                .containsEntry("type", "cv")
                .containsEntry("chunk", 1)
                .containsEntry("totalChunks", 1);
    }

    @Test
    @DisplayName("Should chunk long multi-paragraph content with metadata")
    void processDocument_longContent_splitsIntoChunks() {
        // Given
        final String paragraph = "word ".repeat(200).trim(); // ~1000 chars
        final String content = paragraph + "\n\n" + paragraph + "\n\n" + paragraph;

        // When
        ragDocumentService.processDocument(content, "long.txt");

        // Then
        final ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(3)).add(captor.capture());

        final List<List<Document>> allAdds = captor.getAllValues();
        assertThat(allAdds).hasSize(3);
        assertThat(allAdds.get(0).get(0).getMetadata()).containsEntry("chunk", 1).containsEntry("totalChunks", 3);
        assertThat(allAdds.get(1).get(0).getMetadata()).containsEntry("chunk", 2);
        assertThat(allAdds.get(2).get(0).getMetadata()).containsEntry("chunk", 3);
    }

    @Test
    @DisplayName("Should split an oversized single paragraph into fixed-size chunks")
    void processDocument_oversizedParagraph_splitBySize() {
        // Given
        final String hugeParagraph = "x".repeat(5_000); // > DEFAULT_VECTOR_CHUNK_SIZE (2000)

        // When
        ragDocumentService.processDocument(hugeParagraph, "huge.txt");

        // Then
        final ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(3)).add(captor.capture());
        final List<List<Document>> allAdds = captor.getAllValues();
        assertThat(allAdds.get(0).get(0).getContent()).hasSize(2000);
        assertThat(allAdds.get(2).get(0).getContent()).hasSize(1000);
    }

    // ------------------------------------------------------------------
    // processAndStoreFile
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should extract text from a plain text file and store it")
    void processAndStoreFile_textFile_storesContent() {
        // Given
        final MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "Plain text content to store.".getBytes());

        // When
        ragDocumentService.processAndStoreFile(file);

        // Then
        final ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getContent()).contains("Plain text content to store.");
        assertThat(captor.getValue().get(0).getMetadata()).containsEntry("filename", "notes.txt");
    }

    @Test
    @DisplayName("Should reject a null or empty file")
    void processAndStoreFile_emptyFile_throws() {
        assertThatThrownBy(() -> ragDocumentService.processAndStoreFile(
                new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File cannot be null or empty");
    }
}
