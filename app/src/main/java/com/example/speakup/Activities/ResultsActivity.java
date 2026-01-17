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
 * Activity for displaying the results and AI-generated feedback of a recorded practice session.
 * <p>
 * This activity provides a comprehensive review of the user's performance, including:
 * <ul>
 *     <li>Overall score visualization via a ProgressBar and TextView.</li>
 *     <li>Detailed feedback sections for Topic Development, Delivery, Vocabulary, and Language.</li>
 *     <li>Expandable UI components to toggle the visibility of specific feedback details.</li>
 *     <li>Audio playback functionality to listen to the original recording with a synchronized SeekBar.</li>
 *     <li>Automatic synchronization of audio files between local cache and Firebase Storage.</li>
 * </ul>
 * </p>
 */
public class ResultsActivity extends Utilities {

    /**
     * Header container for the Topic Development feedback section.
     */
    private LinearLayout headerTopic;

    /**
     * Header container for the Delivery feedback section.
     */
    private LinearLayout headerDelivery;

    /**
     * Header container for the Vocabulary feedback section.
     */
    private LinearLayout headerVocab;

    /**
     * Header container for the Language feedback section.
     */
    private LinearLayout headerLanguage;

    /**
     * TextView for displaying the overall score (e.g., "85/100").
     */
    private TextView scoreTv;

    /**
     * TextView for the Topic Development section title and score.
     */
    private TextView topicTv;

    /**
     * TextView for the Delivery section title and score.
     */
    private TextView deliveryTv;

    /**
     * TextView for the Vocabulary section title and score.
     */
    private TextView vocabTv;

    /**
     * TextView for the Language section title and score.
     */
    private TextView languageTv;

    /**
     * TextView for the detailed feedback text of the Topic Development section.
     */
    private TextView contentTopic;

    /**
     * TextView for the detailed feedback text of the Delivery section.
     */
    private TextView contentDelivery;

    /**
     * TextView for the detailed feedback text of the Vocabulary section.
     */
    private TextView contentVocab;

    /**
     * TextView for the detailed feedback text of the Language section.
     */
    private TextView contentLanguage;

    /**
     * TextView for the overall AI-generated summary of the practice session.
     */
    private TextView contentSummary;

    /**
     * TextView displaying the current playback time of the recording.
     */
    private TextView recordedTimeTv;

    /**
     * Arrow indicator for the Topic Development feedback section.
     */
    private ImageView arrowTopic;

    /**
     * Arrow indicator for the Delivery feedback section.
     */
    private ImageView arrowDelivery;

    /**
     * Arrow indicator for the Vocabulary feedback section.
     */
    private ImageView arrowVocab;

    /**
     * Arrow indicator for the Language feedback section.
     */
    private ImageView arrowLanguage;

    /**
     * ProgressBar visualizing the overall score.
     */
    private ProgressBar scoreProgressBar;

    /**
     * Button to toggle playback (Play/Pause) of the recorded audio.
     */
    private ImageButton playRecordingBtn;

    /**
     * SeekBar representing the progress of the audio recording playback.
     */
    private SeekBar recordingSeekBar;

    /**
     * The intent used to launch this activity, containing the {@link Recording} object.
     */
    private Intent gi;

    /**
     * Reference to the local audio file (.aac) for playback.
     */
    private File localAudioFile;

    /**
     * MediaPlayer instance for handling audio playback of the recording.
     */
    private MediaPlayer mediaPlayer;

    /**
     * Handler for managing periodic UI updates of the SeekBar during playback.
     */
    private final Handler seekBarHandler = new Handler();

    /**
     * Runnable task that updates the SeekBar progress and time labels during playback.
     */
    private Runnable updateSeekBarRunnable;

