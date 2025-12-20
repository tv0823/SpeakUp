package com.example.speakup.Objects;

/**
 * Represents a user in the SpeakUp application.
 * <p>
 * This class stores basic user information such as the unique user ID and the username.
 * It is used for mapping user data to and from the Firebase Realtime Database.
 * </p>
 */
public class User {
    /**
     * The unique identifier for the user.
     */
    private String userId;

    /**
     * The display name of the user.
     */
    private String username;

    /**
     * Constructs a new User with the specified ID and username.
     *
     * @param userId   The unique identifier for the user.
     * @param username The display name of the user.
     */
    public User(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    /**
     * Gets the unique identifier of the user.
     *
     * @return The user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the display name of the user.
     *
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the display name of the user.
     *
     * @param username The new username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Default constructor required for calls to DataSnapshot.getValue(User.class).
     */
    public User() {}

}
