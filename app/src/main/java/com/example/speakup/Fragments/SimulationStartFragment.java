package com.example.speakup.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.speakup.Activities.SimulationsActivity;
import com.example.speakup.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Fragment that serves as the entry point for starting a full exam simulation.
 * <p>
 * This fragment displays a start button that launches the {@link SimulationsActivity}.
 * it also handles the system back button to navigate back to the home screen.
 * </p>
 */
public class SimulationStartFragment extends Fragment {

    /**
     * Inflates the layout for this fragment.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simulation_start, container, false);
    }

    /**
     * Initializes the UI components and sets up the click listener for the start button.
     * Also configures the back press callback to navigate to the home fragment.
     *
     * @param view               The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnStartSim = view.findViewById(R.id.btnStartSim);
        btnStartSim.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), SimulationsActivity.class);
            startActivity(intent);
        });

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
