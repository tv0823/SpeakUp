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

public class PracticeQuestionActivity extends Utilities {
    private TextView subTopicTitleTv, keyPointsTv, currentTimeTv, totalTimeTv;
    private SeekBar ttsSeekBar;
    private ImageButton playPauseBtn;
    private Handler timerHandler;

    private int currentProgress = 0;
    private int totalSeconds = 0;
    private int maxProgress = 0;
    private static final int TICK_INTERVAL = 100;

    private Question question;
    private TtsHelper tts;

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
     * Replaces the old Firebase fetch logic.
     * Sets the text and calculates the TTS duration.
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
        if (tts != null) tts.destroy();
    }
}