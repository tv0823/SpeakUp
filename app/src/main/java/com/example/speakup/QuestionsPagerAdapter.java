package com.example.speakup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.speakup.Fragments.QuestionsGeneralFragment;
import com.example.speakup.Fragments.RecordingsGeneralFragment;

/**
 * Adapter for managing fragments inside a ViewPager2 component.
 *
 * <p>This adapter is responsible for supplying the correct {@link Fragment}
 * based on the selected tab position and the given {@code type}.
 * It dynamically switches between two modes:
 * <ul>
 *     <li><b>Practice Mode</b> ("Practice Topics"):
 *     Displays {@link QuestionsGeneralFragment} instances.</li>
 *
 *     <li><b>Recording Mode</b> (any other value):
 *     Displays {@link RecordingsGeneralFragment} instances.</li>
 * </ul>
 *
 * <p>The adapter supports the following categories (tabs):
 * <ol>
 *     <li>Personal Questions</li>
 *     <li>Project Questions</li>
 *     <li>Video Clip Questions</li>
 *     <li>Simulation (only in Recording Mode)</li>
 * </ol>
 *
 * <p>Each fragment is created using a factory method ({@code newInstance})
 * and receives the category name as an argument.
 */
public class QuestionsPagerAdapter extends FragmentStateAdapter {

    /**
     * Defines which type of content should be displayed in the ViewPager.
     * <p>
     * If the value equals "Practice Topics", the adapter will create
     * {@link QuestionsGeneralFragment} instances.
     * Otherwise, it will create {@link RecordingsGeneralFragment} instances.
     */
    private final String type;

    /**
     * Constructs a new {@code QuestionsPagerAdapter}.
     *
     * @param fragment The hosting {@link Fragment} that contains the ViewPager2.
     * @param type     The content type that determines which fragments to display.
     *                 Expected value: "Practice Topics" for questions,
     *                 any other value for recordings.
     */
    public QuestionsPagerAdapter(@NonNull Fragment fragment, String type) {
        super(fragment);
        this.type = type;
    }

    /**
     * Creates and returns the appropriate {@link Fragment} for the given position.
     *
     * <p>The returned fragment depends on:
     * <ul>
     *     <li>The current tab {@code position}</li>
     *     <li>The adapter {@link #type}</li>
     * </ul>
     *
     * <p><b>Practice Mode:</b>
     * <ul>
     *     <li>0 → Personal Questions</li>
     *     <li>1 → Project Questions</li>
     *     <li>2 → Video Clip Questions</li>
     * </ul>
     *
     * <p><b>Recording Mode:</b>
     * <ul>
     *     <li>0 → Personal Questions</li>
     *     <li>1 → Project Questions</li>
     *     <li>2 → Video Clip Questions</li>
     *     <li>3 → Simulation</li>
     * </ul>
     *
     * @param position The index of the selected tab (0-based).
     * @return A new instance of either {@link QuestionsGeneralFragment}
     *         or {@link RecordingsGeneralFragment}, depending on {@link #type}.
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
                case 3:
                    return RecordingsGeneralFragment.newInstance("Simulation");
                default:
                    return RecordingsGeneralFragment.newInstance("Personal Questions");
            }
        }
    }

    /**
     * Returns the number of pages (tabs) in the ViewPager2.
     *
     * <p>The number depends on the adapter mode:
     * <ul>
     *     <li><b>Practice Mode:</b> 3 tabs</li>
     *     <li><b>Recording Mode:</b> 4 tabs (includes Simulation)</li>
     * </ul>
     *
     * @return Total number of pages to display.
     */
    @Override
    public int getItemCount() {
        return "Practice Topics".equals(this.type) ? 3 : 4;
    }
}