package ie.com.rag.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enum containing all skill databases organized by categories
 */
public enum SkillDatabase {

    // Technical Skills
    JAVA("Java", SkillCategory.TECHNICAL),
    PYTHON("Python", SkillCategory.TECHNICAL),
    JAVASCRIPT("JavaScript", SkillCategory.TECHNICAL),
    TYPESCRIPT("TypeScript", SkillCategory.TECHNICAL),
    CPP("C++", SkillCategory.TECHNICAL),
    CSHARP("C#", SkillCategory.TECHNICAL),
    GO("Go", SkillCategory.TECHNICAL),
    RUST("Rust", SkillCategory.TECHNICAL),
    KOTLIN("Kotlin", SkillCategory.TECHNICAL),
    SWIFT("Swift", SkillCategory.TECHNICAL),
    PHP("PHP", SkillCategory.TECHNICAL),
    RUBY("Ruby", SkillCategory.TECHNICAL),
    SCALA("Scala", SkillCategory.TECHNICAL),
    CLOJURE("Clojure", SkillCategory.TECHNICAL),
    PERL("Perl", SkillCategory.TECHNICAL),
    R("R", SkillCategory.TECHNICAL),
    MATLAB("MATLAB", SkillCategory.TECHNICAL),
    DART("Dart", SkillCategory.TECHNICAL),
    OBJECTIVE_C("Objective-C", SkillCategory.TECHNICAL),

    // Frameworks
    REACT("React", SkillCategory.FRAMEWORK),
    ANGULAR("Angular", SkillCategory.FRAMEWORK),
    VUE("Vue", SkillCategory.FRAMEWORK),
    SPRING("Spring", SkillCategory.FRAMEWORK),
    NODE_JS("Node.js", SkillCategory.FRAMEWORK),
    EXPRESS("Express", SkillCategory.FRAMEWORK),
    DJANGO("Django", SkillCategory.FRAMEWORK),
    FLASK("Flask", SkillCategory.FRAMEWORK),
    LARAVEL("Laravel", SkillCategory.FRAMEWORK),
    SYMFONY("Symfony", SkillCategory.FRAMEWORK),
    RUBY_ON_RAILS("Ruby on Rails", SkillCategory.FRAMEWORK),
    ASP_NET("ASP.NET", SkillCategory.FRAMEWORK),
    BOOTSTRAP("Bootstrap", SkillCategory.FRAMEWORK),
    TAILWIND("Tailwind", SkillCategory.FRAMEWORK),
    JQUERY("jQuery", SkillCategory.FRAMEWORK),
    HIBERNATE("Hibernate", SkillCategory.FRAMEWORK),
    STRUTS("Struts", SkillCategory.FRAMEWORK),
    PLAY_FRAMEWORK("Play Framework", SkillCategory.FRAMEWORK),
    QUARKUS("Quarkus", SkillCategory.FRAMEWORK),
    MICRONAUT("Micronaut", SkillCategory.FRAMEWORK),

    // Databases
    MYSQL("MySQL", SkillCategory.DATABASE),
    POSTGRESQL("PostgreSQL", SkillCategory.DATABASE),
    MONGODB("MongoDB", SkillCategory.DATABASE),
    ORACLE("Oracle", SkillCategory.DATABASE),
    SQL_SERVER("SQL Server", SkillCategory.DATABASE),
    SQLITE("SQLite", SkillCategory.DATABASE),
    REDIS("Redis", SkillCategory.DATABASE),
    CASSANDRA("Cassandra", SkillCategory.DATABASE),
    NEO4J("Neo4j", SkillCategory.DATABASE),
    DYNAMODB("DynamoDB", SkillCategory.DATABASE),
    ELASTICSEARCH("Elasticsearch", SkillCategory.DATABASE),
    INFLUXDB("InfluxDB", SkillCategory.DATABASE),
    COUCHDB("CouchDB", SkillCategory.DATABASE),
    MARIADB("MariaDB", SkillCategory.DATABASE),

