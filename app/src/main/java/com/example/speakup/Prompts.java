package com.example.speakup;

/**
 * A utility class that holds the constant strings for AI prompts and JSON schemas.
 * <p>
 * This class centralizes the instructions and structured data formats used when interacting
 * with the Gemini AI model to grade student recordings for the COBE (Computerized Oral
 * Bagrut Exam) in English. It includes rubrics for grading, specific task prompts for
 * different exam sections, and JSON schemas to ensure consistent AI responses.
 * </p>
 */
public class Prompts {

    // --- SCHEMAS ---

    /**
     * JSON schema for an individual feedback section (e.g., Personal Response, Project, or Video).
     * Defines the structure for scores (Topic Development, Delivery, Vocabulary, Language),
     * total score, and detailed "keep/improve" feedback for each category.
     */
    public static final String COMPONENT_SCHEMA =
            "{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {\n" +
                    "    \"topicDevelopment\": {\"type\": \"integer\", \"description\": \"Score out of 50 based on depth and relevance\"},\n" +
                    "    \"delivery\": {\"type\": \"integer\", \"description\": \"Score out of 15 based on fluency and pace\"},\n" +
                    "    \"vocabulary\": {\"type\": \"integer\", \"description\": \"Score out of 20 based on variety and chunks\"},\n" +
                    "    \"language\": {\"type\": \"integer\", \"description\": \"Score out of 15 based on grammar and English usage\"},\n" +
                    "    \"totalSectionScore\": {\"type\": \"integer\", \"description\": \"Sum of the 4 categories (out of 100)\"},\n" +
                    "    \"feedback\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"properties\": {\n" +
                    "        \"topicDevelopment\": {\"type\": \"object\", \"properties\": {\"keep\": {\"type\": \"string\"}, \"improve\": {\"type\": \"string\"}}},\n" +
                    "        \"delivery\": {\"type\": \"object\", \"properties\": {\"keep\": {\"type\": \"string\"}, \"improve\": {\"type\": \"string\"}}},\n" +
                    "        \"vocabulary\": {\"type\": \"object\", \"properties\": {\"keep\": {\"type\": \"string\"}, \"improve\": {\"type\": \"string\"}}},\n" +
                    "        \"language\": {\"type\": \"object\", \"properties\": {\"keep\": {\"type\": \"string\"}, \"improve\": {\"type\": \"string\"}}},\n" +
                    "        \"overallSummary\": {\"type\": \"string\"}\n" +
                    "      }\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"required\": [\"topicDevelopment\", \"delivery\", \"vocabulary\", \"language\", \"totalSectionScore\", \"feedback\"]\n" +
                    "}";

    /**
     * Main JSON schema for the entire exam grading.
     * Combines multiple {@link #COMPONENT_SCHEMA} objects for each part of the COBE exam
     * and includes a final overall grade.
     */
    public static final String MAIN_EXAM_SCHEMA =
            "{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {\n" +
                    "    \"personalResponse\": " + COMPONENT_SCHEMA + ",\n" +
                    "    \"projectPresentation\": " + COMPONENT_SCHEMA + ",\n" +
                    "    \"videoClipResponse\": " + COMPONENT_SCHEMA + ",\n" +
                    "    \"finalExamGrade\": {\"type\": \"number\"}\n" +
                    "  }\n" +
                    "}";

    // --- MEGA PROMPT INSTRUCTIONS ---

