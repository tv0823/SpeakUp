package com.example.speakup.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.speakup.Objects.Recording;
import com.example.speakup.R;
import com.example.speakup.Utils.Utilities;

import java.util.ArrayList;

/**
 * Activity for displaying the summary results of a completed exam simulation.
 * <p>
 * This activity shows the user's overall simulation score and provides a list of
 * all individual recordings made during the session. Users can select a specific
 * recording from a dropdown menu to view its detailed AI feedback.
 * </p>
 */
public class SimulationResultsActivity extends Utilities {

    /**
     * TextView for displaying the numerical overall score (e.g., "85/100").
     */
    private TextView overallScoreTv;

    /**
     * ProgressBar for visually representing the overall simulation score.
     */
    private ProgressBar overallScoreProgressBar;

    /**
     * Spinner allowing the user to select one of the recordings from the simulation.
     */
    private Spinner spinnerRecordings;

    /**
     * Button to navigate to the detailed results view for the selected recording.
     */
    private Button btnViewDetails;

    /**
     * List of {@link Recording} objects associated with this simulation session.
     */
    private ArrayList<Recording> recordings = new ArrayList<>();

    /**
     * Initializes the activity, sets the content view, and populates the results data.
     * <p>
     * It retrieves the overall score and the list of recordings from the starting intent.
     * If recordings are found, it populates the spinner with recording titles and scores.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation_results);

        overallScoreTv = findViewById(R.id.overallScoreTv);
        overallScoreProgressBar = findViewById(R.id.overallScoreProgressBar);
        spinnerRecordings = findViewById(R.id.spinnerRecordings);
        btnViewDetails = findViewById(R.id.btnViewDetails);

        int overallScore = getIntent().getIntExtra("overallScore", -1);
        Object maybe = getIntent().getSerializableExtra("recordings");
        if (maybe instanceof ArrayList) {
            //noinspection unchecked
            recordings = (ArrayList<Recording>) maybe;
        }

        if (overallScore >= 0) {
            overallScoreTv.setText(overallScore + "/100");
            overallScoreProgressBar.setProgress(overallScore);
        }

        if (recordings == null || recordings.isEmpty()) {
            Toast.makeText(this, "No recordings found", Toast.LENGTH_SHORT).show();
            btnViewDetails.setEnabled(false);
            return;
        }

        ArrayList<String> titles = new ArrayList<>();
        for (Recording r : recordings) {
            String t = (r.getDisplayTitle() != null) ? r.getDisplayTitle() : "Recording";
            t += " (" + r.getScore() + "/100)";
            titles.add(t);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, titles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecordings.setAdapter(adapter);

        btnViewDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = spinnerRecordings.getSelectedItemPosition();
                if (pos < 0 || pos >= recordings.size()) return;
                Recording selected = recordings.get(pos);

                Intent i = new Intent(SimulationResultsActivity.this, ResultsActivity.class);
                i.putExtra("recording", selected);
                startActivity(i);
            }
        });
    }

    /**
     * Finishes the current activity and returns to the previous screen.
     *
     * @param v The view that was clicked.
     */
    public void goBack(android.view.View v) {
        finish();
    }
}
