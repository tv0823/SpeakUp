package com.example.speakup.Fragments;

import static com.example.speakup.Utils.FBRef.refAuth;
import static com.example.speakup.Utils.FBRef.refQuestionMedia;
import static com.example.speakup.Utils.FBRef.refQuestions;
import static com.example.speakup.Utils.FBRef.refRecordings;
import static com.example.speakup.Utils.FBRef.refSimulations;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import com.example.speakup.Activities.SimulationResultsActivity;
import com.example.speakup.Objects.Question;
import com.example.speakup.Objects.Recording;
import com.example.speakup.Objects.Simulation;
import com.example.speakup.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A fragment that displays a sortable grid of recordings for a specific
 * category.
 * <p>
 * This fragment fetches recording data from Firebase Realtime Database, filters
 * them by category,
 * and displays them in a two-column staggered layout. It supports:
 * <ul>
 * <li>Sorting by Grade (Score) or Date Recorded.</li>
 * <li>Toggling sort direction (Ascending vs. Descending).</li>
 * <li>Navigating to the {@link ResultsActivity} or
 * {@link SimulationResultsActivity} on click.</li>
 * <li>Renaming recordings via long-press.</li>
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
     * Card view shown when no recordings are found.
     */
    private View cardNoData;

    /**
     * Text view within the cardNoData to display the message.
     */
    private TextView tvNoDataMessage;

    /**
     * Container for the grid columns.
     */
    private LinearLayout mainColumnsContainer;

    /**
     * List of all recordings fetched from Firebase that belong to the current
     * category.
     */
    private ArrayList<Recording> allRecordingsList;

    /**
     * List of all simulations fetched from Firebase.
     */
    private ArrayList<Simulation> allSimulationsList;

    /**
     * The UID of the currently authenticated user.
     */
    private String currentUserId;

    /**
     * The category identifier used to filter recordings (e.g., "Personal Questions"
     * or "Simulation").
     */
    private String categoryPath;

    /**
     * Flag to track if the current sort direction is Ascending (true) or Descending
     * (false).
     * Defaults to Descending (Newest first / Highest score first).
     */
    private boolean isAscending = false;

    /**
     * Progress dialog shown while fetching data from Firebase.
     */
    private ProgressDialog pD;

    /**
     * Maps Firebase questionId -> normalized image key used in Firebase Storage.
     */
    private Map<String, String> questionIdToImageKey;

    /**
     * Required empty public constructor for fragment instantiation.
     */
    public RecordingsGeneralFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new instance of this fragment with a specific category filter.
     *
     * @param category The category path used to filter recordings (e.g., "Personal
     *                 Questions").
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
     * @param savedInstanceState If the fragment is being re-created from a previous
     *                           saved state.
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
     * @param inflater           The LayoutInflater object that can be used to
     *                           inflate views.
     * @param container          The parent view that the fragment's UI should be
     *                           attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recordings_general, container, false);

        columnLeft = view.findViewById(R.id.columnLeft);
        columnRight = view.findViewById(R.id.columnRight);
        toggleGroup = view.findViewById(R.id.toggleGroup);
        btnSortDirection = view.findViewById(R.id.btnSortDirection);
        cardNoData = view.findViewById(R.id.cardNoData);
        tvNoDataMessage = view.findViewById(R.id.tvNoDataMessage);
        mainColumnsContainer = view.findViewById(R.id.mainColumnsContainer);

        pD = new ProgressDialog(getContext());
        if ("Simulation".equals(categoryPath)) {
            pD.setMessage("Loading simulations...");
        } else {
            pD.setMessage("Loading recordings...");
        }
        pD.setCancelable(false);
        pD.show();

        allRecordingsList = new ArrayList<>();
        allSimulationsList = new ArrayList<>();
        questionIdToImageKey = new HashMap<>();

        if (refAuth.getCurrentUser() != null) {
            currentUserId = refAuth.getCurrentUser().getUid();
        }

        setupToggleLogic();
        setupSortDirectionLogic();
        if ("Simulation".equals(categoryPath)) {
            fetchSimulationsFromFirebase();
        } else {
            fetchRecordingsFromFirebase();
        }

        return view;
    }

    /**
     * Configures behavior after the view has been created, specifically the back
     * button logic.
     *
     * @param view               The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // if the user press back button, go to home
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                        nav.setSelectedItemId(R.id.nav_home);
                    }
                });
    }

    /**
     * Sets up the listener for the sort criteria toggle group (Date vs. Grade).
     */
    private void setupToggleLogic() {
        toggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (isChecked) {
                    applySortAndDisplay(checkedId == R.id.btnGrade);
                }
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
     * <p>
     * This method performs a two-step data retrieval process:
     * </p>
     * <ol>
     * <li>
     * Retrieves all questions under the current {@link #categoryPath} and builds a
     * set of
     * allowed question IDs. During this step, it also prepares a mapping between
     * question IDs and their corresponding image keys for later use.
     * </li>
     * <li>
     * Fetches all recordings for the current user and filters them by checking
     * whether
     * their question ID exists in the allowed set.
     * </li>
     * </ol>
     *
     * <p>
     * All matching recordings are stored in {@link #allRecordingsList}, after which
     * the UI
     * is updated by applying sorting and displaying the results.
     * </p>
     *
     * <p>
     * If no recordings are found for the selected category, a toast message is
     * shown.
     * </p>
     */
    private void fetchRecordingsFromFirebase() {
        if (categoryPath == null || currentUserId == null) {
            if (pD != null)
                pD.dismiss();
            return;
        }

        // get allowed question IDs
        refQuestions.child(categoryPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot catSnapshot) {

                Set<String> allowedIds = new HashSet<>();

                for (DataSnapshot topicSnapshot : catSnapshot.getChildren()) {
                    for (DataSnapshot qSnap : topicSnapshot.getChildren()) {
                        String qId = qSnap.getKey();
                        if (qId != null) {
                            allowedIds.add(qId);

                            // build image key
                            Question q = qSnap.getValue(Question.class);
                            if (q != null) {
                                String sub = q.getSubTopic();
                                if (sub == null || sub.equals("null"))
                                    sub = q.getTopic();

                                if (sub != null) {
                                    String key = sub.split(" Set")[0]
                                            .replace(' ', '_')
                                            .toLowerCase();
                                    questionIdToImageKey.put(qId, key);
                                }
                            }
                        }
                    }
                }

                // fetch recordings once
                refRecordings.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                        if (pD != null)
                            pD.dismiss();

                        allRecordingsList.clear();

                        for (DataSnapshot questionSnapshot : userSnapshot.getChildren()) {
                            String qId = questionSnapshot.getKey();
                            if (!allowedIds.contains(qId))
                                continue;

                            for (DataSnapshot recSnap : questionSnapshot.getChildren()) {
                                Recording rec = recSnap.getValue(Recording.class);
                                if (rec != null)
                                    allRecordingsList.add(rec);
                            }
                        }

                        if (allRecordingsList.isEmpty()) {
                            updateVisibility(false, "No recordings for this tab");
                            return;
                        }

                        updateVisibility(true, null);

                        applySortAndDisplay(true);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (pD != null)
                            pD.dismiss();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (pD != null)
                    pD.dismiss();
            }
        });
    }

    /**
     * Sorts the recording list based on the chosen criteria and direction, then
     * refreshes the UI.
     * <p>
     * Uses a Selection Sort implementation to reorder {@link #allRecordingsList} or
     * {@link #allSimulationsList}.
     * </p>
     *
     * @param sortByGrade If true, sorts by score. If false, sorts by recording
     *                    date.
     */
    private void applySortAndDisplay(boolean sortByGrade) {
        if ("Simulation".equals(categoryPath)) {
            applySortAndDisplaySimulation(sortByGrade);
            return;
        }
        if (allRecordingsList == null || allRecordingsList.isEmpty())
            return;
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
     * Sorts the simulation list based on the chosen criteria and direction, then
     * refreshes the UI.
     *
     * @param sortByGrade If true, sorts by score. If false, sorts by completion
     *                    date.
     */
    private void applySortAndDisplaySimulation(boolean sortByGrade) {
        if (allSimulationsList == null || allSimulationsList.isEmpty())
            return;
        int n = allSimulationsList.size();

        for (int i = 0; i < n - 1; i++) {
            int targetIndex = i;
            for (int j = i + 1; j < n; j++) {
                Simulation s1 = allSimulationsList.get(targetIndex);
                Simulation s2 = allSimulationsList.get(j);
                boolean shouldSwap = false;

                if (sortByGrade) {
                    if (isAscending) {
                        shouldSwap = s2.getOverAllScore() < s1.getOverAllScore();
                    } else {
                        shouldSwap = s2.getOverAllScore() > s1.getOverAllScore();
                    }
                } else {
                    Date d1 = s1.getDateCompleted();
                    Date d2 = s2.getDateCompleted();
                    if (d1 != null && d2 != null) {
                        if (isAscending) {
                            shouldSwap = d2.before(d1);
                        } else {
                            shouldSwap = d2.after(d1);
                        }
                    }
                }

                if (shouldSwap) {
                    targetIndex = j;
                }
            }
            Simulation temp = allSimulationsList.get(targetIndex);
            allSimulationsList.set(targetIndex, allSimulationsList.get(i));
            allSimulationsList.set(i, temp);
        }

        displaySimulations();
    }

    /**
     * Clears and repopulates the two-column grid with recording cards.
     */
    private void displayRecordings() {
        if (columnLeft == null || columnRight == null)
            return;
        columnLeft.removeAllViews();
        columnRight.removeAllViews();

        for (int i = 0; i < allRecordingsList.size(); i++) {
            LinearLayout target = (i % 2 == 0) ? columnLeft : columnRight;
            addRecordingCard(target, allRecordingsList.get(i));
        }
    }

    /**
     * Clears and repopulates the two-column grid with simulation cards.
     */
    private void displaySimulations() {
        if (columnLeft == null || columnRight == null)
            return;
        columnLeft.removeAllViews();
        columnRight.removeAllViews();

        for (int i = 0; i < allSimulationsList.size(); i++) {
            LinearLayout target = (i % 2 == 0) ? columnLeft : columnRight;
            addSimulationCard(target, allSimulationsList.get(i));
        }
    }

    /**
     * Inflates a recording card view and adds it to the specified container.
     *
     * @param container The layout column to add the card to.
     * @param recording The recording data to display on the card.
     */
    private void addRecordingCard(LinearLayout container, Recording recording) {
        View cardView = getLayoutInflater().inflate(R.layout.item_topic_card, container, false);
        TextView titleText = cardView.findViewById(R.id.topicTitleText);
        ImageView imageView = cardView.findViewById(R.id.topicImageView);

        titleText.setText(recording.getDisplayTitle() + "\nScore: " + recording.getScore());
        String imageKey = (questionIdToImageKey != null) ? questionIdToImageKey.get(recording.getQuestionId()) : null;
        // Fallback to questionId if we couldn't determine image key.
        loadTopicImage(imageView, (imageKey != null) ? imageKey : recording.getQuestionId());

        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent si = new Intent(getContext(), ResultsActivity.class);
                si.putExtra("recording", recording);
                startActivity(si);
            }
        });

        cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showRenameDialog(recording);
                return true; // Returns true to indicate the click was handled
            }
        });

        container.addView(cardView);
    }

    /**
     * Inflates a simulation card view and adds it to the specified container.
     *
     * @param container  The layout column to add the card to.
     * @param simulation The simulation data to display on the card.
     */
    private void addSimulationCard(LinearLayout container, Simulation simulation) {
        View cardView = getLayoutInflater().inflate(R.layout.item_topic_card, container, false);
        TextView titleText = cardView.findViewById(R.id.topicTitleText);
        ImageView imageView = cardView.findViewById(R.id.topicImageView);

        String dateText = "-";
        if (simulation.getDateCompleted() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            dateText = sdf.format(simulation.getDateCompleted());
        }
        titleText.setText("Simulation\nScore: " + simulation.getOverAllScore() + "\n" + dateText);
        loadSimulationCardImage(imageView);

        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSimulationResult(simulation);
            }
        });
        container.addView(cardView);
    }

    /**
     * Fetches simulation data from Firebase Realtime Database for the current user.
     */
    private void fetchSimulationsFromFirebase() {
        if (currentUserId == null) {
            if (pD != null)
                pD.dismiss();
            return;
        }

        refSimulations.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (pD != null && pD.isShowing())
                    pD.dismiss();
                allSimulationsList.clear();

                for (DataSnapshot simSnapshot : snapshot.getChildren()) {
                    Simulation simulation = simSnapshot.getValue(Simulation.class);
                    if (simulation != null) {
                        allSimulationsList.add(simulation);
                    }
                }

                if (allSimulationsList.isEmpty()) {
                    updateVisibility(false, "No simulations for this tab");
                    return;
                }

                updateVisibility(true, null);

                applySortAndDisplay(toggleGroup.getCheckedButtonId() == R.id.btnGrade);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (pD != null)
                    pD.dismiss();
            }
        });
    }

    /**
     * Resolves individual recordings for a simulation and opens the results
     * activity.
     *
     * @param simulation The simulation object to view results for.
     */
    private void openSimulationResult(Simulation simulation) {
        if (currentUserId == null)
            return;
        if (simulation.getRecordingsIds() == null || simulation.getRecordingsIds().isEmpty()) {
            Toast.makeText(getContext(), "No recordings for this simulation", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog loadingDialog = new ProgressDialog(getContext());
        loadingDialog.setMessage("Loading simulation details...");
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        refRecordings.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userRecordingsSnapshot) {
                if (loadingDialog.isShowing())
                    loadingDialog.dismiss();

                ArrayList<Recording> resolvedRecordings = new ArrayList<>();
                for (String recordingId : simulation.getRecordingsIds()) {
                    Recording resolved = findRecordingById(userRecordingsSnapshot, recordingId);
                    if (resolved != null) {
                        resolvedRecordings.add(resolved);
                    }
                }

                if (resolvedRecordings.isEmpty()) {
                    Toast.makeText(getContext(), "Simulation recordings not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(getContext(), SimulationResultsActivity.class);
                intent.putExtra("overallScore", simulation.getOverAllScore());
                intent.putExtra("recordings", resolvedRecordings);
                startActivity(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (loadingDialog.isShowing())
                    loadingDialog.dismiss();
                Toast.makeText(getContext(), "Failed to load simulation details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Searches for a recording by ID within the user's recordings snapshot.
     *
     * @param userRecordingsSnapshot The snapshot containing all the user's
     *                               recordings.
     * @param recordingId            The ID of the recording to find.
     * @return The resolved Recording object, or null if not found.
     */
    private Recording findRecordingById(DataSnapshot userRecordingsSnapshot, String recordingId) {
        for (DataSnapshot questionNode : userRecordingsSnapshot.getChildren()) {
            DataSnapshot recNode = questionNode.child(recordingId);
            if (!recNode.exists())
                continue;
            Recording rec = recNode.getValue(Recording.class);
            if (rec != null)
                return rec;
        }
        return null;
    }

    /**
     * Displays a dialog allowing the user to rename a specific recording.
     *
     * @param recording The recording object to be renamed.
     */
    private void showRenameDialog(Recording recording) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                getContext());
        builder.setTitle("Rename your recording");

        final android.widget.EditText eT = new android.widget.EditText(getContext());
        String oldName = recording.getDisplayTitle();
        eT.setText(recording.getDisplayTitle()); // Pre-fill with old name
        eT.setSelection(eT.getText().length()); // Move cursor to end
        builder.setView(eT);

        builder.setPositiveButton("Rename", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
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
            }
        });

        builder.setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    /**
     * Updates the recording's title in Firebase and refreshes the local UI upon
     * success.
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
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if (isAdded()) {
                            // 1. Update the local object now that we know Firebase is updated
                            recording.setDisplayTitle(newName);

                            // 2. Refresh the UI columns to show the new name
                            displayRecordings();

                            Toast.makeText(getContext(), "Renamed successfully!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to update name: " + e.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        }
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

        refFile.getDownloadUrl().addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<android.net.Uri>() {
            @Override
            public void onSuccess(android.net.Uri uri) {
                if (getContext() != null && isAdded()) {
                    Glide.with(RecordingsGeneralFragment.this)
                            .load(uri)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.error_image)
                            .centerCrop()
                            .into(imageView);
                }
            }
        }).addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                imageView.setImageResource(R.drawable.error_image);
            }
        });
    }

    /**
     * Loads a generic simulation image from Firebase Storage for simulation cards.
     *
     * @param imageView The target ImageView.
     */
    private void loadSimulationCardImage(ImageView imageView) {
        StorageReference refFile = refQuestionMedia.child("simulation.jpg");
        refFile.getDownloadUrl().addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<android.net.Uri>() {
            @Override
            public void onSuccess(android.net.Uri uri) {
                if (getContext() != null && isAdded()) {
                    Glide.with(RecordingsGeneralFragment.this)
                            .load(uri)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.placeholder)
                            .centerCrop()
                            .into(imageView);
                }
            }
        }).addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                imageView.setImageResource(R.drawable.placeholder);
            }
        });
    }

    /**
     * Updates the visibility of the UI components based on whether data is available.
     *
     * @param hasData If true, shows the data grid. If false, shows the "No Data" card.
     * @param message The message to display in the "No Data" card.
     */
    private void updateVisibility(boolean hasData, String message) {
        if (!isAdded()) return;
        
        if (mainColumnsContainer == null) return;

        if (hasData) {
            if (cardNoData != null) cardNoData.setVisibility(View.GONE);
            mainColumnsContainer.setVisibility(View.VISIBLE);
            if (toggleGroup != null) toggleGroup.setVisibility(View.VISIBLE);
            if (btnSortDirection != null) btnSortDirection.setVisibility(View.VISIBLE);
        } else {
            if (cardNoData != null) {
                cardNoData.setVisibility(View.VISIBLE);
                if (tvNoDataMessage != null && message != null) {
                    tvNoDataMessage.setText(message);
                }
            }
            mainColumnsContainer.setVisibility(View.GONE);
            if (toggleGroup != null) toggleGroup.setVisibility(View.GONE);
            if (btnSortDirection != null) btnSortDirection.setVisibility(View.GONE);
        }
    }
}
