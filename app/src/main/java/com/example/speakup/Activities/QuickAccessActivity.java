package com.example.speakup.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;

import com.example.speakup.R;
import com.example.speakup.Utilities;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class QuickAccessActivity extends Utilities {
    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_access);

        lineChart = findViewById(R.id.lineChart);
        setupChartData(); // create the graph
        configureChartAppearance(); // set description and X,Y axis
        lineChart.invalidate(); // reload graph and show it

        //disable the back button so the user cant go to log in screen after registering
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    public void goToPracticeQuestionsActivity(View view) {
        Intent intent = new Intent(this, PracticeTopicsActivity.class);
        startActivity(intent);
    }

    public void goToSimulationsActivity(View view) {
        Intent intent = new Intent(this, SimulationsActivity.class);
        startActivity(intent);
    }

    public void goToPastRecordingsActivity(View view) {
        Intent intent = new Intent(this, PastRecordingsActivity.class);
        startActivity(intent);
    }

    public void goToRemindersActivity(View view) {
        Intent intent = new Intent(this, RemindersActivity.class);
        startActivity(intent);
    }

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