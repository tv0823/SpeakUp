package com.example.speakup;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;

import java.util.Locale;

/**
 * Helper class for managing Text-To-Speech (TTS) operations within the
 * application.
 * <p>
 * This class wraps the Android {@link TextToSpeech} engine to provide a
 * simplified interface for
 * text playback. It handles initialization, language selection (US English),
 * and offers
 * specialized functionality like speaking from a specific percentage of the
 * text, which
 * is useful for synchronization with UI components like SeekBars.
 * </p>
 */
public class TtsHelper {
    /**
     * The internal {@link TextToSpeech} engine instance.
     */
    private TextToSpeech tts;

    /**
     * Flag indicating whether the TTS engine has been successfully initialized and
     * is ready for use.
     */
    private boolean isInitialized = false;

    /**
     * Callback for TTS initialization status.
     */
    public interface TtsInitListener {
        void onInitStatus(boolean success);
    }

    private TtsInitListener initListener;

    /**
     * Constructs a new TtsHelper and begins the asynchronous initialization of the
     * TTS engine.
     * <p>
     * On successful initialization, the language is set to {@link Locale#US} and
     * the speech rate
     * is adjusted to 0.7f for better clarity. If initialization fails or the
     * language is
     * unavailable, a {@link Toast} message is displayed to the user.
     * </p>
     *
     * @param context The {@link Context} used to initialize the engine and display
     *                status messages.
     */
    public TtsHelper(final Context context) {
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(context, "Language not supported", Toast.LENGTH_SHORT).show();
                    } else {
                        // Set a natural speaking rate
                        tts.setSpeechRate(0.7f);
                        isInitialized = true;
                        if (initListener != null)
                            initListener.onInitStatus(true);
                    }
                } else {
                    Toast.makeText(context, "TTS initialization failed", Toast.LENGTH_SHORT).show();
                    if (initListener != null)
                        initListener.onInitStatus(false);
                }
            }
        });
    }

    /**
     * Sets a listener to be notified of the TTS engine initialization status.
     *
     * @param listener The {@link TtsInitListener} to receive the status update.
     */
    public void setTtsInitListener(TtsInitListener listener) {
        this.initListener = listener;
        // If it's already initialized, notify the listener immediately
        if (isInitialized && initListener != null) {
            initListener.onInitStatus(true);
        }
    }

    /**
     * Returns whether the TTS engine is currently initialized and ready for use.
     *
     * @return {@code true} if initialized, {@code false} otherwise.
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Registers a listener to receive callbacks during the speech synthesis
     * process.
     *
     * @param listener The {@link UtteranceProgressListener} to be notified of
     *                 speech start,
     *                 completion, and errors.
     */
    public void setUtteranceProgressListener(UtteranceProgressListener listener) {
        if (tts != null) {
            tts.setOnUtteranceProgressListener(listener);
        }
    }

    /**
     * Speaks the provided text starting from a specific character index.
     * <p>
     * This method stops any current playback before starting the new utterance.
     * </p>
     *
     * @param fullText  The complete string of text.
     * @param charIndex The index of the character to start speaking from.
     */
    public void speakFromIndex(String fullText, int charIndex) {
        if (!isInitialized || fullText == null || fullText.isEmpty())
            return;

        tts.stop();

        if (charIndex >= fullText.length())
            return;
        if (charIndex < 0)
            charIndex = 0;

        String remainingText = fullText.substring(charIndex);

        if (!remainingText.trim().isEmpty()) {
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
     * This method should be called when the helper is no longer needed (e.g., in
     * {@code onDestroy()}
     * of an Activity) to avoid memory leaks and ensure the service is properly
     * closed.
     * </p>
     */
    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
