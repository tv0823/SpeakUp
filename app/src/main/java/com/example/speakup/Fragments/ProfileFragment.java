package com.example.speakup.Fragments;

import static android.content.Context.MODE_PRIVATE;
import static com.example.speakup.Utils.FBRef.refAuth;
import static com.example.speakup.Utils.FBRef.refRecordings;
import static com.example.speakup.Utils.FBRef.refST;
import static com.example.speakup.Utils.FBRef.refUsers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.speakup.Activities.HelpAndAboutActivity;
import com.example.speakup.Activities.RemindersActivity;
import com.example.speakup.Activities.WelcomeScreenActivity;
import com.example.speakup.Objects.Recording;
import com.example.speakup.Objects.User;
import com.example.speakup.R;
import com.example.speakup.Utils.Utilities;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * Fragment that displays and manages the user's profile information.
 * <p>
 * This fragment provides several functionalities:
 * <ul>
 *     <li>Displays user details such as username and joining date.</li>
 *     <li>Shows performance statistics, including the total number of recordings and average score.</li>
 *     <li>Allows users to view and update their profile picture using the camera or gallery.</li>
 *     <li>Provides navigation to Help & About and Reminders screens.</li>
 *     <li>Handles user logout and session cleanup.</li>
 * </ul>
 * </p>
 */
public class ProfileFragment extends Fragment {
    /**
     * Temporary ImageView used for profile picture preview in dialogs.
     */
    private ImageView iV;

    /**
     * The unique identifier of the currently logged-in user.
     */
    private String uid;

    /**
     * The absolute path to the current image file being processed for profile update.
     */
    private String currentPath;

    /**
     * Request code for camera permission.
     */
    private static final int REQUEST_CAMERA_PERMISSION = 6709;

    /**
     * Request code for image chooser (camera or gallery).
     */
    private static final int REQUEST_IMAGE_CHOOSER = 9051;

