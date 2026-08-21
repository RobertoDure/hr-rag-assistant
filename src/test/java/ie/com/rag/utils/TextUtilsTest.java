package ie.com.rag.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextUtilsTest {

    @Test
    @DisplayName("Should return empty string for null input")
    void sanitize_nullInput_returnsEmpty() {
        assertThat(TextUtils.sanitizeTextContent(null)).isEmpty();
    }

    @Test
    @DisplayName("Should remove null bytes that break PostgreSQL UTF-8 encoding")
    void sanitize_nullBytes_removed() {
        final String result = TextUtils.sanitizeTextContent("He\u0000llo \u0000World");
        assertThat(result).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("Should remove control characters but keep whitespace")
    void sanitize_controlCharacters_removed() {
        final String result = TextUtils.sanitizeTextContent("Line1\u0001Line2\u007F");
        assertThat(result).isEqualTo("Line1Line2");
    }

    @Test
    @DisplayName("Should normalize Windows and old Mac line endings to \\n")
    void sanitize_lineEndings_normalized() {
        final String result = TextUtils.sanitizeTextContent("first\r\nsecond\rthird");
        assertThat(result).isEqualTo("first\nsecond\nthird");
    }

    @Test
    @DisplayName("Should collapse multiple spaces and tabs into a single space")
    void sanitize_multipleSpaces_collapsed() {
        final String result = TextUtils.sanitizeTextContent("Java\t\t  Spring    Boot");
        assertThat(result).isEqualTo("Java Spring Boot");
    }

    @Test
    @DisplayName("Should remove leading and trailing whitespace on lines")
    void sanitize_lineWhitespace_trimmed() {
        final String result = TextUtils.sanitizeTextContent("  alpha  \n   beta   ");
        assertThat(result).isEqualTo("alpha\nbeta");
    }

    @Test
    @DisplayName("Should collapse three or more newlines into a double newline")
    void sanitize_excessiveNewlines_collapsed() {
        final String result = TextUtils.sanitizeTextContent("one\n\n\n\ntwo");
        assertThat(result).isEqualTo("one\n\ntwo");
    }

    @Test
    @DisplayName("Should trim leading and trailing whitespace of the whole content")
    void sanitize_surroundingWhitespace_trimmed() {
        final String result = TextUtils.sanitizeTextContent("   hello world   ");
        assertThat(result).isEqualTo("hello world");
    }

    @Test
    @DisplayName("Should preserve paragraph structure")
    void sanitize_paragraphs_preserved() {
        final String result = TextUtils.sanitizeTextContent("Para one\n\nPara two");
        assertThat(result).isEqualTo("Para one\n\nPara two");
    }
}
