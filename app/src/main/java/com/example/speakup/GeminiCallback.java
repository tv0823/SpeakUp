package com.example.speakup;

/**
 * Callback interface for handling responses from Gemini AI requests.
 * <p>
 * This interface provides methods to handle successful results and error cases
 * when interacting with the Gemini generative AI model.
 * </p>
 */
public interface GeminiCallback {
    /**
     * Called when the Gemini AI request is completed successfully.
     *
     * @param result The generated text response from the AI model.
     */
    void onSuccess(String result);

    /**
     * Called when an error occurs during the Gemini AI request.
     *
     * @param error The exception or error that caused the failure.
     */
    void onFailure(Throwable error);
}
