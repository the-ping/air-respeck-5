package com.specknet.airrespeck.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.models.XAxisValueFormatter;
import com.specknet.airrespeck.utils.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Darius on 21.02.2017.
 */

public class BreathingGraphFragment extends BaseFragment {

    private LineChart mBreathingSignalChart;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BreathingGraphFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize the utilities
        Utils.init(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_breathing_graph, container, false);

        mConnectingLayout = (LinearLayout) view.findViewById(R.id.connecting_layout);

        mBreathingSignalChart = (LineChart) view.findViewById(R.id.breathing_signal_line_chart);

        setupBreathingSignalChart();
        setupLineDataSet();

        return view;
    }

    /**
     * Setup Breathing Signal chart.
     */
    private void setupBreathingSignalChart() {
        // no description text
        Description emptyDescription = new Description();
        emptyDescription.setText("");
        mBreathingSignalChart.setDescription(emptyDescription);
        mBreathingSignalChart.setDrawGridBackground(false);
        mBreathingSignalChart.getLegend().setEnabled(false);

        XAxis xAxis = mBreathingSignalChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setValueFormatter(new XAxisValueFormatter());

        YAxis leftAxis = mBreathingSignalChart.getAxisLeft();
        leftAxis.setLabelCount(5, false);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mBreathingSignalChart.getAxisRight();
        rightAxis.setLabelCount(5, false);

        mBreathingSignalChart.animateX(750);
    }

    private void setupLineDataSet() {
        LineDataSet dataSet = new LineDataSet(new ArrayList<Entry>(), getString(R.string.graphs_breathing_signal_title));

        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);
        dataSet.setColor(Color.rgb(0, 0, 255));
        dataSet.setDrawValues(false);

        // add the datasets
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        // create a data object with the datasets
        LineData data = new LineData(dataSets);

        // set data
        mBreathingSignalChart.setData(data);
    }

    /**
     * Helper to add new values to Breathing Signal chart.
     *
     * @param value float The new value.
     */
    public void addBreathingSignalData(final float timestamp, final float value) {

        Log.i("DF", "Add breathing signal with timestamp: " + String.valueOf(timestamp));
        if (mBreathingSignalChart != null) {
            LineDataSet dataSet = (LineDataSet) mBreathingSignalChart.getData().getDataSetByIndex(0);
            dataSet.addEntry(new Entry(timestamp, value));

            // Remove any values older than the number of breathing signal points we want to display
            while (dataSet.getValues().size() > Constants.NUMBER_BREATHING_SIGNAL_SAMPLES_ON_CHART) {
                dataSet.removeFirst();
            }
            mBreathingSignalChart.getData().notifyDataChanged();
            mBreathingSignalChart.notifyDataSetChanged();
            mBreathingSignalChart.invalidate();
        }
    }
}
