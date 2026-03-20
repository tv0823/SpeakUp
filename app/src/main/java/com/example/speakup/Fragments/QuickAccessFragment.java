package com.example.speakup.Fragments;

import static com.example.speakup.FBRef.refAuth;
import static com.example.speakup.FBRef.refQuestions;
import static com.example.speakup.FBRef.refRecordings;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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
    private Spinner spinnerChartCategory;
    private TextView tvAvgGrade;

    private DatabaseReference userRecordingsRef;
    private String currentUserId;
    private ProgressDialog chartProgressDialog;

    private static final String CATEGORY_PERSONAL = "Personal Questions";
    private static final String CATEGORY_VIDEO = "Video Clip Questions";
    private static final String CATEGORY_PROJECT = "Project Questions";

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissChartLoading();
        spinnerChartCategory = null;
        lineChart = null;
        tvAvgGrade = null;
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
                    // Switch to the same simulation start screen used by the bottom nav.
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new SimulationStartFragment())
                            .commit();

                    if (getActivity() instanceof MasterActivity) {
                        BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                        nav.setSelectedItemId(R.id.nav_simulations);
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
        categories.add("Personal Questions");
        categories.add("Video Clip Questions");
        categories.add("Project Questions");
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
            fetchAndRenderChartOnce(null, "over all");
            return;
        }

        final String categoryPath;
        final String label;
        if (selected != null && selected.equalsIgnoreCase(CATEGORY_PERSONAL)) {
            categoryPath = CATEGORY_PERSONAL;
            label = "personal";
        } else if (selected != null && selected.equalsIgnoreCase(CATEGORY_VIDEO)) {
            categoryPath = CATEGORY_VIDEO;
            label = "video clips";
        } else if (selected != null && selected.equalsIgnoreCase(CATEGORY_PROJECT)) {
            categoryPath = CATEGORY_PROJECT;
            label = "project";
        } else {
            fetchAndRenderChartOnce(null, "over all");
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
                fetchAndRenderChartOnce(allowedQuestionIds, label);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Failed to load chart category", Toast.LENGTH_SHORT).show();
                fetchAndRenderChartOnce(null, "over all");
            }
        });
    }

    private void fetchAndRenderChartOnce(@Nullable Set<String> allowedQuestionIds, @NonNull String label) {
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
                    for (DataSnapshot recordingSnapshot : questionSnapshot.getChildren()) {
                        Recording recording = recordingSnapshot.getValue(Recording.class);
                        if (recording != null) recordings.add(recording);
                    }
                }

                Collections.sort(recordings, new Comparator<Recording>() {
                    @Override
                    public int compare(Recording o1, Recording o2) {
                        if (o1 == null || o2 == null) return 0;
                        if (o1.getDateRecorded() == null && o2.getDateRecorded() == null) return 0;
                        if (o1.getDateRecorded() == null) return -1;
                        if (o2.getDateRecorded() == null) return 1;
                        return o1.getDateRecorded().compareTo(o2.getDateRecorded());
                    }
                });

                ArrayList<Entry> points = new ArrayList<>();
                if (recordings.isEmpty()) {
                    // Static preview data (for design) until Firebase has recordings
                    points.add(new Entry(0, 55f));
                    points.add(new Entry(1, 62f));
                    points.add(new Entry(2, 58f));
                    points.add(new Entry(3, 70f));
                    points.add(new Entry(4, 66f));
                    points.add(new Entry(5, 78f));
                    points.add(new Entry(6, 73f));
                    points.add(new Entry(7, 82f));
                    points.add(new Entry(8, 76f));
                    points.add(new Entry(9, 88f));
                } else {
                    // Keep the last 30 points
                    int start = Math.max(0, recordings.size() - 30);
                    for (int i = start; i < recordings.size(); i++) {
                        Recording r = recordings.get(i);
                        points.add(new Entry(i - start, (float) r.getScore()));
                    }
                }

                if (tvAvgGrade != null) {
                    if (points.isEmpty()) {
                        tvAvgGrade.setText("--%");
                    } else {
                        float sum = 0f;
                        for (Entry e : points) sum += e.getY();
                        int avg = Math.round(sum / points.size());
                        tvAvgGrade.setText(avg + "%");
                    }
                }

                LineDataSet dataSet = new LineDataSet(points, label);
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                dataSet.setCubicIntensity(0.2f);
                dataSet.setColor(Color.parseColor("#3B82F6")); // blue-500
                dataSet.setLineWidth(2.75f);
                dataSet.setDrawCircles(true);
                dataSet.setCircleColor(Color.parseColor("#1D4ED8")); // blue-700
                dataSet.setCircleRadius(4f);
                dataSet.setDrawCircleHole(true);
                dataSet.setCircleHoleRadius(2f);
                dataSet.setCircleHoleColor(Color.WHITE);
                dataSet.setDrawValues(false);
                dataSet.setDrawFilled(true);
                dataSet.setFillColor(Color.parseColor("#93C5FD")); // blue-300
                dataSet.setFillAlpha(70);
                dataSet.setHighLightColor(Color.parseColor("#94A3B8")); // slate-400
                dataSet.setDrawHorizontalHighlightIndicator(false);

                lineChart.setData(new LineData(dataSet));
                lineChart.invalidate();

                dismissChartLoading();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Failed to load chart data", Toast.LENGTH_SHORT).show();
                dismissChartLoading();
            }
        });
    }

    private void showChartLoading() {
        if (!isAdded()) return;
        if (chartProgressDialog == null) {
            chartProgressDialog = new ProgressDialog(requireContext());
            chartProgressDialog.setCancelable(false);
            chartProgressDialog.setMessage("Loading chart...");
        }
        if (!chartProgressDialog.isShowing()) chartProgressDialog.show();
    }

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
                // Show 1..30 instead of 0..29
                return String.valueOf(((int) value) + 1);
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
