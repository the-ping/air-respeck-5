package com.specknet.airrespeck.fragments;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.specknet.airrespeck.models.XAxisValueFormatter;
import com.specknet.airrespeck.utils.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class SupervisedAQGraphsFragment extends BaseFragment {

    public static class PMs implements Serializable {
        final static int PMS_NUM = 3;

        private float mPM1;
        private float mPM2_5;
        private float mPM10;

        public PMs(final float pm1, final float pm2_5, final float pm10) {
            mPM1 = pm1;
            mPM2_5 = pm2_5;
            mPM10 = pm10;
        }

        float getPM1() {
            return mPM1;
        }

        float getPM2_5() {
            return mPM2_5;
        }

        float getPM10() {
            return mPM10;
        }

        @Override
        public String toString() {
            return String.format(Locale.UK, "PM1: %s, PM 2.5: %s, PM 10: %s", getPM1(), getPM2_5(), getPM10());
        }
    }

    private int BINS_NUMBER = 16;
    private List<Float> mBinsData;
    private List<PMs> mPMsData;
    private List<Float> mPMTimestamps;

    private BarChart mBinsLineChart;
    private LineChart mPMsLineChart;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SupervisedAQGraphsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize the utilities
        Utils.init(getContext());

        mPMsData = new ArrayList<>();
        mPMTimestamps = new ArrayList<>();

        mBinsData = new ArrayList<>();

        // Load previously used data if there is any (this is used when the fragment is stopped which happens
        // when the user swipes two Fragments away)
        if (savedInstanceState != null) {
            Log.i("AQ graphs", "bundle: " + savedInstanceState);
            if (savedInstanceState.containsKey("binsData")) {
                mBinsData = new ArrayList<>(Arrays.asList((Float[]) savedInstanceState.getSerializable("binsData")));
            }

            if (savedInstanceState.containsKey("pmTimestamps")) {
                mPMTimestamps = new ArrayList<>(
                        Arrays.asList((Float[]) savedInstanceState.getSerializable("pmTimestamps")));

                // If the last timestamp is too far in the past (more than one minute ago), delete saved data
                if (mPMTimestamps.size() > 0) {
                    Float lastTimestamp = mPMTimestamps.get(mPMTimestamps.size() - 1);
                    float currentTimestamp = com.specknet.airrespeck.utils.Utils.onlyKeepTimeInHour(
                            com.specknet.airrespeck.utils.Utils.getUnixTimestamp());

                    if (lastTimestamp != null && lastTimestamp > currentTimestamp - 60000) {
                        if (savedInstanceState.containsKey("pmData")) {
                            mPMsData = new ArrayList<>(Arrays.asList((PMs[]) savedInstanceState.getSerializable("pmData")));
                        } else {
                            mPMTimestamps = new ArrayList<>();
                        }
                    } else {
                        Log.i("AQ graphs", "Saved instance was too far in the past, start new graphs!");
                        mPMTimestamps = new ArrayList<>();
                    }
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("binsData", mBinsData.toArray(new Float[mBinsData.size()]));
        outState.putSerializable("pmData", mPMsData.toArray(new PMs[mPMsData.size()]));
        outState.putSerializable("pmTimestamps", mPMTimestamps.toArray(new Float[mPMTimestamps.size()]));

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_graphs, container, false);

        mConnectingLayout = (LinearLayout) view.findViewById(R.id.connecting_layout);
        TextView textConnectionLayout = (TextView) mConnectingLayout.findViewById(R.id.connection_text);

        boolean isAirspeckEnabled = Boolean.parseBoolean(
                com.specknet.airrespeck.utils.Utils.getInstance(getContext()).getProperties().getProperty(
                        Constants.Config.IS_AIRSPECK_ENABLED));

        // Change the connection text if we only connect to RESpeck
        if (!isAirspeckEnabled) {
            textConnectionLayout.setText(getString(R.string.connection_text_respeck_only));
        }

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

        for (int i = 0; i < mPMsData.size(); ++i) {
            v1.add(new Entry(mPMTimestamps.get(i), mPMsData.get(i).getPM1()));
            v2.add(new Entry(mPMTimestamps.get(i), mPMsData.get(i).getPM2_5()));
            v3.add(new Entry(mPMTimestamps.get(i), mPMsData.get(i).getPM10()));
        }

        LineDataSet setPM1 = new LineDataSet(v1, "PM 1");
        setPM1.setLineWidth(2.5f);
        setPM1.setCircleRadius(4.5f);
        setPM1.setColor(Color.rgb(0, 0, 0));
        setPM1.setCircleColor(Color.rgb(0, 0, 0));
        setPM1.setDrawValues(false);
//        setPM1.setFillColor(Color.rgb(0, 0, 0));

        LineDataSet setPM2_5 = new LineDataSet(v2, "PM 2.5");
        setPM2_5.setLineWidth(2.5f);
        setPM2_5.setCircleRadius(4.5f);
        setPM2_5.setColor(Color.rgb(255, 0, 0));
        setPM2_5.setCircleColor(Color.rgb(255, 0, 0));
        setPM2_5.setDrawValues(false);
//        setPM2_5.setFillColor(Color.rgb(255, 0, 0));

        LineDataSet setPM10 = new LineDataSet(v3, "PM 10");
        setPM10.setLineWidth(2.5f);
        setPM10.setCircleRadius(4.5f);
        setPM10.setColor(Color.rgb(0, 0, 255));
        setPM10.setCircleColor(Color.rgb(0, 0, 255));
        setPM10.setDrawValues(false);
//        setPM10.setFillColor(Color.rgb(0, 0, 255));

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
     * Add new entries to each of the line graphs (PM1, PM2.5, and PM10) in PMs chart
     *
     * @param pMs       PMs The new values.
     * @param timestamp The timestamp at which the PM values were recorded
     */
    private void addPMEntries(final PMs pMs, float timestamp) {
        LineData lineData = mPMsLineChart.getLineData();

        for (int i = 0; i < PMs.PMS_NUM; ++i) {
            // choose value
            float yValue = 0f;
            switch (i) {
                case 0:
                    yValue = pMs.getPM1();
                    break;
                case 1:
                    yValue = pMs.getPM2_5();
                    break;
                case 2:
                    yValue = pMs.getPM10();
                    break;
            }

            LineDataSet dataSet = (LineDataSet) lineData.getDataSetByIndex(i);
            dataSet.addEntry(new Entry(timestamp, yValue));

            while (dataSet.getEntryCount() > Constants.PM_CHART_NUMBER_OF_SAMPLES) {
                dataSet.removeFirst();
            }
            dataSet.notifyDataSetChanged();
        }
        lineData.notifyDataChanged();
        mPMsLineChart.notifyDataSetChanged();
        mPMsLineChart.invalidate();
    }

    /**
     * Helper to add new values to PMs chart.
     *
     * @param pMs PMs The new values.
     */
    public void addPMsChartData(final PMs pMs, final float timestamp) {
        if (mIsCreated) {
            mPMsData.add(pMs);
            while (mPMsData.size() > Constants.PM_CHART_NUMBER_OF_SAMPLES) {
                mPMsData.remove(0);
            }

            mPMTimestamps.add(timestamp);
            while (mPMTimestamps.size() > Constants.PM_CHART_NUMBER_OF_SAMPLES) {
                mPMTimestamps.remove(0);
            }

            addPMEntries(pMs, timestamp);
        }
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

        // Update chart
        updateBinsChartData();

        // Disable helper lines showing an element the user touched
        mBinsLineChart.getData().setHighlightEnabled(false);

        // Disable zoom on double tap
        mBinsLineChart.setDoubleTapToZoomEnabled(false);
    }

    /**
     * Update the dataset for the Bins chart.
     */
    private void updateBinsChartData() {
        ArrayList<BarEntry> values = new ArrayList<>();

        for (int i = 0; i < mBinsData.size(); ++i) {
            values.add(new BarEntry(i, mBinsData.get(i)));
        }

        // There is an existing data set. Update that.
        BarDataSet barData = (BarDataSet) mBinsLineChart.getData().getDataSetByIndex(0);
        barData.setValues(values);

        mBinsLineChart.getData().notifyDataChanged();
        mBinsLineChart.notifyDataSetChanged();

        // refresh the drawing
        mBinsLineChart.invalidate();
    }

    /**
     * Helper to set the values for the Bins chart.
     *
     * @param binsData List<Float> The new values.
     */
    public void setBinsChartData(List<Float> binsData) {
        if (mIsCreated) {
            mBinsData = binsData;
            updateBinsChartData();
        }
    }
}
