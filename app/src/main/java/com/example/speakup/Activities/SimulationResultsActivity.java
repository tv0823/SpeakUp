package com.example.speakup.Activities;

import android.content.Intent;
import android.os.Bundle;
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
 * Shows simulation overall score and allows selecting which recording's details to view.
 */
public class SimulationResultsActivity extends Utilities {

    private TextView overallScoreTv;
    private ProgressBar overallScoreProgressBar;
    private Spinner spinnerRecordings;
    private Button btnViewDetails;

    private ArrayList<Recording> recordings = new ArrayList<>();

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

        btnViewDetails.setOnClickListener(v -> {
            int pos = spinnerRecordings.getSelectedItemPosition();
            if (pos < 0 || pos >= recordings.size()) return;
            Recording selected = recordings.get(pos);

            Intent i = new Intent(SimulationResultsActivity.this, ResultsActivity.class);
            i.putExtra("recording", selected);
            startActivity(i);
        });
    }

    public void goBack(android.view.View v) {
        finish();
    }
}

