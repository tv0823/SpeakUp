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

    /**
     * A sample JSON response used as a few-shot example for the AI.
     * This ensures the AI uses the correct flat structure for scores (integers).
     */
    public static final String JSON_EXAMPLE =
            "{\n" +
            "  \"topicDevelopment\": 45,\n" +
            "  \"delivery\": 12,\n" +
            "  \"vocabulary\": 18,\n" +
            "  \"language\": 14,\n" +
            "  \"totalSectionScore\": 89,\n" +
            "  \"feedback\": {\n" +
            "    \"topicDevelopment\": {\"keep\": \"Great detail\", \"improve\": \"Add more examples\"},\n" +
            "    \"delivery\": {\"keep\": \"Clear pace\", \"improve\": \"Slightly faster\"},\n" +
            "    \"vocabulary\": {\"keep\": \"Varied words\", \"improve\": \"Use more chunks\"},\n" +
            "    \"language\": {\"keep\": \"Good grammar\", \"improve\": \"Watch tenses\"},\n" +
            "    \"overallSummary\": \"Solid response overall.\"\n" +
            "  }\n" +
            "}";

    // --- MEGA PROMPT INSTRUCTIONS ---

    /**
     * Core instructions for the AI model to define its persona and high-level behavioral constraints.
     * Integrated into individual prompts for simple GenerativeModel usage.
     */
    private static final String BASE_SYSTEM_INSTRUCTION =
            "Persona: You are an expert English examiner for the Israeli MOE COBE (Computerized Oral Bagrut Exam).\n" +
            "Task: You will be provided with a student recording answering a specific question. Your job is to grade the recording based on the provided rubric.\n" +
            "Constraints:\n" +
            "1. NEVER answer the question yourself. Even if asked 'What resources did you use?', you must evaluate if the student described their resources.\n" +
            "2. Stay strictly in the role of a grader. Do not provide conversational advice or audio analysis outside of the JSON evaluation schema.\n" +
            "3. If the audio is empty or unintelligible, follow the EMPTY_AUDIO_RULE provided in the rubric.\n" +
            "4. Return ONLY valid JSON. The category scores (topicDevelopment, delivery, vocabulary, language) MUST be flat INTEGERS, not objects.\n" +
            "5. Do not use Markdown backticks (```json).";

    /**
     * Detailed grading scale and criteria used by the AI to evaluate recordings.
     */
    private static final String RUBRIC_INSTRUCTIONS =
            "Grading Scale & Criteria (Israeli MOE COBE Standard):\n" +
            "1. Topic Development (50%):\n" +
            "   - COMPLETENESS RULE: Check if the student answered ALL parts of the question. If a question has two parts (e.g., 'What is it?' AND 'Why do you like it?') and the student only answers ONE, limit the Topic Development score to a MAXIMUM of 25/50.\n" +
            "   - 100-76 (40-50 pts): Relevant, complete understanding, logical, in-depth with detailed examples.\n" +
            "   - 75-55 (28-39 pts): Mostly relevant, lacks some detail or misses a minor sub-question.\n" +
            "   - 54-26 (13-27 pts): Partially relevant, partial understanding, fails to answer significant parts of the question.\n" +
            "   - 25-0 (0-12 pts): Irrelevant, lacks organization, or fails to answer the main question.\n" +
            "2. Delivery (15%):\n" +
            "   - NOTE: Only deduct points if hesitations are excessive or hinder comprehension. Allow for natural pauses.\n" +
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
            "<system_instruction>\n" +
            BASE_SYSTEM_INSTRUCTION + "\n" +
            "</system_instruction>\n" +
            "<rubric>\n" +
            RUBRIC_INSTRUCTIONS + "\n" + EMPTY_AUDIO_RULES +
            "</rubric>\n" +
            "<task_context>\n" +
            "Exam Section: Personal Interview (COBE Part A)\n" +
            "Target: Student must answer precisely. Ideal length: 2-3 detailed sentences.\n" +
            "</task_context>\n" +
            "<input>\n" +
            "<question>{QUESTION_TEXT}</question>\n" +
            "<student_audio>The attached audio file contains the student's response.</student_audio>\n" +
            "</input>\n" +
            "<output_format>\n" +
            "Return the result as a JSON object matching the COMPONENT_SCHEMA.\n" +
            "</output_format>\n" +
            "<final_instruction>\n" +
            "Verify how many parts the question has. If the student only answered some parts, strictly apply the COMPLETENESS RULE. Return ONLY the JSON object.\n" +
            "</final_instruction>";

    /**
     * AI prompt for evaluating the 'Project Presentation' section of the exam.
     */
    public static final String PROJECT_PROMPT =
            "<system_instruction>\n" +
            BASE_SYSTEM_INSTRUCTION + "\n" +
            "</system_instruction>\n" +
            "<rubric>\n" +
            RUBRIC_INSTRUCTIONS + "\n" + EMPTY_AUDIO_RULES +
            "</rubric>\n" +
            "<task_context>\n" +
            "Exam Section: Project Presentation (COBE Part B)\n" +
            "Requirement: Students describe their personal research project. Grade based on topic, learning, and personal gain.\n" +
            "Scrutinize the content for depth. Penalize heavily if they only state the title without explaining content.\n" +
            "</task_context>\n" +
            "<input>\n" +
            "<question>{QUESTION_TEXT}</question>\n" +
            "<student_audio>The attached audio file contains the student's response.</student_audio>\n" +
            "</input>\n" +
            "<output_format>\n" +
            "Return the result as a JSON object matching the COMPONENT_SCHEMA.\n" +
            "</output_format>\n" +
            "<final_instruction>\n" +
            "Verify answer depth and completeness. Return ONLY the JSON object.\n" +
            "</final_instruction>";

    /**
     * AI prompt for evaluating the 'Video Clip Responses' section of the exam.
     */
    public static final String VIDEO_CLIPS_PROMPT =
            "<system_instruction>\n" +
            BASE_SYSTEM_INSTRUCTION + "\n" +
            "</system_instruction>\n" +
            "<rubric>\n" +
            RUBRIC_INSTRUCTIONS + "\n" + EMPTY_AUDIO_RULES +
            "</rubric>\n" +
            "<task_context>\n" +
            "Exam Section: Video Clip Response (COBE Part C)\n" +
            "Crucial: Answers MUST be based on the information provided in the video clip. If the answer is factually wrong compared to video facts, deduct Topic Development points.\n" +
            "</task_context>\n" +
            "<input>\n" +
            "<question>{QUESTION_TEXT}</question>\n" +
            "<student_audio>The attached audio file contains the student's response.</student_audio>\n" +
            "</input>\n" +
            "<output_format>\n" +
            "Return the result as a JSON object matching the COMPONENT_SCHEMA.\n" +
            "</output_format>\n" +
            "<final_instruction>\n" +
            "Grade the response based on video accuracy and completeness. Return ONLY the JSON object.\n" +
            "</final_instruction>";


    /**
     * Master prompt for all 4 recordings. Placeholders {RECORDINGS_DETAILS} and {CATEGORY_TASKS} will be replaced
     * in SimulationsActivity for each simulation run.
     */
    public static final String SIMULATION_MASTER_PROMPT =
            "<system_instruction>\n" +
            BASE_SYSTEM_INSTRUCTION + "\n" +
            "</system_instruction>\n\n" +
            "<recordings_details>\n" +
            "{RECORDINGS_DETAILS}\n" +
            "</recordings_details>\n\n" +
            "<grading_tasks>\n" +
            "{CATEGORY_TASKS}\n" +
            "</grading_tasks>\n\n" +
            "<output_requirement>\n" +
            "Return ONLY valid JSON in this exact format:\n" +
            "{ \"recordings\": [ " + JSON_EXAMPLE + ", ... ] }\n" +
            "Each object must match the COMPONENT_SCHEMA structure and the example above.\n" +
            "DO NOT use Markdown backticks.\n" +
            "</output_requirement>";
}