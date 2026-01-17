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

public class TopicsFragment extends Fragment {
    private String type;

    public TopicsFragment() {
        // Required empty public constructor
    }

    public TopicsFragment(String type) {
        this.type = type;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_topics, container, false);
    }

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

        //Setup Tab Titles
        String[] tabTitles = new String[]{"Personal Questions", "Project Questions", "Video Clip Questions"};

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

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