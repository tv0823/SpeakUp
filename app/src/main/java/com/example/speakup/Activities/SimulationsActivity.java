package com.example.speakup.Activities;

import static com.example.speakup.FBRef.refQuestions;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
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

import com.example.speakup.Objects.Question;
import com.example.speakup.R;
import com.example.speakup.RecordingManager;
import com.example.speakup.TtsHelper;
import com.example.speakup.Utilities;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulations);

        initViews();
        initLogic();
        checkPermissions();
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

        simulationTimer = new CountDownTimer(30 * 60 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            public void onFinish() {
                tvTimer.setText("00:00");
                finishSimulation();
            }
        }.start();
    }

    private void changeQuestion(int direction) {
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
        if (simulationTimer != null) {
            simulationTimer.cancel();
        }
        Toast.makeText(this, "Simulation finished!", Toast.LENGTH_LONG).show();
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
