package com.example.speakup.Activities;

import static com.example.speakup.FBRef.refAuth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;


import com.example.speakup.R;
import com.example.speakup.Utilities;

/**
 * Activity representing the welcome screen of the application.
 * <p>
 * This is the entry point for the application. It provides options for users to either log in
 * or sign up. It also checks for an existing user session on startup; if the user has previously
 * opted to stay connected, they are automatically redirected to the {@link com.example.speakup.Fragments.QuickAccessFragment}.
 * </p>
 */
public class WelcomeScreenActivity extends Utilities {

    /**
     * Called when the activity is starting.
     * Initializes the UI by setting the content view to the welcome screen layout.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_screen);
    }

    /**
     * Called when the activity is becoming visible to the user.
     * <p>
     * Checks if a user is currently logged in and if the "stayConnected" preference is enabled.
     * If both conditions are met, the user is automatically redirected to the {@link MasterActivity},
     * bypassing the login/signup screens.
     * </p>
     */
    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences settings = getSharedPreferences("STATUS", MODE_PRIVATE);
        Boolean isChecked = settings.getBoolean("stayConnected", false);
        if (refAuth.getCurrentUser() != null && isChecked) {
            Intent intent = new Intent(this, MasterActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Navigates to the SignUpActivity.
     *
     * @param view The view that was clicked.
     */
    public void goToSignUpActivity(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    /**
     * Navigates to the LogInActivity.
     *
     * @param view The view that was clicked.
     */
    public void goToLogInActivity(View view) {
        Intent intent = new Intent(this, LogInActivity.class);
        startActivity(intent);
    }
}
