package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.mkobos.pca_transform.PCA;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.ReadingItemArrayAdapter;
import com.specknet.airrespeck.models.BreathingGraphData;
import com.specknet.airrespeck.models.ReadingItem;
import com.specknet.airrespeck.models.XAxisValueFormatter;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import Jama.Matrix;

/**
 * Created by Darius on 21.02.2017.
 * Fragment to display the respiratory signal
 */

public class SupervisedRESpeckReadingsFragment extends BaseFragment {

    // Breathing values
    private ArrayList<ReadingItem> mReadingItems;
    private ReadingItemArrayAdapter mListViewAdapter;

    // Graphs
    private LineChart mBreathingFlowChart;
    private LineChart mBreathingPCAChart;

    private LinkedList<BreathingGraphData> mPreFilteringQueue = new LinkedList<>();
    private LinkedList<BreathingGraphData> mBreathingDataQueue = new LinkedList<>();
    private LinkedList<Float> mPcaValueQueue = new LinkedList<>();
    private LinkedList<Float> mMeanPcaValueQueue = new LinkedList<>();

    private final int FLOW_CHART = 0;
    private final int PCA_CHART = 1;

    // Config variable storing visibility of PCA graph
    private boolean mShowPCAGraph;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SupervisedRESpeckReadingsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize the utilities
        com.github.mikephil.charting.utils.Utils.init(getContext());

