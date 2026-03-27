package com.example.speakup.Activities;

import static com.example.speakup.Utils.FBRef.refAuth;
import static com.example.speakup.Utils.FBRef.refQuestions;
import static com.example.speakup.Utils.FBRef.refRecordings;
import static com.example.speakup.Utils.FBRef.refRecordingsMedia;
import static com.example.speakup.Utils.FBRef.refSimulations;

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
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.speakup.GeminiCallback;
import com.example.speakup.GeminiManager;
import com.example.speakup.Objects.Question;
import com.example.speakup.Objects.Recording;
import com.example.speakup.Objects.Simulation;
import com.example.speakup.Objects.TopicDetail;
import com.example.speakup.R;
import com.example.speakup.RecordingManager;
import com.example.speakup.TtsHelper;
import com.example.speakup.Utils.Prompts;
import com.example.speakup.Utils.Utilities;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Activity that manages a full exam simulation session.
 * <p>
 * This activity handles the entire simulation lifecycle:
 * <ul>
 *     <li>Random selection of questions (1 Personal, 1 Project, 2 Video).</li>
 *     <li>A 30-minute global countdown timer.</li>
 *     <li>Multi-question navigation and audio recording.</li>
 *     <li>Batch AI analysis of all 4 recordings via Gemini.</li>
 *     <li>Saving simulation results and individual recordings to Firebase.</li>
 * </ul>
 * </p>
 */
public class SimulationsActivity extends Utilities implements View.OnClickListener {

    /**
     * Request code for audio recording permission.
     */
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // UI Components
    private LinearLayout questionsContainer, timerLayout;
    private Button btnNext, btnPrevious, btnFinishSim;
    private TextView tvTimer, questionTitleTv, currentTimeTv, totalTimeTv, recordedTimeTv, playbackSubTitle;
    private SeekBar ttsSeekBar, recordingSeekBar;
    private ImageButton playPauseBtn, recordBtn, playRecordingBtn;
    private FrameLayout linesContainer;
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer activeYouTubePlayer;
    private String currentVideoId;

    // Logic Helpers
    private Handler timerHandler, recordingTimerHandler;
    private TtsHelper tts;
    private CountDownTimer simulationTimer;
    private MediaPlayer mediaPlayer;

    // State Variables
    /**
     * The list of questions selected for this simulation.
     */
    private final ArrayList<Question> questions = new ArrayList<Question>();

    /**
     * Recording managers for each question in the simulation.
     */
    private final ArrayList<RecordingManager> recordingManagers = new ArrayList<RecordingManager>();

    /**
     * Index of the question currently being displayed.
     */
    private int currentQuestionIndex = 0;

    /**
     * The recording manager for the current question.
     */
    private RecordingManager currentRecordingManager;

    private int currentProgress = 0, totalSeconds = 0, maxProgress = 0, recordedSeconds = 0;
    private int pauseTimeInSeconds = -1;
    private boolean isFinishDialogOpen = false;

    /**
     * Total duration allowed for the simulation (30 minutes).
     */
    private static final long SIMULATION_DURATION_MS = 30 * 60 * 1000L;

    /**
     * Remaining time in the simulation.
     */
    private long remainingSimulationMillis = SIMULATION_DURATION_MS;

