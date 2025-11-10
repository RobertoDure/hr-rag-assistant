package ie.com.rag.controller;

import ie.com.rag.dto.QAHistoryDTO;
import ie.com.rag.dto.UploadedDocumentDTO;
import ie.com.rag.service.DashboardService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ie.com.rag.Constants.PROMPT;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final DashboardService dashboardService;

    // Maximum tokens for context (leaving room for prompt, question, and response)
    private static final int MAX_CONTEXT_TOKENS = 5000;
    // Rough estimation: 4 characters ≈ 1 token
    private static final int CHARS_PER_TOKEN = 4;
    private static final int MAX_DOCUMENT_CHARS = MAX_CONTEXT_TOKENS * CHARS_PER_TOKEN;

    public RagController(ChatModel chatModel, VectorStore vectorStore, DashboardService dashboardService) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.dashboardService = dashboardService;
    }

    /**
     * This endpoint is used to answer questions based on the documents processed in the RAG system.
     */
    @GetMapping
    public String simplify(@RequestParam(value = "question",
    defaultValue = "Acknowledge about the passed document")
                           String question) {
        // Create a prompt template with the provided prompt string
        PromptTemplate template = new PromptTemplate(PROMPT);
        Map<String, Object> promptsParameters = new HashMap<>();
        promptsParameters.put("input", question);
        promptsParameters.put("documents", findSimilarData(question));

        String answer = chatModel
                .call(template.create(promptsParameters))
                .getResult()
                .getOutput()
                .getContent();

        // Save QA to history
        dashboardService.saveQAHistory(question, answer);

        return answer;
    }

    /**
     * Get QA history for dashboard
     */
    @GetMapping("/qa-history")
    public List<QAHistoryDTO> getQAHistory() {
        return dashboardService.getQAHistory();
    }

    /**
     * Get uploaded documents for dashboard
     */
    @GetMapping("/uploaded-documents")
    public List<UploadedDocumentDTO> getUploadedDocuments() {
        return dashboardService.getUploadedDocuments();
    }

    private String findSimilarData(String question) {
        List<Document> documents =
                vectorStore.similaritySearch(SearchRequest
                .query(question)
                        .withTopK(5));

        StringBuilder result = new StringBuilder();
        int currentLength = 0;

        for (Document document : documents) {
            String content = document.getContent();
            if (content != null) {
                // Check if adding this document would exceed the limit
                if (currentLength + content.length() > MAX_DOCUMENT_CHARS) {
                    // Add only the portion that fits
                    int remainingChars = MAX_DOCUMENT_CHARS - currentLength;
                    if (remainingChars > 0) {
                        result.append(content, 0, remainingChars);
                        result.append("... [Document truncated to fit context limit]");
                    }
                    break;
                } else {
                    result.append(content);
                    result.append("\n\n---\n\n"); // Add separator between documents
                    currentLength = result.length();
                }
            }
        }

        return result.toString();
    }
}
