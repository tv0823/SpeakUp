package com.example.speakup.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.speakup.R;
import com.example.speakup.Utilities;

public class MainActivity extends Utilities {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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