package com.example.speakup.Fragments;

import static com.example.speakup.FBRef.refQuestionMedia;
import static com.example.speakup.FBRef.refQuestions;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.speakup.Activities.PracticeQuestionActivity;
import com.example.speakup.Objects.Question;
import com.example.speakup.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

/**
 * A generic Fragment for displaying lists of practice questions for a specific category.
 * <p>
 * This fragment fetches questions from Firebase Database based on a provided category path.
 * It displays questions in a two-column layout. For specific categories (like "Personal Questions"),
 * it includes a Spinner to filter questions by topic.
 * Clicking a card navigates to the {@link PracticeQuestionActivity} to practice the selected question.
 * </p>
 */
public class QuestionsGeneralFragment extends Fragment {

    /**
     * Layout container for the left column of cards.
     */
    private LinearLayout columnLeft;

    /**
     * Layout container for the right column of cards.
     */
    private LinearLayout columnRight;

    /**
     * Spinner for filtering questions by topic (used for "Personal Questions").
     */
    private Spinner spinnerTopics;

    /**
     * The database path for the question category.
     */
    private String categoryPath;

    /**
     * Global list to store all questions retrieved for the current category.
     */
    private ArrayList<Question> allQuestionsList = new ArrayList<>();

    /**
     * Default constructor.
     * Required for Fragment instantiation.
     */
    public QuestionsGeneralFragment() {}

    /**
     * Factory method to create a new instance of this fragment.
     *
     * @param category The category path to fetch questions for.
     * @return A new instance of fragment QuestionsGeneralFragment.
     */
    public static QuestionsGeneralFragment newInstance(String category) {
        QuestionsGeneralFragment fragment = new QuestionsGeneralFragment();
        Bundle args = new Bundle();
        args.putString("ARG_CATEGORY", category);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called to do initial creation of a fragment.
     * Retrieves the category path from the arguments.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryPath = getArguments().getString("ARG_CATEGORY");
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Initializes the UI components and triggers the data fetch from Firebase.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questions_general, container, false);

        columnLeft = view.findViewById(R.id.columnLeft);
        columnRight = view.findViewById(R.id.columnRight);
        spinnerTopics = view.findViewById(R.id.spinnerTopics);

        fetchTopicsFromFirebase();
        return view;
    }

    /**
     * Fetches questions for the current category from Firebase Database.
     * <p>
     * Iterates through topics and questions, populates the {@code allQuestionsList},
     * and sets up the spinner with unique topics found in the data.
     * </p>
     */
    private void fetchTopicsFromFirebase() {
        if (categoryPath == null) return;

        refQuestions.child(categoryPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dS) {
                if (!dS.exists() || !dS.hasChildren()) {
                    handleEmptyState();
                    return;
                }

                allQuestionsList.clear();
                ArrayList<String> uniqueTopicsList = new ArrayList<>();
                uniqueTopicsList.add("All Topics");

                for (DataSnapshot topicSnapshot : dS.getChildren()) {
                    for (DataSnapshot questionSnapshot : topicSnapshot.getChildren()) {
                        Question question = questionSnapshot.getValue(Question.class);
                        if (question != null && question.getQuestionId() != null) {
                            allQuestionsList.add(question);

                            // Collect unique topic names for the spinner
                            String topic = question.getTopic();
                            if (!uniqueTopicsList.contains(topic)) {
                                uniqueTopicsList.add(topic);
                            }
                        }
                    }
                }

                setupSpinner(uniqueTopicsList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError dbError) {
                Log.e("Firebase", "Error: " + dbError.getMessage());
            }
        });
    }

    /**
     * Configures the topic selection Spinner.
     * <p>
     * The spinner is only visible and clickable if the category is "Personal Questions".
     * Sets up an adapter with the provided list of topics and an item selected listener
     * to filter the displayed questions.
     * </p>
     *
     * @param topics The list of unique topics to display in the spinner.
     */
    private void setupSpinner(ArrayList<String> topics) {
        // Show the spinner ONLY if the category is PersonalQuestions
        if ("Personal Questions".equals(categoryPath)) {
            spinnerTopics.setVisibility(View.VISIBLE);
            spinnerTopics.setClickable(true);
        } else {
            spinnerTopics.setVisibility(View.INVISIBLE);
            spinnerTopics.setClickable(false);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, topics);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTopics.setAdapter(adapter);

        spinnerTopics.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTopic = topics.get(position);
                filterAndDisplay(selectedTopic);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Filters the local list of questions based on the selected topic and updates the UI.
     * <p>
     * Clears the current view and repopulates the two-column layout with questions
     * that match the selected topic (or all questions if "All Topics" is selected).
     * </p>
     *
     * @param topic The topic to filter by.
     */
    private void filterAndDisplay(String topic) {
        columnLeft.removeAllViews();
        columnRight.removeAllViews();
        int index = 0;

        for (Question q : allQuestionsList) {
            if (topic.equals("All Topics") || q.getTopic().equalsIgnoreCase(topic)) {
                LinearLayout targetColumn = (index % 2 == 0) ? columnLeft : columnRight;
                addCardToLayout(targetColumn, q);
                index++;
            }
        }
    }

    /**
     * Creates a card view for a question and adds it to the specified container.
     *
     * @param container The layout container (left or right column) to add the card to.
     * @param question The question object containing details to display.
     */
    private void addCardToLayout(LinearLayout container, Question question) {
        View cardView = getLayoutInflater().inflate(R.layout.item_topic_card, container, false);

        TextView textView = cardView.findViewById(R.id.topicTitleText);
        ImageView imageView = cardView.findViewById(R.id.topicImageView);

        // Preference: SubTopic -> Topic
        if (!question.getSubTopic().equals("null")) {
            textView.setText(question.getSubTopic());
        } else {
            textView.setText(question.getTopic());
        }

        loadTopicImage(imageView, question.getQuestionId());

        cardView.setOnClickListener(v -> {
            Intent si = new Intent(getContext(), PracticeQuestionActivity.class);
            si.putExtra("question", question); // Note: Ensure Question implements Serializable/Parcelable
            startActivity(si);
        });

        container.addView(cardView);
    }

    /**
     * Loads the image associated with a question from Firebase Storage into an ImageView.
     * Uses Glide for image loading and caching.
     *
     * @param imageView The target ImageView.
     * @param questionId The ID of the question used to construct the image filename.
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
        }).addOnFailureListener(e -> {
            imageView.setImageResource(R.drawable.error_image);
        });
    }

    /**
     * Handles the case where no questions are found for the category.
     * Displays a toast message to the user.
     */
    private void handleEmptyState() {
        if (isVisible()) {
            Toast.makeText(getContext(), "No available questions for " + categoryPath, Toast.LENGTH_LONG).show();
        }
    }
}
