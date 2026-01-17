package com.example.speakup.Fragments;

import static com.example.speakup.FBRef.refAuth;
import static com.example.speakup.FBRef.refQuestionMedia;
import static com.example.speakup.FBRef.refQuestions;
import static com.example.speakup.FBRef.refRecordings;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.speakup.Activities.ResultsActivity;
import com.example.speakup.Objects.Recording;
import com.example.speakup.R;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class RecordingsGeneralFragment extends Fragment {
    private LinearLayout columnLeft;
    private LinearLayout columnRight;
    private MaterialButtonToggleGroup toggleGroup;
    private ImageButton btnSortDirection;

    private ArrayList<Recording> allRecordingsList;
    private String currentUserId;
    private String categoryPath;

    // Flag to track if we are sorting Ascending (Low to High) or Descending (High to Low)
    private boolean isAscending = false;

    public RecordingsGeneralFragment() {
        // Required empty public constructor
    }

    public static RecordingsGeneralFragment newInstance(String category) {
        RecordingsGeneralFragment fragment = new RecordingsGeneralFragment();
        Bundle args = new Bundle();
        args.putString("ARG_CATEGORY", category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryPath = getArguments().getString("ARG_CATEGORY");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recordings_general, container, false);

        columnLeft = view.findViewById(R.id.columnLeft);
        columnRight = view.findViewById(R.id.columnRight);
        toggleGroup = view.findViewById(R.id.toggleGroup);
        btnSortDirection = view.findViewById(R.id.btnSortDirection);

        allRecordingsList = new ArrayList<>();

        if (refAuth.getCurrentUser() != null) {
            currentUserId = refAuth.getCurrentUser().getUid();
        }

        setupToggleLogic();
        setupSortDirectionLogic();
        fetchRecordingsFromFirebase();

        return view;
    }

    private void setupToggleLogic() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                applySortAndDisplay(checkedId == R.id.btnGrade);
            }
        });
    }

    private void setupSortDirectionLogic() {
        // Initial icon set to Down (High to Low)
        btnSortDirection.setImageResource(android.R.drawable.arrow_down_float);

        btnSortDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isAscending = !isAscending; // Toggle the boolean

                // Change the icon visually to provide feedback to user
                if (isAscending) {
                    btnSortDirection.setImageResource(android.R.drawable.arrow_up_float);
                } else {
                    btnSortDirection.setImageResource(android.R.drawable.arrow_down_float);
                }

                // Re-sort the current list with the new direction
                applySortAndDisplay(toggleGroup.getCheckedButtonId() == R.id.btnGrade);
            }
        });
    }

    private void fetchRecordingsFromFirebase() {
        if (categoryPath == null || currentUserId == null) return;

        refRecordings.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                allRecordingsList.clear();

                for (DataSnapshot questionSnapshot : userSnapshot.getChildren()) {
                    String qId = questionSnapshot.getKey();

                    refQuestions.child(categoryPath).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot catSnapshot) {
                            boolean belongsToCategory = false;
                            for (DataSnapshot topicSnapshot : catSnapshot.getChildren()) {
                                if (topicSnapshot.hasChild(qId)) {
                                    belongsToCategory = true;
                                    break;
                                }
                            }

                            if (belongsToCategory) {
                                for (DataSnapshot recordingSnapshot : questionSnapshot.getChildren()) {
                                    Recording recording = recordingSnapshot.getValue(Recording.class);
                                    if (recording != null) {
                                        allRecordingsList.add(recording);
                                    }
                                }
                                applySortAndDisplay(toggleGroup.getCheckedButtonId() == R.id.btnGrade);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applySortAndDisplay(boolean sortByGrade) {
        if (allRecordingsList == null || allRecordingsList.isEmpty()) return;
        int n = allRecordingsList.size();

        // Selection Sort Implementation
        for (int i = 0; i < n - 1; i++) {
            int targetIndex = i;
            for (int j = i + 1; j < n; j++) {
                Recording r1 = allRecordingsList.get(targetIndex);
                Recording r2 = allRecordingsList.get(j);
                boolean shouldSwap = false;

                if (sortByGrade) {
                    if (isAscending) {
                        // Low to High
                        shouldSwap = r2.getScore() < r1.getScore();
                    } else {
                        // High to Low
                        shouldSwap = r2.getScore() > r1.getScore();
                    }
                } else {
                    if (r2.getDateRecorded() != null && r1.getDateRecorded() != null) {
                        if (isAscending) {
                            // Oldest to Newest
                            shouldSwap = r2.getDateRecorded().before(r1.getDateRecorded());
                        } else {
                            // Newest to Oldest
                            shouldSwap = r2.getDateRecorded().after(r1.getDateRecorded());
                        }
                    }
                }

                if (shouldSwap) {
                    targetIndex = j;
                }
            }
            Recording temp = allRecordingsList.get(targetIndex);
            allRecordingsList.set(targetIndex, allRecordingsList.get(i));
            allRecordingsList.set(i, temp);
        }

        displayRecordings();
    }

    private void displayRecordings() {
        if (columnLeft == null || columnRight == null) return;
        columnLeft.removeAllViews();
        columnRight.removeAllViews();

        for (int i = 0; i < allRecordingsList.size(); i++) {
            LinearLayout target = (i % 2 == 0) ? columnLeft : columnRight;
            addRecordingCard(target, allRecordingsList.get(i));
        }
    }

    private void addRecordingCard(LinearLayout container, Recording recording) {
        View cardView = getLayoutInflater().inflate(R.layout.item_topic_card, container, false);
        TextView titleText = cardView.findViewById(R.id.topicTitleText);
        ImageView imageView = cardView.findViewById(R.id.topicImageView);

        titleText.setText(recording.getDisplayTitle() + "\nScore: " + recording.getScore());
        loadTopicImage(imageView, recording.getQuestionId());

        cardView.setOnClickListener(view -> {
            Intent si = new Intent(getContext(), ResultsActivity.class);
            si.putExtra("recording", recording);
            startActivity(si);
        });

        cardView.setOnLongClickListener(v -> {
            showRenameDialog(recording);
            return true; // Returns true to indicate the click was handled
        });

        container.addView(cardView);
    }

    private void showRenameDialog(Recording recording) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Rename your recording");

        final android.widget.EditText eT = new android.widget.EditText(getContext());
        String oldName = recording.getDisplayTitle();
        eT.setText(recording.getDisplayTitle()); // Pre-fill with old name
        eT.setSelection(eT.getText().length()); // Move cursor to end
        builder.setView(eT);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newName = eT.getText().toString().trim();

            // CHECK 1: Is it empty?
            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // CHECK 2: Is it actually different?
            if (newName.equals(oldName)) {
                // No change detected, just close the dialog
                dialog.dismiss();
            } else {
                // Change detected, proceed to Firebase
                updateRecordingNameInFirebase(recording, newName);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void updateRecordingNameInFirebase(Recording recording, String newName) {
        // Path: recordings / userId / questionId / recordingId / displayTitle
        refRecordings.child(currentUserId)
                .child(recording.getQuestionId())
                .child(recording.getRecordingId())
                .child("displayTitle")
                .setValue(newName)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        // 1. Update the local object now that we know Firebase is updated
                        recording.setDisplayTitle(newName);

                        // 2. Refresh the UI columns to show the new name
                        displayRecordings();

                        Toast.makeText(getContext(), "Renamed successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to update name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTopicImage(ImageView imageView, String questionId) {
        String fileName = questionId + ".jpg";
        StorageReference refFile = refQuestionMedia.child(fileName);

        refFile.getDownloadUrl().addOnSuccessListener(uri -> {
            if (getContext() != null && isAdded()) {
                Glide.with(this)
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(R.drawable.error_image)
                        .centerCrop()
                        .into(imageView);
            }
        }).addOnFailureListener(e -> imageView.setImageResource(R.drawable.error_image));
    }
}