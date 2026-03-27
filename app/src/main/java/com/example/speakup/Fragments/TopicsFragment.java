package com.example.speakup.Fragments;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.speakup.QuestionsPagerAdapter;
import com.example.speakup.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * A fragment that displays a tabbed interface for different question categories or past recordings.
 * <p>
 * This fragment uses a {@link ViewPager2} and a {@link TabLayout} to allow users to switch between
 * "Personal Questions", "Project Questions", "Video Clip Questions", and optionally "Simulation" (for past recordings).
 * It dynamically configures its content based on whether it's used for practice or viewing history.
 * </p>
 */
public class TopicsFragment extends Fragment {
    /**
     * Argument key for the fragment type.
     */
    private static final String ARG_TYPE = "arg_type";

    /**
     * The type of the screen (e.g., "Practice Topics" or "Past Recordings").
     */
    private String type;

    /**
     * Default constructor for fragment instantiation.
     */
    public TopicsFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create a new instance of this fragment with a specific type.
     *
     * @param type The type of the screen, determining which categories to show.
     * @return A new instance of fragment TopicsFragment.
     */
    public static TopicsFragment newInstance(String type) {
        TopicsFragment fragment = new TopicsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Gets the current type of the fragment.
     *
     * @return The screen type string.
     */
    public String getType() {
        // If 'type' is null, try to get it from arguments just in case
        if (type == null && getArguments() != null) {
            type = getArguments().getString(ARG_TYPE);
        }
        return type;
    }

    /**
     * Initializes the fragment and retrieves the type argument.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the data from the Bundle
        if (getArguments() != null) {
            type = getArguments().getString(ARG_TYPE);
        }
    }

    /**
     * Inflates the fragment layout.
     *
     * @param inflater           The LayoutInflater object.
     * @param container          The parent view container.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_topics, container, false);
    }

    /**
     * Initializes UI components, sets up the ViewPager2 adapter, and configures the TabLayout.
     * Also sets up the system back button logic to navigate to the home fragment.
     *
     * @param view               The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Initialize Views
        TextView typeTv = view.findViewById(R.id.typeTv);
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);

        //The type of the screen past recordings/ practice topics
        typeTv.setText(this.type);

        //Setup Adapter (Using 'this' for child fragment management)
        QuestionsPagerAdapter adapter = new QuestionsPagerAdapter(this, this.type);
        viewPager.setAdapter(adapter);

        // Setup tab titles by screen type.
        String[] tabTitles;
        if ("Practice Topics".equals(this.type)) {
            tabTitles = new String[]{"Personal Questions", "Project Questions", "Video Clip Questions"};
        } else {
            tabTitles = new String[]{"Personal Questions", "Project Questions", "Video Clip Questions", "Simulation"};
        }

        new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(tabTitles[position]);
            }
        }).attach();

        // if the user press back button, go to home
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                nav.setSelectedItemId(R.id.nav_home);
            }
        });
    }
}
