package ie.com.rag.service;

import ie.com.rag.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private static final int MAX_CONTEXT_TOKENS = 5000;
    private static final int CHARS_PER_TOKEN = 4;
    private static final int MAX_DOCUMENT_CHARS = MAX_CONTEXT_TOKENS * CHARS_PER_TOKEN;

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final DashboardService dashboardService;

    public String ask(final String question) {
        final PromptTemplate template = new PromptTemplate(Constants.PROMPT);
        final Map<String, Object> promptParameters = Map.of(
                "input", question,
                "documents", findSimilarData(question)
        );

        final String answer = chatModel
                .call(template.create(promptParameters))
                .getResult()
                .getOutput()
                .getContent();

        dashboardService.saveQAHistory(question, answer);
        log.info("[RagWiser/RagService] - ask: question answered successfully");
        return answer;
    }

    private String findSimilarData(final String question) {
        final List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.query(question).withTopK(5));

        final StringBuilder result = new StringBuilder();
        int currentLength = 0;

        for (Document document : documents) {
            final String content = document.getContent();
            if (content != null) {
                if (currentLength + content.length() > MAX_DOCUMENT_CHARS) {
                    final int remainingChars = MAX_DOCUMENT_CHARS - currentLength;
                    if (remainingChars > 0) {
                        result.append(content, 0, remainingChars);
                        result.append("... [Document truncated to fit context limit]");
                    }
                    break;
                } else {
                    result.append(content);
                    result.append("\n\n---\n\n");
                    currentLength = result.length();
                }
            }
        }

        return result.toString();
    }
}
