package com.example.speakup;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Base activity class providing common configuration for activities in the SpeakUp application.
 * <p>
 * This class extends {@link AppCompatActivity} and serves as a superclass for other activities.
 * It enforces light mode (disabling night mode) and applies edge-to-edge display logic
 * to ensure proper padding for system bars (status bar and navigation bar).
 * </p>
 */
public class Utilities extends AppCompatActivity {

    /**
     * Called when the activity is starting.
     * <p>
     * Forces the application to use the non-night mode (light mode) globally for this activity.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    /**
     * Set the activity content from a layout resource.  The resource will be
     * inflated, adding all top-level views to the activity.
     * <p>
     * This override also triggers the application of edge-to-edge insets to the root view.
     * </p>
     *
     * @param layoutResID Resource ID to be inflated.
     */
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyEdgeToEdgeLogic();
    }

    /**
     * Enables edge-to-edge display and sets a window insets listener.
     * <p>
     * This method ensures that the content view does not overlap with system bars (like the status bar
     * and navigation bar) by applying appropriate padding to the root view of the activity.
     * </p>
     */
    private void applyEdgeToEdgeLogic() {
        EdgeToEdge.enable(this);
        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }
}
