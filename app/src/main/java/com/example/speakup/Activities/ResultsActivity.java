package com.example.speakup.Activities;

import static com.example.speakup.FBRef.refAuth;
import static com.example.speakup.FBRef.refRecordings;
import static com.example.speakup.FBRef.refRecordingsMedia;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.speakup.Objects.Recording;
import com.example.speakup.Objects.TopicDetail;
import com.example.speakup.R;
import com.example.speakup.Utilities;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Activity for displaying the results and AI feedback of a recorded practice session.
 * <p>
 * This activity handles:
 * <ul>
 * <li>Retrieving recording data from Intent or Firebase.</li>
 * <li>Displaying scores and feedback for different categories (Topic, Delivery, Vocabulary, Language).</li>
 * <li>Playback of the recorded audio with a progress seek bar.</li>
 * <li>Expandable UI sections for detailed feedback.</li>
 * </ul>
 * </p>
 */
public class ResultsActivity extends Utilities {
    private LinearLayout headerTopic, headerDelivery, headerVocab, headerLanguage;
    private TextView scoreTv, topicTv, deliveryTv, vocabTv, languageTv;
    private TextView contentTopic, contentDelivery, contentVocab, contentLanguage, contentSummary, recordedTimeTv;
    private ImageView arrowTopic, arrowDelivery, arrowVocab, arrowLanguage;
    private ProgressBar scoreProgressBar;
    private ImageButton playRecordingBtn;
    private SeekBar recordingSeekBar;

    private Intent gi;
    private File localAudioFile;
    private MediaPlayer mediaPlayer;
    private Handler seekBarHandler = new Handler();
    private Runnable updateSeekBarRunnable;

