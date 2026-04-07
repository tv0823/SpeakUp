package com.example.speakup.Fragments;

import static com.example.speakup.Utils.FBRef.refAuth;
import static com.example.speakup.Utils.FBRef.refQuestions;
import static com.example.speakup.Utils.FBRef.refRecordings;
import static com.example.speakup.Utils.FBRef.refSimulations;

import android.app.ProgressDialog;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.speakup.Activities.MasterActivity;
import com.example.speakup.Activities.RemindersActivity;
import com.example.speakup.Objects.Recording;
import com.example.speakup.Objects.Simulation;
import com.example.speakup.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    /**
     * Spinner for selecting the category of data to display in the chart.
     */
    private Spinner spinnerChartCategory;

    /**
     * TextView displaying the average grade for the selected category.
     */
    private TextView tvAvgGrade;

    /**
     * Reference to the user's recordings in the Firebase database.
     */
    private DatabaseReference userRecordingsRef;

    /**
     * The unique identifier of the currently authenticated user.
     */
    private String currentUserId;

    /**
     * Progress dialog shown while loading chart data.
     */
    private ProgressDialog chartProgressDialog;

    /**
     * Constant representing the 'Personal Questions' category.
     */
    private static final String CATEGORY_PERSONAL = "Personal Questions";

    /**
     * Constant representing the 'Video Clip Questions' category.
     */
    private static final String CATEGORY_VIDEO = "Video Clip Questions";

    /**
     * Constant representing the 'Project Questions' category.
     */
    private static final String CATEGORY_PROJECT = "Project Questions";

    /**
     * Constant representing the 'Simulations' category.
     */
    private static final String CATEGORY_SIMULATIONS = "Simulations";

    /**
     * Required empty public constructor for fragment instantiation.
     */
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

    /**
     * Inflates the fragment layout.
     *
     * @param inflater           The LayoutInflater object.
     * @param container          The parent view container.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quick_access, container, false);
    }

    /**
     * Initializes UI components, sets up listeners, and triggers initial data loading.
     *
     * @param view               The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lineChart = view.findViewById(R.id.lineChart);
        spinnerChartCategory = view.findViewById(R.id.spinnerChartCategory);
        tvAvgGrade = view.findViewById(R.id.tvAvgGrade);

        currentUserId = (refAuth.getCurrentUser() != null) ? refAuth.getCurrentUser().getUid() : null;
        userRecordingsRef = (currentUserId != null) ? refRecordings.child(currentUserId) : null;

        setupCategorySpinner(); // spinner + initial load
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

    /**
     * Cleans up resources when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissChartLoading();
        spinnerChartCategory = null;
        lineChart = null;
        tvAvgGrade = null;
    }

    /**
     * Configures click listeners for the dashboard navigation buttons.
     *
     * @param view The root view of the fragment.
     */
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
                    // Switch to the same simulation start screen used by the bottom nav.
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new SimulationStartFragment())
                            .commit();

                    if (getActivity() instanceof MasterActivity) {
                        BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                        nav.setSelectedItemId(R.id.nav_simulation);
                    }
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
     * Configures the spinner that selects which category to chart, and triggers Firebase reloads.
     */
    private void setupCategorySpinner() {
        if (spinnerChartCategory == null) return;

        ArrayList<String> categories = new ArrayList<>();
        categories.add(CATEGORY_PERSONAL);
        categories.add(CATEGORY_VIDEO);
        categories.add(CATEGORY_PROJECT);
        categories.add(CATEGORY_SIMULATIONS);
        categories.add("Over All");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChartCategory.setAdapter(adapter);

        spinnerChartCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = categories.get(position);
                reloadChartForSelection(selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Trigger initial load (default = "Over All")
        int defaultIndex = categories.size() - 1; // last item is "Over All"
        spinnerChartCategory.setSelection(defaultIndex, false);
        reloadChartForSelection(categories.get(defaultIndex));
    }

    /**
     * Reloads chart data based on the selected category from the spinner.
     *
     * @param selected The name of the selected category.
     */
    private void reloadChartForSelection(String selected) {
        if (lineChart == null) return;
        if (currentUserId == null || userRecordingsRef == null) {
            lineChart.clear();
            lineChart.setNoDataText("Please log in to see your progress");
            lineChart.invalidate();
            if (tvAvgGrade != null) tvAvgGrade.setText("--%");
            return;
        }

        showChartLoading();

        String selectedNormalized = (selected == null) ? "" : selected.trim().toLowerCase();
        if (selectedNormalized.equals("over all") || selectedNormalized.equals("overall")) {
            fetchAndRenderChartCumulativeProgress(null, "Over All Progress");
            return;
        }

        if (selected != null && selected.equalsIgnoreCase(CATEGORY_SIMULATIONS)) {
            fetchAndRenderChartSimulationsProgress();
            return;
        }

        final String categoryPath;
        final String label;
        if (selected != null && selected.equalsIgnoreCase(CATEGORY_PERSONAL)) {
            categoryPath = CATEGORY_PERSONAL;
            label = "Personal Progress";
        } else if (selected != null && selected.equalsIgnoreCase(CATEGORY_VIDEO)) {
            categoryPath = CATEGORY_VIDEO;
            label = "Video Clips Progress";
        } else if (selected != null && selected.equalsIgnoreCase(CATEGORY_PROJECT)) {
            categoryPath = CATEGORY_PROJECT;
            label = "Project Progress";
        } else {
            fetchAndRenderChartCumulativeProgress(null, "Over All Progress");
            return;
        }

        // Build a set of questionIds that belong to this category, then filter recordings by them.
        refQuestions.child(categoryPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot categorySnapshot) {
                if (!isAdded()) return;

                Set<String> allowedQuestionIds = new HashSet<>();
                for (DataSnapshot topicSnapshot : categorySnapshot.getChildren()) {
                    for (DataSnapshot questionSnapshot : topicSnapshot.getChildren()) {
                        String qId = questionSnapshot.getKey();
                        if (qId != null) allowedQuestionIds.add(qId);
                    }
                }
                fetchAndRenderChartCumulativeProgress(allowedQuestionIds, label);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Failed to load chart category", Toast.LENGTH_SHORT).show();
                fetchAndRenderChartCumulativeProgress(null, "Over All Progress");
            }
        });
    }

    /**
     * Fetches all recordings from Firebase, optionally filters them by a set of allowed question IDs,
     * sorts them chronologically, calculates the cumulative average score, and renders the chart.
     *
     * @param allowedQuestionIds A set of question IDs to include; if null, all recordings are included.
     * @param label              The label to display for the data set in the chart.
     */
    private void fetchAndRenderChartCumulativeProgress(@Nullable Set<String> allowedQuestionIds, @NonNull String label) {
        if (userRecordingsRef == null) return;

        userRecordingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (!isAdded() || lineChart == null) return;

                ArrayList<Recording> recordings = new ArrayList<>();
                for (DataSnapshot questionSnapshot : userSnapshot.getChildren()) {
                    String questionId = questionSnapshot.getKey();
                    if (allowedQuestionIds != null && (questionId == null || !allowedQuestionIds.contains(questionId))) {
                        continue;
                    }
                    for (DataSnapshot recSnapshot : questionSnapshot.getChildren()) {
                        Recording rec = recSnapshot.getValue(Recording.class);
                        if (rec != null && rec.getDateRecorded() != null) {
                            recordings.add(rec);
                        }
                    }
                }

                sortRecordingsByDate(recordings);

                ArrayList<Float> cumulativeAverages = new ArrayList<>();
                float sum = 0;
                for (int i = 0; i < recordings.size(); i++) {
                    sum += recordings.get(i).getScore();
                    cumulativeAverages.add(sum / (i + 1));
                }

                renderChartProgress(cumulativeAverages, label, false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                dismissChartLoading();
            }
        });
    }

    /**
     * Fetches all simulation scores, sorts by date, and renders them as dots on a line.
     */
    private void fetchAndRenderChartSimulationsProgress() {
        refSimulations.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || lineChart == null) return;

                ArrayList<Simulation> sims = new ArrayList<>();
                for (DataSnapshot simSnapshot : snapshot.getChildren()) {
                    Simulation sim = simSnapshot.getValue(Simulation.class);
                    if (sim != null && currentUserId.equals(sim.getUserId()) && sim.getDateCompleted() != null) {
                        sims.add(sim);
                    }
                }

                sortSimulationsByDate(sims);

                ArrayList<Float> scores = new ArrayList<>();
                for (Simulation sim : sims) {
                    scores.add((float) sim.getOverAllScore());
                }

                renderChartProgress(scores, "Simulations Progress", true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                dismissChartLoading();
            }
        });
    }

    /**
     * Renders the provided list of score points on the {@link LineChart}.
     * Configures the dataset appearance, average grade display, and axis formatting.
     *
     * @param points       A list of float values representing the data points to plot.
     * @param label        The label for the data series.
     * @param showDotsOnly If true, increases circle radius and hides X-axis labels (typically for simulation dots).
     */
    private void renderChartProgress(ArrayList<Float> points, String label, boolean showDotsOnly) {
        if (!isAdded() || lineChart == null) return;

        ArrayList<Entry> entries = new ArrayList<>();
        if (points.isEmpty()) {
            // Static preview data
            for (int i = 0; i < 5; i++) entries.add(new Entry(i, 50f + i * 5));
        } else {
            for (int i = 0; i < points.size(); i++) {
                entries.add(new Entry(i, points.get(i)));
            }
        }

        if (tvAvgGrade != null) {
            if (points.isEmpty()) {
                tvAvgGrade.setText("--%");
            } else {
                tvAvgGrade.setText(Math.round(points.get(points.size() - 1)) + "%");
            }
        }
        
        TextView rangeLabel = getView() != null ? getView().findViewById(R.id.tvChartRangeLabel) : null;
        if (rangeLabel != null) rangeLabel.setText(showDotsOnly ? "Simulation Progress" : "Cumulative Average");

        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (showDotsOnly) ? "" : String.valueOf((int) value + 1);
            }
        });
        lineChart.getXAxis().setLabelCount(Math.min(points.size(), 6), false);

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(Color.parseColor("#3B82F6"));
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#1D4ED8"));
        dataSet.setCircleRadius(showDotsOnly ? 5f : 3f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#93C5FD"));
        dataSet.setFillAlpha(60);

        lineChart.setData(new LineData(dataSet));
        lineChart.invalidate();
        dismissChartLoading();
    }
    /**
     * Manually sorts a list of recordings by date using bubble sort.
     *
     * @param recordings The list to sort.
     */
    private void sortRecordingsByDate(ArrayList<Recording> recordings) {
        int n = recordings.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (recordings.get(j).getDateRecorded().after(recordings.get(j + 1).getDateRecorded())) {
                    Recording temp = recordings.get(j);
                    recordings.set(j, recordings.get(j + 1));
                    recordings.set(j + 1, temp);
                }
            }
        }
    }

    /**
     * Manually sorts a list of simulations by date using bubble sort.
     *
     * @param sims The list of simulations to sort.
     */
    private void sortSimulationsByDate(ArrayList<Simulation> sims) {
        int n = sims.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (sims.get(j).getDateCompleted().after(sims.get(j + 1).getDateCompleted())) {
                    Simulation temp = sims.get(j);
                    sims.set(j, sims.get(j + 1));
                    sims.set(j + 1, temp);
                }
            }
        }
    }

    /**
     * Shows the chart loading progress dialog.
     */
    private void showChartLoading() {
        if (!isAdded()) return;
        if (chartProgressDialog == null) {
            chartProgressDialog = new ProgressDialog(requireContext());
            chartProgressDialog.setCancelable(false);
            chartProgressDialog.setMessage("Loading chart...");
        }
        if (!chartProgressDialog.isShowing()) chartProgressDialog.show();
    }

    /**
     * Dismisses the chart loading progress dialog.
     */
    private void dismissChartLoading() {
        if (chartProgressDialog != null && chartProgressDialog.isShowing()) {
            chartProgressDialog.dismiss();
        }
    }

    /**
     * Configures the visual appearance of the chart.
     * Sets the description, legend, axis properties (position, granularity, range), and interaction settings.
     */
    private void configureChartAppearance() {
        // Description for the graph
        Description description = new Description();
        description.setText("Grade progress");
        description.setTextSize(12f);
        description.setTextColor(Color.parseColor("#475569")); // slate-600
        lineChart.setDescription(description);

        lineChart.setNoDataText("No data yet");
        lineChart.setNoDataTextColor(Color.parseColor("#64748B")); // slate-500
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.TRANSPARENT);
        lineChart.setExtraOffsets(12f, 8f, 12f, 10f);

        // Legend
        lineChart.getLegend().setEnabled(false);

        // Remove right Y axis
        lineChart.getAxisRight().setEnabled(false);

        // X axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(11f);
        xAxis.setTextColor(Color.parseColor("#64748B")); // slate-500
        xAxis.setLabelCount(6, false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        // Y axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawGridLines(true);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setGridColor(Color.parseColor("#E2E8F0")); // slate-200
        leftAxis.setTextSize(11f);
        leftAxis.setTextColor(Color.parseColor("#64748B")); // slate-500
        leftAxis.setLabelCount(5, false);

        lineChart.setTouchEnabled(false);
        lineChart.setHighlightPerTapEnabled(false);
    }
}
