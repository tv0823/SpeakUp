package com.example.speakup;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

/**
 * Helper class for managing Text-To-Speech (TTS) operations.
 * <p>
 * This class encapsulates the Android {@link TextToSpeech} engine, providing methods to
 * initialize the service, control playback (start, stop), and speak text starting from a
 * specific progress point (percentage). It sets the language to US English and a custom
 * speech rate.
 * </p>
 */
public class TtsHelper {
    /**
     * The internal TextToSpeech engine instance.
     */
    private TextToSpeech tts;

    /**
     * Constructs a new TtsHelper and initializes the TextToSpeech engine.
     * <p>
     * The initialization process sets the language to {@link Locale#US} and the speech rate to 0.8x.
     * Displays a Toast message if initialization fails or the language is not supported.
     * </p>
     *
     * @param context The application context used to initialize the TextToSpeech engine and show Toasts.
     */
    public TtsHelper(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "Language not supported", Toast.LENGTH_SHORT).show();
                } else {
                    // Set a natural speaking rate
                    tts.setSpeechRate(0.8f);
                }
            } else {
                Toast.makeText(context, "TTS initialization failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Speaks the provided text starting from a calculated character index based on a percentage.
     * <p>
     * This method is useful for seeking within the text (e.g., using a SeekBar).
     * It attempts to start speaking from the beginning of the next word to avoid cutting words in half.
     * </p>
     *
     * @param fullText   The complete text to be spoken.
     * @param percentage The percentage (0.0 to 1.0) indicating the starting position.
     */
    public void speakFromPercentage(String fullText, float percentage) {
        if (fullText == null || fullText.isEmpty()) return;

        tts.stop();

        // 1. If we are at the very start (percentage is 0), speak the whole text
        if (percentage <= 0.01f) {
            tts.speak(fullText, TextToSpeech.QUEUE_FLUSH, null, "start_speech");
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
            remainingText = fullText.substring(charIndex).trim();
        }

        if (!remainingText.isEmpty()) {
            tts.speak(remainingText, TextToSpeech.QUEUE_FLUSH, null, "jumped_speech");
        }
    }

    /**
     * Checks if the TTS engine is currently speaking.
     *
     * @return {@code true} if the engine is speaking, {@code false} otherwise.
     */
    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    /**
     * Stops the current TTS playback.
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    /**
     * Stops playback and releases the TextToSpeech resources.
     * Should be called when the helper is no longer needed (e.g., in Activity.onDestroy).
     */
    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
