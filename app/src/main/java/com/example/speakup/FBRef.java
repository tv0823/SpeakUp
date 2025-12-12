package com.example.speakup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FBRef {
    public static FirebaseAuth refAuth = FirebaseAuth.getInstance();

    public static FirebaseDatabase FBDB = FirebaseDatabase.getInstance();
    public static DatabaseReference refUsers = FBDB.getReference("Users");

    public static FirebaseStorage storage = FirebaseStorage.getInstance();
    public static StorageReference refST = storage.getReference();
    public static StorageReference refUserProfiles = refST.child("User_Profiles/");
}