    // Cloud Platforms
    AWS("AWS", SkillCategory.CLOUD_PLATFORM),
    AZURE("Azure", SkillCategory.CLOUD_PLATFORM),
    GCP("GCP", SkillCategory.CLOUD_PLATFORM),
    GOOGLE_CLOUD("Google Cloud", SkillCategory.CLOUD_PLATFORM),
    DIGITAL_OCEAN("Digital Ocean", SkillCategory.CLOUD_PLATFORM),
    HEROKU("Heroku", SkillCategory.CLOUD_PLATFORM),
    VERCEL("Vercel", SkillCategory.CLOUD_PLATFORM),
    NETLIFY("Netlify", SkillCategory.CLOUD_PLATFORM),

    // Methodologies
    AGILE("Agile", SkillCategory.METHODOLOGY),
    SCRUM("Scrum", SkillCategory.METHODOLOGY),
    KANBAN("Kanban", SkillCategory.METHODOLOGY),
    DEVOPS("DevOps", SkillCategory.METHODOLOGY),
    CI_CD("CI/CD", SkillCategory.METHODOLOGY),
    TDD("TDD", SkillCategory.METHODOLOGY),
    BDD("BDD", SkillCategory.METHODOLOGY),
    MICROSERVICES("Microservices", SkillCategory.METHODOLOGY),
    SOA("SOA", SkillCategory.METHODOLOGY),

    // Soft Skills
    LEADERSHIP("Leadership", SkillCategory.SOFT_SKILL),
    COMMUNICATION("Communication", SkillCategory.SOFT_SKILL),
    TEAMWORK("Teamwork", SkillCategory.SOFT_SKILL),
    PROBLEM_SOLVING("Problem Solving", SkillCategory.SOFT_SKILL),
    CRITICAL_THINKING("Critical Thinking", SkillCategory.SOFT_SKILL),
    PROJECT_MANAGEMENT("Project Management", SkillCategory.SOFT_SKILL),
    TIME_MANAGEMENT("Time Management", SkillCategory.SOFT_SKILL),
    ADAPTABILITY("Adaptability", SkillCategory.SOFT_SKILL),
    CREATIVITY("Creativity", SkillCategory.SOFT_SKILL),
    COLLABORATION("Collaboration", SkillCategory.SOFT_SKILL);

    private final String skillName;
    private final SkillCategory category;

    SkillDatabase(String skillName, SkillCategory category) {
        this.skillName = skillName;
        this.category = category;
    }

    public String getSkillName() {
        return skillName;
    }

    public SkillCategory getCategory() {
        return category;
    }

    /**
     * Get all skills by category
     */
    public static Set<String> getSkillsByCategory(SkillCategory category) {
        return Arrays.stream(values())
                .filter(skill -> skill.category == category)
                .map(SkillDatabase::getSkillName)
                .collect(Collectors.toSet());
    }

    /**
     * Get all technical skills
     */
    public static Set<String> getTechnicalSkills() {
        return getSkillsByCategory(SkillCategory.TECHNICAL);
    }

    /**
     * Get all frameworks
     */
    public static Set<String> getFrameworks() {
        return getSkillsByCategory(SkillCategory.FRAMEWORK);
    }

    /**
     * Get all databases
     */
    public static Set<String> getDatabases() {
        return getSkillsByCategory(SkillCategory.DATABASE);
    }

    /**
     * Get all cloud platforms
     */
    public static Set<String> getCloudPlatforms() {
        return getSkillsByCategory(SkillCategory.CLOUD_PLATFORM);
    }

    /**
     * Get all methodologies
     */
    public static Set<String> getMethodologies() {
        return getSkillsByCategory(SkillCategory.METHODOLOGY);
    }

    /**
     * Get all soft skills
     */
    public static Set<String> getSoftSkills() {
        return getSkillsByCategory(SkillCategory.SOFT_SKILL);
    }

    /**
     * Get all skills as a single set
     */
    public static Set<String> getAllSkills() {
        return Arrays.stream(values())
                .map(SkillDatabase::getSkillName)
                .collect(Collectors.toSet());
    }

    /**
     * Enum for skill categories
     */
    public enum SkillCategory {
        TECHNICAL,
        FRAMEWORK,
        DATABASE,
        CLOUD_PLATFORM,
        METHODOLOGY,
        SOFT_SKILL
    }
}
