package com.example.speakup.Activities;

import static com.example.speakup.Utils.FBRef.refAuth;
import static com.example.speakup.Utils.FBRef.refQuestions;
import static com.example.speakup.Utils.FBRef.refRecordings;
import static com.example.speakup.Utils.FBRef.refRecordingsMedia;
import static com.example.speakup.Utils.FBRef.refSimulations;
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

public class SimulationsActivity extends Utilities implements View.OnClickListener {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // UI Components
    private View introCard;
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
    private final ArrayList<Question> questions = new ArrayList<Question>();
    private final ArrayList<RecordingManager> recordingManagers = new ArrayList<RecordingManager>();
    private int currentQuestionIndex = 0;
    private RecordingManager currentRecordingManager;

    private int currentProgress = 0, totalSeconds = 0, maxProgress = 0, recordedSeconds = 0;
    private int pauseTimeInSeconds = -1;
    private boolean isFinishDialogOpen = false;
    private static final long SIMULATION_DURATION_MS = 30 * 60 * 1000L;
    private long remainingSimulationMillis = SIMULATION_DURATION_MS;
    private boolean isSimulationFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulations);

        initViews();
        initLogic();
        checkPermissions();

        boolean autoStart = getIntent() != null && getIntent().getBooleanExtra("AUTO_START", false);
        if (autoStart) {
            fetchRandomQuestions();
        }
    }

    private void initViews() {
        introCard = findViewById(R.id.introCard);
        questionsContainer = findViewById(R.id.questionsContainer);
        timerLayout = findViewById(R.id.timerLayout);
        Button btnStartSim = findViewById(R.id.btnStartSim);
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

        btnStartSim.setOnClickListener(this);
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

                            // Sort into lists
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

                // Random Selection Logic
                Random random = new Random();
                questions.clear();

                // Pick 1 Personal
                if (!personalList.isEmpty()) {
                    questions.add(personalList.get(random.nextInt(personalList.size())));
                }

                // Pick 1 Project
                if (!projectList.isEmpty()) {
                    questions.add(projectList.get(random.nextInt(projectList.size())));
                }

                // Pick 1 Video Group (and 2 questions from it)
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

                // Final Validation
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

    private void setupRecordingManagers() {
        // Release old ones first to prevent memory leaks/file locks
        for (RecordingManager rm : recordingManagers) {
            rm.release();
        }
        recordingManagers.clear();
        for (Question q : questions) {
            recordingManagers.add(new RecordingManager(this, "SIM_" + q.getQuestionId() + ".aac"));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnStartSim) {
            fetchRandomQuestions();
        } else if (id == R.id.btnNext) {
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

    private void startSimulationFlow() {
        introCard.setVisibility(View.GONE);
        questionsContainer.setVisibility(View.VISIBLE);
        timerLayout.setVisibility(View.VISIBLE);
        loadQuestion(currentQuestionIndex);
        remainingSimulationMillis = SIMULATION_DURATION_MS;
        isSimulationFinished = false;
        startOrResumeSimulationTimer();
    }

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
                isSimulationFinished = true;
                tvTimer.setText("00:00");
                finishSimulation(true);
            }
        }.start();
    }

    private void pauseSimulationTimer() {
        if (simulationTimer != null) {
            simulationTimer.cancel();
            simulationTimer = null;
        }
    }

    private void changeQuestion(int direction) {
        // Prevent navigation while recording is in progress
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
                        boolean[] isEmptyAudio = new boolean[4];

                        // If the user hasn't answered all questions, keep the timer running.
                        if (!prepareSimulationPayload(filesBytes, mimeTypes, audioFilePaths, isEmptyAudio)) {
                            isFinishDialogOpen = false;
                            isSimulationFinished = false;
                            return;
                        }

                        isSimulationFinished = true;
                        pauseSimulationTimer();
                        isFinishDialogOpen = false;
                        analyzeAndSaveSimulation(filesBytes, mimeTypes, audioFilePaths, isEmptyAudio);
                    }
                })
                .setNegativeButton("Cancel", null)
                .setOnDismissListener(dialogInterface -> {
                    isFinishDialogOpen = false;
                })
                .show();
    }

    private void gradeAllSimulationAnswersAndSave() {
        ArrayList<byte[]> filesBytes = new ArrayList<>();
        ArrayList<String> mimeTypes = new ArrayList<>();
        ArrayList<String> audioFilePaths = new ArrayList<>();
        boolean[] isEmptyAudio = new boolean[4];

        if (!prepareSimulationPayload(filesBytes, mimeTypes, audioFilePaths, isEmptyAudio)) return;
        analyzeAndSaveSimulation(filesBytes, mimeTypes, audioFilePaths, isEmptyAudio);
    }

    private boolean prepareSimulationPayload(ArrayList<byte[]> filesBytes,
                                               ArrayList<String> mimeTypes,
                                               ArrayList<String> audioFilePaths,
                                               boolean[] isEmptyAudio) {
        if (questions == null || recordingManagers == null || questions.size() < 4 || recordingManagers.size() < 4) {
            Toast.makeText(this, "Simulation is not ready (missing questions/recordings).", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Collect audio for all 4 questions (stop recording chunks if needed)
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
                isEmptyAudio[i] = isEmptyAudio(bytes, filePath);
            } catch (IOException e) {
                Toast.makeText(this, "Failed reading audio for question " + (i + 1), Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    private void analyzeAndSaveSimulation(ArrayList<byte[]> filesBytes,
                                           ArrayList<String> mimeTypes,
                                           ArrayList<String> audioFilePaths,
                                           boolean[] isEmptyAudio) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setCancelable(false);
        pd.setTitle("Analyzing simulation...");
        pd.setMessage("Waiting for Gemini...");
        pd.show();

        // Build one prompt for Gemini (4 recordings in the same order as questions[0..3])
        String prompt = "You are grading a COBE simulation.\n\n";
        prompt += "There are exactly 4 audio recordings below, in this exact order:\n";
        prompt += "1) Question: " + questions.get(0).getFullQuestion() + "\n";
        if ("Video Clip Questions".equals(questions.get(0).getCategory())) {
            prompt += "Video URL: " + questions.get(0).getVideoUrl() + "\n";
        }
        prompt += "2) Question: " + questions.get(1).getFullQuestion() + "\n";
        if ("Video Clip Questions".equals(questions.get(1).getCategory())) {
            prompt += "Video URL: " + questions.get(1).getVideoUrl() + "\n";
        }
        prompt += "3) Question: " + questions.get(2).getFullQuestion() + "\n";
        if ("Video Clip Questions".equals(questions.get(2).getCategory())) {
            prompt += "Video URL: " + questions.get(2).getVideoUrl() + "\n";
        }
        prompt += "4) Question: " + questions.get(3).getFullQuestion() + "\n";
        if ("Video Clip Questions".equals(questions.get(3).getCategory())) {
            prompt += "Video URL: " + questions.get(3).getVideoUrl() + "\n";
        }

        // Add category-specific instructions per recording (so Gemini uses your existing rubric)
        prompt += "\nCategory-specific grading instructions (recordings 1..4):\n";
        for (int i = 0; i < 4; i++) {
            Question q = questions.get(i);
            prompt += "\n--- Recording " + (i + 1) + " ---\n";

            if ("Personal Questions".equals(q.getCategory())) {
                prompt += PERSONAL_PROMPT;
            } else if ("Project Questions".equals(q.getCategory())) {
                prompt += PROJECT_PROMPT;
            } else if ("Video Clip Questions".equals(q.getCategory())) {
                prompt += VIDEO_CLIPS_PROMPT;
            }

            prompt += q.getFullQuestion();

            if ("Video Clip Questions".equals(q.getCategory())) {
                prompt += "\nVideo URL: " + q.getVideoUrl() + "\n";
            }
        }

        prompt += "\nEMPTY AUDIO RULE: If a recording contains no intelligible speech (empty/silence), set:\n";
        prompt += "- topicDevelopment, delivery, vocabulary, language, totalSectionScore = 0\n";
        prompt += "- feedback.overallSummary: \"No speech detected.\"\n";
        prompt += "- feedback.*.keep and feedback.*.improve must mention no speech was detected.\n\n";

        prompt += "Return ONLY valid JSON in this exact format:\n";
        prompt += "{ \"recordings\": [ obj1, obj2, obj3, obj4 ] }\n";
        prompt += "Where each obj1..obj4 is the JSON object described in the corresponding category prompt (i.e., it matches COMPONENT_SCHEMA).\n";
        prompt += "Do NOT wrap in markdown and do NOT include any extra text.";

        GeminiManager.getInstance().sendTextWithFilesPrompt(prompt, filesBytes, mimeTypes, new GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                pd.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        parseAndSaveSimulation(result, audioFilePaths, isEmptyAudio);
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

    private boolean isEmptyAudio(byte[] bytes, String filePath) {
        try {
            File f = new File(filePath);
            long len = (f.exists()) ? f.length() : 0L;
            if (bytes == null) return true;
            if (bytes.length < 8000) return true;
            return len < 8000;
        } catch (Exception e) {
            return false;
        }
    }

    private void parseAndSaveSimulation(String json, ArrayList<String> audioFilePaths, boolean[] isEmptyAudio) {
        String cleanedJson = (json == null) ? "" : json.replaceAll("```json", "").replaceAll("```", "").trim();
        try {
            JSONObject root = new JSONObject(cleanedJson);
            JSONArray recordingsArr = root.getJSONArray("recordings");
            if (recordingsArr.length() < 4) {
                Toast.makeText(this, "AI returned unexpected format (missing recordings).", Toast.LENGTH_SHORT).show();
                return;
            }

            // Build per-recording feedback + scores
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

                if (isEmptyAudio[i]) {
                    totalScore = 0;
                    String msg = "No speech detected in the recording (empty/silence).";
                    aiFeedBack.put("topicDevelopment", new TopicDetail(0, msg));
                    aiFeedBack.put("delivery", new TopicDetail(0, msg));
                    aiFeedBack.put("vocabulary", new TopicDetail(0, msg));
                    aiFeedBack.put("language", new TopicDetail(0, msg));
                    aiFeedBack.put("overall", new TopicDetail(0, msg));
                } else {
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
                }

                Recording rec = new Recording(userId, q.getQuestionId(), finalTitle, simulationDate, totalScore, aiFeedBack);
                rec.setRecordingId(recordingId);

                recordingsToSave.add(rec);
                recordingIds.add(recordingId);
                scores.add(totalScore);
            }

            int overallScore = 0;
            for (int s : scores) overallScore += s;
            overallScore = Math.round(overallScore / 4f);

            // Upload + save recordings sequentially, then create Simulation and navigate.
            uploadRecordingsSequentially(0, recordingsToSave, recordingIds, audioFilePaths, overallScore, simulationDate);
        } catch (JSONException e) {
            Toast.makeText(this, "Failed parsing Gemini JSON.", Toast.LENGTH_SHORT).show();
            Log.e("SimulationsActivity", "Parse error", e);
        }
    }

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

    private void submitCurrentAnswerForGrading() {
        if (currentRecordingManager == null) {
            Toast.makeText(this, "No recording manager found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (recordedSeconds <= 0) {
            Toast.makeText(this, "You need to record an answer first", Toast.LENGTH_SHORT).show();
            return;
        }

        // If still recording, stop the current chunk so we have a usable file
        if (currentRecordingManager.isRecording()) {
            currentRecordingManager.stopRecordingChunk();
        }

        // Mirror the practice flow: allow either a paused single chunk or a finalized merged recording
        if (!(currentRecordingManager.isPaused() || currentRecordingManager.isFinalized())) {
            Toast.makeText(this, "Please finish your recording first", Toast.LENGTH_SHORT).show();
            return;
        }

        String filePath = currentRecordingManager.getFinalFilePath();
        if (filePath == null) {
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] bytes;
        try {
            bytes = currentRecordingManager.getBytes(filePath);
        } catch (IOException e) {
            Log.e("SimulationsActivity", "Error reading file", e);
            Toast.makeText(this, "Error reading recording file", Toast.LENGTH_SHORT).show();
            return;
        }

        // If the audio is basically empty/silence, skip Gemini and force a 0 score.
        // (Gemini may hallucinate scores even when there's no speech.)
        try {
            File recFile = new File(filePath);
            long sizeBytes = recFile.exists() ? recFile.length() : 0L;
            if (recordedSeconds < 2 || sizeBytes < 8000L || (bytes != null && bytes.length < 8000)) {
                Map<String, TopicDetail> emptyFeedback = new HashMap<>();
                String emptySummary = "No speech detected in the recording (empty/silence).";
                emptyFeedback.put("topicDevelopment", new TopicDetail(0, emptySummary));
                emptyFeedback.put("delivery", new TopicDetail(0, emptySummary));
                emptyFeedback.put("vocabulary", new TopicDetail(0, emptySummary));
                emptyFeedback.put("language", new TopicDetail(0, emptySummary));
                emptyFeedback.put("overall", new TopicDetail(0, emptySummary));
                saveSimulationToFirebase(emptyFeedback, 0, filePath, questions.get(currentQuestionIndex));
                return;
            }
        } catch (Exception ignore) {
            // Fall back to Gemini if our empty check fails.
        }

        ProgressDialog pD = new ProgressDialog(this);
        pD.setTitle("Analyzing answer...");
        pD.setMessage("Waiting for response...");
        pD.setCancelable(false);
        pD.show();

        Question q = questions.get(currentQuestionIndex);
        String prompt = "";
        switch (q.getCategory()) {
            case "Personal Questions": prompt = PERSONAL_PROMPT; break;
            case "Project Questions": prompt = PROJECT_PROMPT; break;
            case "Video Clip Questions": prompt = VIDEO_CLIPS_PROMPT; break;
        }
        prompt += q.getFullQuestion();
        prompt += "\n\nIMPORTANT: If the audio contains no intelligible speech (empty audio/silence), " +
                "set topicDevelopment, delivery, vocabulary, language, and totalSectionScore to 0, " +
                "and in feedback.*.keep/feedback.*.improve write that no speech was detected. " +
                "Return ONLY valid JSON matching the schema.";

        GeminiManager.getInstance().sendTextWithFilePrompt(prompt, bytes, "audio/aac", new GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                pD.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleSimulationGradingResult(result, filePath, q);
                    }
                });
            }

            @Override
            public void onFailure(Throwable error) {
                pD.dismiss();
                Toast.makeText(SimulationsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSimulationGradingResult(String json, String filePath, Question q) {
        Map<String, TopicDetail> aiFeedBack = new HashMap<>();
        int totalScore;
        try {
            String cleanedJson = json.replaceAll("```json", "").replaceAll("```", "").trim();
            JSONObject root = new JSONObject(cleanedJson);

            int topicDevelopmentScore = root.getInt("topicDevelopment");
            int deliveryScore = root.getInt("delivery");
            int vocabularyScore = root.getInt("vocabulary");
            int languageScore = root.getInt("language");
            totalScore = root.getInt("totalSectionScore");

            JSONObject feedback = root.getJSONObject("feedback");

            aiFeedBack.put("topicDevelopment", new TopicDetail(topicDevelopmentScore, parseFeedbackSection(feedback.getJSONObject("topicDevelopment"))));
            aiFeedBack.put("delivery", new TopicDetail(deliveryScore, parseFeedbackSection(feedback.getJSONObject("delivery"))));
            aiFeedBack.put("vocabulary", new TopicDetail(vocabularyScore, parseFeedbackSection(feedback.getJSONObject("vocabulary"))));
            aiFeedBack.put("language", new TopicDetail(languageScore, parseFeedbackSection(feedback.getJSONObject("language"))));
            aiFeedBack.put("overall", new TopicDetail(totalScore, feedback.getString("overallSummary")));
        } catch (JSONException e) {
            Log.e("JSON_ERROR", "Failed to parse: " + json, e);
            Toast.makeText(this, "AI Formatting Error", Toast.LENGTH_SHORT).show();
            return;
        }

        saveSimulationToFirebase(aiFeedBack, totalScore, filePath, q);
    }

    private String parseFeedbackSection(JSONObject section) throws JSONException {
        return "Keep: " + section.getString("keep") + "\nImprove: " + section.getString("improve");
    }

    private void saveSimulationToFirebase(Map<String, TopicDetail> feedback, int score, String filePath, Question q) {
        String userId = refAuth.getCurrentUser() != null ? refAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        refRecordings.child(userId).child(q.getQuestionId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dS) {
                long count = dS.getChildrenCount() + 1;
                String base = (q.getSubTopic() == null || q.getSubTopic().equals("null")) ? q.getTopic() : q.getSubTopic();
                String finalTitle = base + " (Simulation " + count + ")";

                String recordingId = refRecordings.child(userId).push().getKey();
                if (recordingId == null) {
                    Toast.makeText(SimulationsActivity.this, "Failed to create recording id", Toast.LENGTH_SHORT).show();
                    return;
                }

                Recording rec = new Recording(userId, q.getQuestionId(), finalTitle, new Date(), score, feedback);
                rec.setRecordingId(recordingId);
                uploadSimulationAudioAndMetadata(rec, filePath);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SimulationsActivity.this, "Failed to save recording", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadSimulationAudioAndMetadata(Recording rec, String filePath) {
        StorageReference fileRef = refRecordingsMedia.child(rec.getUserId() + "/" + rec.getRecordingId() + ".aac");

        ProgressDialog pD = new ProgressDialog(this);
        pD.setCancelable(false);
        pD.setMessage("Saving simulation...");
        pD.show();

        fileRef.putFile(Uri.fromFile(new File(filePath)))
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        refRecordings.child(rec.getUserId()).child(rec.getQuestionId()).child(rec.getRecordingId()).setValue(rec)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        String simulationId = refSimulations.push().getKey();
                                        if (simulationId == null) {
                                            pD.dismiss();
                                            Toast.makeText(SimulationsActivity.this, "Failed to create simulation", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        Simulation sim = new Simulation();
                                        sim.setSimulationId(simulationId);
                                        sim.setUserId(rec.getUserId());
                                        sim.setDateCompleted(new Date());
                                        sim.setOverAllScore(rec.getScore());

                                        java.util.List<String> ids = new java.util.ArrayList<>();
                                        ids.add(rec.getRecordingId());
                                        sim.setRecordingsIds(ids);

                                        refSimulations.child(simulationId).setValue(sim)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        pD.dismiss();
                                                        Intent si = new Intent(SimulationsActivity.this, ResultsActivity.class);
                                                        si.putExtra("recording", rec);
                                                        si.putExtra("audio_path", filePath);
                                                        startActivity(si);
                                                        finish();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        pD.dismiss();
                                                        Toast.makeText(SimulationsActivity.this, "Failed to save simulation: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        pD.dismiss();
                                        Toast.makeText(SimulationsActivity.this, "Failed to save recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pD.dismiss();
                        Toast.makeText(SimulationsActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void prepareVideo(String videoUrl) {
        String videoId = extractVideoId(videoUrl);
        if (videoId.isEmpty()) return;

        currentVideoId = videoId;
        if (activeYouTubePlayer != null) {
            activeYouTubePlayer.cueVideo(videoId, 0f);
        }
    }

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

    private void updateTimeLabels() {
        int sec = currentProgress / 10;
        currentTimeTv.setText(String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60));
        totalTimeTv.setText(String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60));
    }

    private void updateRecordedTimeLabel() {
        recordedTimeTv.setText(String.format(Locale.getDefault(), "%02d:%02d", recordedSeconds / 60, recordedSeconds % 60));
    }

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

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

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