    /**
     * Detailed grading scale and criteria used by the AI to evaluate recordings.
     * Covers weightings and descriptors for:
     * 1. Topic Development (50%)
     * 2. Delivery (15%)
     * 3. Vocabulary (20%)
     * 4. Language (15%)
     */
    private static final String RUBRIC_INSTRUCTIONS =
            "Grading Scale & Criteria:\n" +
                    "1. Topic Development (50%): \n" +
                    "   - 100-76: Relevant, complete understanding, logical, in-depth with detailed examples.\n" +
                    "   - 75-55: Mostly relevant, lacks some detail or examples.\n" +
                    "   - 54-26: Partially relevant, partial understanding, lacks development.\n" +
                    "   - 25-0: Irrelevant, lacks organization, no depth.\n" +
                    "2. Delivery (15%): \n" +
                    "   - 100-76: Comprehensible, clear pace/intonation, almost no hesitations.\n" +
                    "   - 75-55: Mostly comprehensible, some hesitations.\n" +
                    "   - 54-26: Difficult to comprehend, many hesitations.\n" +
                    "   - 25-0: Unintelligible, mostly hesitant.\n" +
                    "3. Vocabulary (20%): \n" +
                    "   - 100-76: Correct and varied use of appropriate words and chunks.\n" +
                    "   - 75-55: Mostly correct and varied.\n" +
                    "   - 54-26: Partial use, some inappropriate repetition.\n" +
                    "   - 25-0: Incorrect and repetitive.\n" +
                    "4. Language (15%): \n" +
                    "   - 100-76: Correct structures, English only (except religious/national holidays).\n" +
                    "   - 75-55: Mostly correct structures.\n" +
                    "   - 54-26: Partial use of structures with many errors.\n" +
                    "   - 25-0: Mostly incorrect, uses languages other than English.\n";

    /**
     * AI prompt for evaluating the 'Personal Response' section of the exam.
     */
    public static final String PERSONAL_PROMPT =
            "Task: Grade the 12th grade student on the 'Personal Response' recording of the COBE exam.\n" +
                    RUBRIC_INSTRUCTIONS + "\n" +
                    "Instructions: Provide a score for each category and a specific summary for Topic Development, Delivery, Vocabulary, and Language.\n" +
                    "Identify what to 'keep' (strengths) and what to 'improve' (weaknesses) for EACH category.\n" +
                    "Return the result as a JSON object matching this schema:\n" + COMPONENT_SCHEMA + "\n" +
                    "This is the question: ";

    /**
     * AI prompt for evaluating the 'Project Presentation' section of the exam.
     * Includes requirements for explaining research processes and personal insights.
     */
    public static final String PROJECT_PROMPT =
            "Task: Grade the 12th grade student on the 'Project Presentation' recording of the COBE exam.\n" +
                    "Ensure the student explains the research process and personal insights.\n" +
                    RUBRIC_INSTRUCTIONS + "\n" +
                    "Instructions: Provide a score for each category and a specific summary for Topic Development, Delivery, Vocabulary, and Language.\n" +
                    "Identify what to 'keep' (strengths) and what to 'improve' (weaknesses) for EACH category.\n" +
                    "Return the result as a JSON object matching this schema:\n" + COMPONENT_SCHEMA + "\n" +
                    "This is the question: ";

    /**
     * AI prompt for evaluating the 'Video Clip Responses' section of the exam.
     * Emphasizes that answers must be derived from the video's spoken content.
     */
    public static final String VIDEO_CLIPS_PROMPT =
            "Task: Grade the 12th grade student on the 'Video Clip Responses' (Part C) recording.\n" +
                    "Crucial: Answers MUST be based on the spoken text from the video clip. Deduct Topic Development points if inaccurate.\n" +
                    RUBRIC_INSTRUCTIONS + "\n" +
                    "Instructions: Provide a score for each category and a specific summary for Topic Development, Delivery, Vocabulary, and Language.\n" +
                    "Identify what to 'keep' (strengths) and what to 'improve' (weaknesses) for EACH category.\n" +
                    "Return the result as a JSON object matching this schema:\n" + COMPONENT_SCHEMA + "\n" +
                    "This is the question: ";

    /**
     * AI prompt for aggregating all recorded sections to calculate the final COBE exam grade.
     * Specifies equal weighting (25%) for each section.
     */
    public static final String ALL_RECORDINGS_PROMPT =
            "Task: Calculate the final COBE exam grade.\n" +
                    "Weights: Personal (25%), Project (25%), Video Clip (25%), and Other (25%).\n" +
                    "Aggregate the data from all sections. Ensure each recording has its own detailed category-by-category feedback.\n" +
                    "Return the result as a JSON object matching this schema:\n" + MAIN_EXAM_SCHEMA;
}
