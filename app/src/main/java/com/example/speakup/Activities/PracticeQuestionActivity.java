package com.example.speakup.Activities;

import static com.example.speakup.Utils.FBRef.refAuth;
import static com.example.speakup.Utils.FBRef.refRecordings;
import static com.example.speakup.Utils.FBRef.refRecordingsMedia;
import static com.example.speakup.Utils.Prompts.PERSONAL_PROMPT;
import static com.example.speakup.Utils.Prompts.PROJECT_PROMPT;
import static com.example.speakup.Utils.Prompts.VIDEO_CLIPS_PROMPT;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.speakup.GeminiCallback;
import com.example.speakup.GeminiManager;
import com.example.speakup.Objects.Question;
import com.example.speakup.Objects.Recording;
import com.example.speakup.Objects.TopicDetail;
import com.example.speakup.R;
import com.example.speakup.RecordingManager;
import com.example.speakup.TtsHelper;
import com.example.speakup.Utils.Utilities;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
/**
 * Activity responsible for handling a full speaking practice session.
 *
 * <p>This activity allows the user to:
 * <ul>
 * <li>Listen to a question using Text-to-Speech (TTS)</li>
 * <li>Record their spoken answer</li>
 * <li>Pause and resume recordings</li>
 * <li>Play back their recording</li>
 * <li>Send the recording to AI (Gemini) for evaluation</li>
 * <li>Upload results and audio to Firebase</li>
 * </ul>
 *
 * <p>It also supports video-based questions via YouTube integration.
 *
 * <p>Main components:
 * <ul>
 * <li>{@link TtsHelper} - Handles text-to-speech playback</li>
 * <li>{@link RecordingManager} - Manages audio recording lifecycle</li>
 * <li>{@link MediaPlayer} - Plays recorded audio</li>
 * <li>{@link GeminiManager} - Sends audio for AI evaluation</li>
 * </ul>
 *
 */
public class PracticeQuestionActivity extends Utilities {
    /** Request code for microphone permission */
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // UI Components
    private TextView subTopicTitleTv, currentTimeTv, totalTimeTv, recordedTimeTv;
    private SeekBar ttsSeekBar, recordingSeekBar;
    private ImageButton playPauseBtn, recordBtn, playRecordingBtn, deleteRecordingBtn;
    private FrameLayout linesContainer;
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer activeYouTubePlayer;

    // Logic & helpers
    private Handler recordingTimerHandler;
    private Question question;
    private TtsHelper tts;
    private RecordingManager recordingManager;
    private MediaPlayer mediaPlayer;

    // State variables
    private int currentProgress = 0, maxProgress = 0, recordedSeconds = 0;
    private int pauseTimeInSeconds = -1;
    private int ttsOffset = 0; // The character index where the current utterance started

