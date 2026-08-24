package ie.com.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NLPSkillExtractorService}.
 *
 * <p>The Stanford CoreNLP pipeline is intentionally not initialized here (no Spring context),
 * so {@code extractByNer} degrades to an empty set and only the pattern/context extraction
 * paths are exercised against the real bundled skill database.</p>
 */
class NLPSkillExtractorServiceTest {

    private NLPSkillExtractorService service;

    @BeforeEach
    void setUp() {
        service = new NLPSkillExtractorService();
        ReflectionTestUtils.invokeMethod(service, "initializeSkillDatabase");
    }

    @Test
    @DisplayName("Should return empty list for null or blank content")
    void extractSkills_blankContent_returnsEmpty() {
        assertThat(service.extractSkills(null)).isEmpty();
        assertThat(service.extractSkills("   ")).isEmpty();
        assertThat(service.extractSkills("")).isEmpty();
    }

    @Test
    @DisplayName("Should extract skills listed in a skills section")
    void extractSkills_skillSectionLine_extractsSkills() {
        final List<String> skills = service.extractSkills("John Doe\nSkills: Java, Python, PostgreSQL");
        assertThat(skills).contains("Java", "Python", "PostgreSQL");
    }

    @Test
    @DisplayName("Should extract known skills from experience context")
    void extractSkills_experienceContext_extractsKnownSkills() {
        final List<String> skills = service.extractSkills("Worked with Java Spring and SQL for 5 years");
        assertThat(skills).contains("Java", "Spring", "SQL");
    }

    @Test
    @DisplayName("Should extract technology names with versions")
    void extractSkills_versionedTechnology_extractsBaseTechnology() {
        final List<String> skills = service.extractSkills("Java 17 developer, Spring Boot 3");
        assertThat(skills).contains("Java", "Spring");
    }

    @Test
    @DisplayName("Should extract multi-word technologies via pattern matching")
    void extractSkills_techPattern_extractsMultiWordSkills() {
        final List<String> skills = service.extractSkills("Built machine learning models using Python");
        assertThat(skills).contains("Machine learning", "Python");
    }

    @Test
    @DisplayName("Should deduplicate repeated skills")
    void extractSkills_duplicates_removed() {
        final List<String> skills = service.extractSkills("Skills: Java, Java, Python");
        assertThat(skills).hasSize(2);
        assertThat(skills).containsExactlyInAnyOrder("Java", "Python");
    }

    @Test
    @DisplayName("Should return empty list when no skills are present")
    void extractSkills_noSkills_returnsEmpty() {
        assertThat(service.extractSkills("Hello world, this is a plain paragraph.")).isEmpty();
    }

    @Test
    @DisplayName("Should apply proper capitalization to known acronyms")
    void extractSkills_knownAcronyms_properCapitalization() {
        final List<String> skills = service.extractSkills("Skills: sql, html, aws");
        assertThat(skills).contains("SQL", "HTML", "AWS");
    }
}