    /**
     * Called when the activity is starting.
     * Initializes views, retrieves intent data, and sets up expandable UI components.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        gi = getIntent();
        initViews();

        String recordingId = gi.getStringExtra("recording_id");
        String practiceCachePath = gi.getStringExtra("recordingPath");

        if (recordingId == null) {
            // WAY 1: Coming from Practice Screen
            Recording rec = (Recording) gi.getSerializableExtra("recording");
            if (rec != null) {
                setDataRecording(rec);
                handleAudioLogic(rec, practiceCachePath);
            }
        } else {
            // WAY 2: Coming from History Screen
            String questionId = gi.getStringExtra("question_Id");
            initRecordingData(questionId, recordingId);
        }

        setupExpandableBubble(headerTopic, contentTopic, arrowTopic);
        setupExpandableBubble(headerDelivery, contentDelivery, arrowDelivery);
        setupExpandableBubble(headerVocab, contentVocab, arrowVocab);
        setupExpandableBubble(headerLanguage, contentLanguage, arrowLanguage);
    }

    /**
     * Initializes all UI views and sets up the recording seek bar listener.
     */
    private void initViews() {
        headerTopic = findViewById(R.id.headerTopic);
        headerDelivery = findViewById(R.id.headerDelivery);
        headerVocab = findViewById(R.id.headerVocab);
        headerLanguage = findViewById(R.id.headerLanguage);

        scoreTv = findViewById(R.id.scoreTv);
        topicTv = findViewById(R.id.topicTv);
        deliveryTv = findViewById(R.id.deliveryTv);
        vocabTv = findViewById(R.id.vocabTv);
        languageTv = findViewById(R.id.languageTv);

        contentTopic = findViewById(R.id.contentTopic);
        contentDelivery = findViewById(R.id.contentDelivery);
        contentVocab = findViewById(R.id.contentVocab);
        contentLanguage = findViewById(R.id.contentLanguage);
        contentSummary = findViewById(R.id.contentSummary);

        arrowTopic = findViewById(R.id.arrowTopic);
        arrowDelivery = findViewById(R.id.arrowDelivery);
        arrowVocab = findViewById(R.id.arrowVocab);
        arrowLanguage = findViewById(R.id.arrowLanguage);

        scoreProgressBar = findViewById(R.id.scoreProgressBar);
        playRecordingBtn = findViewById(R.id.playRecordingBtn);
        recordingSeekBar = findViewById(R.id.recordingSeekBar);
        recordedTimeTv = findViewById(R.id.recordedTimeTv);

        recordingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    updateTimeUI(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /**
     * Fetches recording data from Firebase Database for a specific user and question.
     *
     * @param questionId The ID of the question the recording belongs to.
     * @param recId      The unique ID of the recording.
     */
    private void initRecordingData(String questionId, String recId) {
        String userId = refAuth.getCurrentUser().getUid();
        refRecordings.child(userId).child(questionId).child(recId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dS) {
                Recording rec = dS.getValue(Recording.class);
                if (rec != null) {
                    setDataRecording(rec);
                    handleAudioLogic(rec, null);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError dbError) {}
        });
    }

    /**
     * Manages the logic for obtaining the audio file (either from local cache or Firebase).
     *
     * @param rec       The recording object.
     * @param cachePath The path to a temporary cache file, if available.
     */
    private void handleAudioLogic(Recording rec, String cachePath) {
        localAudioFile = new File(getFilesDir(), rec.getRecordingId() + ".aac");

        if (localAudioFile.exists()) return;

        if (cachePath != null) {
            File tempFile = new File(cachePath);
            if (tempFile.exists()) {
                moveFile(tempFile, localAudioFile);
                return;
            }
        }
        downloadFromFirebase(rec);
    }

    /**
     * Downloads the audio file associated with the recording from Firebase Storage.
     *
     * @param rec The recording object containing metadata for the download.
     */
    private void downloadFromFirebase(Recording rec) {
        StorageReference refFile = refRecordingsMedia.child(rec.getUserId() + "/" + rec.getRecordingId() + ".aac");
        refFile.getFile(localAudioFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d("Audio", "Downloaded");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("Audio", "Download failed");
            }
        });
    }

    /**
     * Moves a file from a source location to a destination location.
     *
     * @param source      The file to be moved.
     * @param destination The destination location.
     */
    private void moveFile(File source, File destination) {
        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(destination)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            source.delete();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Populates the UI with data from the provided Recording object.
     *
     * @param rec The recording data to display.
     */
    private void setDataRecording(Recording rec) {
        Map<String, TopicDetail> fb = rec.getAiFeedBack();
        scoreTv.setText(String.valueOf(rec.getScore()));
        scoreProgressBar.setProgress(rec.getScore());

        if (fb != null) {
            updateBubbleUI(fb.get("topicDevelopment"), topicTv, contentTopic, "Topic Development", 50);
            updateBubbleUI(fb.get("delivery"), deliveryTv, contentDelivery, "Delivery", 15);
            updateBubbleUI(fb.get("vocabulary"), vocabTv, contentVocab, "Vocabulary", 20);
            updateBubbleUI(fb.get("language"), languageTv, contentLanguage, "Language", 15);

            TopicDetail summary = fb.get("overall");
            if (summary != null) contentSummary.setText(summary.getSummary());
        }
    }

    /**
     * Updates the UI for a specific feedback category (bubble).
     *
     * @param detail  The TopicDetail object containing score and summary.
     * @param title   The TextView for the category title.
     * @param content The TextView for the category feedback content.
     * @param label   The label name for the category.
     * @param max     The maximum possible score for this category.
     */
    private void updateBubbleUI(TopicDetail detail, TextView title, TextView content, String label, int max) {
        if (detail != null) {
            title.setText(label + " (" + detail.getScore() + "/" + max + ")");
            content.setText(detail.getSummary());
        }
    }

    /**
     * Toggles between play and pause for the recorded audio.
     *
     * @param view The view that was clicked.
     */
    public void playPauseRecording(View view) {
        if (localAudioFile == null || !localAudioFile.exists()) {
            Toast.makeText(this, "Loading audio...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playRecordingBtn.setImageResource(android.R.drawable.ic_media_play);
        } else if (mediaPlayer != null) {
            mediaPlayer.start();
            playRecordingBtn.setImageResource(android.R.drawable.ic_media_pause);
            startSeekBarUpdate();
        } else {
            initMediaPlayer();
        }
    }

    /**
     * Initializes the MediaPlayer with the local audio file and starts playback.
     */
    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(localAudioFile.getAbsolutePath());
            mediaPlayer.prepare();
            recordingSeekBar.setMax(mediaPlayer.getDuration());
            mediaPlayer.start();
            playRecordingBtn.setImageResource(android.R.drawable.ic_media_pause);
            startSeekBarUpdate();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playRecordingBtn.setImageResource(android.R.drawable.ic_media_play);
                    recordingSeekBar.setProgress(0);
                    stopSeekBarUpdate();
                }
            });
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Starts the periodic update of the recording seek bar and time display.
     */
    private void startSeekBarUpdate() {
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int current = mediaPlayer.getCurrentPosition();
                    recordingSeekBar.setProgress(current);
                    updateTimeUI(current);
                    seekBarHandler.postDelayed(this, 500);
                }
            }
        };
        seekBarHandler.postDelayed(updateSeekBarRunnable, 0);
    }

    /**
     * Stops the periodic update of the recording seek bar.
     */
    private void stopSeekBarUpdate() {
        if (updateSeekBarRunnable != null) seekBarHandler.removeCallbacks(updateSeekBarRunnable);
    }

    /**
     * Updates the time text view based on the current playback position in milliseconds.
     *
     * @param ms Current position in milliseconds.
     */
    private void updateTimeUI(int ms) {
        int sec = (ms / 1000) % 60;
        int min = (ms / 1000) / 60;
        recordedTimeTv.setText(String.format("%02d:%02d", min, sec));
    }

    /**
     * Configures a layout section to be expandable on click, toggling content visibility and arrow rotation.
     *
     * @param header  The clickable layout header.
     * @param content The TextView content to be shown/hidden.
     * @param arrow   The ImageView arrow to rotate.
     */
    private void setupExpandableBubble(final LinearLayout header, final TextView content, final ImageView arrow) {
        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (content.getVisibility() == View.GONE) {
                    content.setVisibility(View.VISIBLE);
                    arrow.setRotation(180f);
                } else {
                    content.setVisibility(View.GONE);
                    arrow.setRotation(0f);
                }
            }
        });
    }

    /**
     * Finishes the activity and returns to the previous screen.
     *
     * @param v The view that was clicked.
     */
    public void goBack(View v) { finish(); }

    /**
     * Performs final cleanup before the activity is destroyed.
     * Releases the MediaPlayer and stops seek bar updates.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        stopSeekBarUpdate();
    }
}
