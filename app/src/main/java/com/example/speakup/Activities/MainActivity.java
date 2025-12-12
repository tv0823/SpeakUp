package com.example.speakup.Activities;

import static com.example.speakup.FBRef.refAuth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.speakup.R;
import com.example.speakup.Utilities;
import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends Utilities {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        File file = new File(getFilesDir(), INTERNAL_FILENAME);
        if(file.exists() && refAuth.getCurrentUser() != null){
            try {
                FileInputStream fiS = openFileInput(INTERNAL_FILENAME);
                InputStreamReader iSR = new InputStreamReader(fiS);
                BufferedReader bR = new BufferedReader(iSR);
                String logInStatus = bR.readLine();
                bR.close();
                iSR.close();
                fiS.close();
                if (logInStatus.equals("true")) {
                    Intent intent = new Intent(this, QuickAccessActivity.class);
                    startActivity(intent);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void goToSignUpActivity(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    public void goToLogInActivity(View view) {
        Intent intent = new Intent(this, LogInActivity.class);
        startActivity(intent);
    }
}