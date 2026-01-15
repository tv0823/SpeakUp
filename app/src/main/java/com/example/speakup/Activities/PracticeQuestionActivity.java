package com.example.speakup.Activities;

import static com.example.speakup.Prompts.PERSONAL_PROMPT;
import static com.example.speakup.Prompts.PROJECT_PROMPT;
import static com.example.speakup.Prompts.VIDEO_CLIPS_PROMPT;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.speakup.GeminiCallback;
import com.example.speakup.GeminiManager;
import com.example.speakup.Objects.Question;
import com.example.speakup.R;
import com.example.speakup.TtsHelper;
import com.example.speakup.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Activity for practicing a specific question.
 * <p>
 * This activity displays question details (topic, key points) and allows the user to:
 * <ul>
 *     <li>Listen to the question text via TTS.</li>
 *     <li>Record their answer in chunks (supports pause/resume).</li>
 *     <li>Visualize the recording progress and pause locations.</li>
 *     <li>Play back the recorded answer.</li>
 *     <li>Delete and restart the recording.</li>
 * </ul>
 * </p>
 */
public class PracticeQuestionActivity extends Utilities {
    /**
     * Permission request code for audio recording.
     */
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // UI Components
    private TextView subTopicTitleTv, keyPointsTv, currentTimeTv, totalTimeTv, recordedTimeTv;
    private SeekBar ttsSeekBar, recordingSeekBar;
    private ImageButton playPauseBtn, recordBtn, playRecordingBtn, deleteRecordingBtn;
    private FrameLayout linesContainer;

    // Logic Helpers
    private Handler timerHandler, recordingTimerHandler;
    private Question question;
    private TtsHelper tts;

    // State Variables
    private int currentProgress = 0, totalSeconds = 0, maxProgress = 0, recordedSeconds = 0;
    private int pauseTimeInSeconds = -1;

    private boolean isRecording = false;
    private boolean isPaused = false;
    private boolean hasPausedOnce = false;
    private boolean isFinalized = false;

    // Media Objects
    private MediaRecorder recorder;
    private MediaPlayer mediaPlayer;
    private String finalFileName;
    private ArrayList<File> audioChunks = new ArrayList<>();
    private int chunkCount = 0;

    private GeminiManager geminiManager;

