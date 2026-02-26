package com.example.speakup.Objects;

import java.io.Serializable;

/**
 * Represents a practice question in the SpeakUp application.
 * <p>
 * This class holds all the necessary details for a question, including its category,
 * topic, the full question text, a brief summary, and an optional video URL for video-based questions.
 * It is used for storing and retrieving question data from Firebase Realtime Database.
 * </p>
 * <p>
 * This class implements {@link Serializable}, allowing Question objects to be serialized
 * and passed between activities via Intents.
 * </p>
 */
public class Question implements Serializable {
    /**
     * Unique identifier for the question.
     */
    private String questionId;

    /**
     * The category of the question (e.g., "Personal Questions", "Video Clip Questions").
     */
    private String category;

    /**
     * The main topic associated with the question.
     */
    private String topic;

    /**
     * The sub-topic or specific focus area of the question.
     */
    private String subTopic;

    /**
     * The complete text of the question.
     */
    private String fullQuestion;

    /**
     * The URL of the associated video clip, if applicable.
     * Defaults to "null" if not a video clip question.
     */
    private String videoUrl;

    /**
     * Constructs a new Question with the specified details.
     *
     * @param category      The category of the question.
     * @param topic         The main topic.
     * @param subTopic      The sub-topic.
     * @param fullQuestion  The full text of the question.
     * @param videoUrl      The URL of the video (used only if category is "Video Clip Questions").
     */
    public Question(String category, String topic, String subTopic, String fullQuestion, String videoUrl) {
        this.category = category;
        this.topic = topic;
        this.subTopic = subTopic;
        this.fullQuestion = fullQuestion;
        if (this.category.equals("Video Clip Questions")) {
            this.videoUrl = videoUrl;
        } else {
            this.videoUrl = "null";
        }
    }

    /**
     * Gets the unique identifier of the question.
     *
     * @return The question ID.
     */
    public String getQuestionId() {
        return questionId;
    }

    /**
     * Sets the unique identifier of the question.
     *
     * @param questionId The new question ID.
     */
    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    /**
     * Gets the category of the question.
     *
     * @return The category name.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Gets the main topic of the question.
     *
     * @return The topic name.
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Gets the sub-topic of the question.
     *
     * @return The sub-topic name.
     */
    public String getSubTopic() {
        return subTopic;
    }

    /**
     * Gets the full text of the question.
     *
     * @return The full question text.
     */
    public String getFullQuestion() {
        return fullQuestion;
    }

    /**
     * Gets the video URL associated with the question.
     *
     * @return The video URL, or "null" if not applicable.
     */
    public String getVideoUrl() {
        return videoUrl;
    }

    /**
     * Default constructor required for calls to DataSnapshot.getValue(Question.class).
     */
    public Question() {}
}
