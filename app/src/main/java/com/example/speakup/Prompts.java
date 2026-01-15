package com.example.speakup;

public class Prompts {
// --- SCHEMAS ---

    // Schema for individual sections (Personal, Project, Video)
    public static final String COMPONENT_SCHEMA =
            "{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {\n" +
                    "    \"topicDevelopment\": {\"type\": \"integer\", \"description\": \"Score out of 50 based on depth and relevance\"},\n" +
                    "    \"delivery\": {\"type\": \"integer\", \"description\": \"Score out of 15 based on fluency and pace\"},\n" +
                    "    \"vocabulary\": {\"type\": \"integer\", \"description\": \"Score out of 20 based on variety and chunks\"},\n" +
                    "    \"language\": {\"type\": \"integer\", \"description\": \"Score out of 15 based on grammar and English usage\"},\n" +
                    "    \"totalSectionScore\": {\"type\": \"integer\"},\n" +
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

    // Main schema for the entire exam
    public static final String MAIN_EXAM_SCHEMA =
            "{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {\n" +
                    "    \"personalResponse\": " + COMPONENT_SCHEMA + ",\n" +
                    "    \"projectPresentation\": " + COMPONENT_SCHEMA + ",\n" +
                    "    \"videoClipResponse\": " + COMPONENT_SCHEMA + ",\n" +
                    "    \"finalExamGrade\": {\"type\": \"number\"},\n" +
                    "  }\n" +
                    "}";

    // --- PROMPTS ---

    private static final String RUBRIC_INSTRUCTIONS =
            "Use these criteria for grading (0-100 scale converted to 25 points per section):\n" +
                    "1. Topic Development: 100-76 (Relevant, complete understanding, logical, in-depth), 75-55 (Mostly relevant, lacking detail), 54-26 (Partial relevance/understanding), 25-0 (Irrelevant, no depth).\n" +
                    "2. Delivery: 100-76 (Comprehensible, easy pace, no hesitations), 75-55 (Some hesitations), 54-26 (Difficult to comprehend, many hesitations), 25-0 (Unintelligible).\n" +
                    "3. Vocabulary: 100-76 (Varied use of appropriate words/chunks), 75-55 (Mostly correct/varied), 54-26 (Repetitive/inappropriate), 25-0 (Incorrect).\n" +
                    "4. Language: 100-76 (Correct structures, English only), 75-55 (Mostly correct), 54-26 (Many errors), 25-0 (Incorrect/non-English used).\n" +
                    "Each of the 4 categories is worth 25% of the recording score.\n";

    public static final String PERSONAL_PROMPT =
            "Task: Grade the 12th grade student on the 'Personal Response' section of the COBE exam.\n" +
                    RUBRIC_INSTRUCTIONS + "\n" +
                    "Provide a specific summary for each category (Language, Vocabulary, Delivery, Topic Development) with what to keep and what to improve.\n" +
                    "Return the result as a JSON object matching this schema:\n" + COMPONENT_SCHEMA + "\n" +
                    "This is the question: ";

    public static final String PROJECT_PROMPT =
            "Task: Grade the 12th grade student on the 'Project Presentation' section of the COBE exam.\n" +
                    "Evaluate their ability to explain their research and personal reflection.\n" +
                    RUBRIC_INSTRUCTIONS + "\n" +
                    "Provide a specific summary for each category (Language, Vocabulary, Delivery, Topic Development) with what to keep and what to improve.\n" +
                    "Return the result as a JSON object matching this schema:\n" + COMPONENT_SCHEMA + "\n" +
                    "This is the question: ";

    public static final String VIDEO_CLIPS_PROMPT =
            "Task: Grade the student on the 'Video Clip Responses' (Part C) of the COBE exam.\n" +
                    "Note: Answers must be based accurately on the spoken text in the video.\n" +
                    RUBRIC_INSTRUCTIONS + "\n" +
                    "Provide a specific summary for each category (Language, Vocabulary, Delivery, Topic Development) with what to keep and what to improve.\n" +
                    "Return the result as a JSON object matching this schema:\n" + COMPONENT_SCHEMA + "\n" +
                    "This is the question: ";

    public static final String ALL_RECORDINGS_PROMPT =
            "Task: Aggregate the final COBE grade for the student.\n" +
                    "Calculation: Personal (25%) + Project (25%) + Video Clips (25% for both answers combined) + Other Recordings (25%).\n" +
                    "Ensure each sub-recording has its own detailed feedback for Language, Vocabulary, Delivery, and Topic Development.\n" +
                    "Provide a final summary of the student's overall performance.\n" +
                    "Return the result as a JSON object matching this schema:\n" + MAIN_EXAM_SCHEMA;
}
