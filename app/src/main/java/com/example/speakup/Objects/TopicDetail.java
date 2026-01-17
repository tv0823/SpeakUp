package com.example.speakup.Objects;

import java.io.Serializable;

/**
 * Represents the details and performance summary for a specific topic or feedback category.
 * <p>
 * This class stores the score achieved and a textual summary/feedback for a topic
 * practiced by the user. It is used for mapping performance data to and from the
 * Firebase Realtime Database and for passing between components via {@link Serializable}.
 * </p>
 */
public class TopicDetail implements Serializable {

    /**
     * The score achieved for the topic/category.
     */
    private int score;

    /**
     * A textual summary or feedback for the topic/category.
     */
    private String summary;

    /**
     * Default constructor required for calls to DataSnapshot.getValue(TopicDetail.class).
     */
    public TopicDetail() {}

    /**
     * Constructs a new TopicDetail with the specified score and summary.
     *
     * @param score   The score achieved for the topic.
     * @param summary A textual summary or feedback.
     */
    public TopicDetail(int score, String summary) {
        this.score = score;
        this.summary = summary;
    }

    /**
     * Gets the score achieved for the topic/category.
     *
     * @return The score.
     */
    public int getScore() {
        return score;
    }

    /**
     * Sets the score achieved for the topic/category.
     *
     * @param score The score to set.
     */
    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Gets the summary or feedback for the topic/category.
     *
     * @return The summary text.
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Sets the summary or feedback for the topic/category.
     *
     * @param summary The summary text to set.
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }
}
