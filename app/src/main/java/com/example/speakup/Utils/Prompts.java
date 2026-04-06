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
            "Grading Scale & Criteria (Israeli MOE COBE Standard):\n" +
                    "1. Topic Development (50%):\n" +
                    "   - COMPLETENESS RULE: Check if the student answered ALL parts of the question. If a question has two parts and the student only answers one, limit the score to a maximum of 25/50.\n" +
                    "   - 100-76 (40-50 pts): Relevant, complete understanding, logical, in-depth with detailed examples.\n" +
                    "   - 75-55 (28-39 pts): Mostly relevant, lacks some detail or misses a minor sub-question.\n" +
                    "   - 54-26 (13-27 pts): Partially relevant, partial understanding, fails to answer significant parts of the question.\n" +
                    "   - 25-0 (0-12 pts): Irrelevant, lacks organization, or fails to answer the main question.\n" +
                    "2. Delivery (15%):\n" +
                    "   - 100-76 (12-15 pts): Fluent, clear pace/intonation, almost no hesitations.\n" +
                    "   - 75-55 (9-11 pts): Mostly comprehensible, some hesitations but does not stop flow.\n" +
                    "   - 54-26 (4-8 pts): Difficult to comprehend, many long pauses and hesitations.\n" +
                    "   - 25-0 (0-3 pts): Unintelligible or mostly hesitant.\n" +
                    "3. Vocabulary (20%):\n" +
                    "   - 100-76 (16-20 pts): Correct and varied use of appropriate words and COBE lexical chunks.\n" +
                    "   - 75-55 (11-15 pts): Mostly correct and varied.\n" +
                    "   - 54-26 (5-10 pts): Partial use, high repetition of simple words.\n" +
                    "   - 25-0 (0-4 pts): Incorrect and extremely repetitive.\n" +
                    "4. Language (15%):\n" +
                    "   - 100-76 (12-15 pts): Correct grammar structures (tenses, agreement). English only.\n" +
                    "   - 75-55 (9-11 pts): Mostly correct structures.\n" +
                    "   - 54-26 (4-8 pts): Many grammar errors that sometimes hinder understanding.\n" +
                    "   - 25-0 (0-3 pts): Mostly incorrect language usage.\n" +
                    "\nGeneral Tone: Be encouraging and constructive. Focus on whether the student met the task requirements.\n";

    // Rules for empty audio
    public static final String EMPTY_AUDIO_RULES = "\nEMPTY AUDIO RULE: If a recording contains no intelligible speech (empty/silence), set:\n" +
            "- topicDevelopment, delivery, vocabulary, language, totalSectionScore = 0\n" +
            "- feedback.overallSummary: \"No speech detected.\"\n" +
            "- feedback.*.keep and feedback.*.improve must mention no speech was detected.\n";

    /**
     * AI prompt for evaluating the 'Personal Response' section of the exam.
     */
    public static final String PERSONAL_PROMPT =
            "Task: Grade the student on the 'Personal Interview' (COBE Part A).\n" +
                    "Focus: The student must answer precisely the question asked. For a full score, they should ideally provide at least 2-3 detailed sentences.\n" +
                    RUBRIC_INSTRUCTIONS + "\n" + EMPTY_AUDIO_RULES +
                    "Instructions: Grade how well they answered the input question details.\n" +
                    "Identify what to 'keep' (strengths) and what to 'improve' (weaknesses) for EACH of the 4 categories.\n" +
                    "Return the result as a JSON object matching this schema:\n" + COMPONENT_SCHEMA + "\n" +
                    "Question: ";

    /**
     * AI prompt for evaluating the 'Project Presentation' section of the exam.
     * Includes requirements for explaining research processes and personal insights.
     */
    public static final String PROJECT_PROMPT =
            "Task: Grade the student on the 'Project Presentation' (COBE Part B).\n" +
                    "Requirement: In COBE Part B, students describe their personal research project. Check if they mentioned:\n" +
                    "1. The topic they chose.\n" +
                    "2. What they learned or their final conclusions.\n" +
                    "3. How they felt or what they gained from the work.\n" +
                    RUBRIC_INSTRUCTIONS + "\n" + EMPTY_AUDIO_RULES +
                    "Instructions: Penalize 'Topic Development' heavily if they only state the title without explaining content.\n" +
                    "Identify what to 'keep' (strengths) and what to 'improve' (weaknesses) for EACH of the 4 categories.\n" +
                    "Return the result as a JSON object matching this schema:\n" + COMPONENT_SCHEMA + "\n" +
                    "Question: ";

    /**
     * AI prompt for evaluating the 'Video Clip Responses' section of the exam.
     * Emphasizes that answers must be derived from the video's spoken content.
     */
    public static final String VIDEO_CLIPS_PROMPT =
            "Task: Grade the student on 'Video Clip Response' (COBE Part C).\n" +
                    "Crucial: Answers MUST be based on the information provided in the video clip. If the answer is factually wrong compared to the video, deduct points from 'Topic Development'.\n" +
                    RUBRIC_INSTRUCTIONS + "\n" + EMPTY_AUDIO_RULES +
                    "Instructions: Verify the student's answer against the context of the question and video details.\n" +
                    "Identify what to 'keep' (strengths) and what to 'improve' (weaknesses) for EACH of the 4 categories.\n" +
                    "Return the result as a JSON object matching this schema:\n" + COMPONENT_SCHEMA + "\n" +
                    "Question: ";


    /**
     * Master prompt for all 4 recordings. Placeholders {RECORDINGS_DETAILS} and {CATEGORY_PROMPTS} will be replaced
     * in SimulationsActivity for each simulation run.
     */
    public static final String SIMULATION_MASTER_PROMPT = "You are grading a full COBE simulation.\n\n" +
            "Evaluate these 4 recordings based on the Israeli Bagrut English exam criteria.\n\n" +
            "Audio Recordings (1 to 4):\n" +
            "{RECORDINGS_DETAILS}\n" +
            "Grading Tasks for each recording:\n" +
            "{CATEGORY_TASKS}\n" +
            EMPTY_AUDIO_RULES + "\n" +
            "Return valid JSON ONLY:\n" +
            "{ \"recordings\": [ obj1, obj2, obj3, obj4 ] }\n" +
            "Each object (obj1..obj4) must comply with the COMPONENT_SCHEMA specified in the tasks.";
}