package com.example.speakup.Activities;

import static com.example.speakup.FBRef.refAuth;
import static com.example.speakup.FBRef.refUserProfiles;
import static com.example.speakup.FBRef.refUsers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.speakup.Objects.User;
import com.example.speakup.R;
import com.example.speakup.Utilities;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

/**
 * Activity for registering a new user.
 * <p>
 * This activity allows new users to create an account by providing a username, email, and password.
 * Users can also upload a profile picture using the camera or gallery.
 * The activity handles user creation in Firebase Authentication, storing user details in the Realtime Database,
 * and uploading the profile picture to Firebase Storage.
 * </p>
 */
public class SignUpActivity extends Utilities {
    /**
     * Input field for the username.
     */
    private EditText eTUsername;

    /**
     * Input field for the email address.
     */
    private EditText eTEmail;

    /**
     * Input field for the password.
     */
    private EditText eTPass;

    /**
     * Request code for camera permission.
     */
    private static final int REQUEST_CAMERA_PERMISSION = 6709;

    /**
     * Request code for image chooser (camera or gallery).
     */
    private static final int REQUEST_IMAGE_CHOOSER = 9051;

    /**
     * URI of the image captured by the camera.
     */
    private Uri imageUri;

    /**
     * URI of the image to be uploaded (either from camera or gallery).
     */
    private Uri imageUriToUpload;

    /**
     * Name of the file to be uploaded.
     */
    private String fileName;

    /**
     * Called when the activity is starting.
     * Initializes the UI components.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        eTUsername = findViewById(R.id.eTUsername);
        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
    }

    /**
     * Navigates to the LogInActivity for existing users.
     *
     * @param view The view that was clicked.
     */
    public void goToLogInActivity(View view) {
        Intent intent = new Intent(this, LogInActivity.class);
        startActivity(intent);
    }

    /**
     * Finishes the current activity and returns to the previous screen.
     *
     * @param view The view that was clicked.
     */
    public void goBack(View view) {
        finish();
    }

    /**
     * Called when the activity will start interacting with the user.
     * Checks for camera permissions and requests them if not granted.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    /**
     * Callback for the result from requesting permissions.
     * Handles the response to the camera permission request.
     *
     * @param requestCode The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Initiates the process to upload a profile picture.
     * Opens a chooser dialog to allow the user to select an image from the gallery or take a new photo with the camera.
     *
     * @param view The view that was clicked.
     */
    public void uploadPfp(View view) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;

        try {
            String filename = "img_" + System.currentTimeMillis();
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            photoFile = File.createTempFile(filename,".jpg", storageDir);

            imageUri = FileProvider.getUriForFile(
                    this,
                    "com.example.speakup.fileprovider",
                    photoFile
            );
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        } catch (IOException e) {
            Toast.makeText(this,"Failed to create temporary file for camera",Toast.LENGTH_LONG).show();
            // If file creation fails, set imageUri to null so the camera intent is not added to the intent chooser.
            imageUri = null;
        }

        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Image Source");

        // Only add the camera intent if the photoFile and URI were successfully prepared
        if (imageUri != null) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {cameraIntent});
        }

        startActivityForResult(chooserIntent, REQUEST_IMAGE_CHOOSER);
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * Handles the result from the image chooser (camera or gallery selection).
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult(), allowing you to identify who this result came from.
     * @param resultCode The integer result code returned by the child activity through its setResult().
     * @param data_back An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,@Nullable Intent data_back) {
        super.onActivityResult(requestCode, resultCode, data_back);
        if (requestCode == REQUEST_IMAGE_CHOOSER && resultCode == RESULT_OK) {
            imageUriToUpload = null;
            if (data_back != null && data_back.getData() != null) {
                // Gallery selection was chosen. data_back will contain the Uri.
                imageUriToUpload = data_back.getData();
            } else if (imageUri != null) {
                // Camera was chosen. data_back is null, so we use the pre-saved Uri.
                imageUriToUpload = imageUri;
            }

            if (imageUriToUpload == null) {
                Toast.makeText(this, "Failed to get image URI from chooser", Toast.LENGTH_LONG).show();
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this,"Image selection/capture canceled",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Uploads the selected profile picture to Firebase Storage.
     *
     * @param imageUri The URI of the image to upload.
     */
    private void uploadImage(Uri imageUri) {
        if (imageUri != null) {
            fileName = "profile.jpg";
            StorageReference refFile = refUserProfiles.child(refAuth.getUid().toString() + "/" + fileName);

            refFile.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(SignUpActivity.this, "Upload successful", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(SignUpActivity.this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        }
                    });
        } else {
            Toast.makeText(this, "No image URI provided for upload", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Attempts to create a new user account with the provided details.
     * <p>
     * Validates input fields and ensures a profile picture is selected.
     * Uses Firebase Authentication to create the user.
     * On success, creates a user profile in the Realtime Database and uploads the profile picture.
     * On failure, displays specific error messages based on the exception.
     * </p>
     *
     * @param view The view that was clicked.
     */
    public void createUser(View view) {
        String email = eTEmail.getText().toString();
        String pass = eTPass.getText().toString();
        String username = eTUsername.getText().toString();

        if (email.isEmpty() || pass.isEmpty() || username.isEmpty() || imageUriToUpload == null) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
        } else {
            ProgressDialog pd = new ProgressDialog(this);
            pd.setTitle("Connecting");
            pd.setMessage("Creating user...");
            pd.show();

            refAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            pd.dismiss();

                            if (task.isSuccessful()) {
                                Log.i("SignUpActivity", "createUserWithEmailAndPassword:success");
                                FirebaseUser user = refAuth.getCurrentUser();

                                //create the user and add it to the database
                                User newUser = new User(user.getUid(), eTUsername.getText().toString());
                                refUsers.child(user.getUid()).setValue(newUser);

                                //upload the user's profile picture to storage
                                uploadImage(imageUriToUpload);

                                refAuth.signOut();

                                Toast.makeText(SignUpActivity.this, "User created successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SignUpActivity.this, LogInActivity.class);
                                startActivity(intent);
                            } else {
                                Exception exp = task.getException();

                                if (exp instanceof FirebaseAuthInvalidUserException) {
                                    Toast.makeText(SignUpActivity.this, "Invalid email address.", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseAuthWeakPasswordException) {
                                    Toast.makeText(SignUpActivity.this, "Password too weak.", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseAuthUserCollisionException) {
                                    Toast.makeText(SignUpActivity.this, "User already exists.", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(SignUpActivity.this, "General authentication failure.", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseNetworkException) {
                                    Toast.makeText(SignUpActivity.this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(SignUpActivity.this, "An error occurred. Please try again later.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
        }
    }

}
