package com.example.speakup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.speakup.Fragments.QuestionsGeneralFragment;
import com.example.speakup.Fragments.RecordingsGeneralFragment;

/**
 * Adapter for managing fragments in a ViewPager2 that displays either practice questions or recordings.
 * <p>
 * This adapter handles the creation of fragments based on the current tab position and the specified
 * {@link #type}. It supports two main content types:
 * <ul>
 *     <li>"Practice Topics": Displays {@link QuestionsGeneralFragment} for each category.</li>
 *     <li>Any other value: Displays {@link RecordingsGeneralFragment} for each category.</li>
 * </ul>
 * The categories (tabs) are:
 * <ol>
 *     <li>Personal Questions</li>
 *     <li>Project Questions</li>
 *     <li>Video Clip Questions</li>
 * </ol>
 * </p>
 */
public class QuestionsPagerAdapter extends FragmentStateAdapter {

    /**
     * The type of content to be displayed in the ViewPager2.
     * Determines whether questions or recordings fragments are created.
     */
    private final String type;

    /**
     * Constructs a new QuestionsPagerAdapter.
     *
     * @param fragment The {@link Fragment} hosting the ViewPager2.
     * @param type     The type of content to display (e.g., "Practice Topics").
     */
    public QuestionsPagerAdapter(@NonNull Fragment fragment, String type) {
        super(fragment);
        this.type = type;
    }

    /**
     * Creates a new fragment instance for the specified position.
     * <p>
     * Depending on {@link #type}, this method instantiates either a {@link QuestionsGeneralFragment}
     * or a {@link RecordingsGeneralFragment}, passing the appropriate category title as an argument.
     * </p>
     *
     * @param position The position of the tab (0 for Personal, 1 for Project, 2 for Video Clip).
     * @return A {@link Fragment} instance corresponding to the tab position and content type.
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if ("Practice Topics".equals(this.type)) {
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
     * Returns the total number of pages in the ViewPager2.
     *
     * @return The number of tabs (3).
     */
    @Override
    public int getItemCount() {
        return 3;
    }
}
