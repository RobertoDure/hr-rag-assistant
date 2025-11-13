package ie.com.rag.service;

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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ie.com.rag.Constants.PROMPT;

@Service
public class RagDocumentService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final JdbcClient jdbcClient;

    // Token limits for content management
    private static final int MAX_DOCUMENT_TOKENS = 6000; // Conservative limit for document content
    private static final int APPROX_CHARS_PER_TOKEN = 4; // Rough estimation: 1 token ≈ 4 characters

    public RagDocumentService(ChatModel chatModel, VectorStore vectorStore, JdbcClient jdbcClient) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.jdbcClient = jdbcClient;
    }

    /**
     * This method is used to answer questions based on the documents processed in the RAG system.
     * It uses a prompt template to format the question and the relevant documents.
     * @param question The question to be answered.
     * @return The answer to the question based on the documents.
     */

    @Tool(name = "rag_listed_documents",
          description = "RAG to get knowledge from the documents that have been processed")
    public String knowledgeRAG(String question) {

        // Create a prompt template with the provided prompt string
        PromptTemplate template = new PromptTemplate(PROMPT);
        Map<String, Object> promptsParameters = new HashMap<>();
        promptsParameters.put("input", question);
        promptsParameters.put("documents", findSimilarData(question));

        return chatModel
                .call(template.create(promptsParameters))
                .getResult()
                .getOutput()
                .getContent();

    }

    /**
     * Finds similar data in the vector store based on the question.
     * Now includes proper token management to prevent exceeding limits.
     * @param question The question to search for similar documents
     * @return A string containing the concatenated content of similar documents, truncated if necessary
     */
    private String findSimilarData(String question) {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest
                .query(question)
                .withTopK(5));

        StringBuilder result = new StringBuilder();
        int currentTokenCount = 0;
        int maxChars = MAX_DOCUMENT_TOKENS * APPROX_CHARS_PER_TOKEN;

        for (Document doc : documents) {
            String content = doc.getContent();
            String metadata = doc.getMetadata().toString();

            // Add document separator and metadata
            String docSection = "\n--- Document: " + metadata + " ---\n" + content + "\n";

            // Check if adding this document would exceed our limit
            if (result.length() + docSection.length() > maxChars) {
                // If this is the first document and it's too large, truncate it
                if (result.length() == 0) {
                    int availableChars = maxChars - ("\n--- Document: " + metadata + " ---\n").length() - 50;
                    if (availableChars > 0) {
                        String truncatedContent = content.substring(0, Math.min(content.length(), availableChars));
                        result.append("\n--- Document: ").append(metadata).append(" ---\n")
                               .append(truncatedContent)
                               .append("\n[Content truncated due to length]\n");
                    }
                }
                break; // Stop adding more documents
            }

            result.append(docSection);
        }

        return result.toString();
    }

    /**
     * Process and store document content in the vector store for RAG operations.
     * Now includes content chunking for large documents and text sanitization.
     * @param content The document content to process
     * @param filename The filename for metadata
     */
    public void processDocument(String content, String filename) {
        try {
            // Sanitize content before processing
            content = sanitizeTextContent(content);

            // Split large documents into smaller chunks
            List<String> chunks = chunkContent(content, 2000); // 2000 chars per chunk

            for (int i = 0; i < chunks.size(); i++) {
                // Sanitize each chunk as well
                String sanitizedChunk = sanitizeTextContent(chunks.get(i));

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("filename", filename);
                metadata.put("type", "cv");
                metadata.put("chunk", i + 1);
                metadata.put("totalChunks", chunks.size());

                Document document = new Document(sanitizedChunk, metadata);
                vectorStore.add(List.of(document));
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
    }

    /**
     * Sanitize text content to remove null bytes and other characters that cause PostgreSQL issues
     */
    private String sanitizeTextContent(String content) {
        if (content == null) {
            return "";
        }

        // Remove null bytes (0x00) which cause PostgreSQL UTF-8 encoding issues
        content = content.replace("\u0000", "");

        // Remove other problematic control characters but keep useful whitespace
        content = content.replaceAll("[\u0001-\u0008\u000B\u000C\u000E-\u001F\u007F]", "");

        // Normalize line endings
        content = content.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

        // Remove excessive whitespace but preserve paragraph structure
        content = content.replaceAll("[ \t]+", " "); // Multiple spaces/tabs to single space
        content = content.replaceAll("\n[ \t]+", "\n"); // Remove leading whitespace on lines
        content = content.replaceAll("[ \t]+\n", "\n"); // Remove trailing whitespace on lines
        content = content.replaceAll("\n{3,}", "\n\n"); // Multiple newlines to double newlines

        return content.trim();
    }

    /**
     * Chunks content into smaller pieces to improve vector search and prevent token overflow
     * @param content The content to chunk
     * @param maxChunkSize Maximum size of each chunk in characters
     * @return List of content chunks
     */
    private List<String> chunkContent(String content, int maxChunkSize) {
        List<String> chunks = new java.util.ArrayList<>();

        if (content.length() <= maxChunkSize) {
            chunks.add(content);
            return chunks;
        }

        // Split by paragraphs first
        String[] paragraphs = content.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            // If adding this paragraph would exceed chunk size
            if (currentChunk.length() + paragraph.length() + 2 > maxChunkSize) {
                // Save current chunk if it has content
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }

                // If single paragraph is larger than chunk size, split it
                if (paragraph.length() > maxChunkSize) {
                    for (int i = 0; i < paragraph.length(); i += maxChunkSize) {
                        chunks.add(paragraph.substring(i, Math.min(i + maxChunkSize, paragraph.length())));
                    }
                } else {
                    currentChunk.append(paragraph).append("\n\n");
                }
            } else {
                currentChunk.append(paragraph).append("\n\n");
            }
        }

        // Add remaining content
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * Process and store file using Apache Tika for robust text extraction.
     * Supports multiple file formats (PDF, DOC, DOCX, TXT, etc.) with proper UTF-8 encoding.
     * @param file The uploaded file to process
     * @throws IOException if file processing fails
     */
    public void processAndStoreFile(MultipartFile file) throws IOException {
        try {
            // Count existing documents for logging
            Integer count = jdbcClient.sql("select COUNT(*) from vector_store")
                    .query(Integer.class)
                    .single();

            // Extract text using Apache Tika
            String extractedText = extractTextWithTika(file);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new IOException("No text could be extracted from the file: " + file.getOriginalFilename());
            }

            // Process the extracted text using our existing method
            processDocument(extractedText, file.getOriginalFilename());

        } catch (Exception e) {
            throw new IOException("Failed to process file: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * Extract text from various file formats using Apache Tika with proper UTF-8 handling.
     * This method handles encoding issues automatically and supports many file formats.
     * @param file The multipart file to extract text from
     * @return Extracted text content with proper UTF-8 encoding
     * @throws IOException if text extraction fails
     */
    private String extractTextWithTika(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {

            // Method 1: Simple Tika extraction (handles most cases automatically)
            Tika tika = new Tika();
            tika.setMaxStringLength(10 * 1024 * 1024); // 10MB limit to prevent memory issues

            // Tika automatically handles character encoding detection and conversion to UTF-8
            String extractedText = tika.parseToString(inputStream);

            // If simple method fails or returns empty, try advanced method
            if (extractedText == null || extractedText.trim().isEmpty()) {
                return extractTextWithAdvancedTika(file);
            }

            return extractedText;

        } catch (TikaException e) {
            // If Tika parsing fails, try the advanced method as fallback
            return extractTextWithAdvancedTika(file);
        } catch (Exception e) {
            // Handle any other unexpected exceptions during Tika processing
            throw new IOException("Unexpected error while extracting text from file: " +
                file.getOriginalFilename() + ". Error: " + e.getMessage(), e);
        }
    }

    /**
     * Advanced Apache Tika text extraction with explicit metadata and encoding handling.
     * Used as fallback when simple extraction fails.
     * @param file The multipart file to extract text from
     * @return Extracted text content
     * @throws IOException if extraction fails
     */
    private String extractTextWithAdvancedTika(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {

            // Use AutoDetectParser for better file type detection
            Parser parser = new AutoDetectParser();

            // Create metadata object to capture file information
            Metadata metadata = new Metadata();
            metadata.set("resourceName", file.getOriginalFilename());
            if (file.getContentType() != null) {
                metadata.set("Content-Type", file.getContentType());
            }

            // Use BodyContentHandler with increased string length limit
            BodyContentHandler handler = new BodyContentHandler(10 * 1024 * 1024); // 10MB limit

            // Create parse context for additional configuration
            ParseContext context = new ParseContext();
            context.set(Parser.class, parser);

            // Parse the document
            parser.parse(inputStream, handler, metadata, context);

            // Get the extracted text
            String extractedText = handler.toString();

            // Ensure we have UTF-8 compatible text
            if (extractedText != null) {
                // Convert to bytes and back to ensure proper UTF-8 encoding
                byte[] utf8Bytes = extractedText.getBytes(StandardCharsets.UTF_8);
                extractedText = new String(utf8Bytes, StandardCharsets.UTF_8);
            }

            return extractedText;

        } catch (SAXException | TikaException e) {
            throw new IOException("Failed to extract text using advanced Tika parser: " + e.getMessage(), e);
        } catch (Exception e) {
            // Handle any other unexpected exceptions during advanced Tika processing
            throw new IOException("Unexpected error in advanced Tika parser while extracting text from file: " +
                file.getOriginalFilename() + ". Error: " + e.getMessage(), e);
        }
    }
}
