package com.example.speakup.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.speakup.Activities.SimulationsActivity;
import com.example.speakup.R;

public class SimulationStartFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simulation_start, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnStartSim = view.findViewById(R.id.btnStartSim);
        btnStartSim.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), SimulationsActivity.class);
            intent.putExtra("AUTO_START", true);
            startActivity(intent);
        });
    }
}

