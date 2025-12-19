package com.example.speakup;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

public class TtsHelper {
    private TextToSpeech tts;

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
     * Logic for the time-based SeekBar.
     * Calculates the character index based on the percentage of the bar.
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

    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}