    /**
     * Initializes the activity, sets up the UI, and triggers the data loading process.
     * <p>
     * It retrieves the {@link Recording} data and the audio path from the intent. 
     * If coming from the practice screen, it attempts to move the cached recording to internal storage.
     * Otherwise, it downloads the file from Firebase.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being 
     *                           shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        gi = getIntent();
        initViews();

        Recording rec = (Recording) gi.getSerializableExtra("recording");
        if (rec != null) {
            setDataRecording(rec);
        }

        String practiceCachePath = gi.getStringExtra("audio_path");

        if (practiceCachePath != null) {
            // WAY 1: Coming from Practice Screen
            handleAudioLogic(rec, practiceCachePath);
        } else {
            // WAY 2: Coming from Past Recording Screen
            handleAudioLogic(rec, null);
        }

        setupExpandableBubble(headerTopic, contentTopic, arrowTopic);
        setupExpandableBubble(headerDelivery, contentDelivery, arrowDelivery);
        setupExpandableBubble(headerVocab, contentVocab, arrowVocab);
        setupExpandableBubble(headerLanguage, contentLanguage, arrowLanguage);
    }

    /**
     * Finds and initializes all UI components from the layout.
     * Sets up the {@link SeekBar.OnSeekBarChangeListener} to allow user seeking during playback.
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
     * Handles the acquisition of the audio file for playback.
     * <p>
     * If the file exists in internal storage, it uses it. If a cache path is provided (from a 
     * fresh recording), it moves that file to internal storage. Otherwise, it triggers a 
     * download from Firebase Storage.
     * </p>
     *
     * @param rec       The {@link Recording} object containing the unique ID.
     * @param cachePath The temporary path of a newly recorded audio file, or null.
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
     * Downloads the audio recording file from Firebase Storage and saves it to internal storage.
     *
     * @param rec The {@link Recording} object containing metadata required for the storage path.
     */
    private void downloadFromFirebase(Recording rec) {
        StorageReference refFile = refRecordingsMedia.child(rec.getUserId() + "/" + rec.getRecordingId() + ".aac");
        refFile.getFile(localAudioFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d("Audio", "Downloaded successfully");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("Audio", "Download failed", e);
            }
        });
    }

    /**
     * Moves a file from a source location to a destination location using streams.
     * The source file is deleted upon successful transfer.
     *
     * @param source      The source {@link File}.
     * @param destination The destination {@link File}.
     */
    private void moveFile(File source, File destination) {
        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
            source.delete();
        } catch (Exception e) {
            Log.e("FileMove", "Error moving file", e);
        }
    }

    /**
     * Populates the activity's UI components with data from the {@link Recording} object.
     *
     * @param rec The {@link Recording} data to be displayed.
     */
    private void setDataRecording(Recording rec) {
        Map<String, TopicDetail> fb = rec.getAiFeedBack();
        scoreTv.setText(rec.getScore() + "/100");
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
     * Updates a specific feedback category's UI components.
     *
     * @param detail  The {@link TopicDetail} containing the score and descriptive summary.
     * @param title   The TextView for the category's header title.
     * @param content The TextView containing the detailed feedback text.
     * @param label   The display name of the category (e.g., "Delivery").
     * @param max     The maximum possible points for this specific category.
     */
    private void updateBubbleUI(TopicDetail detail, TextView title, TextView content, String label, int max) {
        if (detail != null) {
            title.setText(label + " (" + detail.getScore() + "/" + max + ")");
            content.setText(detail.getSummary());
        }
    }

    /**
     * Toggles between playing and pausing the audio recording.
     * Initializes the MediaPlayer if it's the first time playing.
     *
     * @param view The view that was clicked (Play/Pause button).
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
     * Configures and starts the {@link MediaPlayer} with the local audio file.
     * Sets the SeekBar max value and defines the completion listener.
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
                    updateTimeUI(0);
                }
            });
        } catch (IOException e) {
            Log.e("MediaPlayer", "Initialization failed", e);
        }
    }

    /**
     * Begins the periodic update of the SeekBar and time UI during audio playback.
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
     * Cancels any pending SeekBar UI updates.
     */
    private void stopSeekBarUpdate() {
        if (updateSeekBarRunnable != null) {
            seekBarHandler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    /**
     * Updates the time TextView based on the current millisecond position of the audio.
     *
     * @param ms The current playback position in milliseconds.
     */
    private void updateTimeUI(int ms) {
        int sec = (ms / 1000) % 60;
        int min = (ms / 1000) / 60;
        recordedTimeTv.setText(String.format("%02d:%02d", min, sec));
    }

    /**
     * Sets up an expandable layout bubble where clicking the header toggles the content visibility.
     *
     * @param header  The {@link LinearLayout} header that acts as a toggle button.
     * @param content The {@link TextView} that is shown or hidden.
     * @param arrow   The {@link ImageView} representing an arrow that rotates based on state.
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
     * Closes the results screen and returns to the previous activity.
     *
     * @param v The view that was clicked.
     */
    public void goBack(View v) {
        finish();
    }

    /**
     * Performs cleanup before the activity is destroyed.
     * Releases the {@link MediaPlayer} and stops UI update handlers.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopSeekBarUpdate();
    }
}
