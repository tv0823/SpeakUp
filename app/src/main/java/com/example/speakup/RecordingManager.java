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

public class RecordingManager {
    private MediaRecorder recorder;
    private MediaPlayer mediaPlayer;
    private String finalFileName;
    private ArrayList<File> audioChunks = new ArrayList<>();
    private int chunkCount = 0;

    private boolean isRecording = false;
    private boolean isPaused = false;
    private boolean hasPausedOnce = false;
    private boolean isFinalized = false;

    private Context context;

    public RecordingManager(Context context, String fileName) {
        this.context = context;
        this.finalFileName = context.getExternalCacheDir().getAbsolutePath() + "/" + fileName;
    }

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

    public String getFinalFilePath() {
        return isFinalized ? finalFileName : (audioChunks.isEmpty() ? null : audioChunks.get(0).getAbsolutePath());
    }

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

    public void release() {
        stopRecordingChunk();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // Getters and Setters for state
    public boolean isRecording() { return isRecording; }
    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { isPaused = paused; }
    public boolean hasPausedOnce() { return hasPausedOnce; }
    public void setHasPausedOnce(boolean hasPausedOnce) { this.hasPausedOnce = hasPausedOnce; }
    public boolean isFinalized() { return isFinalized; }
    public void setFinalized(boolean finalized) { isFinalized = finalized; }
    public ArrayList<File> getAudioChunks() { return audioChunks; }
}