        mReadingItems = new ArrayList<>();
        mReadingItems.add(new ReadingItem(getString(R.string.reading_breathing_rate),
                getString(R.string.reading_unit_bpm), Float.NaN));
        mReadingItems.add(new ReadingItem(getString(R.string.reading_avg_breathing_rate),
                getString(R.string.reading_unit_bpm), Float.NaN));
        mListViewAdapter = new ReadingItemArrayAdapter(getActivity(), mReadingItems);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_respeck_readings, container, false);

        Utils utils = Utils.getInstance(getContext());

        mConnectingLayout = (LinearLayout) view.findViewById(R.id.connecting_layout);
        TextView textConnectionLayout = (TextView) mConnectingLayout.findViewById(R.id.connection_text);
        boolean isAirspeckEnabled = Boolean.parseBoolean(
                utils.getProperties().getProperty(Constants.Config.IS_AIRSPECK_ENABLED));
        // Change the connection text if we only connect to RESpeck
        if (!isAirspeckEnabled) {
            textConnectionLayout.setText(getString(R.string.connection_text_respeck_only));
        }

        // Attach the adapter to a ListView for displaying the RESpeck readings
        ListView mListView = (ListView) view.findViewById(R.id.readings_list);
        mListView.setAdapter(mListViewAdapter);

        // Setup flow graph
        mBreathingFlowChart = (LineChart) view.findViewById(R.id.breathing_flow_line_chart);
        setupBreathingSignalChart(mBreathingFlowChart);
        setupLineDataSetForChart(mBreathingFlowChart);

        // Only display PCA chart if the config value is set to true
        mShowPCAGraph = Boolean.parseBoolean(utils.getProperties().getProperty(Constants.Config.SHOW_PCA_GRAPH));

        if (mShowPCAGraph) {
            mBreathingPCAChart = (LineChart) view.findViewById(R.id.breathing_pca_line_chart);
            setupBreathingSignalChart(mBreathingPCAChart);
            setupLineDataSetForChart(mBreathingPCAChart);
        } else {
            // Hide view
            LinearLayout pcaGraphContainer = (LinearLayout) view.findViewById(R.id.pca_chart_container);
            pcaGraphContainer.setVisibility(View.GONE);
        }
        return view;
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
    }

    // This method is called from the UI handler to update the graph
    public void updateBreathingGraphs(BreathingGraphData data) {
        // Update the graphs if present
        if (mBreathingFlowChart != null) {
            updateFlowGraph(data);
        }
        if (mBreathingPCAChart != null) {
            updatePCAGraph(data);
        }
    }

    private void updateFlowGraph(BreathingGraphData data) {
        Entry newEntry = new Entry(data.getTimestamp(), data.getBreathingSignal());
        updateGraph(newEntry, mBreathingFlowChart, FLOW_CHART);
    }

    private void updatePCAGraph(BreathingGraphData newData) {
        if (mBreathingDataQueue.size() > Constants.NUMBER_OF_SAMPLES_REQUIRED_FOR_PCA) {
            throw new RuntimeException("Breathing data queue exceeds limit!");

        } else if (mBreathingDataQueue.size() == Constants.NUMBER_OF_SAMPLES_REQUIRED_FOR_PCA) {
            // PCA transform on queued data
            // Generate matrix from queue
            double[][] matrixArray = new double[Constants.NUMBER_OF_SAMPLES_REQUIRED_FOR_PCA][3];
            for (int r = 0; r < matrixArray.length; r++) {
                matrixArray[r][0] = mBreathingDataQueue.get(r).getAccelX();
                matrixArray[r][1] = mBreathingDataQueue.get(r).getAccelY();
                matrixArray[r][2] = mBreathingDataQueue.get(r).getAccelZ();
            }
            Matrix trainingMatrix = new Matrix(matrixArray);

            // We want to transform the new data
            Matrix testMatrix = new Matrix(
                    new double[][]{{newData.getAccelX(), newData.getAccelY(), newData.getAccelZ()}});
            PCA pca = new PCA(trainingMatrix);
            Matrix transformedData = pca.transform(testMatrix, PCA.TransformationType.ROTATION);

            // The first value is the one of the dimension with the most information. We want that one!
            float pcaValue = (float) transformedData.get(0, 0);

            // Add the pca value to the queue
            mPcaValueQueue.add(pcaValue);
            limitQueueToSize(mPcaValueQueue, Constants.NUMBER_OF_SAMPLES_FOR_MEAN_SUBTRACTION);

            // If the mPcaValueQueue is long enough, we subtract the mean from the
            // value in the middle of the array
            if (mPcaValueQueue.size() == Constants.NUMBER_OF_SAMPLES_FOR_MEAN_SUBTRACTION) {
                // Calculate mean PCA of all PCA values in queue
                float meanPca = Utils.mean(
                        mPcaValueQueue.toArray(new Float[mPcaValueQueue.size()]));

                // Subtract the mean from the center value in the queue
                float correctedPca = mPcaValueQueue.get(Constants.NUMBER_OF_SAMPLES_FOR_MEAN_SUBTRACTION / 2) - meanPca;

                mMeanPcaValueQueue.add(correctedPca);
                limitQueueToSize(mMeanPcaValueQueue, Constants.NUMBER_OF_SAMPLES_FOR_MEAN_POST_FILTER);

                if (mMeanPcaValueQueue.size() == Constants.NUMBER_OF_SAMPLES_FOR_MEAN_POST_FILTER) {
                    Entry newEntry = new Entry(newData.getTimestamp(), Utils.mean(
                            mMeanPcaValueQueue.toArray(new Float[mMeanPcaValueQueue.size()])));
                    updateGraph(newEntry, mBreathingPCAChart, PCA_CHART);
                }

            }
        }

        // Pre-filtering of acceleration values
        mPreFilteringQueue.add(newData);
        limitQueueToSize(mPreFilteringQueue, Constants.NUMBER_OF_SAMPLES_FOR_MEAN_PRE_FILTER);

        if (mPreFilteringQueue.size() == Constants.NUMBER_OF_SAMPLES_FOR_MEAN_PRE_FILTER) {
            // Add the filtered sample to the mBreathingDataQueue and remove the oldest sample
            float[] xs = new float[mPreFilteringQueue.size()];
            float[] ys = new float[mPreFilteringQueue.size()];
            float[] zs = new float[mPreFilteringQueue.size()];

            for (int i = 0; i < mPreFilteringQueue.size(); i++) {
                xs[i] = mPreFilteringQueue.get(i).getAccelX();
                ys[i] = mPreFilteringQueue.get(i).getAccelY();
                zs[i] = mPreFilteringQueue.get(i).getAccelZ();
            }

            BreathingGraphData meanData = new BreathingGraphData(newData.getTimestamp(),
                    Utils.mean(xs), Utils.mean(ys), Utils.mean(zs), 0.0f);
            mBreathingDataQueue.add(meanData);
            limitQueueToSize(mBreathingDataQueue, Constants.NUMBER_OF_SAMPLES_REQUIRED_FOR_PCA);
        }

    }

    private void limitQueueToSize(Queue queue, int size) {
        while (queue.size() > size) {
            queue.remove();
        }
    }

    private void updateGraph(Entry newEntry, LineChart chart, int charttype) {
        // Set the limits based on the graph type
        float negativeLowerLimit;
        float negativeUpperLimit;
        float positiveLowerLimit;
        float positiveUpperLimit;

        if (charttype == FLOW_CHART) {
            negativeLowerLimit = -2.0f;
            negativeUpperLimit = -0.3f;
            positiveLowerLimit = 0.3f;
            positiveUpperLimit = 2.0f;
        } else if (charttype == PCA_CHART) {
            negativeLowerLimit = -0.2f;
            negativeUpperLimit = -0.03f;
            positiveLowerLimit = 0.03f;
            positiveUpperLimit = 0.2f;
        } else {
            throw new RuntimeException("Chart type unknown");
        }

        LineDataSet dataSet = (LineDataSet) chart.getData().getDataSetByIndex(0);
        dataSet.addEntry(newEntry);

        // Remove any values older than the number of breathing signal points we want to display
        while (dataSet.getValues().size() > Constants.NUMBER_BREATHING_SIGNAL_SAMPLES_ON_CHART) {
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
            Log.i("DF", String.format(Locale.UK, "set minimum based on data: %f",
                    minOfDataSet + negativeUpperLimit * 1.01f));
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
            Log.i("DF", String.format(Locale.UK, "set maximum based on data: %f",
                    maxOfDataSet + positiveUpperLimit * 1.01f));
            chart.getAxisLeft().setAxisMaximum(maxOfDataSet + 0.001f);
            chart.getAxisRight().setAxisMaximum(maxOfDataSet + 0.001f);
        }

        // Update UI
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    public void setReadings(final List<Float> values) {
        if (mReadingItems != null) {
            for (int i = 0; i < mReadingItems.size() && i < values.size(); ++i) {
                mReadingItems.get(i).value = values.get(i);
            }
            mListViewAdapter.notifyDataSetChanged();
        }
    }
}
