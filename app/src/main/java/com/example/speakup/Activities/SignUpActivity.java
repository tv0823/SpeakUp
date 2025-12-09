package com.example.speakup.Activities;

import static com.example.speakup.FBRef.refAuth;
import static com.example.speakup.FBRef.refUsers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.speakup.Objects.User;
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

public class SignUpActivity extends Utilities {
    EditText eTUsername, eTEmail, eTPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        eTUsername = findViewById(R.id.eTUsername);
        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);

    }

    public void goToLogInActivity(View view) {
        Intent intent = new Intent(this, LogInActivity.class);
        startActivity(intent);
    }

    public void goBack(View view) {
        finish();
    }

    public void createUser(View view) {
        String email = eTEmail.getText().toString();
        String pass = eTPass.getText().toString();
        String username = eTUsername.getText().toString();

        if (email.isEmpty() || pass.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
        } else {
            ProgressDialog pd = new ProgressDialog(this);
            pd.setTitle("Connecting");
            pd.setMessage("Creating user...");
            pd.show();

            refAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            pd.dismiss();

                            if (task.isSuccessful()) {
                                Log.i("SignUpActivity", "createUserWithEmailAndPassword:success");
                                FirebaseUser user = refAuth.getCurrentUser();

                                //create the user and add it to the database
                                User newUser = new User(user.getUid(), eTUsername.getText().toString());
                                refUsers.child(user.getUid()).setValue(newUser);

                                Toast.makeText(SignUpActivity.this, "User created successfully", Toast.LENGTH_SHORT).show();


                            } else {
                                Exception exp = task.getException();

                                if (exp instanceof FirebaseAuthInvalidUserException) {
                                    Toast.makeText(SignUpActivity.this, "Invalid email address.", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseAuthWeakPasswordException) {
                                    Toast.makeText(SignUpActivity.this, "Password too weak.", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseAuthUserCollisionException) {
                                    Toast.makeText(SignUpActivity.this, "User already exists.", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(SignUpActivity.this, "General authentication failure.", Toast.LENGTH_SHORT).show();
                                } else if (exp instanceof FirebaseNetworkException) {
                                    Toast.makeText(SignUpActivity.this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(SignUpActivity.this, "An error occurred. Please try again later.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
        }
    }

    public void uploadPfp(View view) {
    }
}