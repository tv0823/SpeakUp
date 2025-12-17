package com.example.speakup.Activities;

import androidx.viewpager2.widget.ViewPager2;
import android.os.Bundle;
import android.view.View;

import com.example.speakup.Utilities;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.example.speakup.PracticeQuestionsPagerAdapter;
import com.example.speakup.R;

public class PracticeQuestionsActivity extends Utilities {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_questions);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        PracticeQuestionsPagerAdapter adapter = new PracticeQuestionsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        String[] tabTitles = new String[]{"Personal Questions", "Project Questions", "Video Clip Questions"};

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();
    }

    public void goBack(View view) {
        finish();
    }
}