package ie.com.rag.service;

/**
 * Enum for handling special skill capitalization cases
 */
public enum SkillCapitalization {
    JAVASCRIPT("javascript", "JavaScript"),
    TYPESCRIPT("typescript", "TypeScript"),
    NODE_JS_DOT("node.js", "Node.js"),
    NODE_JS("nodejs", "Node.js"),
    CI_CD("ci/cd", "CI/CD"),
    HTML("html", "HTML"),
    CSS("css", "CSS"),
    SQL("sql", "SQL"),
    API("api", "API"),
    AWS("aws", "AWS"),
    GCP("gcp", "GCP");

    private final String lowerCase;
    private final String properCase;

    SkillCapitalization(String lowerCase, String properCase) {
        this.lowerCase = lowerCase;
        this.properCase = properCase;
    }

    public String getLowerCase() {
        return lowerCase;
    }

    public String getProperCase() {
        return properCase;
    }

    /**
     * Find the proper capitalization for a given skill
     * @param skill the skill in lowercase
     * @return the properly capitalized skill or null if not found
     */
    public static String getProperCapitalization(String skill) {
        for (SkillCapitalization sc : values()) {
            if (sc.lowerCase.equals(skill.toLowerCase())) {
                return sc.properCase;
            }
        }
        return null;
    }
}