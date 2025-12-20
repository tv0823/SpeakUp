package com.example.speakup.Activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.speakup.Objects.Question;
import com.example.speakup.R;
import com.example.speakup.TtsHelper;
import com.example.speakup.Utilities;

import java.util.Locale;

/**
 * Activity for practicing a specific question.
 * <p>
 * This activity displays a question's details, including its sub-topic or topic and key points.
 * It allows the user to listen to the question text using Text-To-Speech (TTS).
 * It includes a playback control with play/pause functionality, a seek bar, and a timer display.
 * The Question object is received via Intent.
 * </p>
 */
public class PracticeQuestionActivity extends Utilities {
    /**
     * TextView displaying the sub-topic or topic title.
     */
    private TextView subTopicTitleTv;
    
    /**
     * TextView displaying the key points or brief version of the question.
     */
    private TextView keyPointsTv;
    
    /**
     * TextView displaying the current playback time.
     */
    private TextView currentTimeTv;
    
    /**
     * TextView displaying the total duration of the question text.
     */
    private TextView totalTimeTv;
    
    /**
     * SeekBar for controlling playback progress.
     */
    private SeekBar ttsSeekBar;
    
    /**
     * Button to toggle play/pause state.
     */
    private ImageButton playPauseBtn;

    /**
     * Handler for managing the playback timer updates.
     */
    private Handler timerHandler;

    /**
     * Current progress of the playback in 100ms steps.
     */
    private int currentProgress = 0;

    /**
     * Total estimated duration of the question text in seconds.
     */
    private int totalSeconds = 0;

    /**
     * Maximum progress value for the seek bar (totalSeconds * 10).
     */
    private int maxProgress = 0;

    /**
     * Interval in milliseconds for updating the timer and seek bar (0.1 seconds).
     */
    private static final int TICK_INTERVAL = 100;

    /**
     * The question object being practiced.
     */
    private Question question;

    /**
     * Helper for Text-To-Speech functionality.
     */
    private TtsHelper tts;

    /**
     * Runnable that updates the clock and SeekBar smoothly during playback.
     */
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (tts != null && tts.isSpeaking()) {
                if (currentProgress < maxProgress) {
                    currentProgress++;
                    ttsSeekBar.setProgress(currentProgress);
                    if (currentProgress % 10 == 0) {
                        updateTimeLabels();
                    }
                    timerHandler.postDelayed(this, TICK_INTERVAL);
                } else {
                    handlePlaybackFinished();
                }
            } else {
                stopTimer();
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
            }
        }
    };

    /**
     * Called when the activity is starting.
     * Initializes the UI, TTS helper, and retrieves the Question object from the Intent.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_question);

        // Initialize UI
        subTopicTitleTv = findViewById(R.id.subTopicTitleTv);
        keyPointsTv = findViewById(R.id.keyPointsTv);
        currentTimeTv = findViewById(R.id.currentTimeTv);
        totalTimeTv = findViewById(R.id.totalTimeTv);
        ttsSeekBar = findViewById(R.id.ttsSeekBar);
        playPauseBtn = findViewById(R.id.playPauseBtn);

        timerHandler = new Handler();
        tts = new TtsHelper(this);

        // 1. Get the Question object from Intent
        Intent gi = getIntent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            question = gi.getSerializableExtra("question", Question.class);
        } else {
            question = (Question) gi.getSerializableExtra("question");
        }

        // 2. Initialize data immediately if object exists
        if (question != null) {
            setupUIWithQuestionData();
        }

        setupSeekBarListener();
    }

    /**
     * Populates the UI with data from the {@link Question} object.
     * <p>
     * Sets the topic/subtopic title and key points text.
     * Calculates the estimated total duration for TTS based on text length and configures the SeekBar and time labels.
     * </p>
     */
    private void setupUIWithQuestionData() {
        if (!question.getSubTopic().equals("null")) {
            subTopicTitleTv.setText(question.getSubTopic());
        } else {
            subTopicTitleTv.setText(question.getTopic());
        }

        keyPointsTv.setText(question.getBriefQuestion().replace("\\n", "\n"));

        String fullText = question.getFullQuestion();

        // Estimate total time. 13 chars/sec is a good average for 0.8f speed.
        totalSeconds = fullText.length() / 13;
        maxProgress = totalSeconds * 10;

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

    /**
     * Sets up the listener for the SeekBar to handle user interaction (seeking).
     */
    private void setupSeekBarListener() {
        ttsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentProgress = progress;
                    updateTimeLabels();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopTimer();
                if (tts != null) tts.stop();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (question != null) {
                    float percentage = (float) seekBar.getProgress() / seekBar.getMax();
                    tts.speakFromPercentage(question.getFullQuestion(), percentage);
                    playPauseBtn.setImageResource(android.R.drawable.ic_media_pause);
                    startTimer();
                }
            }
        });
    }

    /**
     * Toggles between playing and pausing the question TTS.
     *
     * @param view The view that was clicked.
     */
    public void playOrPause(View view) {
        if (question != null) {
            if (tts.isSpeaking()) {
                tts.stop();
                stopTimer();
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
            } else {
                if (currentProgress >= maxProgress) {
                    currentProgress = 0;
                    ttsSeekBar.setProgress(0);
                    updateTimeLabels();
                }
                float percentage = (float) ttsSeekBar.getProgress() / ttsSeekBar.getMax();
                tts.speakFromPercentage(question.getFullQuestion(), percentage);
                playPauseBtn.setImageResource(android.R.drawable.ic_media_pause);
                startTimer();
            }
        }
    }

    /**
     * Updates the text views displaying the current and total playback time.
     */
    private void updateTimeLabels() {
        int displaySeconds = currentProgress / 10;
        String current = String.format(Locale.getDefault(), "%02d:%02d", displaySeconds / 60, displaySeconds % 60);
        String total = String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        currentTimeTv.setText(current);
        totalTimeTv.setText(total);
    }

    /**
     * Handles UI updates when playback finishes successfully.
     */
    private void handlePlaybackFinished() {
        stopTimer();
        currentProgress = maxProgress;
        ttsSeekBar.setProgress(maxProgress);
        updateTimeLabels();
        playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
    }

    /**
     * Starts the timer that updates the progress bar and time labels.
     */
    private void startTimer() {
        stopTimer();
        timerHandler.postDelayed(timerRunnable, TICK_INTERVAL);
    }

    /**
     * Stops the playback timer.
     */
    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    /**
     * Finishes the current activity and returns to the previous screen.
     *
     * @param view The view that was clicked.
     */
    public void goBack(View view) {
        finish();
    }

    /**
     * Called when the activity is destroyed.
     * Cleans up the timer and releases TTS resources.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (tts != null) tts.destroy();
    }
}
