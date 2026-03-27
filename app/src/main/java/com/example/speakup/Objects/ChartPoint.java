package com.example.speakup.Objects;

import java.util.Date;

/**
 * Represents a single data point on a performance chart.
 * <p>
 * This class stores a specific date and a corresponding score, used to plot
 * progress over time in the application's analytics views.
 * </p>
 */
public class ChartPoint {
    /**
     * The date and time when the score was recorded.
     */
    private Date date;

    /**
     * The score achieved at this specific point in time.
     */
    private float score;

    /**
     * Constructs a new ChartPoint with the specified date and score.
     *
     * @param date  The date of the performance record.
     * @param score The numerical score achieved.
     */
    public ChartPoint(Date date, float score) {
        this.date = date;
        this.score = score;
    }

    /**
     * Gets the date of this chart point.
     *
     * @return The recording date.
     */
    public Date getDate() {
        return date;
    }

    /**
     * Gets the score of this chart point.
     *
     * @return The achieved score.
     */
    public float getScore() {
        return score;
    }
}
