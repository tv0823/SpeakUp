package com.example.speakup.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.speakup.Objects.Question;
import com.example.speakup.R;
import com.example.speakup.TtsHelper;
import com.example.speakup.Utilities;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
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
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

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

    private Button recordBtn;

    private Button relistenBtn;

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

    private MediaRecorder recorder;
    private MediaPlayer player;
    private String fileName;

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
        recordBtn = findViewById(R.id.recordBtn);
        relistenBtn = findViewById(R.id.relistenBtn);

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                AlertDialog.Builder adb = new AlertDialog.Builder(PracticeQuestionActivity.this);
                adb.setCancelable(false);
                adb.setTitle("Leave practice");
                adb.setMessage("Are you sure you want to leave the practice?\nOnce you leave the practice you can't recover the recording.");
                adb.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        finish();
                    }
                });
                adb.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog ad = adb.create();
                ad.show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Microphone permission denied. Cannot record.", Toast.LENGTH_LONG).show();
                recordBtn.setEnabled(false);
            }
        }
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

    public void recordBtn(View view) {
        if (recorder == null && fileName != null) {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setCancelable(false);
            adb.setTitle("Delete Recording?");
            adb.setMessage("Are you sure you want to delete this recording?\nOnce you delete the recording you can't recover it.");
            adb.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    startRecording();
                    recordBtn.setText("Click to stop recording");
                    recordBtn.setBackgroundColor(Color.parseColor("#d65c54"));
                }
            });
            adb.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog ad = adb.create();
            ad.show();
        }
        else if (recorder == null) {
            startRecording();
            recordBtn.setText("Click to stop recording");
            recordBtn.setBackgroundColor(Color.parseColor("#d65c54"));
        } else {
            stopRecording();
            recordBtn.setText("Record");
            recordBtn.setBackgroundColor(Color.parseColor("#13A4EC"));
        }
    }

    private void startRecording() {
        fileName = getExternalCacheDir().getAbsolutePath() + "/audiorecord.mp3";

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // Better container for high quality
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);    // High-quality modern codec

        recorder.setAudioSamplingRate(44100);
        recorder.setAudioEncodingBitRate(128000); // 128 kbps (Clear voice)
        recorder.setAudioChannels(1);

        recorder.setOutputFile(fileName);

        try {
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    public void reListenToRecording(View view) {
        if (recorder == null) {
            if (player == null && fileName != null) {
                startPlaying();
                relistenBtn.setText("Stop");
                relistenBtn.setTextColor(Color.parseColor("#ffffff"));
                relistenBtn.setBackgroundColor(Color.parseColor("#d65c54"));
            } else {
                stopPlaying();
                relistenBtn.setText("Re-listen");
                relistenBtn.setTextColor(Color.parseColor("#000000"));
                relistenBtn.setBackgroundColor(Color.parseColor("#F1F3F4"));
            }
        }
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare(); // Buffers the file
            player.start();   // Begins playback
            player.setOnCompletionListener(mp -> {
                relistenBtn.setText("Re-listen");
                relistenBtn.setTextColor(Color.parseColor("#000000"));
                relistenBtn.setBackgroundColor(Color.parseColor("#F1F3F4"));
            });
        } catch (IOException e) {
            Log.e("AudioPlay", "prepare() failed");
        }
    }

    private void stopPlaying() {
        player.release();
        player = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
        }
        if (player != null) {
            player.release();
        }
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
