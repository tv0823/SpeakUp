package com.example.speakup.Activities;

import static com.example.speakup.FBRef.refAuth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.speakup.Fragments.QuickAccessFragment;
import com.example.speakup.R;
import com.example.speakup.Utilities;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

/**
 * Activity handling the user login process.
 * <p>
 * This activity provides a user interface for existing users to log in using their email and password.
 * It interacts with Firebase Authentication to verify credentials and manages user session preferences.
 * </p>
 */
public class LogInActivity extends Utilities {
    /**
     * Input field for the user's email address.
     */
    EditText eTEmail;

    /**
     * Input field for the user's password.
     */
    EditText eTPass;

    /**
     * CheckBox to remember the user's login status.
     */
    CheckBox checkBox;

    /**
     * Called when the activity is starting.
     * Initializes the UI components.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        checkBox = findViewById(R.id.checkBox);
    }

    /**
     * Navigates to the SignUpActivity for new users to register.
     *
     * @param view The view that was clicked.
     */
    public void goToSignUpActivity(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    /**
     * Finishes the current activity and returns to the previous screen.
     *
     * @param view The view that was clicked.
     */
    public void goBack(View view) {
        finish();
    }

    /**
     * Attempts to log in the user using the provided credentials.
     * <p>
     * Validates that email and password fields are not empty.
     * Shows a progress dialog during the authentication process.
     * On success, navigates to {@link QuickAccessFragment} and saves the login status if "Remember me" is checked.
     * On failure, displays a specific error message based on the exception type.
     * </p>
     *
     * @param view The view that was clicked.
     */
    public void logIn(View view) {
        String email = eTEmail.getText().toString();
        String pass = eTPass.getText().toString();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
        } else {
            ProgressDialog pd = new ProgressDialog(this);
            pd.setTitle("Connecting");
            pd.setMessage("Logging in user...");
            pd.show();

            refAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            pd.dismiss();

                            if (task.isSuccessful()) {
                                Log.i("LogInActivity", "signInWithEmailAndPassword:success");
                                SharedPreferences settings = getSharedPreferences("STATUS", MODE_PRIVATE);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putBoolean("stayConnected", checkBox.isChecked());
                                editor.commit();

                                Toast.makeText(LogInActivity.this, "User logged in successfully", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(LogInActivity.this, MasterActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Exception exp = task.getException();

                                if (exp instanceof FirebaseAuthInvalidUserException) {
                                    Toast.makeText(LogInActivity.this, "Invalid email address.", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseAuthWeakPasswordException) {
                                    Toast.makeText(LogInActivity.this, "Password too weak.", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseAuthUserCollisionException) {
                                    Toast.makeText(LogInActivity.this, "User already exists.", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(LogInActivity.this, "General authentication failure.", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseNetworkException) {
                                    Toast.makeText(LogInActivity.this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(LogInActivity.this, "An error occurred. Please try again later.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
        }
    }
}
