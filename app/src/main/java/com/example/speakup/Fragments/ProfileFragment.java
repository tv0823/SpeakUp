package com.example.speakup.Fragments;

import static android.content.Context.MODE_PRIVATE;
import static com.example.speakup.FBRef.refAuth;
import static com.example.speakup.FBRef.refRecordings;
import static com.example.speakup.FBRef.refST;
import static com.example.speakup.FBRef.refUsers;

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
import com.example.speakup.Utilities;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {
    private ImageView iV;
    private String uid;
    private String currentPath;

    /**
     * Request code for camera permission.
     */
    private static final int REQUEST_CAMERA_PERMISSION = 6709;

    /**
     * Request code for image chooser (camera or gallery).
     */
    private static final int REQUEST_IMAGE_CHOOSER = 9051;

    public ProfileFragment() {
        // Required empty public constructor
    }

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
     * Fetches and displays basic user information like username and join date.
     *
     * @param userNameTv The TextView to display the username.
     * @param dateTv     The TextView to display the join date.
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

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(requireActivity(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

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
