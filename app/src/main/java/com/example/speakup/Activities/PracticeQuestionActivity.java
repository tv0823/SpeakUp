package com.example.speakup.Activities;

import static com.example.speakup.FBRef.refQuestions;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.speakup.Objects.Question;
import com.example.speakup.R;
import com.example.speakup.TtsHelper;
import com.example.speakup.Utilities;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class PracticeQuestionActivity extends Utilities {
    private TextView topicTitleTv, keyPointsTv, currentTimeTv, totalTimeTv;
    private SeekBar ttsSeekBar;
    private ImageButton playPauseBtn;

    private Handler timerHandler;
    private int currentProgress = 0; // Tracking in 100ms steps
    private int totalSeconds = 0;
    private int maxProgress = 0;     // totalSeconds * 10
    private static final int TICK_INTERVAL = 100; // Update every 0.1 seconds

    private Question question;
    private TtsHelper tts;

    // Runnable that updates the clock and SeekBar smoothly
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (tts != null && tts.isSpeaking()) {
                if (currentProgress < maxProgress) {
                    currentProgress++;
                    ttsSeekBar.setProgress(currentProgress);

                    // Update the text clock only every 10 ticks (1 second)
                    if (currentProgress % 10 == 0) {
                        updateTimeLabels();
                    }

                    timerHandler.postDelayed(this, TICK_INTERVAL);
                } else {
                    handlePlaybackFinished();
                }
            } else {
                // If TTS finishes before the timer, or is stopped
                stopTimer();
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_question);

        // Initialize UI
        topicTitleTv = findViewById(R.id.topicTitleTv);
        keyPointsTv = findViewById(R.id.keyPointsTv);
        currentTimeTv = findViewById(R.id.currentTimeTv);
        totalTimeTv = findViewById(R.id.totalTimeTv);
        ttsSeekBar = findViewById(R.id.ttsSeekBar);
        playPauseBtn = findViewById(R.id.playPauseBtn);

        timerHandler = new Handler();
        tts = new TtsHelper(this);

        Intent gi = getIntent();
        String questionTopic = gi.getStringExtra("questionTopic");
        String questionId = gi.getStringExtra("questionId");
        String categoryPath = gi.getStringExtra("categoryPath");

        topicTitleTv.setText(questionTopic);

        setupSeekBarListener();
        fetchDataFromFirebase(categoryPath, questionTopic, questionId);
    }

    private void setupSeekBarListener() {
        ttsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Only update the time labels and current progress while dragging
                if (fromUser) {
                    currentProgress = progress;
                    updateTimeLabels();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Stop the sound and timer as soon as the user touches the bar
                stopTimer();
                if (tts != null) {
                    tts.stop();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // ONLY PLAY THE SOUND NOW (when the user lets go)
                if (question != null) {
                    float percentage = (float) seekBar.getProgress() / seekBar.getMax();
                    tts.speakFromPercentage(question.getFullQuestion(), percentage);

                    playPauseBtn.setImageResource(android.R.drawable.ic_media_pause);
                    startTimer();
                }
            }
        });
    }

    public void playOrPause(View view) {
        if (question != null) {
            if (tts.isSpeaking()) {
                tts.stop();
                stopTimer();
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
            } else {
                // If the user hits play at the very end, restart from 0
                if (currentProgress >= maxProgress) {
                    currentProgress = 0;
                    ttsSeekBar.setProgress(0);
                    updateTimeLabels();
                }

                // Start speaking from exactly where the bar is
                float percentage = (float) ttsSeekBar.getProgress() / ttsSeekBar.getMax();
                tts.speakFromPercentage(question.getFullQuestion(), percentage);

                playPauseBtn.setImageResource(android.R.drawable.ic_media_pause);
                startTimer();
            }
        }
    }

    private void fetchDataFromFirebase(String categoryPath, String questionTopic, String questionId) {
        ProgressDialog pD = new ProgressDialog(this);
        pD.setTitle("Loading...");
        pD.setMessage("Fetching question data.");
        pD.setCancelable(false);
        pD.show();

        refQuestions.child(categoryPath).child(questionTopic).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dS) {
                if (pD.isShowing()) pD.dismiss();

                for (DataSnapshot data : dS.getChildren()) {
                    question = data.getValue(Question.class);
                }

                if (question != null) {
                    keyPointsTv.setText(question.getBriefQuestion().replace("\\n", "\n"));

                    String fullText = question.getFullQuestion();

                    // Estimate total time. 13 chars/sec is a good average for 0.8f speed.
                    totalSeconds = fullText.length() / 13;
                    maxProgress = totalSeconds * 10; // High resolution steps

                    if (maxProgress > 0) {
                        ttsSeekBar.setMax(maxProgress);
                    } else {
                        maxProgress = 10;
                        ttsSeekBar.setMax(10);
                    }

                    currentProgress = 0;
                    ttsSeekBar.setProgress(0);
                    updateTimeLabels();
                    playPauseBtn.setClickable(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError dbError) {
                if (pD.isShowing()) pD.dismiss();
                Log.e("Firebase", "Error: " + dbError.getMessage());
            }
        });
    }

    private void updateTimeLabels() {
        int displaySeconds = currentProgress / 10;
        String current = String.format(Locale.getDefault(), "%02d:%02d", displaySeconds / 60, displaySeconds % 60);
        String total = String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        currentTimeTv.setText(current);
        totalTimeTv.setText(total);
    }

    private void handlePlaybackFinished() {
        stopTimer();
        currentProgress = maxProgress;
        ttsSeekBar.setProgress(maxProgress);
        updateTimeLabels();
        playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
    }

    private void startTimer() {
        stopTimer();
        timerHandler.postDelayed(timerRunnable, TICK_INTERVAL);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    public void goBack(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (tts != null) {
            tts.destroy();
        }
    }
}