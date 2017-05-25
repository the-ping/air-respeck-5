package com.specknet.airrespeck.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.models.BreathingGraphData;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.models.XAxisValueFormatter;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;

/**
 * Created by Darius on 08.02.2017.
 */

public class SubjectWindmillFragment extends BaseFragment implements RESpeckDataObserver {

    TextView breathingRateText;
    TextView averageBreathingRateText;

    ImageView activityIcon;

    private ImageView connectedStatusRESpeck;

    private int updateDelayBreathingGraph;
    private Handler breathingGraphHandler;
    private LinkedList<BreathingGraphData> mBreathingDataQueue = new LinkedList<>();

    private final int DEFAULT_DELAY = Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS /
            Constants.NUMBER_OF_SAMPLES_PER_BATCH;

    // Graphs
    private LineChart mBreathingFlowChart;


    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SubjectWindmillFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);
        startBreathingGraphUpdaterTask();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_windmill, container, false);

        // Load connection symbols
        connectedStatusRESpeck = (ImageView) view.findViewById(R.id.connected_status_respeck);

        // Load breathing textviews and icon
        breathingRateText = (TextView) view.findViewById(R.id.text_breathing);
        averageBreathingRateText = (TextView) view.findViewById(R.id.text_breathing_average);
        activityIcon = (ImageView) view.findViewById(R.id.activity_icon);

        // Setup flow graph
        mBreathingFlowChart = (LineChart) view.findViewById(R.id.breathing_flow_line_chart);
        setupBreathingSignalChart(mBreathingFlowChart);
        setupLineDataSetForChart(mBreathingFlowChart);

        // Setup onClick handler for buttons. We can't define them in the xml as that would search in MainActivity
        ImageButton diaryButton = (ImageButton) view.findViewById(R.id.image_button_diary);
        ImageButton rehabButton = (ImageButton) view.findViewById(R.id.image_button_exercise);

        diaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDiary();
            }
        });

        rehabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchRehab();
            }
        });

        mIsCreated = true;

        // Update connection symbol based on state stored in MainActivity
        updateRESpeckConnectionSymbol(((MainActivity) getActivity()).getIsRESpeckConnected());

        return view;
    }

    // This method gets called from the rehab button in this fragment.
    private void launchRehab() {
        Intent LaunchIntent = getContext().getPackageManager().getLaunchIntentForPackage("com.specknet.rehab2");
        try {
            startActivity(LaunchIntent);
        } catch (NullPointerException e) {
            ((MainActivity) getActivity()).showOnSnackbar("Unable to start Rehab app. Is app installed?");
        }
    }

    // This method gets called from the diary button in this fragment.
    private void launchDiary() {
        Intent LaunchIntent = getContext().getPackageManager().getLaunchIntentForPackage("com.specknet.rehabdiary");
        try {
            startActivity(LaunchIntent);
        } catch (NullPointerException e) {
            ((MainActivity) getActivity()).showOnSnackbar("Unable to start Diary app. Is app installed?");
        }
    }

    public void updateRESpeckConnectionSymbol(boolean isConnected) {
        if (connectedStatusRESpeck != null) {
            if (isConnected) {
                // "Flash" with symbol when updating to indicate data coming in
                connectedStatusRESpeck.setImageResource(R.drawable.vec_wireless);
                connectedStatusRESpeck.setVisibility(View.INVISIBLE);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connectedStatusRESpeck.setVisibility(View.VISIBLE);
                    }
                }, 100);

            } else {
                connectedStatusRESpeck.setImageResource(R.drawable.vec_xmark);
            }
        }
    }

    private void updateReadings(RESpeckLiveData data) {
        // Update UI
        mBreathingFlowChart.notifyDataSetChanged();
        mBreathingFlowChart.invalidate();

        // Update readings and activity symbol
        // Set breathing rate text to currently calculated rates
        breathingRateText.setText(String.format(Locale.UK, "%.2f BrPM", data.getBreathingRate()));
        averageBreathingRateText.setText(String.format(Locale.UK, "%.2f BrPM", data.getAvgBreathingRate()));

        // Set activity icon to reflect currently predicted activity
        switch (data.getActivityType()) {
            case Constants.ACTIVITY_LYING:
                activityIcon.setImageResource(R.drawable.vec_lying);
                break;
            case Constants.ACTIVITY_WALKING:
                activityIcon.setImageResource(R.drawable.vec_walking);
                break;
            case Constants.ACTIVITY_STAND_SIT:
            default:
                activityIcon.setImageResource(R.drawable.vec_standing_sitting);
        }
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        Log.i("RESpeckReadings", "updateRESpeckData");
        if (mIsCreated) {
           updateReadings(data);

            // Update the graphs if present
            BreathingGraphData breathingGraphData = new BreathingGraphData(
                    Utils.onlyKeepTimeInHour(data.getPhoneTimestamp()), data.getAccelX(), data.getAccelY(),
                    data.getAccelZ(), data.getBreathingSignal());
            mBreathingDataQueue.add(breathingGraphData);
        }
    }

    /**
     * Setup Breathing Signal chart.
     */
    private void setupBreathingSignalChart(LineChart chart) {
        // Empty description text
        Description emptyDescription = new Description();
        emptyDescription.setText("");
        chart.setDescription(emptyDescription);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new XAxisValueFormatter());

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setLabelCount(5, false);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setLabelCount(5, false);

        chart.animateX(750);
    }

    private void setupLineDataSetForChart(LineChart chart) {
        // create dataset for graph
        LineDataSet dataSet = new LineDataSet(new ArrayList<Entry>(), "");

        dataSet.setLineWidth(2.5f);
        dataSet.setColor(ContextCompat.getColor(getContext(), R.color.breathing_graph_line_color));
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);

        // add the dataset
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        chart.setData(new LineData(dataSets));

        // Disable helper lines showing an element the user touched
        chart.getData().setHighlightEnabled(false);

        // Disable zoom on double tap
        chart.setDoubleTapToZoomEnabled(false);
    }

    private void updateBreathingGraph(BreathingGraphData data) {
        Entry newEntry = new Entry(data.getTimestamp(), data.getBreathingSignal());
        updateGraph(newEntry, mBreathingFlowChart);
    }

    private void updateGraph(Entry newEntry, LineChart chart) {
        // Set the limits based on the graph type
        float negativeLowerLimit = -2.0f;
        float negativeUpperLimit = -0.3f;
        ;
        float positiveLowerLimit = 0.3f;
        ;
        float positiveUpperLimit = 2.0f;
        ;

        LineDataSet dataSet = (LineDataSet) chart.getData().getDataSetByIndex(0);
        dataSet.addEntry(newEntry);

        // Remove any values older than the number of breathing signal points we want to display
        while (dataSet.getValues().size() > Constants.BREATHING_SIGNAL_CHART_NUMBER_OF_SAMPLES) {
            dataSet.removeFirst();
        }

        // Recalculate dataSet parameters
        chart.getData().notifyDataChanged();

        // Load the current min and max of the data set
        float minOfDataSet = dataSet.getYMin();
        float maxOfDataSet = dataSet.getYMax();

        // Adjust the minimum of the displayed chart based on the minimum of the dataset and the minimum/maximum limit
        if (minOfDataSet < negativeLowerLimit) {
            chart.getAxisLeft().setAxisMinimum(negativeLowerLimit);
            chart.getAxisRight().setAxisMinimum(negativeLowerLimit);
        } else if (minOfDataSet > negativeUpperLimit) {
            chart.getAxisLeft().setAxisMinimum(negativeUpperLimit);
            chart.getAxisRight().setAxisMinimum(negativeUpperLimit);
        } else {
            // Display slightly more than the current dataset, so that the lowest value doesn't get cut off
            chart.getAxisLeft().setAxisMinimum(minOfDataSet - 0.001f);
            chart.getAxisRight().setAxisMinimum(minOfDataSet - 0.001f);
        }

        // Adjust the maximum of the displayed chart based on the maximum of the dataset and the minimum/maximum limit
        if (maxOfDataSet < positiveLowerLimit) {
            chart.getAxisLeft().setAxisMaximum(positiveLowerLimit);
            chart.getAxisRight().setAxisMaximum(positiveLowerLimit);
        } else if (maxOfDataSet > positiveUpperLimit) {
            chart.getAxisLeft().setAxisMaximum(positiveUpperLimit);
            chart.getAxisRight().setAxisMaximum(positiveUpperLimit);
        } else {
            // Display slightly more than the current dataset, so that the highest value doesn't get cut off
            chart.getAxisLeft().setAxisMaximum(maxOfDataSet + 0.001f);
            chart.getAxisRight().setAxisMaximum(maxOfDataSet + 0.001f);
        }

        // Update UI
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void startBreathingGraphUpdaterTask() {
        breathingGraphHandler = new Handler();

        // This class is used to determine the update frequency of the breathing data graph. The data from the
        // RESpeck is coming in in batches, which would make the graph hard to read. Therefore, we store the
        // incoming data in a queue and update the graph smoothly.
        class breathingUpdaterRunner implements Runnable {
            private boolean queueHadBeenFilled = false;

            @Override
            public void run() {
                Log.v("DF", String.format(Locale.UK, "Breathing data queue length: %d",
                        mBreathingDataQueue.size()));

                if (mBreathingDataQueue.isEmpty()) {
                    // If the queue is empty and there has been data in the queue previously,
                    // this means we were too fast. Wait for another delay and decrease the processing speed.
                    // Only do this if we're below a certain threshold (set with intuition here)
                    if (queueHadBeenFilled && updateDelayBreathingGraph <= 1.1 * DEFAULT_DELAY) {
                        updateDelayBreathingGraph += 1;
                        Log.v("DF", String.format(Locale.UK,
                                "Breathing graph data queue empty: decrease processing speed to: %d ms",
                                updateDelayBreathingGraph));
                    }

                    breathingGraphHandler.postDelayed(this, updateDelayBreathingGraph);
                } else {
                    // Remember the fact that we have already received data
                    queueHadBeenFilled = true;

                    if (isAdded()) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateBreathingGraph(mBreathingDataQueue.removeFirst());
                            }
                        });
                    }

                    // If our queue is too long, increase processing speed. The size threshold was set intuitively and
                    // might have to be adjusted
                    if (mBreathingDataQueue.size() > Constants.NUMBER_OF_SAMPLES_PER_BATCH) {
                        updateDelayBreathingGraph -= 1;
                        Log.v("DF", String.format(Locale.UK,
                                "Breathing graph data queue too full: increase processing speed to: %d ms",
                                updateDelayBreathingGraph));
                    }

                    breathingGraphHandler.postDelayed(this, updateDelayBreathingGraph);
                }
            }
        }
        breathingGraphHandler.postDelayed(new breathingUpdaterRunner(), updateDelayBreathingGraph);
    }

    @Override
    public void onDetach() {
        if (breathingGraphHandler != null) {
            breathingGraphHandler.removeCallbacksAndMessages(null);
        }
        super.onDetach();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        updateDelayBreathingGraph = DEFAULT_DELAY;
        mBreathingDataQueue.clear();
    }
}


