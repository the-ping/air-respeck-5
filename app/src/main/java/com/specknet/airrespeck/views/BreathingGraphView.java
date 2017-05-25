package com.specknet.airrespeck.views;

import android.app.Activity;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.models.BreathingGraphData;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.models.XAxisValueFormatter;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;

/**
 * Graph which displays a live breathing signal when continuously fed with new data. It takes care of buffering incoming data to
 * make the updates smooth
 */

public class BreathingGraphView extends LineChart {

    private int mUpdateDelayBreathingGraph;
    private Handler mBreathingGraphHandler;
    private LinkedList<BreathingGraphData> mBreathingDataQueue = new LinkedList<>();

    private final int DEFAULT_DELAY = Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS /
            Constants.NUMBER_OF_SAMPLES_PER_BATCH;

    private Activity mParentActivity;

    public BreathingGraphView(Activity parentActivity) {
        super(parentActivity);

        mParentActivity = parentActivity;

        setupBreathingSignalChart();
        setupLineDataSetForChart();
    }

    public void addToBreathingGraphQueue(RESpeckLiveData data) {
        BreathingGraphData breathingGraphData = new BreathingGraphData(
                Utils.onlyKeepTimeInHour(data.getPhoneTimestamp()), data.getAccelX(), data.getAccelY(),
                data.getAccelZ(), data.getBreathingSignal());
        mBreathingDataQueue.add(breathingGraphData);
    }

    /**
     * Setup Breathing Signal chart.
     */
    private void setupBreathingSignalChart() {
        // Empty description text
        Description emptyDescription = new Description();
        emptyDescription.setText("");
        setDescription(emptyDescription);
        setDrawGridBackground(false);
        getLegend().setEnabled(false);

        // Set axis properties
        XAxis xAxis = getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new XAxisValueFormatter());

        YAxis leftAxis = getAxisLeft();
        leftAxis.setLabelCount(5, false);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = getAxisRight();
        rightAxis.setLabelCount(5, false);

