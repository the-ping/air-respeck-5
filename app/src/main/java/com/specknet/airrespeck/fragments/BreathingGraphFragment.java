package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
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
import com.mkobos.pca_transform.PCA;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.models.BreathingGraphData;
import com.specknet.airrespeck.models.XAxisValueFormatter;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import Jama.Matrix;

/**
 * Created by Darius on 21.02.2017.
 * Fragment to display the respiratory signal
 */

public class BreathingGraphFragment extends BaseFragment {

    private LineChart mBreathingFlowChart;
    private LineChart mBreathingPCAChart;


    private LinkedList<BreathingGraphData> mPreFilteringQueue = new LinkedList<>();
    private LinkedList<BreathingGraphData> mBreathingDataQueue = new LinkedList<>();
    private LinkedList<Float> mPcaValueQueue = new LinkedList<>();
    private LinkedList<Float> mMeanPcaValueQueue = new LinkedList<>();
    private float mLastCorrectedPcaValue = 0f;

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
        com.github.mikephil.charting.utils.Utils.init(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_breathing_graph, container, false);

        mConnectingLayout = (LinearLayout) view.findViewById(R.id.connecting_layout);

        mBreathingFlowChart = (LineChart) view.findViewById(R.id.breathing_flow_line_chart);
        mBreathingPCAChart = (LineChart) view.findViewById(R.id.breathing_pca_line_chart);

        setupBreathingSignalChart(mBreathingFlowChart);
        setupBreathingSignalChart(mBreathingPCAChart);

        setupLineDataSetForChart(mBreathingFlowChart);
        setupLineDataSetForChart(mBreathingPCAChart);

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
        if (mBreathingFlowChart != null && mBreathingPCAChart != null) {
            updateFlowGraph(data);
            updatePCAGraph(data);
        }
    }

    private void updateFlowGraph(BreathingGraphData data) {
        Entry newEntry = new Entry(data.getTimestamp(), data.getBreathingSignal());
        updateGraph(newEntry, mBreathingFlowChart);
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

                /*
                // Smooth the data: momentum method
                // ALPHA is the smoothing factor. The higher, the more the data is smoothed.
                final float ALPHA = 0.9f;
                float filteredPca = correctedPca + ALPHA * (mLastCorrectedPcaValue - correctedPca);
                mLastCorrectedPcaValue = filteredPca;

                Entry newEntry = new Entry(data.getTimestamp(), filteredPca);
                updateGraph(newEntry, mBreathingPCAChart);
                */

                mMeanPcaValueQueue.add(correctedPca);
                limitQueueToSize(mMeanPcaValueQueue, Constants.NUMBER_OF_SAMPLES_FOR_MEAN_POST_FILTER);

                if (mMeanPcaValueQueue.size() == Constants.NUMBER_OF_SAMPLES_FOR_MEAN_POST_FILTER) {
                    Entry newEntry = new Entry(newData.getTimestamp(), Utils.mean(
                                    mMeanPcaValueQueue.toArray(new Float[mMeanPcaValueQueue.size()])));
                    updateGraph(newEntry, mBreathingPCAChart);
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

    private void updateGraph(Entry newEntry, LineChart chart) {
        LineDataSet dataSet = (LineDataSet) chart.getData().getDataSetByIndex(0);
        dataSet.addEntry(newEntry);

        // Remove any values older than the number of breathing signal points we want to display
        while (dataSet.getValues().size() > Constants.NUMBER_BREATHING_SIGNAL_SAMPLES_ON_CHART) {
            dataSet.removeFirst();
        }

        // Update UI
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    public static void main(String[] args) {
        double[][] matrixArray = new double[][]{{1, 2, 3}, {2, 3, 4}, {3, 2, 2}, {5, 4, 3}};
        Matrix trainingMatrix = new Matrix(matrixArray);
        // We want to transform the new data
        Matrix testMatrix = new Matrix(new double[][]{{3, 2, 1}});
        PCA pca = new PCA(trainingMatrix);
        Matrix transformedData = pca.transform(testMatrix, PCA.TransformationType.ROTATION);
        for (int pos = 0; pos < 3; pos++) {
            System.out.println(pca.getEigenvalue(pos));
        }
        for (double data : transformedData.getArray()[0]) {
            System.out.println("transformed: " + data);
        }
    }
}