    /**
     * Runnable to update the recording timer and seek bar every second.
     */
    private final Runnable recordingTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                recordedSeconds++;
                updateRecordedTimeLabel();
                recordingSeekBar.setMax(recordedSeconds);
                recordingSeekBar.setProgress(recordedSeconds);
                updateLinePosition();
                recordingTimerHandler.postDelayed(this, 1000);
            }
        }
    };

    /**
     * Runnable to update the TTS playback timer and seek bar.
     */
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (tts != null && tts.isSpeaking()) {
                currentProgress++;
                ttsSeekBar.setProgress(currentProgress);
                updateTimeLabels();
                timerHandler.postDelayed(this, 100);
            }
        }
    };

    /**
     * Called when the activity is starting.
     * Initializes views, logic, intent data, and permissions.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_question);
        initViews();
        initLogic();
        handleIntent();
        setupBackPress();
        checkPermissions();
    }

    /**
     * Initializes all UI views and sets up initial listeners.
     */
    private void initViews() {
        subTopicTitleTv = findViewById(R.id.subTopicTitleTv);
        keyPointsTv = findViewById(R.id.keyPointsTv);
        currentTimeTv = findViewById(R.id.currentTimeTv);
        totalTimeTv = findViewById(R.id.totalTimeTv);
        recordedTimeTv = findViewById(R.id.recordedTimeTv);
        ttsSeekBar = findViewById(R.id.ttsSeekBar);
        recordingSeekBar = findViewById(R.id.recordingSeekBar);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        recordBtn = findViewById(R.id.recordBtn);
        playRecordingBtn = findViewById(R.id.playRecordingBtn);
        deleteRecordingBtn = findViewById(R.id.deleteRecordingBtn);
        linesContainer = findViewById(R.id.linesContainer);

        // Recording SeekBar is for display only
        recordingSeekBar.setOnTouchListener((v, event) -> true);
        recordingSeekBar.setAlpha(1.0f);
        recordingSeekBar.setMax(0);
        recordingSeekBar.setProgress(0);

        deleteRecordingBtn.setOnClickListener(v -> confirmDeleteRecording());

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
                if (tts.isSpeaking()) {
                    tts.stop();
                    timerHandler.removeCallbacks(timerRunnable);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // If the user was playing, we might want to resume, but typical behavior is stay paused
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
            }
        });
    }

    /**
     * Initializes logic components like Handlers and TTS helper.
     * Sets up the file path for the final merged recording.
     */
    private void initLogic() {
        timerHandler = new Handler();
        recordingTimerHandler = new Handler();
        tts = new TtsHelper(this);
        // Saved as .aac because ADTS format is raw AAC
        finalFileName = getExternalCacheDir().getAbsolutePath() + "/final_record.aac";
        geminiManager = GeminiManager.getInstance();

        tts.setUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    timerHandler.removeCallbacks(timerRunnable);
                    currentProgress = maxProgress;
                    ttsSeekBar.setProgress(maxProgress);
                    updateTimeLabels();
                    playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
                });
            }

            @Override
            public void onError(String utteranceId) {}
        });
    }

    /**
     * Handles the record button click.
     * Manages state transitions: Start -> Pause -> Resume -> Stop (Finalize).
     *
     * @param view The view that was clicked.
     */
    public void recordBtn(View view) {
        if (isFinalized) {
            Toast.makeText(this, "Recording finished. Delete to restart.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isRecording && !isPaused && !hasPausedOnce) {
            // Case 1: Initial Start - Start recording the first chunk
            startRecordingChunk();
            recordBtn.setImageResource(android.R.drawable.ic_media_pause);
        } else if (isRecording && !hasPausedOnce) {
            // Case 2: First Pause - Stop current chunk, mark position, and enter paused state
            stopRecordingChunk();
            addPauseLine();
            hasPausedOnce = true;
            isPaused = true;
            recordBtn.setImageResource(android.R.drawable.ic_btn_speak_now);
        } else if (isPaused) {
            // Case 3: Resume - Start recording a new chunk
            startRecordingChunk();
            isPaused = false;
            recordBtn.setImageResource(android.R.drawable.ic_media_pause);
        } else if (isRecording && hasPausedOnce) {
            // Case 4: Final Stop - Stop recording, merge all chunks, and finalize
            stopRecordingChunk();
            mergeChunks(); // Creates the one final file
            isFinalized = true;
            recordBtn.setImageResource(android.R.drawable.ic_btn_speak_now);
        }
    }

    /**
     * Starts recording a new chunk of audio.
     */
    private void startRecordingChunk() {
        isRecording = true;
        chunkCount++;
        File chunkFile = new File(getExternalCacheDir(), "chunk_" + chunkCount + ".aac");
        audioChunks.add(chunkFile);

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // CRITICAL: AAC_ADTS allows for appending files via byte streams because it includes headers
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioSamplingRate(44100);
        recorder.setOutputFile(chunkFile.getAbsolutePath());

        try {
            recorder.prepare();
            recorder.start();
            recordingTimerHandler.postDelayed(recordingTimerRunnable, 100);
        } catch (IOException e) {
            Log.e("Recorder", "Start failed", e);
        }
    }

    /**
     * Stops the current recording chunk and releases the recorder.
     */
    private void stopRecordingChunk() {
        if (recorder != null) {
            try {
                recorder.stop();
                recorder.release();
            } catch (Exception e) {
                Log.e("Recorder", "Stop failed", e);
            }
            recorder = null;
        }
        recordingTimerHandler.removeCallbacks(recordingTimerRunnable);
        isRecording = false;
    }

    /**
     * Merges all recorded audio chunks into a single final AAC file.
     * Reads bytes from each chunk file and writes them sequentially to the final file.
     */
    private void mergeChunks() {
        File finalFile = new File(finalFileName);
        // Automatic resource management ensures 'fos' and 'fis' close
        try (FileOutputStream fos = new FileOutputStream(finalFile)) {
            for (File chunk : audioChunks) {
                if (chunk.exists()) {
                    try (FileInputStream fis = new FileInputStream(chunk)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = fis.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
            fos.flush();
        } catch (IOException e) {
            Log.e("Merge", "Failed to merge", e);
        }
    }

    /**
     * Plays the recorded audio.
     * Can play the finalized file or the first chunk if paused.
     *
     * @param view The view that was clicked.
     */
    public void playRecording(View view) {
        if (isRecording) {
            Toast.makeText(this, "Stop recording first", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileToPlayPath = "";
        if (isFinalized) {
            fileToPlayPath = finalFileName;
        } else if (isPaused && !audioChunks.isEmpty()) {
            // While paused, play the first part to let user hear what they have so far
            fileToPlayPath = audioChunks.get(0).getAbsolutePath();
        }

        if (fileToPlayPath.isEmpty() || !new File(fileToPlayPath).exists()) {
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playRecordingBtn.setImageResource(android.R.drawable.ic_media_play);
        } else if (mediaPlayer != null) {
            mediaPlayer.start();
            playRecordingBtn.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(fileToPlayPath);
                mediaPlayer.prepare();
                mediaPlayer.start();
                playRecordingBtn.setImageResource(android.R.drawable.ic_media_pause);
                mediaPlayer.setOnCompletionListener(mp -> {
                    playRecordingBtn.setImageResource(android.R.drawable.ic_media_play);
                    mediaPlayer.release();
                    mediaPlayer = null;
                });
            } catch (IOException e) {
                Log.e("Player", "Play failed", e);
            }
        }
    }

    /**
     * Shows a confirmation dialog before deleting the current recording.
     */
    private void confirmDeleteRecording() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("Delete Recording?");
        adb.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
                stopRecordingChunk();
                clearAllFiles();
                resetUI();
            }
        });
        adb.setNegativeButton("Cancel", null);
        adb.show();
    }

    /**
     * Deletes the final recording file and all temporary chunks.
     */
    private void clearAllFiles() {
        new File(finalFileName).delete();
        for (File chunk : audioChunks) { if (chunk.exists()) chunk.delete(); }
        audioChunks.clear();
        chunkCount = 0;
    }

    /**
     * Resets the UI and state variables to their initial state.
     */
    private void resetUI() {
        isFinalized = false;
        isPaused = false;
        hasPausedOnce = false;
        pauseTimeInSeconds = -1;
        recordedSeconds = 0;
        recordingSeekBar.setMax(0);
        recordingSeekBar.setProgress(0);
        updateRecordedTimeLabel();
        recordBtn.setImageResource(android.R.drawable.ic_btn_speak_now);
        playRecordingBtn.setImageResource(android.R.drawable.ic_media_play);
        linesContainer.removeAllViews();
    }

    /**
     * Adds a visual marker (line) on the seek bar to indicate where a pause occurred.
     */
    private void addPauseLine() {
        pauseTimeInSeconds = recordedSeconds;
        View pauseLine = new View(this);
        pauseLine.setTag("pause_marker");
        pauseLine.setBackgroundColor(Color.parseColor("#9CA3AF"));
        float density = getResources().getDisplayMetrics().density;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) (2 * density), (int) (28 * density));
        params.gravity = android.view.Gravity.CENTER_VERTICAL;
        pauseLine.setLayoutParams(params);
        linesContainer.addView(pauseLine);
        updateLinePosition();
    }

    /**
     * Updates the position of the pause marker based on the current recording progress.
     * Calculates relative position on the SeekBar.
     */
    private void updateLinePosition() {
        if (pauseTimeInSeconds == -1) return;
        linesContainer.post(() -> {
            int maxVal = recordingSeekBar.getMax();
            int width = linesContainer.getWidth();
            if (maxVal > 0) {
                View line = linesContainer.findViewWithTag("pause_marker");
                if (line != null) {
                    float xPos = ((float) pauseTimeInSeconds / maxVal) * width;
                    line.setTranslationX(xPos);
                }
            }
        });
    }

    /**
     * Shows a confirmation dialog when attempting to exit with an active recording.
     */
    private void showExitDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("Exit Practice?");
        adb.setMessage("Your recording will be deleted.");
        adb.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        adb.setNegativeButton("Stay", null).show();
    }

    /**
     * Toggles play/pause for the TTS engine.
     *
     * @param view The view that was clicked.
     */
    public void playOrPause(View view) {
        if (tts.isSpeaking()) {
            tts.stop();
            timerHandler.removeCallbacks(timerRunnable);
            playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
        } else {
            // If we are at the end, reset to start
            if (currentProgress >= maxProgress) {
                currentProgress = 0;
                ttsSeekBar.setProgress(0);
                updateTimeLabels();
            }
            float pct = (float) ttsSeekBar.getProgress() / ttsSeekBar.getMax();
            tts.speakFromPercentage(question.getFullQuestion(), pct);
            playPauseBtn.setImageResource(android.R.drawable.ic_media_pause);
            timerHandler.postDelayed(timerRunnable, 100);
        }
    }

    /**
     * Updates the TTS playback timer labels.
     */
    private void updateTimeLabels() {
        int sec = currentProgress / 10;
        currentTimeTv.setText(String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60));
        totalTimeTv.setText(String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60));
    }

    /**
     * Updates the recording timer label.
     */
    private void updateRecordedTimeLabel() {
        recordedTimeTv.setText(String.format(Locale.getDefault(), "%02d:%02d", recordedSeconds / 60, recordedSeconds % 60));
    }

    /**
     * Retrieves the Question object from the intent.
     */
    private void handleIntent() {
        Intent gi = getIntent();
        question = (Question) gi.getSerializableExtra("question");
        if (question != null) setupUIWithQuestionData();
    }

    /**
     * Populates UI fields with data from the Question object.
     */
    private void setupUIWithQuestionData() {
        subTopicTitleTv.setText(question.getSubTopic().equals("null") ? question.getTopic() : question.getSubTopic());
        keyPointsTv.setText(question.getBriefQuestion().replace("\\n", "\n"));
        totalSeconds = Math.max(question.getFullQuestion().length() / 15, 1);
        maxProgress = totalSeconds * 10;
        ttsSeekBar.setMax(maxProgress);
        updateTimeLabels();
    }

    /**
     * Checks and requests audio recording permissions if not granted.
     */
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    /**
     * Sets up the custom back press behavior to warn about deleting recordings.
     */
    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (recordedSeconds > 0) showExitDialog();
                else finish();
            }
        });
    }

    /**
     * Handles the "Check answer" button click.
     * Starts the process of analyzing the recorded answer.
     *
     * @param view The view that was clicked.
     */
    public void checkRecording(View view) {
        if (recordedSeconds > 0 && isPaused) {
            if (isRecording) {
                stopRecordingChunk();
            }
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("Check answer");
            adb.setMessage("Do you want to proceed to check your answer?");
            adb.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String filePath = "";
                    if (isFinalized) {
                        filePath = finalFileName;
                    } else if (isPaused && !audioChunks.isEmpty()) {
                        filePath = audioChunks.get(0).getAbsolutePath();
                    }

                    byte[] bytes = null;
                    try {
                        bytes = getBytes(filePath);
                    } catch (IOException e) {
                        Log.e("PracticeQuestion", "Error reading file for checkRecording", e);
                        Toast.makeText(PracticeQuestionActivity.this, "Error reading recording file", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ProgressDialog pD = new ProgressDialog(PracticeQuestionActivity.this);
                    pD.setTitle("Analyzing answer...");
                    pD.setMessage("Waiting for response...");
                    pD.setCancelable(false);
                    pD.show();

                    // Logic for sending to Gemini...
                    String prompt = "";
                    switch (question.getCategory()) {
                        case "Personal Questions":
                            prompt = PERSONAL_PROMPT;
                            break;
                        case "Project Presentation":
                            prompt = PROJECT_PROMPT;
                            break;
                        case "Video Clip Questions":
                            prompt = VIDEO_CLIPS_PROMPT;
                            break;
                    }
                    prompt += question.getFullQuestion();
                    String mimeType = "audio/aac";

                    geminiManager.sendTextWithFilePrompt(prompt, bytes, mimeType, new GeminiCallback() {
                        @Override
                        public void onSuccess(String result) {
                            pD.dismiss();
                            keyPointsTv.setText(result);
                        }

                        @Override
                        public void onFailure(Throwable error) {
                            pD.dismiss();
                            Toast.makeText(PracticeQuestionActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            adb.setNegativeButton("Cancel", null);
            adb.show();
        } else {
            Toast.makeText(this, "You need to record your answer.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Reads the recording file and converts it to a byte array.
     *
     * @param filePath The path to the audio file.
     * @return The byte array representation of the file.
     * @throws IOException If an error occurs during file reading.
     */
    private byte[] getBytes(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(bytes);
        }
        return bytes;
    }

    /**
     * Handles the back button click event from the UI.
     *
     * @param v The view that was clicked.
     */
    public void goBack(View v) {
        if (recordedSeconds > 0) showExitDialog();
        else { clearAllFiles(); finish(); }
    }

    /**
     * Called when the activity is destroyed.
     * Cleans up resources (recorder, player, TTS, temporary files).
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecordingChunk();
        clearAllFiles();
        if (tts != null) tts.destroy();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
    }
}
