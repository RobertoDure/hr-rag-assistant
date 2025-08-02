package ie.com.rag;

import org.springframework.stereotype.Component;

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
}
