package com.example.speakup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.speakup.Fragments.PersonalQuestionsFragment;
import com.example.speakup.Fragments.ProjectQuestionsFragment;
import com.example.speakup.Fragments.VideoClipQuestionsFragment;

public class PracticeQuestionsPagerAdapter extends FragmentStateAdapter {

    public PracticeQuestionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new PersonalQuestionsFragment();
            case 1:
                return new ProjectQuestionsFragment();
            case 2:
                return new VideoClipQuestionsFragment();
            default:
                return new PersonalQuestionsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}