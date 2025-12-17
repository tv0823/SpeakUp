package com.example.speakup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.speakup.Fragments.QuestionsGeneralFragment;

public class PracticeQuestionsPagerAdapter extends FragmentStateAdapter {

    public PracticeQuestionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return QuestionsGeneralFragment.newInstance("Personal Questions");
            case 1:
                return QuestionsGeneralFragment.newInstance("Project Questions");
            case 2:
                return QuestionsGeneralFragment.newInstance("Video Clip Questions");
            default:
                return QuestionsGeneralFragment.newInstance("Personal Questions");
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}