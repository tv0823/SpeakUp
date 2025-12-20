package com.example.speakup.Activities;

import androidx.viewpager2.widget.ViewPager2;
import android.os.Bundle;
import android.view.View;

import com.example.speakup.Utilities;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.example.speakup.PracticeQuestionsPagerAdapter;
import com.example.speakup.R;

/**
 * Activity hosting the practice topics screen.
 * <p>
 * This activity sets up a ViewPager2 with a TabLayout to allow users to navigate between different
 * categories of practice questions (Personal, Project, and Video Clip questions).
 * </p>
 */
public class PracticeTopicsActivity extends Utilities {

    /**
     * Called when the activity is starting.
     * Initializes the UI components including the ViewPager2 and TabLayout.
     * Sets up the {@link PracticeQuestionsPagerAdapter} to manage the fragments for each topic.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_topics);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        PracticeQuestionsPagerAdapter adapter = new PracticeQuestionsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        String[] tabTitles = new String[]{"Personal Questions", "Project Questions", "Video Clip Questions"};

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();
    }

    /**
     * Finishes the current activity and returns to the previous screen.
     *
     * @param view The view that was clicked.
     */
    public void goBack(View view) {
        finish();
    }
}
