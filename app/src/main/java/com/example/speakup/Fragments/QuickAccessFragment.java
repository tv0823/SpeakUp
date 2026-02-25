package com.example.speakup.Fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.speakup.Activities.MasterActivity;
import com.example.speakup.Activities.RemindersActivity;
import com.example.speakup.Activities.SimulationsActivity;
import com.example.speakup.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

/**
 * Fragment providing quick access to the main features of the application.
 * <p>
 * This fragment serves as a dashboard, displaying a performance chart (currently using sample data)
 * and providing navigation buttons to different sections of the app such as Practice Questions,
 * Simulations, Past Recordings, and Reminders.
 * </p>
 */
public class QuickAccessFragment extends Fragment {

    /**
     * The line chart view used to display performance data.
     */
    private LineChart lineChart;

    public QuickAccessFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment QuickAccessFragment.
     */
    public static QuickAccessFragment newInstance() {
        return new QuickAccessFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_quick_access, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lineChart = view.findViewById(R.id.lineChart);
        setupChartData(); // create the graph
        configureChartAppearance(); // set description and X,Y axis
        lineChart.invalidate(); // reload graph and show it

        // Setup navigation buttons
        setupNavigationButtons(view);

        // Disable the back button logic
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing to disable back button
            }
        });
    }

    private void setupNavigationButtons(View view) {
        MaterialButton btnPractice = view.findViewById(R.id.btnPracticeQuestions);
        if (btnPractice != null) {
            btnPractice.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 1. Switch the Fragment
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, TopicsFragment.newInstance("Practice Topics"))
                            .commit();

                    // 2. Sync the Bottom Navigation Bar UI
                    if (getActivity() instanceof MasterActivity) {
                        BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                        nav.setSelectedItemId(R.id.nav_practice);
                    }
                }
            });
        }
        
        MaterialButton btnSimulations = view.findViewById(R.id.btnSimulations);
        if (btnSimulations != null) {
            btnSimulations.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), SimulationsActivity.class);
                    startActivity(intent);
                }
            });
        }

        MaterialButton btnPastRecordings = view.findViewById(R.id.btnPastRecordings);
        if (btnPastRecordings != null) {
            btnPastRecordings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 1. Switch the Fragment
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, TopicsFragment.newInstance("Past Recordings"))
                            .commit();

                    // 2. Sync the Bottom Navigation Bar UI
                    if (getActivity() instanceof MasterActivity) {
                        BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                        nav.setSelectedItemId(R.id.nav_recordings);
                    }
                }
            });
        }

        MaterialButton btnReminders = view.findViewById(R.id.btnReminders);
        if (btnReminders != null) {
            btnReminders.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), RemindersActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    /**
     * Configures the data for the line chart with sample points.
     * Sets up the data set with styling (colors, line width, etc.).
     */
    private void setupChartData() {
        ArrayList<Entry> points = new ArrayList<>();

        points.add(new Entry(0, 10f));
        points.add(new Entry(1, 15f));
        points.add(new Entry(2, 30f));
        points.add(new Entry(3, 67f));
        points.add(new Entry(4, 40f));
        points.add(new Entry(5, 80f));
        points.add(new Entry(6, 45f));

        LineDataSet dataSet = new LineDataSet(points, "grades");

        dataSet.setColor(Color.parseColor("#42A5F5")); // color for lines

        dataSet.setCircleColor(Color.parseColor("#1565C0")); // color for dots
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(true); // show the number that the dot is at
        dataSet.setValueTextSize(10f);

        // create the line graph
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
    }

    /**
     * Configures the visual appearance of the chart.
     * Sets the description, legend, axis properties (position, granularity, range), and interaction settings.
     */
    private void configureChartAppearance() {
        // Description for the graph
        Description description = new Description();
        description.setText("גרף דו מימדי");
        description.setTextSize(12f);
        description.setTextColor(Color.DKGRAY);
        lineChart.setDescription(description);

        // Show mikra
        lineChart.getLegend().setEnabled(true);

        // Remove right Y axis
        lineChart.getAxisRight().setEnabled(false);

        // X axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setLabelCount(7, true);

        // Y axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setTextSize(10f);
        leftAxis.setLabelCount(5, true);

        lineChart.setTouchEnabled(false);
        lineChart.setHighlightPerTapEnabled(false);
    }
}
