package com.example.speakup;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Manager class responsible for handling multi-stage audio recording.
 * <p>
 * This class allows recording audio in multiple chunks (to support pause/resume functionality),
 * merging those chunks into a single finalized AAC file, and managing the lifecycle of
 * recording files in the application's external cache directory.
 * </p>
 */
public class RecordingManager {
    /**
     * The MediaRecorder instance used for capturing audio.
     */
    private MediaRecorder recorder;

    /**
     * The MediaPlayer instance used for cleaning up resources.
     */
    private MediaPlayer mediaPlayer;

    /**
     * The absolute path where the merged final audio file will be stored.
     */
    private String finalFileName;

    /**
     * A list of temporary file chunks created during a multi-stage recording session.
     */
    private ArrayList<File> audioChunks = new ArrayList<>();

    /**
     * Counter for the number of chunks recorded in the current session.
     */
    private int chunkCount = 0;

    /**
     * Flag indicating if a recording chunk is currently active.
     */
    private boolean isRecording = false;

    /**
     * Flag indicating if the recording session is currently paused.
     */
    private boolean isPaused = false;

    /**
     * Flag indicating if the user has paused the recording at least once.
     */
    private boolean hasPausedOnce = false;

    /**
     * Flag indicating if the chunks have been merged into the final file.
     */
    private boolean isFinalized = false;

    /**
     * The application context used to access cache directories.
     */
    private Context context;

    /**
     * Constructs a new RecordingManager.
     *
     * @param context  The Context used to access the cache directory.
     * @param fileName The desired name for the finalized audio file (e.g., "my_recording.aac").
     */
    public RecordingManager(Context context, String fileName) {
        this.context = context;
        this.finalFileName = context.getExternalCacheDir().getAbsolutePath() + "/" + fileName;
    }

    /**
     * Starts recording a new audio chunk.
     * Creates a temporary file for the chunk and configures the MediaRecorder for AAC output.
     */
    public void startRecordingChunk() {
        isRecording = true;
        chunkCount++;
        File chunkFile = new File(context.getExternalCacheDir(), "chunk_" + System.currentTimeMillis() + "_" + chunkCount + ".aac");
        audioChunks.add(chunkFile);

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioSamplingRate(44100);
        recorder.setOutputFile(chunkFile.getAbsolutePath());

        try {
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            Log.e("RecordingManager", "Start failed", e);
        }
    }

    /**
     * Stops the current recording chunk and releases the MediaRecorder resources.
     */
    public void stopRecordingChunk() {
        if (recorder != null) {
            try {
                recorder.stop();
                recorder.release();
            } catch (Exception e) {
                Log.e("RecordingManager", "Stop failed", e);
            }
            recorder = null;
        }
        isRecording = false;
    }

    /**
     * Merges all recorded audio chunks into a single finalized audio file.
     * Sets the finalized state to true upon successful completion.
     */
    public void mergeChunks() {
        File finalFile = new File(finalFileName);
        try (FileOutputStream fos = new FileOutputStream(finalFile)) {
            for (File chunk : audioChunks) {
                if (chunk.exists()) {
                    byte[] chunkBytes = getBytes(chunk.getAbsolutePath());
                    fos.write(chunkBytes);
                }
            }
            fos.flush();
            isFinalized = true;
        } catch (IOException e) {
            Log.e("RecordingManager", "Failed to merge", e);
        }
    }

    /**
     * Reads the content of a file into a byte array.
     *
     * @param filePath The absolute path of the file to read.
     * @return A byte array containing the file's content.
     * @throws IOException If the file is not found or an error occurs during reading.
     */
    public byte[] getBytes(String filePath) throws IOException {
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
     * Returns the path to the finalized merged file if it exists, otherwise returns the first chunk path.
     *
     * @return The absolute path to the recording file, or null if no recording exists.
     */
    public String getFinalFilePath() {
        return isFinalized ? finalFileName : (audioChunks.isEmpty() ? null : audioChunks.get(0).getAbsolutePath());
    }

    /**
     * Deletes all temporary chunks and the finalized audio file, resetting the manager state.
     */
    public void clearAllFiles() {
        new File(finalFileName).delete();
        for (File chunk : audioChunks) {
            if (chunk.exists()) chunk.delete();
        }
        audioChunks.clear();
        chunkCount = 0;
        isFinalized = false;
        isPaused = false;
        hasPausedOnce = false;
    }

    /**
     * Releases all recording and playback resources held by the manager.
     */
    public void release() {
        stopRecordingChunk();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * Checks if a recording chunk is currently being captured.
     * @return true if recording, false otherwise.
     */
    public boolean isRecording() { return isRecording; }

    /**
     * Checks if the recording session is currently paused.
     * @return true if paused, false otherwise.
     */
    public boolean isPaused() { return isPaused; }

    /**
     * Sets the paused state of the recording session.
     * @param paused The new paused state.
     */
    public void setPaused(boolean paused) { isPaused = paused; }

    /**
     * Checks if the user has paused the recording at least once in this session.
     * @return true if paused once, false otherwise.
     */
    public boolean hasPausedOnce() { return hasPausedOnce; }

    /**
     * Sets the flag indicating if the user has paused the recording at least once.
     * @param hasPausedOnce The new flag value.
     */
    public void setHasPausedOnce(boolean hasPausedOnce) { this.hasPausedOnce = hasPausedOnce; }

    /**
     * Checks if the recording has been finalized (chunks merged).
     * @return true if finalized, false otherwise.
     */
    public boolean isFinalized() { return isFinalized; }

    /**
     * Sets the finalized state of the recording.
     * @param finalized The new finalized state.
     */
    public void setFinalized(boolean finalized) { isFinalized = finalized; }

    /**
     * Gets the list of temporary audio chunk files.
     * @return An ArrayList of File objects.
     */
    public ArrayList<File> getAudioChunks() { return audioChunks; }
}
