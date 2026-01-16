package com.example.speakup;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.BlobPart;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.TextPart;

import java.util.ArrayList;
import java.util.List;

import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * Manager class for interacting with the Gemini generative AI model.
 * <p>
 * This class follows the singleton pattern to provide a centralized access point for
 * sending prompts (optionally with file data like audio) to the Gemini AI and handling
 * the responses via callbacks.
 * </p>
 */
public class GeminiManager {
    /**
     * The single instance of GeminiManager.
     */
    private static GeminiManager instance;

    /**
     * The GenerativeModel instance used to generate content.
     */
    private GenerativeModel gemini;

    /**
     * Private constructor for initializing the GenerativeModel.
     * Uses the "gemini-2.5-flash" model and the API key from BuildConfig.
     */
    private GeminiManager() {
        gemini = new GenerativeModel(
                "gemini-2.5-flash",
                BuildConfig.Gemini_API_Key
        );
    }

    /**
     * Returns the singleton instance of GeminiManager.
     *
     * @return The GeminiManager instance.
     */
    public static GeminiManager getInstance() {
        if (instance == null) {
            instance = new GeminiManager();
        }
        return instance;
    }

    /**
     * Sends a text prompt along with a file (blob) to the Gemini AI model.
     * <p>
     * This method constructs a multi-part content request and executes it asynchronously.
     * The result or error is returned through the provided {@link GeminiCallback}.
     * </p>
     *
     * @param prompt   The text prompt describing the task for the AI.
     * @param bytes    The byte array of the file data (e.g., audio recording).
     * @param mimeType The MIME type of the file data (e.g., "audio/aac").
     * @param callback The callback to handle success or failure of the AI request.
     */
    public void sendTextWithFilePrompt(String prompt, byte[] bytes, String mimeType, GeminiCallback callback) {
        List<Part> parts = new ArrayList<>();
        parts.add(new TextPart(prompt));
        parts.add(new BlobPart(mimeType, bytes));

        Content[] content = new Content[1];
        content[0] = new Content(parts);

        gemini.generateContent(content,
                new Continuation<GenerateContentResponse>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object result) {
                        if (result instanceof Result.Failure) {
                            Log.i("GeminiManager", "Error: " + ((Result.Failure) result).exception.getMessage());
                            callback.onFailure(((Result.Failure) result).exception);
                        } else {
                            callback.onSuccess(((GenerateContentResponse) result).getText());
                        }
                    }
                });
    }
}
