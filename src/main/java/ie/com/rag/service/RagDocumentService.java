package ie.com.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ie.com.rag.Constants.PROMPT;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagDocumentService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    private static final int MAX_DOCUMENT_TOKENS = 6000;
    private static final int APPROX_CHARS_PER_TOKEN = 4;
    private static final int DEFAULT_VECTOR_CHUNK_SIZE = 2000;
    private static final int TIKA_MAX_STRING_LENGTH = 10 * 1024 * 1024;

    /**
     * Uses Retrieval-Augmented Generation (RAG) to answer a user's question based on processed documents.
     *
     * @param question the prompt or question from the user
     * @return the AI-generated answer using document context
     */
    @Tool(name = "rag_listed_documents",
          description = "RAG to get knowledge from the documents that have been processed")
    public String knowledgeRAG(final String question) {
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("Question cannot be null or empty");
        }

        final PromptTemplate template = new PromptTemplate(PROMPT);
        final Map<String, Object> promptsParameters = new HashMap<>();
        promptsParameters.put("input", question);
        promptsParameters.put("documents", findSimilarData(question));

        return chatModel
                .call(template.create(promptsParameters))
                .getResult()
                .getOutput()
                .getContent();

    }

    /**
     * Retrieves similar document data from the vector store given a query string.
     *
     * @param question the user's query
     * @return a concatenated string containing formatted context from matched documents
     */
    private String findSimilarData(final String question) {
        final List<Document> documents = vectorStore.similaritySearch(SearchRequest
                .query(question)
                .withTopK(5));
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        final StringBuilder result = new StringBuilder();
        final int maxChars = MAX_DOCUMENT_TOKENS * APPROX_CHARS_PER_TOKEN;

        for (final Document doc : documents) {
            final String content = StringUtils.hasText(doc.getContent()) ? doc.getContent() : "";
            final String metadata = doc.getMetadata() == null ? "{}" : doc.getMetadata().toString();

            final String docSection = "\n--- Document: " + metadata + " ---\n" + content + "\n";

            if (result.length() + docSection.length() > maxChars) {
                // Guardrail: keep context under the token budget expected by the prompt.
                if (result.isEmpty()) {
                    final int availableChars = maxChars - ("\n--- Document: " + metadata + " ---\n").length() - 50;
                    if (availableChars > 0) {
                        final String truncatedContent = content.substring(0, Math.min(content.length(), availableChars));
                        result.append("\n--- Document: ").append(metadata).append(" ---\n")
                               .append(truncatedContent)
                               .append("\n[Content truncated due to length]\n");
                    }
                }
                break;
            }

            result.append(docSection);
        }

        return result.toString();
    }

    /**
     * Processes a document's content, chunks it appropriately, and stores it in the vector store.
     *
     * @param content  the raw textual data to process
     * @param filename the name associated with the text source
     */
    public void processDocument(final String content, final String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        final String sanitizedContent = sanitizeTextContent(content);
        if (!StringUtils.hasText(sanitizedContent)) {
            log.warn("Skipping vector storage for empty sanitized content, filename: {}", filename);
            return;
        }

        final List<String> chunks = chunkContent(sanitizedContent);
        if (chunks.isEmpty()) {
            return;
        }

        for (int i = 0; i < chunks.size(); i++) {
            final String sanitizedChunk = sanitizeTextContent(chunks.get(i));
            if (!StringUtils.hasText(sanitizedChunk)) {
                continue;
            }

            final Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", filename);
            metadata.put("type", "cv");
            metadata.put("chunk", i + 1);
            metadata.put("totalChunks", chunks.size());

            final Document document = new Document(sanitizedChunk, metadata);
            try {
                vectorStore.add(List.of(document));
            } catch (final RuntimeException e) {
                throw new IllegalStateException("Failed to persist document chunk in vector store", e);
            }
        }
    }

    /**
     * Cleans up text content by normalizing whitespace, removing control characters, and standardizing line breaks.
     *
     * @param content the text to sanitize
     * @return the sanitized string ready for chunking
     */
    private String sanitizeTextContent(final String content) {
        if (content == null) {
            return "";
        }

        String sanitized = content.replace("\u0000", "");
        sanitized = sanitized.replaceAll("[\u0001-\u0008\u000B\u000C\u000E-\u001F\u007F]", "");
        sanitized = sanitized.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        sanitized = sanitized.replaceAll("[ \t]+", " ");
        sanitized = sanitized.replaceAll("\n[ \t]+", "\n");
        sanitized = sanitized.replaceAll("[ \t]+\n", "\n");
        sanitized = sanitized.replaceAll("\n{3,}", "\n\n");

        return sanitized.trim();
    }

    /**
     * Splits a long string of text into smaller context chunks suitable for vector storage and embedding.
     *
     * @param content the sanitized text of the document
     * @return a list of textual chunks
     */
    private List<String> chunkContent(final String content) {
        if (!StringUtils.hasText(content)) {
            return Collections.emptyList();
        }

        final List<String> chunks = new ArrayList<>();

        if (content.length() <= DEFAULT_VECTOR_CHUNK_SIZE) {
            chunks.add(content);
            return chunks;
        }

        final String[] paragraphs = content.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();

        for (final String paragraph : paragraphs) {
            if (!StringUtils.hasText(paragraph)) {
                continue;
            }

            if (currentChunk.length() + paragraph.length() + 2 > DEFAULT_VECTOR_CHUNK_SIZE) {
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }

                if (paragraph.length() > DEFAULT_VECTOR_CHUNK_SIZE) {
                    for (int i = 0; i < paragraph.length(); i += DEFAULT_VECTOR_CHUNK_SIZE) {
                        chunks.add(paragraph.substring(i, Math.min(i + DEFAULT_VECTOR_CHUNK_SIZE, paragraph.length())));
                    }
                } else {
                    currentChunk.append(paragraph).append("\n\n");
                }
            } else {
                currentChunk.append(paragraph).append("\n\n");
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * Receives a multipart file, extracts its text content using Apache Tika, and stores it into the vector database.
     *
     * @param file the uploaded file to process
     * @throws IOException if the file processing encounters an I/O error or extraction fails
     */
    public void processAndStoreFile(final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        final String filename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "unknown-file";

        try {
            final String extractedText = extractTextWithTika(file);

            if (!StringUtils.hasText(extractedText)) {
                throw new IllegalStateException("No text could be extracted from the file: " + filename);
            }

            processDocument(extractedText, filename);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to process file: " + filename, e);
        }
    }

    /**
     * Extracts text content from a multipart file leveraging Apache Tika's basic payload parser.
     *
     * @param file the file to parse
     * @return the textual content of the file
     * @throws IOException if reading the file fails or the text extraction encounters severe errors
     */
    private String extractTextWithTika(final MultipartFile file) throws IOException {
        try (final InputStream inputStream = file.getInputStream()) {
            final Tika tika = new Tika();
            tika.setMaxStringLength(TIKA_MAX_STRING_LENGTH);

            final String extractedText = tika.parseToString(inputStream);
            if (!StringUtils.hasText(extractedText)) {
                // Explicit fallback path: advanced parser can recover content for some edge files.
                return extractTextWithAdvancedTika(file);
            }

            return extractedText;

        } catch (final TikaException e) {
            return extractTextWithAdvancedTika(file);
        } catch (final RuntimeException e) {
            throw new IOException("Unexpected runtime error while extracting text from file: "
                    + file.getOriginalFilename(), e);
        }
    }

    /**
     * Fallback mechanism using an advanced AutoDetectParser from Apache Tika to parse complex documents.
     *
     * @param file the file that failed initial basic parsing
     * @return the textual content derived from the advanced processing
     * @throws IOException if parsing severely fails or a read limit is exceeded
     */
    private String extractTextWithAdvancedTika(final MultipartFile file) throws IOException {
        try (final InputStream inputStream = file.getInputStream()) {
            final Parser parser = new AutoDetectParser();

            final Metadata metadata = new Metadata();
            metadata.set("resourceName", file.getOriginalFilename());
            if (file.getContentType() != null) {
                metadata.set("Content-Type", file.getContentType());
            }

            final BodyContentHandler handler = new BodyContentHandler(TIKA_MAX_STRING_LENGTH);
            final ParseContext context = new ParseContext();
            context.set(Parser.class, parser);

            parser.parse(inputStream, handler, metadata, context);

            final String extractedText = handler.toString();
            if (!StringUtils.hasText(extractedText)) {
                return "";
            }

            final byte[] utf8Bytes = extractedText.getBytes(StandardCharsets.UTF_8);
            return new String(utf8Bytes, StandardCharsets.UTF_8);

        } catch (final SAXException | TikaException e) {
            throw new IOException("Failed to extract text using advanced Tika parser: " + e.getMessage(), e);
        } catch (final RuntimeException e) {
            throw new IOException("Unexpected runtime error in advanced Tika parser while extracting text from file: "
                    + file.getOriginalFilename(), e);
        }
    }
}
