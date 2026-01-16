package com.example.speakup.Objects;

/**
 * Represents the details and performance summary for a specific topic.
 * <p>
 * This class stores the score achieved and a textual summary/feedback for a topic
 * practiced by the user. It is used for mapping topic-specific performance data
 * to and from the Firebase Realtime Database.
 * </p>
 */
public class TopicDetail {
    /**
     * The score achieved for the topic.
     */
    private int score;

    /**
     * A textual summary or feedback for the topic.
     */
    private String summary;

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
     * Gets the score achieved for the topic.
     *
     * @return The score.
     */
    public int getScore() { return score; }

    /**
     * Gets the summary or feedback for the topic.
     *
     * @return The summary text.
     */
    public String getSummary() { return summary; }

    /**
     * Default constructor required for calls to DataSnapshot.getValue(TopicDetail.class).
     */
    public TopicDetail() {}
}