    /**
     * Default constructor for fragment instantiation.
     */
    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Inflates the fragment layout and initializes UI components and click listeners.
     *
     * @param inflater           The LayoutInflater object.
     * @param container          The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView userNameTv = view.findViewById(R.id.userNameTv);
        TextView dateTv = view.findViewById(R.id.dateTv);
        TextView avgScoreTv = view.findViewById(R.id.avgScoreTv);
        TextView recordingCountTv = view.findViewById(R.id.recordingCountTv);
        MaterialButton btnHelpAndAbout = view.findViewById(R.id.btnHelp);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);
        MaterialButton btnEditProfile = view.findViewById(R.id.btnEditProfile);
        MaterialButton btnReminders = view.findViewById(R.id.btnReminders);
        ShapeableImageView profileImage = view.findViewById(R.id.profileImage);

        uid = refAuth.getCurrentUser().getUid();

        setUserData(userNameTv, dateTv, profileImage);
        setRecordingCountAndAvgScore(recordingCountTv, avgScoreTv);

        btnHelpAndAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireActivity(), HelpAndAboutActivity.class);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logOut(v);
            }
        });

        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeProfilePicture(profileImage);
            }
        });

        btnReminders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireActivity(), RemindersActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    /**
     * Configures behavior after the view has been created, specifically the back button logic.
     *
     * @param view               The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // if the user press back button, go to home
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                nav.setSelectedItemId(R.id.nav_home);
            }
        });
    }

    /**
     * Fetches and displays basic user information like username and join date.
     *
     * @param userNameTv   The TextView to display the username.
     * @param dateTv       The TextView to display the join date.
     * @param profileImage The ImageView to display the profile picture.
     */
    private void setUserData(TextView userNameTv, TextView dateTv, ShapeableImageView profileImage) {
        ProgressDialog pD = new ProgressDialog(requireActivity());
        pD.setMessage("Loading user data...");
        pD.setCancelable(false);
        pD.show();

        setProfilePicture(profileImage);

        refUsers.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dS) {
                User user = dS.getValue(User.class);
                if (user != null) {
                    userNameTv.setText(user.getUsername());

                    FirebaseUserMetadata metadata = refAuth.getCurrentUser().getMetadata();
                    if (metadata != null) {
                        long creationTimestamp = metadata.getCreationTimestamp();
                        Calendar creationDate = Calendar.getInstance();
                        creationDate.setTimeInMillis(creationTimestamp);

                        String dateStr = String.format("Joined %d/%d/%d",
                                creationDate.get(Calendar.DAY_OF_MONTH),
                                (creationDate.get(Calendar.MONTH) + 1),
                                creationDate.get(Calendar.YEAR));

                        dateTv.setText(dateStr);
                    }
                }
                pD.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                pD.dismiss();
            }
        });
    }

    /**
     * Calculates and displays the total number of recordings and the average score of the user.
     *
     * @param recordingCountTv The TextView to display the recording count.
     * @param avgScoreTv       The TextView to display the average score.
     */
    private void setRecordingCountAndAvgScore(TextView recordingCountTv, TextView avgScoreTv) {
        refRecordings.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dS) {
                int totalCount = 0;
                double totalScore = 0;

                // Iterate through questionId nodes
                for (DataSnapshot questionSnapshot : dS.getChildren()) {
                    // Iterate through recordingId nodes
                    for (DataSnapshot recordingSnapshot : questionSnapshot.getChildren()) {
                        Recording recording = recordingSnapshot.getValue(Recording.class);
                        if (recording != null) {
                            totalCount++;
                            totalScore += recording.getScore();
                        }
                    }
                }

                // Update UI on the main thread
                recordingCountTv.setText(String.valueOf(totalCount));
                if (totalCount > 0) {
                    int avg = (int) (totalScore / totalCount);
                    avgScoreTv.setText(String.valueOf(avg));
                } else {
                    avgScoreTv.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Signs out the current user and redirects to the Welcome Screen.
     *
     * @param view The clicked view.
     */
    public void logOut(View view) {
        refAuth.signOut();

        // Clear the "stayConnected" preference
        SharedPreferences settings = requireActivity().getSharedPreferences("STATUS", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("stayConnected", false);
        editor.commit();

        Intent intent = new Intent(requireActivity(), WelcomeScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        requireActivity().finish();
    }

    /**
     * Fetches the user's profile picture from Firebase Storage and loads it using Glide.
     *
     * @param profilePicture The ShapeableImageView to load the image into.
     */
    private void setProfilePicture(ShapeableImageView profilePicture) {
        StorageReference refFile = refST.child("User_Profiles/" + uid + ".jpg");

        refFile.getDownloadUrl().addOnSuccessListener(uri -> {
            if (getContext() != null && isAdded()) {
                Glide.with(this)
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(R.drawable.error_image)
                        .centerCrop()
                        .into(profilePicture);
            }
        }).addOnFailureListener(e -> {
            profilePicture.setImageResource(R.drawable.placeholder);
            Toast.makeText(getActivity(), "Profile image failed to load", Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Displays a dialog to allow the user to select or capture a new profile picture.
     *
     * @param profilePicture The main profile ImageView to be updated upon success.
     */
    private void changeProfilePicture(ShapeableImageView profilePicture) {
        AlertDialog.Builder adb;
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.change_profile_picture_dialog, null);
        iV = layout.findViewById(R.id.ivProfile);
        Button btnUpload = layout.findViewById(R.id.btnUpload);
        Button btnSave = layout.findViewById(R.id.btnSave);

        adb = new AlertDialog.Builder(requireActivity());
        adb.setView(layout);
        AlertDialog dialog = adb.create();
        dialog.show();

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPhoto();
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentPath != null) {
                    File file = new File(currentPath);
                    Uri fileUri = Uri.fromFile(file);

                    // Start the upload
                    updateProfilePicture(fileUri, profilePicture, dialog);
                } else {
                    Toast.makeText(requireActivity(), "Please select an image first", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Checks for camera permissions when the fragment is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    /**
     * Callback for the result from requesting permissions.
     *
     * @param requestCode  The request code.
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(requireActivity(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Initiates the process of capturing a photo with the camera or selecting one from the gallery.
     */
    private void getPhoto() {
        String filename = "tempfile";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Uri imageUri;
        Intent takePhotoIntent = null;

        try {
            File imgFile = File.createTempFile(filename, ".jpg", storageDir);
            currentPath = imgFile.getAbsolutePath();

            imageUri = FileProvider.getUriForFile(
                    requireActivity(),
                    "com.example.speakup.fileprovider",
                    imgFile
            );
            takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        } catch (IOException e) {
            Toast.makeText(requireActivity(), "Failed to create temporary file", Toast.LENGTH_LONG).show();
            return;
        }

        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_PICK);
        galleryIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        Intent chooserIntent = Intent.createChooser(
                galleryIntent,
                "Select Source"
        );

        if (takePhotoIntent != null) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});
        }

        if (chooserIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(chooserIntent, REQUEST_IMAGE_CHOOSER);
        } else {
            Toast.makeText(requireActivity(), "No compatible application found.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handles the result of the image chooser (camera or gallery).
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult().
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param data_back   An Intent, which can return result data to the caller.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data_back) {
        super.onActivityResult(requestCode, resultCode, data_back);

        if ((requestCode == REQUEST_IMAGE_CHOOSER) && (resultCode == Activity.RESULT_OK)) {
            Bitmap finalBitmap = null;
            Bitmap tempBitmap = null;

            try {
                if (data_back != null && data_back.getData() != null) {
                    // --- GALLERY CASE ---
                    Uri selectedImageUri = data_back.getData();
                    tempBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImageUri);
                    finalBitmap = Utilities.getRotateBitmap(requireActivity(), selectedImageUri, tempBitmap);
                    Utilities.saveBitmapToFile(finalBitmap, currentPath);
                } else if (currentPath != null) {
                    // --- CAMERA CASE ---
                    tempBitmap = BitmapFactory.decodeFile(currentPath);

                    // Fix rotation using the String path-based utility method
                    finalBitmap = Utilities.getRotateBitmap(currentPath, tempBitmap);
                }

                if (finalBitmap != null) {
                    iV.setImageBitmap(finalBitmap);

                    // Memory Cleanup: if finalBitmap is a new rotated copy, delete the sideways tempBitmap
                    if (tempBitmap != null && tempBitmap != finalBitmap) {
                        tempBitmap.recycle();
                    }
                }

            } catch (IOException e) {
                Log.e("ProfileFragment", "Error processing image", e);
                Toast.makeText(requireActivity(), "Failed to load image", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Uploads the selected profile picture to Firebase Storage and updates the UI on success.
     *
     * @param imageUri          The URI of the image to be uploaded.
     * @param mainProfileView   The ImageView on the profile screen to be updated.
     * @param dialog            The selection dialog to be dismissed on success.
     */
    private void updateProfilePicture(Uri imageUri, ShapeableImageView mainProfileView, AlertDialog dialog) {
        ProgressDialog pD = new ProgressDialog(requireActivity());
        pD.setTitle("Uploading...");
        pD.show();

        StorageReference refFile = refST.child("User_Profiles/" + uid + ".jpg");

        refFile.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        pD.dismiss();
                        Toast.makeText(requireActivity(), "Profile Updated!", Toast.LENGTH_LONG).show();

                        // Refresh the image in the fragment
                        setProfilePicture(mainProfileView);

                        if (dialog != null) dialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pD.dismiss();
                        Toast.makeText(requireActivity(), "Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