    /**
     * Initializes the activity, sets the content view, and triggers data loading if auto-start is requested.
     *
     * @param savedInstanceState If non-null, this activity is being re-constructed from a previous saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulations);

        initViews();
        initLogic();
        checkPermissions();

        fetchRandomQuestions();
    }

    /**
     * Initializes all UI components and interaction listeners.
     */
    private void initViews() {
        questionsContainer = findViewById(R.id.questionsContainer);
        timerLayout = findViewById(R.id.timerLayout);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnFinishSim = findViewById(R.id.btnFinishSim);
        tvTimer = findViewById(R.id.tvTimer);
        questionTitleTv = findViewById(R.id.questionTitleTv);
        currentTimeTv = findViewById(R.id.currentTimeTv);
        totalTimeTv = findViewById(R.id.totalTimeTv);
        recordedTimeTv = findViewById(R.id.recordedTimeTv);
        playbackSubTitle = findViewById(R.id.playbackSubTitle);
        ttsSeekBar = findViewById(R.id.ttsSeekBar);
        recordingSeekBar = findViewById(R.id.recordingSeekBar);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        recordBtn = findViewById(R.id.recordBtn);
        playRecordingBtn = findViewById(R.id.playRecordingBtn);
        ImageButton deleteRecordingBtn = findViewById(R.id.deleteRecordingBtn);
        linesContainer = findViewById(R.id.linesContainer);
        youTubePlayerView = findViewById(R.id.youtube_player_view);

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                activeYouTubePlayer = youTubePlayer;
                if (currentVideoId != null) {
                    activeYouTubePlayer.cueVideo(currentVideoId, 0f);
                }
            }
        });

        btnNext.setOnClickListener(this);
        btnPrevious.setOnClickListener(this);
        btnFinishSim.setOnClickListener(this);
        playPauseBtn.setOnClickListener(this);
        recordBtn.setOnClickListener(this);
        playRecordingBtn.setOnClickListener(this);
        deleteRecordingBtn.setOnClickListener(this);

        recordingSeekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return currentRecordingManager != null && currentRecordingManager.isRecording();
            }
        });
        recordingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress * 1000);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /**
     * Initializes core logic helpers including TTS and progress progress handlers.
     */
    private void initLogic() {
        timerHandler = new Handler();
        recordingTimerHandler = new Handler();
        tts = new TtsHelper(this);

        tts.setUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        timerHandler.removeCallbacks(timerRunnable);
                        currentProgress = maxProgress;
                        ttsSeekBar.setProgress(maxProgress);
                        updateTimeLabels();
                        playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
                    }
                });
            }

            @Override
            public void onError(String utteranceId) {}
        });
    }

    /**
     * Fetches a randomized set of 4 questions from Firebase to form a COBE exam simulation.
     * Selects 1 Personal Response, 1 Project Presentation, and 2 questions from a single Video Clip.
     */
    private void fetchRandomQuestions() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Preparing simulation questions...");
        pd.setCancelable(false);
        pd.show();

        refQuestions.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Question> personalList = new ArrayList<>();
                ArrayList<Question> projectList = new ArrayList<>();
                ArrayList<ArrayList<Question>> videoGroups = new ArrayList<>();
                ArrayList<String> videoUrls = new ArrayList<>();

                for (DataSnapshot categorySnap : snapshot.getChildren()) {
                    String category = categorySnap.getKey();

                    for (DataSnapshot topicSnap : categorySnap.getChildren()) {
                        for (DataSnapshot questionSnap : topicSnap.getChildren()) {
                            Question q = questionSnap.getValue(Question.class);
                            if (q == null || q.getQuestionId() == null) continue;

                            String catSafe = (category != null) ? category.trim() : "";

                            if (catSafe.equals("Personal Questions")) {
                                personalList.add(q);
                            } else if (catSafe.equals("Project Questions")) {
                                projectList.add(q);
                            } else if (catSafe.equals("Video Clip Questions")) {
                                groupVideoQuestions(q, videoUrls, videoGroups);
                            }
                        }
                    }
                }

                Random random = new Random();
                questions.clear();

                if (!personalList.isEmpty()) {
                    questions.add(personalList.get(random.nextInt(personalList.size())));
                }

                if (!projectList.isEmpty()) {
                    questions.add(projectList.get(random.nextInt(projectList.size())));
                }

                ArrayList<ArrayList<Question>> eligibleGroups = new ArrayList<>();
                for (ArrayList<Question> group : videoGroups) {
                    if (group.size() >= 2) eligibleGroups.add(group);
                }

                if (!eligibleGroups.isEmpty()) {
                    ArrayList<Question> chosen = eligibleGroups.get(random.nextInt(eligibleGroups.size()));
                    questions.add(chosen.get(0));
                    questions.add(chosen.get(1));
                }

                pd.dismiss();

                if (questions.size() < 4) {
                    Toast.makeText(SimulationsActivity.this,
                            "Simulation requires 1 Personal, 1 Project, and 2 Video questions.",
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    setupRecordingManagers();
                    startSimulationFlow();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                pd.dismiss();
                Log.e("Firebase", error.getMessage());
            }
        });
    }

    /**
     * Helper to group video questions by their shared YouTube URL.
     *
     * @param q           The question to evaluate.
     * @param videoUrls   A list of already processed video URLs.
     * @param videoGroups A list of question lists grouped by URL.
     */
    private void groupVideoQuestions(Question q, ArrayList<String> videoUrls, ArrayList<ArrayList<Question>> videoGroups) {
        String url = q.getVideoUrl();
        if (url == null || url.isEmpty() || url.equals("null")) return;

        int index = videoUrls.indexOf(url);
        if (index == -1) {
            videoUrls.add(url);
            ArrayList<Question> newList = new ArrayList<>();
            newList.add(q);
            videoGroups.add(newList);
        } else {
            videoGroups.get(index).add(q);
        }
    }

    /**
     * Sets up {@link RecordingManager} instances for each question in the simulation.
     */
    private void setupRecordingManagers() {
        for (RecordingManager rm : recordingManagers) {
            rm.release();
        }
        recordingManagers.clear();
        for (Question q : questions) {
            recordingManagers.add(new RecordingManager(this, "SIM_" + q.getQuestionId() + ".aac"));
        }
    }

    /**
     * Handles clicks for all buttons in the activity.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnNext) {
            changeQuestion(1);
        } else if (id == R.id.btnPrevious) {
            changeQuestion(-1);
        } else if (id == R.id.btnFinishSim) {
            finishSimulation();
        } else if (id == R.id.playPauseBtn) {
            playOrPause();
        } else if (id == R.id.recordBtn) {
            recordBtn();
        } else if (id == R.id.playRecordingBtn) {
            playRecording();
        } else if (id == R.id.deleteRecordingBtn) {
            confirmDeleteRecording();
        }
    }

    /**
     * Transitions the UI from the intro card to the question flow and starts the global timer.
     */
    private void startSimulationFlow() {
        questionsContainer.setVisibility(View.VISIBLE);
        timerLayout.setVisibility(View.VISIBLE);
        loadQuestion(currentQuestionIndex);
        remainingSimulationMillis = SIMULATION_DURATION_MS;
        startOrResumeSimulationTimer();
    }

    /**
     * Starts or resumes the global 30-minute simulation countdown timer.
     */
    private void startOrResumeSimulationTimer() {
        if (simulationTimer != null) {
            simulationTimer.cancel();
        }

        simulationTimer = new CountDownTimer(remainingSimulationMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                remainingSimulationMillis = millisUntilFinished;
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            public void onFinish() {
                remainingSimulationMillis = 0;
                tvTimer.setText("00:00");
                finishSimulation(true);
            }
        }.start();
    }

    /**
     * Pauses the global simulation countdown timer.
     */
    private void pauseSimulationTimer() {
        if (simulationTimer != null) {
            simulationTimer.cancel();
            simulationTimer = null;
        }
    }

    /**
     * Moves to the next or previous question in the simulation sequence.
     *
     * @param direction 1 for next, -1 for previous.
     */
    private void changeQuestion(int direction) {
        if (currentRecordingManager != null && currentRecordingManager.isRecording()) {
            Toast.makeText(this, "Stop recording before changing questions", Toast.LENGTH_SHORT).show();
            return;
        }

        int newIndex = currentQuestionIndex + direction;
        if (newIndex >= 0 && newIndex < questions.size()) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            currentQuestionIndex = newIndex;
            loadQuestion(currentQuestionIndex);
        }
    }

    /**
     * Loads the data and UI state for a specific question in the simulation.
     *
     * @param index The index of the question to load.
     */
    private void loadQuestion(int index) {
        tts.stop();
        timerHandler.removeCallbacks(timerRunnable);

        Question q = questions.get(index);
        currentRecordingManager = recordingManagers.get(index);

        questionTitleTv.setText(String.format(Locale.getDefault(), "Question %d/%d: %s", index + 1, questions.size(), q.getCategory()));
        playbackSubTitle.setText(q.getSubTopic());

        totalSeconds = Math.max(q.getFullQuestion().length() / 15, 1);
        maxProgress = totalSeconds * 10;
        ttsSeekBar.setMax(maxProgress);
        currentProgress = 0;
        ttsSeekBar.setProgress(0);
        updateTimeLabels();

        resetRecordingUI();

        if (q.getVideoUrl() != null && !q.getVideoUrl().equals("null") && !q.getVideoUrl().isEmpty()) {
            youTubePlayerView.setVisibility(View.VISIBLE);
            prepareVideo(q.getVideoUrl());
            View space = findViewById(R.id.videoAudioSpace);
            if (space != null) space.setVisibility(View.VISIBLE);
        } else {
            youTubePlayerView.setVisibility(View.GONE);
            currentVideoId = null;
            if (activeYouTubePlayer != null) activeYouTubePlayer.pause();
            View space = findViewById(R.id.videoAudioSpace);
            if (space != null) space.setVisibility(View.GONE);
        }

        btnPrevious.setVisibility(index > 0 ? View.VISIBLE : View.INVISIBLE);
        btnNext.setVisibility(index < questions.size() - 1 ? View.VISIBLE : View.INVISIBLE);
        btnFinishSim.setVisibility(index == questions.size() - 1 ? View.VISIBLE : View.GONE);
    }

    /**
     * Toggles the Text-to-Speech playback of the current question.
     */
    private void playOrPause() {
        if (tts.isSpeaking()) {
            tts.stop();
            timerHandler.removeCallbacks(timerRunnable);
            playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
        } else {
            if (currentProgress >= maxProgress) {
                currentProgress = 0;
                ttsSeekBar.setProgress(0);
                updateTimeLabels();
            }
            float pct = (float) ttsSeekBar.getProgress() / ttsSeekBar.getMax();
            tts.speakFromPercentage(questions.get(currentQuestionIndex).getFullQuestion(), pct);
            playPauseBtn.setImageResource(android.R.drawable.ic_media_pause);
            timerHandler.postDelayed(timerRunnable, 100);
        }
    }

    /**
     * Manages the recording lifecycle for the current question (start, pause, resume, stop).
     */
    public void recordBtn() {
        if (currentRecordingManager.isFinalized()) {
            Toast.makeText(this, "Recording finished. Delete to restart.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!currentRecordingManager.isRecording() && !currentRecordingManager.isPaused() && !currentRecordingManager.hasPausedOnce()) {
            currentRecordingManager.startRecordingChunk();
            recordBtn.setImageResource(android.R.drawable.ic_media_pause);
            recordingTimerHandler.postDelayed(recordingTimerRunnable, 100);
        } else if (currentRecordingManager.isRecording() && !currentRecordingManager.hasPausedOnce()) {
            currentRecordingManager.stopRecordingChunk();
            addPauseLine();
            currentRecordingManager.setHasPausedOnce(true);
            currentRecordingManager.setPaused(true);
            recordBtn.setImageResource(android.R.drawable.ic_btn_speak_now);
            recordingTimerHandler.removeCallbacks(recordingTimerRunnable);
        } else if (currentRecordingManager.isPaused()) {
            currentRecordingManager.startRecordingChunk();
            currentRecordingManager.setPaused(false);
            recordBtn.setImageResource(android.R.drawable.ic_media_pause);
            recordingTimerHandler.postDelayed(recordingTimerRunnable, 100);
        } else if (currentRecordingManager.isRecording() && currentRecordingManager.hasPausedOnce()) {
            currentRecordingManager.stopRecordingChunk();
            currentRecordingManager.mergeChunks();
            currentRecordingManager.setFinalized(true);
            recordBtn.setImageResource(android.R.drawable.ic_btn_speak_now);
            recordingTimerHandler.removeCallbacks(recordingTimerRunnable);
        }
    }

    /**
     * Toggles playback of the finalized recording for the current question.
     */
    public void playRecording() {
        if (currentRecordingManager.isRecording()) {
            Toast.makeText(this, "Stop recording first", Toast.LENGTH_SHORT).show();
            return;
        }

        String path = currentRecordingManager.getFinalFilePath();
        if (path == null) {
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playRecordingBtn.setImageResource(android.R.drawable.ic_media_play);
            recordingTimerHandler.removeCallbacks(playbackUpdaterRunnable);
        } else if (mediaPlayer != null) {
            mediaPlayer.start();
            playRecordingBtn.setImageResource(android.R.drawable.ic_media_pause);
            recordingTimerHandler.post(playbackUpdaterRunnable);
        } else {
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
     * Shows a confirmation dialog before deleting the current question's recording.
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
                        currentRecordingManager.clearAllFiles();
                        resetRecordingUI();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Resets recording-related UI components to their initial state.
     */
    private void resetRecordingUI() {
        recordedSeconds = 0;
        pauseTimeInSeconds = -1;
        recordingSeekBar.setMax(0);
        recordingSeekBar.setProgress(0);
        updateRecordedTimeLabel();
        linesContainer.removeAllViews();
        recordBtn.setImageResource(android.R.drawable.ic_btn_speak_now);
        playRecordingBtn.setImageResource(android.R.drawable.ic_media_play);
    }

    private void finishSimulation() {
        finishSimulation(false);
    }

    /**
     * Finishes the simulation and triggers the grading process.
     *
     * @param forceFinishNow If true, bypasses the confirmation dialog (e.g., when timer expires).
     */
    private void finishSimulation(boolean forceFinishNow) {
        if (isFinishDialogOpen) return;

        if (forceFinishNow) {
            pauseSimulationTimer();
            gradeAllSimulationAnswersAndSave();
            return;
        }

        isFinishDialogOpen = true;
        new AlertDialog.Builder(this)
                .setTitle("Finish simulation?")
                .setMessage("Are you sure you want to finish and submit all answers for grading?")
                .setPositiveButton("Finish", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<byte[]> filesBytes = new ArrayList<>();
                        ArrayList<String> mimeTypes = new ArrayList<>();
                        ArrayList<String> audioFilePaths = new ArrayList<>();

                        if (!prepareSimulationPayload(filesBytes, mimeTypes, audioFilePaths)) {
                            isFinishDialogOpen = false;
                            return;
                        }

                        pauseSimulationTimer();
                        isFinishDialogOpen = false;
                        analyzeAndSaveSimulation(filesBytes, mimeTypes, audioFilePaths);
                    }
                })
                .setNegativeButton("Cancel", null)
                .setOnDismissListener(dialogInterface -> {
                    isFinishDialogOpen = false;
                })
                .show();
    }

    /**
     * Direct entry point for grading all answers after force-finishing.
     */
    private void gradeAllSimulationAnswersAndSave() {
        ArrayList<byte[]> filesBytes = new ArrayList<>();
        ArrayList<String> mimeTypes = new ArrayList<>();
        ArrayList<String> audioFilePaths = new ArrayList<>();

        if (!prepareSimulationPayload(filesBytes, mimeTypes, audioFilePaths)) return;
        analyzeAndSaveSimulation(filesBytes, mimeTypes, audioFilePaths);
    }

    /**
     * Collects audio data and metadata for all 4 questions to prepare for AI analysis.
     *
     * @param filesBytes     List to populate with audio data byte arrays.
     * @param mimeTypes      List to populate with audio MIME types.
     * @param audioFilePaths List to populate with local file paths.
     * @return true if all data was successfully prepared; false otherwise.
     */
    private boolean prepareSimulationPayload(ArrayList<byte[]> filesBytes,
                                               ArrayList<String> mimeTypes,
                                               ArrayList<String> audioFilePaths) {
        if (questions == null || recordingManagers == null || questions.size() < 4 || recordingManagers.size() < 4) {
            Toast.makeText(this, "Simulation is not ready (missing questions/recordings).", Toast.LENGTH_SHORT).show();
            return false;
        }

        for (int i = 0; i < 4; i++) {
            RecordingManager rm = recordingManagers.get(i);
            if (rm == null) {
                Toast.makeText(this, "Missing recording manager for question " + (i + 1), Toast.LENGTH_SHORT).show();
                return false;
            }

            if (rm.isRecording()) {
                rm.stopRecordingChunk();
            }

            String filePath = rm.getFinalFilePath();
            if (filePath == null) {
                Toast.makeText(this, "Please record an answer for question " + (i + 1), Toast.LENGTH_SHORT).show();
                return false;
            }

            try {
                byte[] bytes = rm.getBytes(filePath);
                filesBytes.add(bytes);
                mimeTypes.add("audio/aac");
                audioFilePaths.add(filePath);
            } catch (IOException e) {
                Toast.makeText(this, "Failed reading audio for question " + (i + 1), Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    /**
     * Constructs the batch prompt for Gemini and initiates the AI analysis for all recordings.
     *
     * @param filesBytes     Audio data for the 4 recordings.
     * @param mimeTypes      MIME types for the audio files.
     * @param audioFilePaths Local paths for the audio files.
     */
    private void analyzeAndSaveSimulation(ArrayList<byte[]> filesBytes,
                                          ArrayList<String> mimeTypes,
                                          ArrayList<String> audioFilePaths) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setCancelable(false);
        pd.setTitle("Analyzing simulation...");
        pd.setMessage("Waiting for Gemini...");
        pd.show();

        StringBuilder recordingsDetails = new StringBuilder();
        StringBuilder categoryPrompts = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            Question q = questions.get(i);

            recordingsDetails.append(i + 1)
                    .append(") Question: ").append(q.getFullQuestion()).append("\n");

            if ("Video Clip Questions".equals(q.getCategory()) && q.getVideoUrl() != null && !q.getVideoUrl().equals("null")) {
                recordingsDetails.append("Video URL: ").append(q.getVideoUrl()).append("\n");
            }

            categoryPrompts.append("\n--- Recording ").append(i + 1).append(" ---\n");

            switch (q.getCategory()) {
                case "Personal Questions":
                    categoryPrompts.append(Prompts.PERSONAL_PROMPT);
                    break;
                case "Project Questions":
                    categoryPrompts.append(Prompts.PROJECT_PROMPT);
                    break;
                case "Video Clip Questions":
                    categoryPrompts.append(Prompts.VIDEO_CLIPS_PROMPT);
                    break;
            }

            categoryPrompts.append("\nQuestion: ").append(q.getFullQuestion());
            if ("Video Clip Questions".equals(q.getCategory()) && q.getVideoUrl() != null && !q.getVideoUrl().equals("null")) {
                categoryPrompts.append("\nVideo URL: ").append(q.getVideoUrl());
            }
            categoryPrompts.append("\n");
        }

        String finalPrompt = Prompts.SIMULATION_MASTER_PROMPT
                .replace("{RECORDINGS_DETAILS}", recordingsDetails.toString())
                .replace("{CATEGORY_TASKS}", categoryPrompts.toString());

        GeminiManager.getInstance().sendTextWithFilesPrompt(finalPrompt, filesBytes, mimeTypes, new GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                pd.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        parseAndSaveSimulation(result, audioFilePaths);
                    }
                });
            }

            @Override
            public void onFailure(Throwable error) {
                pd.dismiss();
                Toast.makeText(SimulationsActivity.this, "Gemini error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Parses the batch AI result and prepares Recording objects for all simulation answers.
     *
     * @param json           The JSON response from Gemini.
     * @param audioFilePaths Local paths for the audio files.
     */
    private void parseAndSaveSimulation(String json, ArrayList<String> audioFilePaths) {
        String cleanedJson = (json == null) ? "" : json.replaceAll("```json", "").replaceAll("```", "").trim();
        try {
            JSONObject root = new JSONObject(cleanedJson);
            JSONArray recordingsArr = root.getJSONArray("recordings");
            if (recordingsArr.length() < 4) {
                Toast.makeText(this, "AI returned unexpected format (missing recordings).", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<Recording> recordingsToSave = new ArrayList<>();
            ArrayList<String> recordingIds = new ArrayList<>();
            ArrayList<Integer> scores = new ArrayList<>();

            String userId = refAuth.getCurrentUser() != null ? refAuth.getCurrentUser().getUid() : null;
            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            Date simulationDate = new Date();
            String simulationSessionLabel = buildSimulationSessionLabel(simulationDate);

            for (int i = 0; i < 4; i++) {
                Question q = questions.get(i);

                String recordingId = refRecordings.child(userId).child(q.getQuestionId()).push().getKey();
                if (recordingId == null) {
                    Toast.makeText(this, "Failed generating recording id.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String base = (q.getSubTopic() == null || q.getSubTopic().equals("null")) ? q.getTopic() : q.getSubTopic();
                String finalTitle = base + " (" + simulationSessionLabel + " - Q" + (i + 1) + ")";

                int totalScore;
                Map<String, TopicDetail> aiFeedBack = new HashMap<>();

                JSONObject obj = recordingsArr.getJSONObject(i);
                int topicDevelopmentScore = obj.getInt("topicDevelopment");
                int deliveryScore = obj.getInt("delivery");
                int vocabularyScore = obj.getInt("vocabulary");
                int languageScore = obj.getInt("language");
                totalScore = obj.getInt("totalSectionScore");

                JSONObject feedback = obj.getJSONObject("feedback");
                aiFeedBack.put("topicDevelopment", new TopicDetail(topicDevelopmentScore, parseFeedbackSection(feedback.getJSONObject("topicDevelopment"))));
                aiFeedBack.put("delivery", new TopicDetail(deliveryScore, parseFeedbackSection(feedback.getJSONObject("delivery"))));
                aiFeedBack.put("vocabulary", new TopicDetail(vocabularyScore, parseFeedbackSection(feedback.getJSONObject("vocabulary"))));
                aiFeedBack.put("language", new TopicDetail(languageScore, parseFeedbackSection(feedback.getJSONObject("language"))));
                aiFeedBack.put("overall", new TopicDetail(totalScore, feedback.getString("overallSummary")));

                Recording rec = new Recording(userId, q.getQuestionId(), finalTitle, simulationDate, totalScore, aiFeedBack);
                rec.setRecordingId(recordingId);

                recordingsToSave.add(rec);
                recordingIds.add(recordingId);
                scores.add(totalScore);
            }

            int overallScore = 0;
            for (int s : scores) overallScore += s;
            overallScore = Math.round(overallScore / 4f);

            uploadRecordingsSequentially(0, recordingsToSave, recordingIds, audioFilePaths, overallScore, simulationDate);
        } catch (JSONException e) {
            Toast.makeText(this, "Failed parsing Gemini JSON.", Toast.LENGTH_SHORT).show();
            Log.e("SimulationsActivity", "Parse error", e);
        }
    }

    /**
     * Sequentially uploads recording files to Firebase Storage and their metadata to the Database.
     *
     * @param idx              Current index in the recordings list.
     * @param recordingsToSave List of Recording objects.
     * @param recordingIds     List of recording IDs.
     * @param audioFilePaths   List of local file paths.
     * @param overallScore     Average score of the simulation.
     * @param simulationDate   Date the simulation was completed.
     */
    private void uploadRecordingsSequentially(final int idx,
                                                final ArrayList<Recording> recordingsToSave,
                                                final ArrayList<String> recordingIds,
                                                final ArrayList<String> audioFilePaths,
                                                final int overallScore,
                                                final Date simulationDate) {
        final int total = recordingsToSave.size();
        if (idx >= total) {
            createSimulationAndNavigate(recordingsToSave, recordingIds, overallScore, simulationDate);
            return;
        }

        String userId = recordingsToSave.get(idx).getUserId();
        Recording rec = recordingsToSave.get(idx);
        String filePath = audioFilePaths.get(idx);

        StorageReference fileRef = refRecordingsMedia.child(userId + "/" + rec.getRecordingId() + ".aac");
        ProgressDialog savingPd = new ProgressDialog(this);
        savingPd.setCancelable(false);
        savingPd.setMessage("Saving simulation recordings (" + (idx + 1) + "/" + total + ")...");
        savingPd.show();

        fileRef.putFile(Uri.fromFile(new File(filePath)))
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        refRecordings.child(userId).child(rec.getQuestionId()).child(rec.getRecordingId())
                                .setValue(rec)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        savingPd.dismiss();
                                        uploadRecordingsSequentially(idx + 1, recordingsToSave, recordingIds, audioFilePaths, overallScore, simulationDate);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        savingPd.dismiss();
                                        Toast.makeText(SimulationsActivity.this, "Failed saving recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        savingPd.dismiss();
                        Toast.makeText(SimulationsActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Persists the global Simulation object to Firebase and navigates to the results screen.
     *
     * @param recordingsToSave List of finalized Recording objects.
     * @param recordingIds     List of finalized recording IDs.
     * @param overallScore     Calculated overall score.
     * @param simulationDate   Completion timestamp.
     */
    private void createSimulationAndNavigate(ArrayList<Recording> recordingsToSave,
                                              ArrayList<String> recordingIds,
                                              int overallScore,
                                              Date simulationDate) {
        String userId = recordingsToSave.get(0).getUserId();
        String simulationId = refSimulations.push().getKey();
        if (simulationId == null) {
            Toast.makeText(this, "Failed creating simulation.", Toast.LENGTH_SHORT).show();
            return;
        }

        Simulation sim = new Simulation();
        sim.setSimulationId(simulationId);
        sim.setUserId(userId);
        sim.setDateCompleted(simulationDate);
        sim.setOverAllScore(overallScore);
        sim.setRecordingsIds(recordingIds);

        refSimulations.child(simulationId).setValue(sim)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Intent i = new Intent(SimulationsActivity.this, SimulationResultsActivity.class);
                        i.putExtra("overallScore", overallScore);
                        i.putExtra("recordings", recordingsToSave);
                        startActivity(i);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SimulationsActivity.this, "Failed saving simulation: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String buildSimulationSessionLabel(Date simulationDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("Simulation dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(simulationDate);
    }

    private String parseFeedbackSection(JSONObject section) throws JSONException {
        return "Keep: " + section.getString("keep") + "\nImprove: " + section.getString("improve");
    }

    /**
     * Prepares the YouTube player with the provided video URL.
     *
     * @param videoUrl The YouTube URL to load.
     */
    private void prepareVideo(String videoUrl) {
        String videoId = extractVideoId(videoUrl);
        if (videoId.isEmpty()) return;

        currentVideoId = videoId;
        if (activeYouTubePlayer != null) {
            activeYouTubePlayer.cueVideo(videoId, 0f);
        }
    }

    /**
     * Extracts the YouTube video ID from a URL.
     *
     * @param videoUrl The URL string.
     * @return The 11-character video ID or empty string if not found.
     */
    private String extractVideoId(String videoUrl) {
        if (videoUrl == null || videoUrl.trim().isEmpty()) return "";
        String videoId = "";
        if (videoUrl.contains("v=")) {
            videoId = videoUrl.split("v=")[1].split("&")[0];
        } else if (videoUrl.contains("youtu.be/")) {
            videoId = videoUrl.split("youtu.be/")[1].split("\\?")[0];
        } else if (videoUrl.contains("embed/")) {
            videoId = videoUrl.split("embed/")[1].split("\\?")[0];
        } else if (videoUrl.length() == 11) {
            videoId = videoUrl;
        }
        return videoId.trim();
    }

    /**
     * Called when the activity is about to be destroyed.
     * <p>
     * Cleans up resources to prevent memory leaks:
     * <ul>
     *     <li>Destroys Text-to-Speech engine (tts)</li>
     *     <li>Cancels simulation timers</li>
     *     <li>Releases MediaPlayer and YouTubePlayerView</li>
     *     <li>Releases all RecordingManager instances</li>
     *     <li>Removes all pending callbacks from handlers</li>
     * </ul>
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) tts.destroy();
        if (simulationTimer != null) simulationTimer.cancel();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
        for (int i = 0; i < recordingManagers.size(); i++) {
            recordingManagers.get(i).release();
        }
        timerHandler.removeCallbacksAndMessages(null);
        recordingTimerHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Runnable that updates TTS progress and UI labels.
     * <p>
     * Runs periodically (every 100ms) while TTS is speaking.
     * Updates:
     * <ul>
     *     <li>Current progress</li>
     *     <li>TTS SeekBar</li>
     *     <li>Time labels</li>
     * </ul>
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
     * Runnable that updates the recording timer and progress.
     * <p>
     * Runs every 1000ms (1 second) while recording.
     * Updates:
     * <ul>
     *     <li>Recorded seconds counter</li>
     *     <li>Recording time label</li>
     *     <li>Recording SeekBar</li>
     *     <li>Position of pause/resume line</li>
     * </ul>
     */
    private final Runnable recordingTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentRecordingManager != null && currentRecordingManager.isRecording()) {
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
     * Runnable that updates the playback SeekBar during media playback.
     * <p>
     * Runs every 500ms while MediaPlayer is playing.
     * Updates:
     * <ul>
     *     <li>Recording SeekBar position</li>
     *     <li>Recorded time label</li>
     * </ul>
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
     * Updates the simulation time labels.
     * <p>
     * Updates the current time and total time TextViews based on {@link #currentProgress} and {@link #totalSeconds}.
     */
    private void updateTimeLabels() {
        int sec = currentProgress / 10;
        currentTimeTv.setText(String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60));
        totalTimeTv.setText(String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60));
    }

    /**
     * Updates the recorded time label during recording.
     * <p>
     * Uses {@link #recordedSeconds} to update the TextView.
     */
    private void updateRecordedTimeLabel() {
        recordedTimeTv.setText(String.format(Locale.getDefault(), "%02d:%02d", recordedSeconds / 60, recordedSeconds % 60));
    }

    /**
     * Adds a visual pause line marker on the recording waveform.
     * <p>
     * Marks the current {@link #recordedSeconds} position as a pause/resume point.
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
     * Updates the position of the pause line marker on the waveform/SeekBar.
     * <p>
     * Calculates X position proportionally to the recorded seconds and SeekBar max value.
     * Runs on the linesContainer's post() to ensure correct width measurements.
     */
    private void updateLinePosition() {
        if (pauseTimeInSeconds == -1) return;
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
     * Checks if the app has audio recording permission.
     * <p>
     * If permission is not granted, requests it at runtime.
     */
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    /**
     * Handles the result of runtime permission requests.
     *
     * @param requestCode  The request code passed in {@link #checkPermissions()}
     * @param permissions  The requested permissions
     * @param grantResults The grant results for the corresponding permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
