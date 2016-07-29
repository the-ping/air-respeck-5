package com.specknet.airrespeck.fragments;


import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Utils;
import com.specknet.airrespeck.R;

import java.util.ArrayList;
import java.util.List;


public class GraphsFragment extends BaseFragment {

    public static class PMs {
        public final static int PMS_NUM = 3;

        private float mPM1;
        private float mPM2_5;
        private float mPM10;

        public PMs(final float pm1, final float pm2_5, final float pm10) {
            mPM1 = pm1;
            mPM2_5 = pm2_5;
            mPM10 = pm10;
        }

        public void setPM1(final float pm1) { mPM1 = pm1; }
        public float getPM1() { return mPM1; }
        public void setPM2_5(final float pm2_5) { mPM2_5 = pm2_5; }
        public float getPM2_5() { return mPM2_5; }
        public void setPM10(final float pm10) { mPM10 = pm10; }
        public float getPM10() { return mPM10; }
    }

    private int BINS_NUMBER = 16;
    private List<Float> mBinsData;
    private List<PMs> mPMsData;

    private LineChart mBinsLineChart;
    private LineChart mPMsLineChart;


    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GraphsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize the utilities
        Utils.init(getContext());

        mPMsData = new ArrayList<PMs>();
        mPMsData.add(new PMs(0f, 0f, 0f));

        mBinsData = new ArrayList<Float>();
        for (int i = 0; i < BINS_NUMBER; ++i) {
            mBinsData.add(0f);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_graphs, container, false);

        mConnectingLayout = (LinearLayout) view.findViewById(R.id.connecting_layout);

        mPMsLineChart = (LineChart) view.findViewById(R.id.pms_line_chart);
        mBinsLineChart = (LineChart) view.findViewById(R.id.bins_line_chart);

        setupPMsChart();
        setupBinsChart();

