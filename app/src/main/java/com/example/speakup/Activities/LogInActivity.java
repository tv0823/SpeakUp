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

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class LogInActivity extends Utilities {
    EditText eTEmail, eTPass;
    CheckBox checkBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        checkBox = findViewById(R.id.checkBox);
    }

    public void goToSignUpActivity(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    public void goBack(View view) {
        finish();
    }

    public void logIn(View view) {
        String email = eTEmail.getText().toString();
        String pass = eTPass.getText().toString();
        boolean remember = checkBox.isChecked();

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
                                FirebaseUser user = refAuth.getCurrentUser();

                                Intent intent = new Intent(LogInActivity.this, QuickAccessActivity.class);
                                startActivity(intent);

                                SharedPreferences settings = getSharedPreferences("STATUS", MODE_PRIVATE);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putBoolean("stayConnected", checkBox.isChecked());
                                editor.commit();

                                Toast.makeText(LogInActivity.this, "User logged in successfully", Toast.LENGTH_SHORT).show();
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