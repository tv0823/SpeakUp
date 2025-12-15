package com.example.speakup.Fragments;

import static com.example.speakup.FBRef.refQuestions;

import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.example.speakup.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProjectQuestionsFragment extends Fragment {

    private LinearLayout buttonsContainer;
    private ArrayList<String> topics;

    public ProjectQuestionsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project_questions, container, false);
        buttonsContainer = view.findViewById(R.id.buttonsContainer);

        topics = new ArrayList<>();

        fetchTopicsFromFirebase();
        return view;
    }

    private void fetchTopicsFromFirebase() {
        refQuestions.child("Project Questions").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dS) {
                topics.clear();
                for(DataSnapshot data : dS.getChildren()) {
                    topics.add(data.getKey());
                }
                loadTopicButtons();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError dbError) {
                Log.e("ProjectQuestionsFragment", "Error reading data: "+dbError);
            }
        });
    }

    private void loadTopicButtons() {
        if (topics.isEmpty()) {
            Toast.makeText(getContext(), "No topics available.", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout column1 = new LinearLayout(getContext());
        column1.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
        column1.setOrientation(LinearLayout.VERTICAL);
        column1.setPadding(0, 0, 8, 0);

        LinearLayout column2 = new LinearLayout(getContext());
        column2.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
        column2.setOrientation(LinearLayout.VERTICAL);
        column2.setPadding(8, 0, 0, 0);

        for (int i = 0; i < topics.size(); i++) {
            String topicName = topics.get(i);

            LinearLayout targetColumn = (i % 2 == 0) ? column1 : column2;

            MaterialButton button = createTopicButton(topicName);
            targetColumn.addView(button);
        }

        buttonsContainer.addView(column1);
        buttonsContainer.addView(column2);
    }

    private MaterialButton createTopicButton(String topicName) {
        MaterialButton button = new MaterialButton(getContext());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 2.0f); // Height 0, Weight 2.0f

        params.bottomMargin = (int) getResources().getDimension(R.dimen.button_margin_bottom);
        button.setLayoutParams(params);

        button.setBackgroundTintList(null);
        button.setBackgroundColor(0x00FFFFFF); //#00FFFFFF

        // Set Gravity: gravity="left|center_vertical" in XML becomes Gravity.START | Gravity.CENTER_VERTICAL
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

        int paddingStart = (int) getResources().getDimension(R.dimen.button_padding_start_30dp);
        button.setPaddingRelative(paddingStart, 0, 0, 0);

        button.setText(topicName.replace("&amp;", "&"));
        button.setTextColor(0xFF000000); // Black
        button.setTextSize(16f); // 16sp
        button.getPaint().setFakeBoldText(true);

        int paddingPixels = (int) getResources().getDimension(R.dimen.icon_text_padding);
        button.setIconPadding(paddingPixels);
        int iconSizePixels = (int) getResources().getDimension(R.dimen.icon_size_large);
        button.setIconSize(iconSizePixels);
        button.setIconTint(null);
        button.setIconResource(R.drawable.practice_questions_logo);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);

        button.setCornerRadius(20);
        button.setStrokeColorResource(R.color.stroke_color_grey);
        button.setStrokeWidth(2);

        button.setOnClickListener(this::handleTopicClick);

        return button;
    }

    private void handleTopicClick(View v) {
        if (v instanceof MaterialButton) {
            MaterialButton button = (MaterialButton) v;
            String topicText = button.getText().toString().replace("\n", " ");

            Toast.makeText(getContext(), "Navigating with topic: " + topicText.trim(), Toast.LENGTH_SHORT).show();
        }
    }
}