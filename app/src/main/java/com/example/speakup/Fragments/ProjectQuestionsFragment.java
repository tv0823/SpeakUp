package com.example.speakup.Fragments;

import static com.example.speakup.FBRef.refQuestionMedia;
import static com.example.speakup.FBRef.refQuestions;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.speakup.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

public class ProjectQuestionsFragment extends Fragment {

    private LinearLayout columnLeft, columnRight;

    public ProjectQuestionsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project_questions, container, false);

        columnLeft = view.findViewById(R.id.columnLeft);
        columnRight = view.findViewById(R.id.columnRight);

        fetchTopicsFromFirebase();
        return view;
    }

    private void fetchTopicsFromFirebase() {
        refQuestions.child("Project Questions").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dS) {
                if (!dS.exists() || !dS.hasChildren()) {
                    handleEmptyState();
                    return;
                }

                columnLeft.removeAllViews();
                columnRight.removeAllViews();
                int index = 0;

                for (DataSnapshot topicSnapshot : dS.getChildren()) {
                    String topicName = topicSnapshot.getKey();
                    for (DataSnapshot questionSnapshot : topicSnapshot.getChildren()) {
                        String questionId = questionSnapshot.getKey();
                        if (questionId != null) {
                            LinearLayout targetColumn = (index % 2 == 0) ? columnLeft : columnRight;
                            addCardToLayout(targetColumn, topicName, questionId);
                            index++;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError dbError) {
                Log.e("Firebase", "Error: " + dbError.getMessage());
            }
        });
    }

    private void addCardToLayout(LinearLayout container, String questionTopic, String questionId) {
        View cardView = getLayoutInflater().inflate(R.layout.item_topic_card, container, false);

        TextView textView = cardView.findViewById(R.id.topicTitleText);
        ImageView imageView = cardView.findViewById(R.id.topicImageView);

        textView.setText(questionTopic);

        loadTopicImage(imageView, questionId);

        cardView.setOnClickListener(v -> {
            if (isVisible()) {
                Toast.makeText(getContext(), "You chose: " + questionTopic, Toast.LENGTH_SHORT).show();
            }
        });

        container.addView(cardView);
    }

    private void loadTopicImage(ImageView imageView, String questionId) {
        String fileName = questionId + ".jpg";
        StorageReference refFile = refQuestionMedia.child(fileName);

        refFile.getDownloadUrl().addOnSuccessListener(uri -> {
            Log.d("GlideDebug", "URL found: " + uri.toString());

            Glide.with(this)
                    .load(uri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.error_image)
                    .into(imageView);

        }).addOnFailureListener(e -> {
            Log.e("GlideDebug", "Firebase rejected request: " + e.getMessage());
            imageView.setImageResource(R.drawable.error_image);
        });
    }

    private void handleEmptyState() {
        if (isVisible()) {
            Toast.makeText(getContext(), "No available questions for now", Toast.LENGTH_LONG).show();
        }
    }
}