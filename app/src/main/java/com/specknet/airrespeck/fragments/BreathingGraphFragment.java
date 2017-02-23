package com.specknet.airrespeck.fragments;

import android.content.res.Resources;
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
import com.github.mikephil.charting.utils.Utils;
import com.mkobos.pca_transform.PCA;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.models.BreathingGraphData;
import com.specknet.airrespeck.models.XAxisValueFormatter;
import com.specknet.airrespeck.utils.Constants;

import java.util.ArrayList;
import java.util.LinkedList;

import Jama.Matrix;

/**
 * Created by Darius on 21.02.2017.
 */

public class BreathingGraphFragment extends BaseFragment {

    private LineChart mBreathingFlowChart;
    private LineChart mBreathingPCAChart;

    private LinkedList<BreathingGraphData> breathingDataQueue = new LinkedList<>();
    private LinkedList<Float> pcaValueQueue = new LinkedList<>();
    private LinkedList<Float> meanPcaValueQueue = new LinkedList<>();
    private float lastCorrectedPcaValue = 0f;

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

    private void updatePCAGraph(BreathingGraphData data) {
        if (breathingDataQueue.size() > Constants.NUMBER_OF_SAMPLES_REQUIRED_FOR_PCA) {
            throw new RuntimeException("Breathing data queue exceeds limit!");

        } else if (breathingDataQueue.size() == Constants.NUMBER_OF_SAMPLES_REQUIRED_FOR_PCA) {
            // PCA transform on queued data
            // Generate matrix from queue
            double[][] matrixArray = new double[Constants.NUMBER_OF_SAMPLES_REQUIRED_FOR_PCA][3];
            for (int r = 0; r < matrixArray.length; r++) {
                matrixArray[r][0] = breathingDataQueue.get(r).getAccelX();
                matrixArray[r][1] = breathingDataQueue.get(r).getAccelY();
                matrixArray[r][2] = breathingDataQueue.get(r).getAccelZ();
            }
            Matrix trainingMatrix = new Matrix(matrixArray);

            // We want to transform the new data
            Matrix testMatrix = new Matrix(new double[][]{{data.getAccelX(), data.getAccelY(), data.getAccelZ()}});
            PCA pca = new PCA(trainingMatrix);
            Matrix transformedData = pca.transform(testMatrix, PCA.TransformationType.ROTATION);

            // The first value is the one of the dimension with the most information. We want that one!
            float pcaValue = (float) transformedData.get(0, 0);

            // Add the pca value to the queue
            pcaValueQueue.add(pcaValue);
            while (pcaValueQueue.size() > Constants.NUMBER_OF_SAMPLES_FOR_MEAN_SUBTRACTION) {
                pcaValueQueue.removeFirst();
            }

            // Add the current sample to the breathingDataQueue and remove the oldest sample
            breathingDataQueue.add(data);
            breathingDataQueue.removeFirst();

            // If the pcaValueQueue is long enough, we subtract the mean from the
            // value in the middle of the array
            if (pcaValueQueue.size() == Constants.NUMBER_OF_SAMPLES_FOR_MEAN_SUBTRACTION) {
                // Calculate mean PCA of all PCA values in queue
                float meanPca = com.specknet.airrespeck.utils.Utils.mean(
                        pcaValueQueue.toArray(new Float[pcaValueQueue.size()]));

                // Subtract the mean from the center value in the queue
                float correctedPca = pcaValueQueue.get(Constants.NUMBER_OF_SAMPLES_FOR_MEAN_SUBTRACTION / 2) - meanPca;

                // Smooth the data
                // ALPHA is the smoothing factor. The higher, the more the data is smoothed.
                final float ALPHA = 0.9f;
                float filteredPca = correctedPca + ALPHA * (lastCorrectedPcaValue - correctedPca);
                lastCorrectedPcaValue = filteredPca;

                Entry newEntry = new Entry(data.getTimestamp(), filteredPca);
                updateGraph(newEntry, mBreathingPCAChart);
            }
        } else {
            breathingDataQueue.add(data);
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
