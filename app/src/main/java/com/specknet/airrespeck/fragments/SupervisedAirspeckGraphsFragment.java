package com.specknet.airrespeck.fragments;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.AirspeckDataObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.XAxisValueFormatter;
import com.specknet.airrespeck.utils.Constants;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.specknet.airrespeck.utils.Utils.onlyKeepTimeInHour;


public class SupervisedAirspeckGraphsFragment extends BaseFragment implements AirspeckDataObserver {

    private int BINS_NUMBER = 16;
    private LinkedList<AirspeckData> dataBuffer = new LinkedList<>();

    private BarChart mBinsLineChart;
    private LineChart mPMsLineChart;

    private final String LAST_VALUES = "last_values";

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SupervisedAirspeckGraphsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the utilities
        Utils.init(getContext());

        ((MainActivity) getActivity()).registerAirspeckDataObserver(this);

        // Load previously used data if there is any (this is used when the fragment is stopped which happens
        // when the user swipes two Fragments away)
        if (savedInstanceState != null) {
            Log.i("AQ graphs", "bundle: " + savedInstanceState);
            if (savedInstanceState.containsKey(LAST_VALUES)) {
                // ON MIUI 9 at least, an ArrayList is returned and trying to cast to LinkedList causes a crash
                //dataBuffer = (LinkedList<AirspeckData>) savedInstanceState.getSerializable(LAST_VALUES);
                dataBuffer = new LinkedList<>((List<AirspeckData>) savedInstanceState.getSerializable(LAST_VALUES));
                if (dataBuffer.size() > 0) {
                    long currentTimestamp = com.specknet.airrespeck.utils.Utils.getUnixTimestamp();
                    if (dataBuffer.getLast().getPhoneTimestamp() < currentTimestamp - 60000) {
                        Log.i("AQ graphs", "Saved instance was too far in the past, start new graphs!");
                        dataBuffer = new LinkedList<>();
                    }
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i("AirspeckGraphs", "onSavedInstanceState");
        outState.putSerializable(LAST_VALUES, dataBuffer);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_graphs, container, false);

        mPMsLineChart = (LineChart) view.findViewById(R.id.pms_line_chart);
        mBinsLineChart = (BarChart) view.findViewById(R.id.bins_line_chart);

        setupPMsChart();
        setupBinsChart();

        mIsCreated = true;

        return view;
    }

    /**
     * Setup PMs (PM1, PM2.5, and PM10) chart.
     */
    private void setupPMsChart() {
        // No description
        Description emptyDescription = new Description();
        emptyDescription.setText("");
        mPMsLineChart.setDescription(emptyDescription);

        mPMsLineChart.setDrawGridBackground(false);

        XAxis xAxis = mPMsLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setValueFormatter(new XAxisValueFormatter());

        YAxis leftAxis = mPMsLineChart.getAxisLeft();
        leftAxis.setLabelCount(5, false);

        YAxis rightAxis = mPMsLineChart.getAxisRight();
        rightAxis.setLabelCount(5, false);
        rightAxis.setDrawGridLines(false);

        // initial data
        // IMPORTANT: must have exactly 3 data sets
        ArrayList<Entry> v1 = new ArrayList<>();
        ArrayList<Entry> v2 = new ArrayList<>();
        ArrayList<Entry> v3 = new ArrayList<>();

        for (int i = 0; i < dataBuffer.size(); ++i) {
            float timestamp = onlyKeepTimeInHour(dataBuffer.get(i).getPhoneTimestamp());
            v1.add(new Entry(timestamp, dataBuffer.get(i).getPm1()));
            v2.add(new Entry(timestamp, dataBuffer.get(i).getPm2_5()));
            v3.add(new Entry(timestamp, dataBuffer.get(i).getPm10()));
        }

        LineDataSet setPM1 = new LineDataSet(v1, "PM 1");
        setPM1.setLineWidth(2.5f);
        setPM1.setCircleRadius(4.5f);
        setPM1.setColor(Color.rgb(0, 0, 0));
        setPM1.setCircleColor(Color.rgb(0, 0, 0));
        setPM1.setDrawValues(false);

        LineDataSet setPM2_5 = new LineDataSet(v2, "PM 2.5");
        setPM2_5.setLineWidth(2.5f);
        setPM2_5.setCircleRadius(4.5f);
        setPM2_5.setColor(Color.rgb(255, 0, 0));
        setPM2_5.setCircleColor(Color.rgb(255, 0, 0));
        setPM2_5.setDrawValues(false);

        LineDataSet setPM10 = new LineDataSet(v3, "PM 10");
        setPM10.setLineWidth(2.5f);
        setPM10.setCircleRadius(4.5f);
        setPM10.setColor(Color.rgb(0, 0, 255));
        setPM10.setCircleColor(Color.rgb(0, 0, 255));
        setPM10.setDrawValues(false);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setPM1);
        dataSets.add(setPM2_5);
        dataSets.add(setPM10);

        // add data object
        mPMsLineChart.setData(new LineData(dataSets));

        // Disable helper lines showing an element the user touched
        mPMsLineChart.getData().setHighlightEnabled(false);

        // Disable zoom on double tap
        mPMsLineChart.setDoubleTapToZoomEnabled(false);
    }