        return  view;
    }

    /**
     * Setup PMs (PM1, PM2.5, and PM10) chart.
     */
    private void setupPMsChart() {
        mPMsLineChart.setDrawGridBackground(false);
        mPMsLineChart.setDescription("");

        XAxis xAxis = mPMsLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);

        YAxis leftAxis = mPMsLineChart.getAxisLeft();
        leftAxis.setLabelCount(5, false);
        leftAxis.setAxisMinValue(0f); // this replaces setStartAtZero(true)

        YAxis rightAxis = mPMsLineChart.getAxisRight();
        rightAxis.setLabelCount(5, false);
        rightAxis.setDrawGridLines(false);
        rightAxis.setAxisMinValue(0f); // this replaces setStartAtZero(true)

        // initial data
        // IMPORTANT: must have exactly 3 data sets
        ArrayList<Entry> v1 = new ArrayList<Entry>();
        ArrayList<Entry> v2 = new ArrayList<Entry>();
        ArrayList<Entry> v3 = new ArrayList<Entry>();

        for (int i = 0; i < mPMsData.size(); ++i) {
            v1.add(new Entry(i, mPMsData.get(i).getPM1()));
            v2.add(new Entry(i, mPMsData.get(i).getPM2_5()));
            v3.add(new Entry(i, mPMsData.get(i).getPM10()));
        }

        LineDataSet set1 = new LineDataSet(v1, "PM 1");
        set1.setLineWidth(2.5f);
        set1.setCircleRadius(4.5f);
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setColor(Color.rgb(255, 255, 255));
        set1.setCircleColor(Color.rgb(255, 255, 255));
        set1.setDrawValues(false);
        set1.setDrawFilled(true);
        set1.setFillColor(Color.rgb(255, 255, 255));

        LineDataSet set2 = new LineDataSet(v2, "PM 2.5");
        set2.setLineWidth(2.5f);
        set2.setCircleRadius(4.5f);
        set2.setHighLightColor(Color.rgb(244, 117, 117));
        set2.setColor(Color.rgb(255, 0, 0));
        set2.setCircleColor(Color.rgb(255, 0, 0));
        set2.setDrawValues(false);
        set2.setDrawFilled(true);
        set2.setFillColor(Color.rgb(255, 0, 0));

        LineDataSet set3 = new LineDataSet(v3, "PM 10");
        set3.setLineWidth(2.5f);
        set3.setCircleRadius(4.5f);
        set3.setHighLightColor(Color.rgb(244, 117, 117));
        set3.setColor(Color.rgb(0, 0, 255));
        set3.setCircleColor(Color.rgb(0, 0, 255));
        set3.setDrawValues(false);
        set3.setDrawFilled(true);
        set3.setFillColor(Color.rgb(0, 0, 255));

        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(set1);
        dataSets.add(set2);
        dataSets.add(set3);

        // add data object
        mPMsLineChart.setData(new LineData(dataSets));
    }

    /**
     * Add new entries to each of the line graphs (PM1, PM2.5, and PM10) in PMs chart
     * @param pMs PMs The new values.
     */
    private void addEntries(final PMs pMs) {
        LineData data = mPMsLineChart.getData();

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

            data.addEntry(new Entry(data.getDataSetByIndex(i).getEntryCount(), yValue), i);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mPMsLineChart.notifyDataSetChanged();

            mPMsLineChart.setVisibleXRangeMaximum(10);
            //mChart.setVisibleYRangeMaximum(15, AxisDependency.LEFT);

            // this automatically refreshes the chart (calls invalidate())
            mPMsLineChart.moveViewTo(data.getEntryCount() - 7, 50f, YAxis.AxisDependency.LEFT);
        }
    }

    /**
     * Helper to add new values to PMs chart.
     * @param pMs PMs The new values.
     */
    public void addPMsChartData(final PMs pMs) {
        if (mPMsData != null && mPMsLineChart != null) {
            mPMsData.add(pMs);
            addEntries(pMs);
        }
    }


    /**
     * Setup Bins chart.
     */
    private void setupBinsChart() {
        mBinsLineChart.setDrawGridBackground(false);

        // no description text
        mBinsLineChart.setDescription("");
        mBinsLineChart.setDrawGridBackground(false);

        XAxis xAxis = mBinsLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);

        YAxis leftAxis = mBinsLineChart.getAxisLeft();
        leftAxis.setLabelCount(5, false);
        leftAxis.setAxisMinValue(0f); // this replaces setStartAtZero(true)

        YAxis rightAxis = mBinsLineChart.getAxisRight();
        rightAxis.setLabelCount(5, false);
        rightAxis.setDrawGridLines(false);
        rightAxis.setAxisMinValue(0f); // this replaces setStartAtZero(true)

        // set data
        updateBinsChartData();

        mBinsLineChart.animateX(750);
    }

    /**
     * Update the dataset for the Bins chart.
     */
    private void updateBinsChartData() {
        int count = mBinsData.size();

        ArrayList<Entry> values = new ArrayList<Entry>();

        for (int i = 0; i < count; ++i) {
            values.add(new Entry(i, mBinsData.get(i)));
        }

        LineDataSet set1;

        if (mBinsLineChart.getData() != null && mBinsLineChart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) mBinsLineChart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mBinsLineChart.getData().notifyDataChanged();
            mBinsLineChart.notifyDataSetChanged();
        }
        else {
            // create a dataset and give it a type
            set1 = new LineDataSet(values, "Bins");

            set1.setLineWidth(2.5f);
            set1.setCircleRadius(4.5f);
            set1.setHighLightColor(Color.rgb(244, 117, 117));
            set1.setColor(Color.rgb(0, 255, 0));
            set1.setCircleColor(Color.rgb(0, 255, 0));
            set1.setDrawValues(false);
            set1.setDrawFilled(true);
            set1.setFillColor(Color.rgb(0, 255, 0));

            // add the datasets
            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);

            // create a data object with the datasets
            LineData data = new LineData(dataSets);

            // set data
            mBinsLineChart.setData(data);
        }

        // refresh the drawing
        mBinsLineChart.invalidate();
    }

    /**
     * Helper to set the values for the Bins chart.
     * @param binsData List<Float> The new values.
     */
    public void setBinsChartData(List<Float> binsData) {
        if (mBinsData != null && mBinsLineChart != null) {
            mBinsData = binsData;
            updateBinsChartData();
        }
    }
}
