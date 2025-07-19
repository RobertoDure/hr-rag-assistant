package ie.com.rag.utils;

import org.springframework.stereotype.Component;

@Component
public class TextUtils {

    /**
     * Normalize and sanitize text content for PostgreSQL compatibility.
     * This method removes problematic characters and normalizes whitespace.
     *
     * @param content The text content to sanitize.
     * @return Sanitized text content.
     */

    public static String sanitizeTextContent(String content) {
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
}
