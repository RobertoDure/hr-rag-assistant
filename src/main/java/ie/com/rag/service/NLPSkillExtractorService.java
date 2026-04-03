package ie.com.rag.service;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import ie.com.rag.Constants;
import ie.com.rag.utils.SkillDatabaseLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NLPSkillExtractorService {

    private StanfordCoreNLP pipeline;
    private Set<String> allKnownSkillsLower;
    private Set<String> technicalTermsLower;

    @PostConstruct
    public void init() {
        initializeSkillDatabase();
        initializePipeline();
    }

    public List<String> extractSkills(final String cvContent) {
        if (!StringUtils.hasText(cvContent)) {
            return List.of();
        }

        try {
            final Set<String> extracted = new HashSet<>();
            extracted.addAll(extractByPatterns(cvContent));
            extracted.addAll(extractByContext(cvContent));
            extracted.addAll(extractByNer(cvContent));

            final List<String> result = buildSkillList(extracted);
            log.debug("[NLPSkillExtractor] - EXTRACT: skills found: {}", result.size());
            return result;

        } catch (final RuntimeException e) {
            log.warn("[NLPSkillExtractor] - EXTRACT: NLP extraction failed, using fallback: {}", e.getMessage());
            return simpleExtract(cvContent);
        }
    }

    private Set<String> extractByNer(final String cvContent) {
        if (pipeline == null) {
            return Set.of();
        }

        final Set<String> skills = new HashSet<>();
        try {
            final Annotation document = new Annotation(cvContent);
            pipeline.annotate(document);

            final List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
            if (sentences == null || sentences.isEmpty()) {
                return skills;
            }

            for (final CoreMap sentence : sentences) {
                final List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
                if (tokens == null) {
                    continue;
                }
                collectSkillsFromTokens(tokens, skills);
            }
        } catch (final RuntimeException e) {
            log.warn("[NLPSkillExtractor] - NER: extraction error: {}", e.getMessage());
        }

        return skills;
    }

    private void collectSkillsFromTokens(final List<CoreLabel> tokens, final Set<String> skills) {
        final StringBuilder phraseBuilder = new StringBuilder();

        for (final CoreLabel token : tokens) {
            final String word = token.get(CoreAnnotations.TextAnnotation.class);
            if (!StringUtils.hasText(word)) {
                continue;
            }

            if (isKnownSkill(word)) {
                skills.add(capitalize(word));
            }

            final String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
            final String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);

            final boolean isTechEntity = "ORGANIZATION".equals(ner) && isTechnicalTerm(word);
            final boolean isTechNoun = pos != null && (pos.startsWith("NNP") || "NN".equals(pos)) && isTechnicalTerm(word);

            if (isTechEntity || isTechNoun) {
                if (phraseBuilder.length() > 0) {
                    phraseBuilder.append(" ");
                }
                phraseBuilder.append(word);
            } else if (phraseBuilder.length() > 0) {
                flushPhrase(phraseBuilder, skills);
            }
        }

        if (phraseBuilder.length() > 0) {
            flushPhrase(phraseBuilder, skills);
        }
    }

    private void flushPhrase(final StringBuilder phraseBuilder, final Set<String> skills) {
        final String phrase = phraseBuilder.toString().trim();
        if (isKnownSkill(phrase)) {
            skills.add(capitalize(phrase));
        }
        phraseBuilder.setLength(0);
    }

    private Set<String> extractByPatterns(final String cvContent) {
        final Set<String> skills = new HashSet<>();

        final Matcher techMatcher = Constants.TECH_PATTERN.matcher(cvContent);
        while (techMatcher.find()) {
            skills.add(capitalize(techMatcher.group()));
        }

        final Matcher versionMatcher = Constants.VERSION_PATTERN.matcher(cvContent);
        while (versionMatcher.find()) {
            final String technology = versionMatcher.group(1);
            if (isTechnicalTerm(technology)) {
                skills.add(capitalize(technology));
            }
        }

        return skills;
    }

    private Set<String> extractByContext(final String cvContent) {
        final Set<String> skills = new HashSet<>();

        for (final String line : cvContent.split("\\n")) {
            if (!StringUtils.hasText(line)) {
                continue;
            }

            final String lowerLine = line.toLowerCase();

            if (isSkillSectionLine(lowerLine)) {
                Arrays.stream(line.split("[,;•\\-*|]"))
                        .map(s -> s.trim().replaceAll("^[\\s•\\-*]+", ""))
                        .filter(s -> s.length() >= Constants.MIN_SKILL_LENGTH && s.length() <= Constants.MAX_SKILL_LENGTH)
                        .filter(this::isValidSkill)
                        .forEach(s -> skills.add(capitalize(s)));
            } else if (isExperienceContextLine(lowerLine)) {
                allKnownSkillsLower.stream()
                        .filter(lowerLine::contains)
                        .forEach(knownSkill -> skills.add(capitalize(knownSkill)));
            }
        }

        return skills;
    }

    private List<String> buildSkillList(final Set<String> rawSkills) {
        return rawSkills.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(this::isValidSkill)
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> simpleExtract(final String cvContent) {
        if (!StringUtils.hasText(cvContent)) {
            return List.of();
        }

        final String lowerContent = cvContent.toLowerCase();
        return allKnownSkillsLower.stream()
                .filter(lowerContent::contains)
                .map(this::capitalize)
                .sorted()
                .toList();
    }

    private void initializeSkillDatabase() {
        technicalTermsLower = SkillDatabaseLoader.getEntries().stream()
                .filter(e -> Constants.TECHNICAL_CATEGORIES.contains(e.getCategory()))
                .map(e -> e.getSkillName().toLowerCase())
                .collect(Collectors.toUnmodifiableSet());

        allKnownSkillsLower = SkillDatabaseLoader.getAllSkills().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void initializePipeline() {
        try {
            final Properties props = new Properties();
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
            props.setProperty("ner.useSUTime", "false");
            props.setProperty("tokenize.language", "en");
            pipeline = new StanfordCoreNLP(props);
            log.info("[NLPSkillExtractor] - INIT: NLP pipeline initialized successfully");
        } catch (final RuntimeException e) {
            log.error("[NLPSkillExtractor] - INIT: Failed to initialize NLP pipeline: {}", e.getMessage(), e);
        }
    }

    private boolean isKnownSkill(final String skill) {
        return StringUtils.hasText(skill) && allKnownSkillsLower.contains(skill.trim().toLowerCase());
    }

    private boolean isTechnicalTerm(final String term) {
        if (!StringUtils.hasText(term)) {
            return false;
        }
        final String normalized = term.trim().toLowerCase();
        return technicalTermsLower.contains(normalized) || Constants.TECH_PATTERN.matcher(normalized).find();
    }

    private boolean isValidSkill(final String skill) {
        if (!StringUtils.hasText(skill)) {
            return false;
        }
        final String clean = skill.trim();
        final boolean isLengthValid = clean.length() >= Constants.MIN_SKILL_LENGTH && clean.length() <= Constants.MAX_SKILL_LENGTH;
        return isLengthValid
                && Constants.VALID_SKILL_PATTERN.matcher(clean).matches()
                && (isKnownSkill(clean) || isTechnicalTerm(clean));
    }

    private boolean isSkillSectionLine(final String lowerLine) {
        return Constants.SKILL_SECTION_MARKERS.stream().anyMatch(lowerLine::contains);
    }

    private boolean isExperienceContextLine(final String lowerLine) {
        return Constants.EXPERIENCE_MARKERS.stream().anyMatch(lowerLine::contains);
    }

    private String capitalize(final String skill) {
        if (!StringUtils.hasText(skill)) {
            return skill;
        }
        final String trimmed = skill.trim();
        final String properCase = SkillDatabaseLoader.getProperCapitalization(trimmed.toLowerCase());
        return properCase != null ? properCase : Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }
}
