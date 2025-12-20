package com.example.speakup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * Helper class holding static references to Firebase services and database paths.
 * <p>
 * This class provides a centralized location for accessing Firebase Authentication,
 * Realtime Database references, and Cloud Storage references used throughout the application.
 * </p>
 */
public class FBRef {
    /**
     * The entry point of the Firebase Authentication SDK.
     */
    public static FirebaseAuth refAuth = FirebaseAuth.getInstance();

    /**
     * The entry point for accessing the Firebase Realtime Database.
     */
    public static FirebaseDatabase FBDB = FirebaseDatabase.getInstance();

    /**
     * Reference to the 'Users' node in the Realtime Database.
     * Contains user account information.
     */
    public static DatabaseReference refUsers = FBDB.getReference("Users");

    /**
     * Reference to the 'Questions' node in the Realtime Database.
     * Contains the hierarchy of practice questions, categories, and topics.
     */
    public static DatabaseReference refQuestions = FBDB.getReference("Questions");

    /**
     * The entry point of the Firebase Storage SDK.
     */
    public static FirebaseStorage storage = FirebaseStorage.getInstance();

    /**
     * The root reference for Firebase Storage.
     */
    public static StorageReference refST = storage.getReference();

    /**
     * Storage reference for user profile pictures.
     * Points to the 'User_Profiles/' directory.
     */
    public static StorageReference refUserProfiles = refST.child("User_Profiles/");

    /**
     * Storage reference for media files associated with questions.
     * Points to the 'Question_Media/' directory.
     */
    public static StorageReference refQuestionMedia = refST.child("Question_Media/");
}
