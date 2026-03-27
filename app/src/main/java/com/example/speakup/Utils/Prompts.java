package com.example.speakup.Utils;

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
                    "   - CRITICAL: Check the answer thoroughly for logic and factual accuracy.\n" +
                    "   - 100-76: Relevant, complete understanding, logical, in-depth with detailed examples.\n" +
                    "   - 75-55: Mostly relevant, lacks some detail or examples.\n" +
                    "   - 54-26: Partially relevant, partial understanding, lacks development.\n" +
                    "   - 25-0: Irrelevant, lacks organization, no depth.\n" +
                    "2. Delivery (15%): \n" +
                    "   - NOTE: Only deduct points if hesitations are excessive or hinder comprehension. Allow for natural pauses.\n" +
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
                    "   - 25-0: Mostly incorrect, uses languages other than English.\n" +
                    "\nGeneral Tone: Be soft, encouraging, and constructive. Focus on content quality first.\n";

    // Rules for empty audio
    public static final String EMPTY_AUDIO_RULES = "\nEMPTY AUDIO RULE: If a recording contains no intelligible speech (empty/silence), set:\n" +
            "- topicDevelopment, delivery, vocabulary, language, totalSectionScore = 0\n" +
            "- feedback.overallSummary: \"No speech detected.\"\n" +
            "- feedback.*.keep and feedback.*.improve must mention no speech was detected.\n";

    /**
     * AI prompt for evaluating the 'Personal Response' section of the exam.
     */
    public static final String PERSONAL_PROMPT =
            "Task: Grade the 12th grade student on the 'Personal Response' recording of the COBE exam.\n" +
                    "Verify the answer logic thoroughly before grading.\n" +
                    RUBRIC_INSTRUCTIONS + "\n" + EMPTY_AUDIO_RULES +
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
                    "Ensure the student explains the research process and personal insights. Scrutinize the content for depth.\n" +
                    RUBRIC_INSTRUCTIONS + "\n" + EMPTY_AUDIO_RULES +
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
                    "Be softer on Delivery here as recalling video facts may cause minor pauses.\n" +
                    RUBRIC_INSTRUCTIONS + "\n" + EMPTY_AUDIO_RULES +
                    "Instructions: Provide a score for each category and a specific summary for Topic Development, Delivery, Vocabulary, and Language.\n" +
                    "Identify what to 'keep' (strengths) and what to 'improve' (weaknesses) for EACH category.\n" +
                    "Return the result as a JSON object matching this schema:\n" + COMPONENT_SCHEMA + "\n" +
                    "This is the question: ";


    /**
     * Master prompt for all 4 recordings. Placeholders {RECORDINGS_DETAILS} and {CATEGORY_PROMPTS} will be replaced
     * in SimulationsActivity for each simulation run.
     */
    public static final String SIMULATION_MASTER_PROMPT = "You are grading a COBE simulation.\n\n" +
            "There are exactly 4 audio recordings below, in this exact order:\n" +
            "{RECORDINGS_DETAILS}\n" +
            "Category-specific grading instructions for recordings 1..4:\n" +
            "{CATEGORY_PROMPTS}\n" +
            EMPTY_AUDIO_RULES + "\n" +
            "Return ONLY valid JSON in this exact format:\n" +
            "{ \"recordings\": [ obj1, obj2, obj3, obj4 ] }\n" +
            "Where each obj1..obj4 is the JSON object described in the corresponding category prompt (i.e., it matches COMPONENT_SCHEMA).\n" +
            "Do NOT wrap in markdown and do NOT include any extra text.";
}