        animateX(750);
    }

    private void setupLineDataSetForChart() {
        // create dataset for graph
        LineDataSet dataSet = new LineDataSet(new ArrayList<Entry>(), "");

        dataSet.setLineWidth(2.5f);
        dataSet.setColor(ContextCompat.getColor(getContext(), R.color.breathing_graph_line_color));
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);

        // Add the dataset
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        setData(new LineData(dataSets));

        // Disable helper lines showing an element the user touched
        getData().setHighlightEnabled(false);

        // Disable zoom on double tap
        setDoubleTapToZoomEnabled(false);
    }

    private void updateBreathingGraph(BreathingGraphData data) {
        Entry newEntry = new Entry(data.getTimestamp(), data.getBreathingSignal());

        // Define the axis limits
        float negativeLowerLimit = -2.0f;
        float negativeUpperLimit = -0.3f;
        float positiveLowerLimit = 0.3f;
        float positiveUpperLimit = 2.0f;

        LineDataSet dataSet = (LineDataSet) getData().getDataSetByIndex(0);
        dataSet.addEntry(newEntry);

        // Remove any values older than the number of breathing signal points we want to display
        while (dataSet.getValues().size() > Constants.BREATHING_SIGNAL_CHART_NUMBER_OF_SAMPLES) {
            dataSet.removeFirst();
        }

        // Recalculate dataSet parameters
        getData().notifyDataChanged();

        // Load the current min and max of the data set
        float minOfDataSet = dataSet.getYMin();
        float maxOfDataSet = dataSet.getYMax();

        // Adjust the minimum of the displayed chart based on the minimum of the dataset and the minimum/maximum limit
        if (minOfDataSet < negativeLowerLimit) {
            getAxisLeft().setAxisMinimum(negativeLowerLimit);
            getAxisRight().setAxisMinimum(negativeLowerLimit);
        } else if (minOfDataSet > negativeUpperLimit) {
            getAxisLeft().setAxisMinimum(negativeUpperLimit);
            getAxisRight().setAxisMinimum(negativeUpperLimit);
        } else {
            // Display slightly more than the current dataset, so that the lowest value doesn't get cut off
            getAxisLeft().setAxisMinimum(minOfDataSet - 0.001f);
            getAxisRight().setAxisMinimum(minOfDataSet - 0.001f);
        }

        // Adjust the maximum of the displayed chart based on the maximum of the dataset and the minimum/maximum limit
        if (maxOfDataSet < positiveLowerLimit) {
            getAxisLeft().setAxisMaximum(positiveLowerLimit);
            getAxisRight().setAxisMaximum(positiveLowerLimit);
        } else if (maxOfDataSet > positiveUpperLimit) {
            getAxisLeft().setAxisMaximum(positiveUpperLimit);
            getAxisRight().setAxisMaximum(positiveUpperLimit);
        } else {
            // Display slightly more than the current dataset, so that the highest value doesn't get cut off
            getAxisLeft().setAxisMaximum(maxOfDataSet + 0.001f);
            getAxisRight().setAxisMaximum(maxOfDataSet + 0.001f);
        }

        // Update UI
        notifyDataSetChanged();
        invalidate();
    }

    private void startBreathingGraphUpdaterTask() {
        mBreathingGraphHandler = new Handler();

        // This class is used to determine the update frequency of the breathing data graph. The data from the
        // RESpeck is coming in in batches, which would make the graph hard to read. Therefore, we store the
        // incoming data in a queue and update the graph smoothly.
        class BreathingUpdaterRunner implements Runnable {
            private boolean queueHadBeenFilled = false;

            @Override
            public void run() {
                /*
                Log.v("BreathingGraph", String.format(Locale.UK, "Breathing data queue length: %d. Processing speed: %d ms",
                        mBreathingDataQueue.size(), mUpdateDelayBreathingGraph));
                */

                if (mBreathingDataQueue.isEmpty()) {
                    // If the queue is empty and there has been data in the queue previously,
                    // this means we were too fast. Wait for another delay and decrease the processing speed.
                    // Only do this if we're below a certain threshold (set with intuition here)
                    if (queueHadBeenFilled && mUpdateDelayBreathingGraph <= 1.3 * DEFAULT_DELAY) {
                        mUpdateDelayBreathingGraph += 1;
                        /*
                        Log.v("BreathingGraph", String.format(Locale.UK,
                                "Breathing graph data queue empty: decrease processing speed to: %d ms",
                                mUpdateDelayBreathingGraph));
                                */
                    }

                    mBreathingGraphHandler.postDelayed(this, mUpdateDelayBreathingGraph);
                } else {
                    // Remember the fact that we have already received data
                    queueHadBeenFilled = true;

                    mParentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateBreathingGraph(mBreathingDataQueue.removeFirst());
                        }
                    });

                    // If our queue is too long, increase processing speed. The size threshold was set intuitively and
                    // might have to be adjusted
                    if (mBreathingDataQueue.size() > Constants.NUMBER_OF_SAMPLES_PER_BATCH) {
                        mUpdateDelayBreathingGraph -= 1;
                        /*
                        Log.v("BreathingGraph", String.format(Locale.UK,
                                "Breathing graph data queue too full: increase processing speed to: %d ms",
                                mUpdateDelayBreathingGraph));
                                */
                    }
                    mBreathingGraphHandler.postDelayed(this, mUpdateDelayBreathingGraph);
                }
            }
        }
        mBreathingGraphHandler.postDelayed(new BreathingUpdaterRunner(), mUpdateDelayBreathingGraph);
    }


    public void startBreathingGraphUpdates() {
        mUpdateDelayBreathingGraph = DEFAULT_DELAY;
        mBreathingDataQueue.clear();
        startBreathingGraphUpdaterTask();
    }

    public void stopBreathingGraphUpdates() {
        if (mBreathingGraphHandler != null) {
            mBreathingGraphHandler.removeCallbacksAndMessages(null);
        }
    }
}
