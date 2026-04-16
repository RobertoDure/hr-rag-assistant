package ie.com.rag;

import ie.com.rag.utils.SkillDatabaseLoader;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class Constants {

    /**
     * The prompt template for the chat model.
     * It instructs the model to answer questions about the Irish Constitution using the provided documents.
     */
    public static final String PROMPT = """
            Your task is to answer the questions about any content available. Use the information from the DOCUMENTS
            section to provide accurate answers. If unsure or if the answer isn't found in the DOCUMENTS section, 
            simply state that you don't know the answer.
            
            QUESTION:
            {input}
            
            DOCUMENTS:
            {documents}
            
            """;

    // Constants for better maintainability
    public static final int DEFAULT_RECENT_ITEMS_LIMIT = 5;
    public static final int TOP_SKILLS_LIMIT = 10;
    public static final int QA_HISTORY_LIMIT = 50;
    public static final int UPLOADED_DOCUMENTS_LIMIT = 50;
    public static final int RECENT_UPLOADS_DAYS = 7;
    public static final int GROWTH_METRICS_DAYS = 30;

    // Skill distribution ranges
    public static final String SKILLS_1_3 = "1-3 skills";
    public static final String SKILLS_4_7 = "4-7 skills";
    public static final String SKILLS_8_10 = "8-10 skills";
    public static final String SKILLS_10_PLUS = "10+ skills";

    // NLP skill extraction
    public static final int MIN_SKILL_LENGTH = 2;
    public static final int MAX_SKILL_LENGTH = 50;

    // Pattern covers current and emerging tech including AI/ML ecosystem
    public static final Pattern TECH_PATTERN = Pattern.compile(
            "\\b(java|python|javascript|typescript|react|angular|vue|spring|spring boot|spring cloud|" +
            "nodejs|docker|kubernetes|aws|azure|gcp|sql|mongodb|postgresql|mysql|git|jenkins|ci/cd|" +
            "agile|scrum|devops|microservices|api|rest|graphql|html|css|sass|less|bootstrap|tailwind|" +
            "maven|gradle|junit|selenium|cypress|terraform|ansible|redis|elasticsearch|kafka|rabbitmq|" +
            "nginx|apache|linux|ubuntu|centos|kotlin|golang|rust|scala|llm|openai|pytorch|tensorflow|" +
            "machine learning|deep learning|langchain|hugging face|natural language processing|" +
            "large language model|vector database|pgvector|spring ai)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public static final Pattern VERSION_PATTERN = Pattern.compile(
            "\\b([a-zA-Z][a-zA-Z0-9+#.-]*)\\s*[vV]?\\d+(\\.\\d+)*\\b"
    );

    public static final Pattern VALID_SKILL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9\\s.\\-+#/]*$"
    );

    public static final Set<String> SKILL_SECTION_MARKERS = Set.of(
            "skills", "technologies", "technical", "proficient", "competencies", "expertise", "tools"
    );

    public static final Set<String> EXPERIENCE_MARKERS = Set.of(
            "experience", "worked with", "using", "developed", "implemented", "proficient in", "built with"
    );

    public static final Set<SkillDatabaseLoader.SkillCategory> TECHNICAL_CATEGORIES = Set.of(
            SkillDatabaseLoader.SkillCategory.TECHNICAL,
            SkillDatabaseLoader.SkillCategory.FRAMEWORK,
            SkillDatabaseLoader.SkillCategory.DATABASE,
            SkillDatabaseLoader.SkillCategory.CLOUD_PLATFORM,
            SkillDatabaseLoader.SkillCategory.METHODOLOGY
    );
}
