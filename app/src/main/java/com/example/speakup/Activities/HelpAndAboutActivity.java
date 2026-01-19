package com.example.speakup.Activities;

import android.os.Bundle;
import android.view.View;

import com.example.speakup.R;
import com.example.speakup.Utilities;

public class HelpAndAboutActivity extends Utilities {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_and_about);
    }

    public void goBack(View view) {
        finish();
    }
}