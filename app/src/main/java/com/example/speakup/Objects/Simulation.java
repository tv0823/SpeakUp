package com.example.speakup.Objects;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Represents a full COBE exam simulation session.
 * Stores the owner, completion time, overall score, and the list of recording IDs.
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
     * The overall score of the simulation.
     */
    private int overAllScore;

    /**
     * List of all recording IDs that belong to this simulation.
     */
    private List<String> recordingsIds;

    public Simulation() {}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSimulationId() {
        return simulationId;
    }

    public void setSimulationId(String simulationId) {
        this.simulationId = simulationId;
    }

    public Date getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Date dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public int getOverAllScore() {
        return overAllScore;
    }

    public void setOverAllScore(int overAllScore) {
        this.overAllScore = overAllScore;
    }

    public List<String> getRecordingsIds() {
        return recordingsIds;
    }

    public void setRecordingsIds(List<String> recordingsIds) {
        this.recordingsIds = recordingsIds;
    }
}