    /**
     * Setup Bins chart.
     */
    private void setupBinsChart() {
        // no description text
        Description emptyDescription = new Description();
        emptyDescription.setText("");
        mBinsLineChart.setDescription(emptyDescription);

        mBinsLineChart.setDrawGridBackground(false);

        XAxis xAxis = mBinsLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setLabelCount(BINS_NUMBER);

        YAxis leftAxis = mBinsLineChart.getAxisLeft();
        leftAxis.setDrawLabels(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setLabelCount(5, false);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(false);

        YAxis rightAxis = mBinsLineChart.getAxisRight();
        rightAxis.setDrawLabels(false);
        rightAxis.setDrawAxisLine(false);
        rightAxis.setLabelCount(5, false);
        rightAxis.setDrawGridLines(false);

        // Create new data set
        BarDataSet barData = new BarDataSet(new ArrayList<BarEntry>(), getString(R.string.graphs_bins_description));
        barData.setColor(ContextCompat.getColor(getContext(), R.color.bins_graph_color));

        // Add data set to list of data sets
        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(barData);

        // Attach data sets to to chart
        mBinsLineChart.setData(new BarData(dataSets));

        // Disable helper lines showing an element the user touched
        mBinsLineChart.getData().setHighlightEnabled(false);

        // Disable zoom on double tap
        mBinsLineChart.setDoubleTapToZoomEnabled(false);

        if (dataBuffer.size() > 0) {
            updateBinsChart();
        }
    }


    @Override
    public void updateAirspeckData(AirspeckData data) {
        dataBuffer.add(data);
        while (dataBuffer.size() > Constants.PM_CHART_NUMBER_OF_SAMPLES) {
            dataBuffer.remove();
        }
        updateBinsChart();
        updatePMChart();
    }

    private void updateBinsChart() {
        ArrayList<BarEntry> values = new ArrayList<>();

        for (int i = 0; i < BINS_NUMBER; ++i) {
            values.add(new BarEntry(i, dataBuffer.peekLast().getBins()[i]));
        }

        // There is an existing data set. Update that.
        BarDataSet barData = (BarDataSet) mBinsLineChart.getData().getDataSetByIndex(0);
        barData.setValues(values);

        mBinsLineChart.getData().notifyDataChanged();
        mBinsLineChart.notifyDataSetChanged();

        // refresh the drawing
        mBinsLineChart.invalidate();
    }

    private void updatePMChart() {
        LineData lineData = mPMsLineChart.getLineData();

        AirspeckData newData = dataBuffer.getLast();

        for (int i = 0; i < 3; ++i) {
            float yValue = 0f;
            switch (i) {
                case 0:
                    yValue = newData.getPm1();
                    break;
                case 1:
                    yValue = newData.getPm2_5();
                    break;
                case 2:
                    yValue = newData.getPm10();
                    break;
            }

            LineDataSet dataSet = (LineDataSet) lineData.getDataSetByIndex(i);
            dataSet.addEntry(new Entry(onlyKeepTimeInHour(newData.getPhoneTimestamp()), yValue));

            while (dataSet.getEntryCount() > Constants.PM_CHART_NUMBER_OF_SAMPLES) {
                dataSet.removeFirst();
            }
            dataSet.notifyDataSetChanged();
        }

        lineData.notifyDataChanged();
        mPMsLineChart.notifyDataSetChanged();
        mPMsLineChart.invalidate();
    }
}
