package com.example.speakup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.speakup.Fragments.QuestionsGeneralFragment;
import com.example.speakup.Fragments.RecordingsGeneralFragment;

/**
 * Adapter for managing the fragments in the practice questions ViewPager.
 * <p>
 * This adapter is responsible for creating and supplying the appropriate {@link Fragment}
 * for each tab in the {@link com.example.speakup.Fragments.TopicsFragment}.
 * It handles navigation between different question categories: Personal, Project, and Video Clip questions.
 * </p>
 */
public class QuestionsPagerAdapter extends FragmentStateAdapter {
    private String type;

    /**
     * Constructs a new QuestionsPagerAdapter.
     *
     * @param fragment The {@link Fragment} that hosts the ViewPager2.
     */
    public QuestionsPagerAdapter(@NonNull Fragment fragment, String type) {
        super(fragment);
        this.type = type;
    }

    /**
     * Creates the fragment corresponding to the specified position.
     * <p>
     * Maps the tab position to a specific question category:
     * <ul>
     *     <li>0: Personal Questions</li>
     *     <li>1: Project Questions</li>
     *     <li>2: Video Clip Questions</li>
     * </ul>
     * </p>
     *
     * @param position The position of the tab/page to create.
     * @return A new instance of {@link QuestionsGeneralFragment} configured for the specific category.
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (this.type.equals("Practice Topics")) {
            switch (position) {
                case 1:
                    return QuestionsGeneralFragment.newInstance("Project Questions");
                case 2:
                    return QuestionsGeneralFragment.newInstance("Video Clip Questions");
                default:
                    return QuestionsGeneralFragment.newInstance("Personal Questions");
            }
        } else {
            switch (position) {
                case 1:
                    return RecordingsGeneralFragment.newInstance("Project Questions");
                case 2:
                    return RecordingsGeneralFragment.newInstance("Video Clip Questions");
                default:
                    return RecordingsGeneralFragment.newInstance("Personal Questions");
            }
        }
    }

    /**
     * Returns the total number of items in the adapter.
     *
     * @return The number of tabs/pages (3).
     */
    @Override
    public int getItemCount() {
        return 3;
    }
}
