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
        mBreathingSignalChart.setDescription(new Description());
        mBreathingSignalChart.setDrawGridBackground(false);

        XAxis xAxis = mBreathingSignalChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setValueFormatter(new XAxisValueFormatter());

        YAxis leftAxis = mBreathingSignalChart.getAxisLeft();
        //leftAxis.setLabelCount(5, false);

        YAxis rightAxis = mBreathingSignalChart.getAxisRight();
        rightAxis.setLabelCount(5, false);
        rightAxis.setDrawGridLines(false);

        mBreathingSignalChart.animateX(750);
    }

    private void setupLineDataSet() {
        LineDataSet set1;
        set1 = new LineDataSet(new ArrayList<Entry>(), getString(R.string.graphs_breathing_signal_title));

        set1.setLineWidth(2.5f);
        //set1.setCircleRadius(4.5f);
        //set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setColor(Color.rgb(0, 0, 255));
        //set1.setCircleColor(Color.rgb(0, 0, 255));
        set1.setDrawValues(false);
        //set1.setDrawFilled(true);
        //set1.setFillColor(Color.rgb(0, 0, 255));

        // add the datasets
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);

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
        if (mBreathingSignalChart != null) {
            Log.i("DF", "addBreathingSignalData");

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
