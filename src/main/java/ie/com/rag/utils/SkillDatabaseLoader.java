package ie.com.rag.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public final class SkillDatabaseLoader {

    private static final String SKILLS_FILE = "/skills.json";
    private static final List<SkillEntry> ENTRIES;
    private static final Map<String, String> CAPITALIZATION_MAP;

    static {
        final SkillsFile file = loadSkillsFile();
        ENTRIES = file != null ? List.copyOf(file.getSkills()) : List.of();
        CAPITALIZATION_MAP = file != null ? buildCapitalizationMap(file.getCapitalization()) : Map.of();
    }

    private SkillDatabaseLoader() {
        throw new UnsupportedOperationException("This class should never be instantiated");
    }

    public enum SkillCategory {
        TECHNICAL,
        FRAMEWORK,
        DATABASE,
        CLOUD_PLATFORM,
        METHODOLOGY,
        SOFT_SKILL
    }

    public static List<SkillEntry> getEntries() {
        return ENTRIES;
    }

    public static Set<String> getAllSkills() {
        return ENTRIES.stream()
                .map(SkillEntry::getSkillName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<String> getSkillsByCategory(final SkillCategory category) {
        return ENTRIES.stream()
                .filter(e -> e.getCategory() == category)
                .map(SkillEntry::getSkillName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<String> getTechnicalSkills() {
        return getSkillsByCategory(SkillCategory.TECHNICAL);
    }

    public static Set<String> getFrameworks() {
        return getSkillsByCategory(SkillCategory.FRAMEWORK);
    }

    public static Set<String> getDatabases() {
        return getSkillsByCategory(SkillCategory.DATABASE);
    }

    public static Set<String> getCloudPlatforms() {
        return getSkillsByCategory(SkillCategory.CLOUD_PLATFORM);
    }

    public static Set<String> getMethodologies() {
        return getSkillsByCategory(SkillCategory.METHODOLOGY);
    }

    public static Set<String> getSoftSkills() {
        return getSkillsByCategory(SkillCategory.SOFT_SKILL);
    }

    public static String getProperCapitalization(final String skill) {
        if (skill == null || skill.isBlank()) {
            return null;
        }
        return CAPITALIZATION_MAP.get(skill.toLowerCase());
    }

    private static SkillsFile loadSkillsFile() {
        try (InputStream is = SkillDatabaseLoader.class.getResourceAsStream(SKILLS_FILE)) {
            if (is == null) {
                log.error("[SkillDatabaseLoader] - INIT: skills.json not found in classpath");
                return null;
            }
            return new ObjectMapper().readValue(is, SkillsFile.class);
        } catch (final IOException e) {
            log.error("[SkillDatabaseLoader] - INIT: Failed to load skills.json: {}", e.getMessage());
            return null;
        }
    }

    private static Map<String, String> buildCapitalizationMap(final List<CapitalizationEntry> entries) {
        if (entries == null) {
            return Map.of();
        }
        return entries.stream()
                .collect(Collectors.toUnmodifiableMap(CapitalizationEntry::getLower, CapitalizationEntry::getProper));
    }

    public static final class SkillEntry {

        private final String name;
        private final SkillCategory category;

        @JsonCreator
        public SkillEntry(
                @JsonProperty("name") final String name,
                @JsonProperty("category") final SkillCategory category) {
            this.name = name;
            this.category = category;
        }

        public String getSkillName() {
            return name;
        }

        public SkillCategory getCategory() {
            return category;
        }
    }

    public static final class CapitalizationEntry {

        private final String lower;
        private final String proper;

        @JsonCreator
        public CapitalizationEntry(
                @JsonProperty("lower") final String lower,
                @JsonProperty("proper") final String proper) {
            this.lower = lower;
            this.proper = proper;
        }

        public String getLower() {
            return lower;
        }

        public String getProper() {
            return proper;
        }
    }

    private static final class SkillsFile {

        private List<CapitalizationEntry> capitalization;
        private List<SkillEntry> skills;

        public List<CapitalizationEntry> getCapitalization() {
            return capitalization;
        }

        public void setCapitalization(final List<CapitalizationEntry> capitalization) {
            this.capitalization = capitalization;
        }

        public List<SkillEntry> getSkills() {
            return skills;
        }

        public void setSkills(final List<SkillEntry> skills) {
            this.skills = skills;
        }
    }
}
