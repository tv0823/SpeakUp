package com.example.speakup.Activities;

import android.os.Bundle;
import android.view.View;

import com.example.speakup.R;
import com.example.speakup.Utils.Utilities;

/**
 * Activity that provides help information and details about the application.
 * <p>
 * This activity displays a static view with information to guide the user on how to use the app
 * and background information about the project.
 * </p>
 */
public class HelpAndAboutActivity extends Utilities {

    /**
     * Initializes the activity and sets the layout for the help and about screen.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_and_about);
    }

    /**
     * Finishes the current activity and returns to the previous screen.
     *
     * @param view The view that was clicked to trigger the back navigation.
     */
    public void goBack(View view) {
        finish();
    }
}
