package com.example.speakup;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;

import java.util.Locale;

/**
 * Helper class for managing Text-To-Speech (TTS) operations within the application.
 * <p>
 * This class wraps the Android {@link TextToSpeech} engine to provide a simplified interface for
 * text playback. It handles initialization, language selection (US English), and offers
 * specialized functionality like speaking from a specific percentage of the text, which
 * is useful for synchronization with UI components like SeekBars.
 * </p>
 */
public class TtsHelper {
    /**
     * The internal {@link TextToSpeech} engine instance.
     */
    private TextToSpeech tts;

    /**
     * Flag indicating whether the TTS engine has been successfully initialized and is ready for use.
     */
    private boolean isInitialized = false;

    /**
     * Constructs a new TtsHelper and begins the asynchronous initialization of the TTS engine.
     * <p>
     * On successful initialization, the language is set to {@link Locale#US} and the speech rate
     * is adjusted to 0.7f for better clarity. If initialization fails or the language is 
     * unavailable, a {@link Toast} message is displayed to the user.
     * </p>
     *
     * @param context The {@link Context} used to initialize the engine and display status messages.
     */
    public TtsHelper(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "Language not supported", Toast.LENGTH_SHORT).show();
                } else {
                    // Set a natural speaking rate
                    tts.setSpeechRate(0.7f);
                    isInitialized = true;
                }
            } else {
                Toast.makeText(context, "TTS initialization failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Registers a listener to receive callbacks during the speech synthesis process.
     *
     * @param listener The {@link UtteranceProgressListener} to be notified of speech start, 
     *                 completion, and errors.
     */
    public void setUtteranceProgressListener(UtteranceProgressListener listener) {
        if (tts != null) {
            tts.setOnUtteranceProgressListener(listener);
        }
    }

    /**
     * Speaks the provided text starting from a position determined by the given percentage.
     * <p>
     * This method stops any current playback before starting the new utterance. To ensure 
     * a smooth experience, it attempts to find the nearest word boundary after the calculated 
     * start index so that words are not cut in half.
     * </p>
     *
     * @param fullText   The complete string of text to be processed.
     * @param percentage A float between 0.0 (start of text) and 1.0 (end of text) indicating 
     *                   where playback should begin.
     */
    public void speakFromPercentage(String fullText, float percentage) {
        if (!isInitialized || fullText == null || fullText.isEmpty()) return;

        tts.stop();

        // 1. If we are at the very start (percentage is 0), speak the whole text
        if (percentage <= 0.01f) {
            tts.speak(fullText, TextToSpeech.QUEUE_FLUSH, null, "speech_utterance");
            return;
        }

        // 2. Otherwise, calculate where to jump
        int charIndex = (int) (fullText.length() * percentage);

        // Find the next space so we don't start in the middle of a word
        int nextSpace = fullText.indexOf(" ", charIndex);
        String remainingText;

        if (nextSpace != -1 && nextSpace < fullText.length()) {
            remainingText = fullText.substring(nextSpace).trim();
        } else {
            remainingText = fullText.substring(Math.min(charIndex, fullText.length())).trim();
        }

        if (!remainingText.isEmpty()) {
            tts.speak(remainingText, TextToSpeech.QUEUE_FLUSH, null, "speech_utterance");
        }
    }

    /**
     * Determines if the TTS engine is currently outputting speech.
     *
     * @return {@code true} if speaking, {@code false} if idle or not initialized.
     */
    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    /**
     * Interrupts and stops any current speech synthesis.
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    /**
     * Shuts down the TTS engine and releases associated resources.
     * <p>
     * This method should be called when the helper is no longer needed (e.g., in {@code onDestroy()} 
     * of an Activity) to avoid memory leaks and ensure the service is properly closed.
     * </p>
     */
    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
