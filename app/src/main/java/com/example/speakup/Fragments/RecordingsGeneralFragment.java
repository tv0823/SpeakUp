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

/**
 * A fragment that displays a sortable grid of recordings for a specific category.
 * <p>
 * This fragment fetches recording data from Firebase Realtime Database, filters them by category,
 * and displays them in a two-column staggered layout. It supports:
 * <ul>
 *     <li>Sorting by Grade (Score) or Date Recorded.</li>
 *     <li>Toggling sort direction (Ascending vs. Descending).</li>
 *     <li>Navigating to the {@link ResultsActivity} on click.</li>
 *     <li>Renaming recordings via long-press.</li>
 * </ul>
 * </p>
 */
public class RecordingsGeneralFragment extends Fragment {

    /**
     * Left column container for the staggered grid.
     */
    private LinearLayout columnLeft;

    /**
     * Right column container for the staggered grid.
     */
    private LinearLayout columnRight;

    /**
     * Toggle group for selecting the sorting criteria (Grade vs. Date).
     */
    private MaterialButtonToggleGroup toggleGroup;

    /**
     * Button to toggle the sorting direction (Up/Down).
     */
    private ImageButton btnSortDirection;

    /**
     * List of all recordings fetched from Firebase that belong to the current category.
     */
    private ArrayList<Recording> allRecordingsList;

    /**
     * The UID of the currently authenticated user.
     */
    private String currentUserId;

    /**
     * The category identifier used to filter recordings (e.g., "Personal Questions").
     */
    private String categoryPath;

    /**
     * Flag to track if the current sort direction is Ascending (true) or Descending (false).
     * Defaults to Descending (Newest first / Highest score first).
     */
    private boolean isAscending = false;

    /**
     * Required empty public constructor for fragment instantiation.
     */
    public RecordingsGeneralFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new instance of this fragment with a specific category filter.
     *
     * @param category The category path used to filter recordings (e.g., "Personal Questions").
     * @return A new instance of RecordingsGeneralFragment.
     */
    public static RecordingsGeneralFragment newInstance(String category) {
        RecordingsGeneralFragment fragment = new RecordingsGeneralFragment();
        Bundle args = new Bundle();
        args.putString("ARG_CATEGORY", category);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Initializes the fragment and retrieves the category argument.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryPath = getArguments().getString("ARG_CATEGORY");
        }
    }

    /**
     * Inflates the layout and initializes UI components and logic listeners.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate views.
     * @param container          The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
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

    /**
     * Sets up the listener for the sort criteria toggle group (Date vs. Grade).
     */
    private void setupToggleLogic() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                applySortAndDisplay(checkedId == R.id.btnGrade);
            }
        });
    }

    /**
     * Sets up the listener for the sort direction button and initializes its icon.
     */
    private void setupSortDirectionLogic() {
        // Initial icon set to Down (High to Low / Newest to Oldest)
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

    /**
     * Fetches recording data from Firebase Realtime Database.
     * <p>
     * It performs a multi-step fetch:
     * 1. Listens for all recordings of the current user.
     * 2. For each question that has recordings, checks if that question belongs to the current {@link #categoryPath}.
     * 3. Aggregates all valid recordings into {@link #allRecordingsList} and triggers a UI update.
     * </p>
     */
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

    /**
     * Sorts the recording list based on the chosen criteria and direction, then refreshes the UI.
     * <p>
     * Uses a Selection Sort implementation to reorder {@link #allRecordingsList}.
     * </p>
     *
     * @param sortByGrade If true, sorts by score. If false, sorts by recording date.
     */
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

    /**
     * Clears and repopulates the two-column grid with recording cards.
     */
    private void displayRecordings() {
        if (columnLeft == null || columnRight == null) return;
        columnLeft.removeAllViews();
        columnRight.removeAllViews();

        for (int i = 0; i < allRecordingsList.size(); i++) {
            LinearLayout target = (i % 2 == 0) ? columnLeft : columnRight;
            addRecordingCard(target, allRecordingsList.get(i));
        }
    }

    /**
     * Inflates a recording card view and adds it to the specified container.
     * <p>
     * Sets up click listeners for viewing results and long-press listeners for renaming.
     * </p>
     *
     * @param container The layout column to add the card to.
     * @param recording The recording data to display on the card.
     */
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

    /**
     * Displays a dialog allowing the user to rename a specific recording.
     *
     * @param recording The recording object to be renamed.
     */
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

    /**
     * Updates the recording's title in Firebase and refreshes the local UI upon success.
     *
     * @param recording The recording object whose title is being updated.
     * @param newName   The new title for the recording.
     */
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

    /**
     * Asynchronously loads a topic image from Firebase Storage using Glide.
     *
     * @param imageView  The target ImageView.
     * @param questionId The ID of the question used to locate the image in storage.
     */
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