    /**
     * Runnable that updates recording duration every second while recording.
     */
    private final Runnable recordingTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (recordingManager != null && recordingManager.isRecording()) {
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
     * Runnable that updates playback UI while audio is playing.
     */
    private final Runnable playbackUpdaterRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int currentPos = mediaPlayer.getCurrentPosition() / 1000;
                recordingSeekBar.setProgress(currentPos);
                recordedTimeTv.setText(String.format(Locale.getDefault(), "%02d:%02d", currentPos / 60, currentPos % 60));
                recordingTimerHandler.postDelayed(this, 500);
            }
        }
    };

    /**
     * Initializes activity, UI, and logic.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_question);

        initViews();
        handleIntent();

        if (question == null) {
            Toast.makeText(this, "Error: Question data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initLogic();
        setupBackPress();
        checkPermissions();
    }

    /**
     * Initializes all UI components and listeners.
     */
    private void initViews() {
        subTopicTitleTv = findViewById(R.id.subTopicTitleTv);
        currentTimeTv = findViewById(R.id.currentTimeTv);
        totalTimeTv = findViewById(R.id.totalTimeTv);
        recordedTimeTv = findViewById(R.id.recordedTimeTv);
        ttsSeekBar = findViewById(R.id.ttsSeekBar);
        recordingSeekBar = findViewById(R.id.recordingSeekBar);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        playPauseBtn.setEnabled(false);
        playPauseBtn.setAlpha(0.5f); // Visual indication that it is disabled
        recordBtn = findViewById(R.id.recordBtn);
        playRecordingBtn = findViewById(R.id.playRecordingBtn);
        deleteRecordingBtn = findViewById(R.id.deleteRecordingBtn);
        linesContainer = findViewById(R.id.linesContainer);
        youTubePlayerView = findViewById(R.id.youtube_player_view);

        recordingSeekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return recordingManager != null && recordingManager.isRecording();
            }
        });

        recordingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null)
                    mediaPlayer.seekTo(progress * 1000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

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
                    playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ttsOffset = seekBar.getProgress();
            }
        });

        deleteRecordingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeleteRecording();
            }
        });
    }

    /**
     * Initializes core logic such as TTS and recording manager.
     */
    private void initLogic() {
        recordingTimerHandler = new Handler();
        tts = new TtsHelper(this);
        tts.setTtsInitListener(new TtsHelper.TtsInitListener() {
            @Override
            public void onInitStatus(final boolean success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (success) {
                            playPauseBtn.setEnabled(true);
                            playPauseBtn.setAlpha(1.0f);
                        }
                    }
                });
            }
        });
        recordingManager = new RecordingManager(this, "practice_" + question.getQuestionId() + ".aac");

        tts.setUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentProgress = maxProgress;
                        ttsSeekBar.setProgress(maxProgress);
                        updateTimeLabels();
                        playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
                    }
                });
            }

            @Override
            public void onError(String utteranceId) {
            }

            @Override
            public void onRangeStart(String utteranceId, int start, int end, int frame) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentProgress = ttsOffset + start;
                        ttsSeekBar.setProgress(currentProgress);
                        updateTimeLabels();
                    }
                });
            }
        });
    }

    /**
     * Handles record button behavior including start, pause, resume and finalize.
     *
     * @param view The clicked view
     */
    public void recordBtn(View view) {
        if (recordingManager.isFinalized()) {
            Toast.makeText(this, "Recording finished. Delete to restart.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!recordingManager.isRecording() && !recordingManager.isPaused() && !recordingManager.hasPausedOnce()) {
            recordingManager.startRecordingChunk();
            recordBtn.setImageResource(android.R.drawable.ic_media_pause);
            recordingTimerHandler.postDelayed(recordingTimerRunnable, 100);
        } else if (recordingManager.isRecording() && !recordingManager.hasPausedOnce()) {
            recordingManager.stopRecordingChunk();
            addPauseLine();
            recordingManager.setHasPausedOnce(true);
            recordingManager.setPaused(true);
            recordBtn.setImageResource(android.R.drawable.ic_btn_speak_now);
            recordingTimerHandler.removeCallbacks(recordingTimerRunnable);
        } else if (recordingManager.isPaused()) {
            recordingManager.startRecordingChunk();
            recordingManager.setPaused(false);
            recordBtn.setImageResource(android.R.drawable.ic_media_pause);
            recordingTimerHandler.postDelayed(recordingTimerRunnable, 100);
        } else if (recordingManager.isRecording() && recordingManager.hasPausedOnce()) {
            recordingManager.stopRecordingChunk();
            recordingManager.mergeChunks();
            recordingManager.setFinalized(true);
            recordBtn.setImageResource(android.R.drawable.ic_btn_speak_now);
            recordingTimerHandler.removeCallbacks(recordingTimerRunnable);
        }
    }

    /**
     * Plays or pauses the recorded audio.
     *
     * @param view The clicked view
     */
    public void playRecording(View view) {
        if (recordingManager.isRecording()) {
            Toast.makeText(this, "Stop recording first", Toast.LENGTH_SHORT).show();
            return;
        }

        String path = recordingManager.getFinalFilePath();
        if (path == null) {
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playRecordingBtn.setImageResource(android.R.drawable.ic_media_play);
            recordingTimerHandler.removeCallbacks(playbackUpdaterRunnable);
        } else if (mediaPlayer != null) {
            // Stop other sources
            if (tts != null && tts.isSpeaking()) {
                tts.stop();
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
            }
            if (activeYouTubePlayer != null) {
                activeYouTubePlayer.pause();
            }

            mediaPlayer.start();
            playRecordingBtn.setImageResource(android.R.drawable.ic_media_pause);
            recordingTimerHandler.post(playbackUpdaterRunnable);
        } else {
            // Stop other sources
            if (tts != null && tts.isSpeaking()) {
                tts.stop();
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
            }
            if (activeYouTubePlayer != null) {
                activeYouTubePlayer.pause();
            }

            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(path);
                mediaPlayer.prepare();
                recordingSeekBar.setMax(mediaPlayer.getDuration() / 1000);
                mediaPlayer.start();
                playRecordingBtn.setImageResource(android.R.drawable.ic_media_pause);
                recordingTimerHandler.post(playbackUpdaterRunnable);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        playRecordingBtn.setImageResource(android.R.drawable.ic_media_play);
                        recordingTimerHandler.removeCallbacks(playbackUpdaterRunnable);
                        recordingSeekBar.setProgress(recordingSeekBar.getMax());
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                });
            } catch (IOException e) {
                Log.e("Player", "Play failed", e);
            }
        }
    }

    /**
     * Shows confirmation dialog before deleting recording.
     */
    private void confirmDeleteRecording() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recording?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mediaPlayer != null) {
                            mediaPlayer.release();
                            mediaPlayer = null;
                        }
                        recordingManager.clearAllFiles();
                        resetUI();
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    /**
     * Resets UI and recording state after deletion.
     */
    private void resetUI() {
        recordedSeconds = 0;
        pauseTimeInSeconds = -1;
        recordingSeekBar.setMax(0);
        recordingSeekBar.setProgress(0);
        updateRecordedTimeLabel();
        recordBtn.setImageResource(android.R.drawable.ic_btn_speak_now);
        playRecordingBtn.setImageResource(android.R.drawable.ic_media_play);
        linesContainer.removeAllViews();
    }

    /**
     * Adds a visual marker where recording was paused.
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
     * Updates position of pause marker on the timeline.
     */
    private void updateLinePosition() {
        if (pauseTimeInSeconds == -1)
            return;
        linesContainer.post(new Runnable() {
            @Override
            public void run() {
                int maxVal = recordingSeekBar.getMax();
                int width = linesContainer.getWidth();
                if (maxVal > 0) {
                    View line = linesContainer.findViewWithTag("pause_marker");
                    if (line != null) {
                        float xPos = ((float) pauseTimeInSeconds / maxVal) * width;
                        line.setTranslationX(xPos);
                    }
                }
            }
        });
    }

    /**
     * Controls TTS playback (play/pause).
     *
     * @param view The clicked view
     */
    public void playOrPause(View view) {
        if (!tts.isInitialized()) {
            Toast.makeText(this, "TTS is still initializing...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tts.isSpeaking()) {
            tts.stop();
            playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
        } else {
            // Stop other sources
            if (activeYouTubePlayer != null) {
                activeYouTubePlayer.pause();
            }
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                playRecordingBtn.setImageResource(android.R.drawable.ic_media_play);
                recordingTimerHandler.removeCallbacks(playbackUpdaterRunnable);
            }

            if (currentProgress >= maxProgress) {
                currentProgress = 0;
                ttsSeekBar.setProgress(0);
            }
            ttsOffset = ttsSeekBar.getProgress();
            tts.speakFromIndex(question.getFullQuestion(), ttsOffset);
            playPauseBtn.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    /**
     * Updates TTS time labels.
     */
    private void updateTimeLabels() {
        // Estimate 15 characters per second for time display
        int totalSec = maxProgress / 15;
        int currentSec = currentProgress / 15;
        currentTimeTv.setText(String.format(Locale.getDefault(), "%02d:%02d", currentSec / 60, currentSec % 60));
        totalTimeTv.setText(String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60));
    }

    /**
     * Updates recorded time label.
     */
    private void updateRecordedTimeLabel() {
        recordedTimeTv
                .setText(String.format(Locale.getDefault(), "%02d:%02d", recordedSeconds / 60, recordedSeconds % 60));
    }

    /**
     * Extracts question object from intent.
     */
    private void handleIntent() {
        question = (Question) getIntent().getSerializableExtra("question");
        if (question != null)
            setupUIWithQuestionData();
    }

    /**
     * Populates UI with question data.
     */
    private void setupUIWithQuestionData() {
        if (subTopicTitleTv != null) {
            subTopicTitleTv
                    .setText(question.getSubTopic().equals("null") ? question.getTopic() : question.getSubTopic());
        }
        maxProgress = question.getFullQuestion().length();
        if (ttsSeekBar != null)
            ttsSeekBar.setMax(maxProgress);
        updateTimeLabels();

        if ("Video Clip Questions".equals(question.getCategory())
                && question.getVideoUrl() != null
                && !question.getVideoUrl().equals("null")
                && !question.getVideoUrl().isEmpty()) {

            youTubePlayerView.setVisibility(View.VISIBLE);
            setupYouTubePlayer(question.getVideoUrl());
            View space = findViewById(R.id.videoAudioSpace);
            if (space != null)
                space.setVisibility(View.VISIBLE);
        } else {
            youTubePlayerView.setVisibility(View.GONE);
            View space = findViewById(R.id.videoAudioSpace);
            if (space != null)
                space.setVisibility(View.GONE);
        }
    }

    /**
     * Requests audio recording permission if not granted.
     */
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO },
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    /**
     * Handles back press with confirmation if recording exists.
     */
    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        });
    }

    /**
     * Shows exit confirmation dialog.
     */
    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Practice?")
                .setMessage(recordedSeconds > 0 ? "Your recording will be deleted." : "Are you sure you want to leave?")
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (recordingManager != null)
                            recordingManager.clearAllFiles();
                        finish();
                    }
                })
                .setNegativeButton("Stay", null).show();
    }

    /**
     * Navigates back with validation.
     *
     * @param v The clicked view
     */
    public void goBack(View v) {
        showExitDialog();
    }

    /**
     * Sends recording to AI for evaluation.
     *
     * @param view The clicked view
     */
    public void checkRecording(View view) {
        if (recordedSeconds > 0 && (recordingManager.isPaused() || recordingManager.isFinalized())) {
            if (recordingManager.isRecording()) {
                recordingManager.stopRecordingChunk();
            }

            new AlertDialog.Builder(this)
                    .setTitle("Check answer")
                    .setMessage("Do you want to proceed to check your answer?")
                    .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String filePath = recordingManager.getFinalFilePath();

                            byte[] bytes;
                            try {
                                bytes = recordingManager.getBytes(filePath);
                            } catch (IOException e) {
                                Log.e("PracticeQuestion", "Error reading file", e);
                                Toast.makeText(PracticeQuestionActivity.this, "Error reading recording file",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            ProgressDialog pD = new ProgressDialog(PracticeQuestionActivity.this);
                            pD.setTitle("Analyzing answer...");
                            pD.setMessage("Waiting for response...");
                            pD.setCancelable(false);
                            pD.show();

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

                            GeminiManager.getInstance().sendTextWithFilePrompt(prompt, bytes, "audio/aac",
                                    new GeminiCallback() {
                                        @Override
                                        public void onSuccess(String result) {
                                            pD.dismiss();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    createRecordingToFirebase(result);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(Throwable error) {
                                            pD.dismiss();
                                            Toast.makeText(PracticeQuestionActivity.this,
                                                    "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    })
                    .setNegativeButton("Cancel", null).show();
        } else {
            Toast.makeText(this, "You need to record your answer.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Parses AI JSON response and prepares feedback.
     *
     * @param json AI response
     */
    private void createRecordingToFirebase(String json) {
        Map<String, TopicDetail> aiFeedBack = new HashMap<>();
        try {
            String cleanedJson = json.replaceAll("```json", "").replaceAll("```", "").trim();
            JSONObject root = new JSONObject(cleanedJson);

            int topicDevelopmentScore = root.getInt("topicDevelopment");
            int deliveryScore = root.getInt("delivery");
            int vocabularyScore = root.getInt("vocabulary");
            int languageScore = root.getInt("language");
            int totalScore = root.getInt("totalSectionScore");

            JSONObject feedback = root.getJSONObject("feedback");

            aiFeedBack.put("topicDevelopment", new TopicDetail(topicDevelopmentScore,
                    parseFeedbackSection(feedback.getJSONObject("topicDevelopment"))));
            aiFeedBack.put("delivery",
                    new TopicDetail(deliveryScore, parseFeedbackSection(feedback.getJSONObject("delivery"))));
            aiFeedBack.put("vocabulary",
                    new TopicDetail(vocabularyScore, parseFeedbackSection(feedback.getJSONObject("vocabulary"))));
            aiFeedBack.put("language",
                    new TopicDetail(languageScore, parseFeedbackSection(feedback.getJSONObject("language"))));
            aiFeedBack.put("overall", new TopicDetail(totalScore, feedback.getString("overallSummary")));

            showNamingDialog(aiFeedBack, totalScore);
        } catch (JSONException e) {
            Log.e("JSON_ERROR", "Failed to parse: " + json, e);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PracticeQuestionActivity.this, "AI Formatting Error", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Parses a feedback section from JSON.
     *
     * @param section JSON object
     * @return formatted feedback string
     */
    private String parseFeedbackSection(JSONObject section) throws JSONException {
        return "Keep: " + section.getString("keep") + "\nImprove: " + section.getString("improve");
    }

    /**
     * Prompts user to name the recording.
     */
    private void showNamingDialog(final Map<String, TopicDetail> feedback, final int score) {
        final EditText eT = new EditText(this);
        eT.setHint("Enter recording name (e.g. My Practice 1)");
        new AlertDialog.Builder(this)
                .setTitle("Name your recording")
                .setView(eT)
                .setCancelable(false)
                .setPositiveButton("Finish", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        processFirebaseStorageAndDatabase(eT.getText().toString(), feedback, score);
                    }
                })
                .show();
    }

    /**
     * Saves recording metadata and uploads audio.
     */
    private void processFirebaseStorageAndDatabase(final String displayTitle, final Map<String, TopicDetail> feedback,
            final int score) {
        final String userId = refAuth.getCurrentUser().getUid();
        refRecordings.child(userId).child(question.getQuestionId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dS) {
                        String finalTitle = displayTitle;
                        if (finalTitle.isEmpty()) {
                            long count = dS.getChildrenCount() + 1;
                            String base = (question.getSubTopic() == null || question.getSubTopic().equals("null"))
                                    ? question.getTopic()
                                    : question.getSubTopic();
                            finalTitle = base + " " + count;
                        }
                        String recordingId = refRecordings.child(userId).push().getKey();
                        Recording rec = new Recording(userId, question.getQuestionId(), finalTitle, new Date(), score,
                                feedback);
                        rec.setRecordingId(recordingId);
                        uploadAudioFile(rec);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                    }
                });
    }

    /**
     * Uploads audio file to Firebase Storage.
     *
     * @param rec Recording object
     */
    private void uploadAudioFile(final Recording rec) {
        final String filePath = recordingManager.getFinalFilePath();
        StorageReference fileRef = refRecordingsMedia.child(rec.getUserId() + "/" + rec.getRecordingId() + ".aac");

        final ProgressDialog pD = new ProgressDialog(this);
        pD.setCancelable(false);
        pD.setMessage("Saving recording...");
        pD.show();

        fileRef.putFile(Uri.fromFile(new File(filePath)))
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        refRecordings.child(rec.getUserId()).child(rec.getQuestionId()).child(rec.getRecordingId())
                                .setValue(rec).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        pD.dismiss();
                                        Intent si = new Intent(PracticeQuestionActivity.this, ResultsActivity.class);
                                        si.putExtra("recording", rec);
                                        si.putExtra("audio_path", filePath);
                                        startActivity(si);
                                        finish();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pD.dismiss();
                        Toast.makeText(PracticeQuestionActivity.this, "Upload failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Initializes YouTube player for video questions.
     *
     * @param videoUrl YouTube URL
     */
    private void setupYouTubePlayer(String videoUrl) {
        String videoId = extractVideoId(videoUrl);
        if (videoId.isEmpty())
            return;

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                activeYouTubePlayer = youTubePlayer;
                youTubePlayer.cueVideo(videoId, 0f);
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer,
                    @NonNull PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.PLAYING) {
                    // Stop TTS
                    if (tts != null && tts.isSpeaking()) {
                        tts.stop();
                        playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
                    }
                    // Stop Recording Playback
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        playRecordingBtn.setImageResource(android.R.drawable.ic_media_play);
                        recordingTimerHandler.removeCallbacks(playbackUpdaterRunnable);
                    }
                }
            }
        });
    }

    /**
     * Extracts YouTube video ID from URL.
     *
     * @param videoUrl URL string
     * @return video ID
     */
    private String extractVideoId(String videoUrl) {
        if (videoUrl == null || videoUrl.trim().isEmpty())
            return "";
        String videoId = "";
        if (videoUrl.contains("v="))
            videoId = videoUrl.split("v=")[1].split("&")[0];
        else if (videoUrl.contains("youtu.be/"))
            videoId = videoUrl.split("youtu.be/")[1].split("\\?")[0];
        else if (videoUrl.contains("embed/"))
            videoId = videoUrl.split("embed/")[1].split("\\?")[0];
        else if (videoUrl.length() == 11)
            videoId = videoUrl;
        return videoId.trim();
    }

    /**
     * Cleans up resources to prevent memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recordingManager != null)
            recordingManager.release();
        if (tts != null)
            tts.destroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
        recordingTimerHandler.removeCallbacksAndMessages(null);
    }
}
