package com.example.speakup.Objects;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Represents a full COBE exam simulation session.
 * Stores the owner, completion time, overall score, and the list of recording IDs.
 * <p>
 * This class is used to persist and retrieve simulation data from Firebase Realtime Database.
 * It implements {@link Serializable} to allow passing between activities and fragments.
 * </p>
 */
public class Simulation implements Serializable {
    /**
     * The unique identifier of the user who completed the simulation.
     */
    private String userId;

    /**
     * The unique identifier of this simulation.
     */
    private String simulationId;

    /**
     * The timestamp when the simulation was completed.
     */
    private Date dateCompleted;

    /**
     * The overall score of the simulation (0-100).
     */
    private int overAllScore;

    /**
     * List of all recording IDs that belong to this simulation.
     */
    private List<String> recordingsIds;

    /**
     * Default constructor required for calls to DataSnapshot.getValue(Simulation.class).
     */
    public Simulation() {}

    /**
     * Gets the unique identifier of the user who completed the simulation.
     *
     * @return The user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the unique identifier of the user who completed the simulation.
     *
     * @param userId The user ID to set.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the unique identifier of this simulation.
     *
     * @return The simulation ID.
     */
    public String getSimulationId() {
        return simulationId;
    }

    /**
     * Sets the unique identifier of this simulation.
     *
     * @param simulationId The simulation ID to set.
     */
    public void setSimulationId(String simulationId) {
        this.simulationId = simulationId;
    }

    /**
     * Gets the timestamp when the simulation was completed.
     *
     * @return The completion date.
     */
    public Date getDateCompleted() {
        return dateCompleted;
    }

    /**
     * Sets the timestamp when the simulation was completed.
     *
     * @param dateCompleted The completion date to set.
     */
    public void setDateCompleted(Date dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    /**
     * Gets the overall score of the simulation.
     *
     * @return The overall score (0-100).
     */
    public int getOverAllScore() {
        return overAllScore;
    }

    /**
     * Sets the overall score of the simulation.
     *
     * @param overAllScore The overall score to set (0-100).
     */
    public void setOverAllScore(int overAllScore) {
        this.overAllScore = overAllScore;
    }

    /**
     * Gets the list of all recording IDs that belong to this simulation.
     *
     * @return A list of recording ID strings.
     */
    public List<String> getRecordingsIds() {
        return recordingsIds;
    }

    /**
     * Sets the list of all recording IDs that belong to this simulation.
     *
     * @param recordingsIds The list of recording IDs to set.
     */
    public void setRecordingsIds(List<String> recordingsIds) {
        this.recordingsIds = recordingsIds;
    }
}
