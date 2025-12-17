package com.example.speakup.Activities;

import static com.example.speakup.FBRef.refAuth;

import android.content.Intent;
import android.content.SharedPreferences;
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

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences settings = getSharedPreferences("STATUS", MODE_PRIVATE);
        Boolean isChecked = settings.getBoolean("stayConnected", false);
        if (refAuth.getCurrentUser() != null && isChecked) {
            Intent intent = new Intent(this, QuickAccessActivity.class);
            startActivity(intent);
            finish();
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