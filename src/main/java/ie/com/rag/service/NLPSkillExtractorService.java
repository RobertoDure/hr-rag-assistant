package ie.com.rag.service;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NLPSkillExtractorService {

    private static final Logger logger = LoggerFactory.getLogger(NLPSkillExtractorService.class);


    private StanfordCoreNLP pipeline;

    // Comprehensive skill database organized by categories
    private Set<String> technicalSkills;
    private Set<String> softSkills;
    private Set<String> frameworks;
    private Set<String> databases;
    private Set<String> cloudPlatforms;
    private Set<String> methodologies;

    @PostConstruct
    public void init() {
        try {
            // Initialize Stanford CoreNLP pipeline
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
            props.setProperty("ner.useSUTime", "false");
            this.pipeline = new StanfordCoreNLP(props);

            // Initialize skill databases
            initializeSkillDatabases();

            logger.info("NLP Skill Extractor Service initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize NLP pipeline: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract skills from CV content using advanced NLP techniques
     */
    public List<String> extractSkills(String cvContent) {
        Set<String> extractedSkills = new HashSet<>();

        try {
            // Method 1: Named Entity Recognition and POS tagging
            extractedSkills.addAll(extractSkillsUsingNER(cvContent));

            // Method 2: Pattern-based extraction for technical terms
            extractedSkills.addAll(extractSkillsUsingPatterns(cvContent));

            // Method 3: Context-aware skill detection
            extractedSkills.addAll(extractSkillsUsingContext(cvContent));

            // Filter and validate extracted skills
            List<String> validatedSkills = validateAndFilterSkills(extractedSkills, cvContent);

            logger.debug("Extracted {} skills using NLP: {}", validatedSkills.size(), validatedSkills);
            return validatedSkills;

        } catch (Exception e) {
            logger.error("Error during NLP skill extraction: {}", e.getMessage(), e);
            // Fallback to simple pattern matching
            return extractSkillsSimple(cvContent);
        }
    }

    /**
     * Extract skills using Named Entity Recognition and Part-of-Speech tagging
     */
    private Set<String> extractSkillsUsingNER(String cvContent) {
        Set<String> skills = new HashSet<>();

        if (pipeline == null) {
            logger.warn("NLP pipeline not initialized, skipping NER extraction");
            return skills;
        }

        try {
            // Create an annotation object
            Annotation document = new Annotation(cvContent);

            // Run all annotators
            pipeline.annotate(document);

            // Extract sentences
            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

            for (CoreMap sentence : sentences) {
                // Look for technical terms and proper nouns that might be skills
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    String word = token.get(CoreAnnotations.TextAnnotation.class);
                    String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                    // Check if it's a known skill
                    if (isKnownSkill(word)) {
                        skills.add(word);
                    }

                    // Look for organization names that might be technologies
                    if ("ORGANIZATION".equals(ner) && isTechnicalTerm(word)) {
                        skills.add(word);
                    }

                    // Look for proper nouns that are technical skills
                    if (pos != null && (pos.startsWith("NNP") || pos.equals("NN")) && isTechnicalTerm(word)) {
                        skills.add(word);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error in NER skill extraction: {}", e.getMessage());
        }

        return skills;
    }

    /**
     * Extract skills using regex patterns for technical terms
     */
    private Set<String> extractSkillsUsingPatterns(String cvContent) {
        Set<String> skills = new HashSet<>();
        String lowerContent = cvContent.toLowerCase();

        // Pattern for programming languages and frameworks
        Pattern techPattern = Pattern.compile("\\b(java|python|javascript|typescript|react|angular|vue|spring|nodejs|docker|kubernetes|aws|azure|gcp|sql|mongodb|postgresql|mysql|git|jenkins|ci/cd|agile|scrum|devops|microservices|api|rest|graphql|html|css|sass|less|bootstrap|tailwind|maven|gradle|junit|selenium|cypress|terraform|ansible|redis|elasticsearch|kafka|rabbitmq|nginx|apache|linux|ubuntu|centos|windows|macos)\\b", Pattern.CASE_INSENSITIVE);

        var matcher = techPattern.matcher(cvContent);
        while (matcher.find()) {
            skills.add(capitalizeSkill(matcher.group()));
        }

        // Pattern for version-specific technologies
        Pattern versionPattern = Pattern.compile("\\b([a-zA-Z]+)\\s*[vV]?\\d+(\\.\\d+)*\\b");
        matcher = versionPattern.matcher(cvContent);
        while (matcher.find()) {
            String tech = matcher.group(1);
            if (isTechnicalTerm(tech)) {
                skills.add(capitalizeSkill(tech));
            }
        }

        return skills;
    }

    /**
     * Extract skills using context-aware detection
     */
    private Set<String> extractSkillsUsingContext(String cvContent) {
        Set<String> skills = new HashSet<>();
        String[] lines = cvContent.split("\\n");

        for (String line : lines) {
            String lowerLine = line.toLowerCase();

            // Look for skill sections
            if (lowerLine.contains("skills") || lowerLine.contains("technologies") ||
                lowerLine.contains("technical") || lowerLine.contains("proficient")) {

                // Extract comma-separated or bullet point skills
                String[] potentialSkills = line.split("[,;•\\-\\*]");
                for (String skill : potentialSkills) {
                    skill = skill.trim().replaceAll("^[\\s•\\-\\*]+", "");
                    if (skill.length() > 2 && skill.length() < 30 && isValidSkill(skill)) {
                        skills.add(capitalizeSkill(skill));
                    }
                }
            }

            // Look for experience mentions
            if (lowerLine.contains("experience") || lowerLine.contains("worked with") ||
                lowerLine.contains("using") || lowerLine.contains("developed")) {

                // Extract skills mentioned in experience context
                for (String knownSkill : getAllKnownSkills()) {
                    if (lowerLine.contains(knownSkill.toLowerCase())) {
                        skills.add(knownSkill);
                    }
                }
            }
        }

        return skills;
    }

    /**
     * Validate and filter extracted skills
     */
    private List<String> validateAndFilterSkills(Set<String> extractedSkills, String cvContent) {
        return extractedSkills.stream()
            .filter(skill -> skill != null && skill.length() > 1)
            .filter(skill -> isValidSkill(skill))
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * Fallback simple extraction method
     */
    private List<String> extractSkillsSimple(String cvContent) {
        String[] commonSkills = {
            "Java", "Python", "JavaScript", "React", "Angular", "Spring", "Node.js",
            "SQL", "PostgreSQL", "MySQL", "MongoDB", "Docker", "Kubernetes",
            "AWS", "Azure", "Git", "Jenkins", "CI/CD", "Agile", "Scrum",
            "HTML", "CSS", "REST API", "Microservices", "Leadership", "Communication"
        };

        return Arrays.stream(commonSkills)
            .filter(skill -> cvContent.toLowerCase().contains(skill.toLowerCase()))
            .collect(Collectors.toList());
    }

    /**
     * Initialize comprehensive skill databases using SkillDatabase enum
     */
    private void initializeSkillDatabases() {
        technicalSkills = SkillDatabase.getTechnicalSkills();
        frameworks = SkillDatabase.getFrameworks();
        databases = SkillDatabase.getDatabases();
        cloudPlatforms = SkillDatabase.getCloudPlatforms();
        methodologies = SkillDatabase.getMethodologies();
        softSkills = SkillDatabase.getSoftSkills();
    }

    private boolean isKnownSkill(String skill) {
        String lowerSkill = skill.toLowerCase();
        return getAllKnownSkills().stream()
            .anyMatch(knownSkill -> knownSkill.toLowerCase().equals(lowerSkill));
    }

    private boolean isTechnicalTerm(String term) {
        return isKnownSkill(term) || term.length() > 2 &&
               (technicalSkills.contains(term) || frameworks.contains(term) ||
                databases.contains(term) || cloudPlatforms.contains(term));
    }

    private boolean isValidSkill(String skill) {
        if (skill == null || skill.trim().isEmpty()) return false;

        String cleanSkill = skill.trim();

        // Check if it's too short or too long
        if (cleanSkill.length() < 2 || cleanSkill.length() > 25) return false;

        // Check if it contains only valid characters
        if (!cleanSkill.matches("^[a-zA-Z0-9\\s\\.\\-\\+#]*$")) return false;

        // Check if it's a known skill or technical term
        return isKnownSkill(cleanSkill) || isTechnicalTerm(cleanSkill);
    }

    private String capitalizeSkill(String skill) {
        if (skill == null || skill.trim().isEmpty()) return skill;

        String trimmed = skill.trim();
        String lowerSkill = trimmed.toLowerCase();

        // Check for special cases using enum
        String specialCase = SkillCapitalization.getProperCapitalization(lowerSkill);
        if (specialCase != null) {
            return specialCase;
        }

        // Default capitalization
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    private Set<String> getAllKnownSkills() {
        return SkillDatabase.getAllSkills();
    }
}
