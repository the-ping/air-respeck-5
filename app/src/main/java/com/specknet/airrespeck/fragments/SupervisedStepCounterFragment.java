package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.Utils;
import com.specknet.airrespeck.views.BreathingGraphView;

/**
 * Created by Darius on 12.07.2017.
 */

public class SupervisedStepCounterFragment extends BaseFragment implements RESpeckDataObserver {

    private static final int ACCEL_RING_SIZE = 50;
    private static final int VEL_RING_SIZE = 10;
    private static final float STEP_THRESHOLD = 0.006f;
    private static final int STEP_DELAY_MS = 250;

    private int accelRingCounter = 0;
    private float[] accelRingX = new float[ACCEL_RING_SIZE];
    private float[] accelRingY = new float[ACCEL_RING_SIZE];
    private float[] accelRingZ = new float[ACCEL_RING_SIZE];
    private int velRingCounter = 0;
    private float[] velRing = new float[VEL_RING_SIZE];
    private long lastStepTimeNs = 0;
    private float oldVelocityEstimate = 0;

    private TextView mStepcountText;
    private BreathingGraphView mVelocityGraphView;

    private void updateStep() {
        // Update text
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mStepcountText.setText(Integer.toString(Integer.parseInt(mStepcountText.getText().toString()) + 1));
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stepcount, container, false);
        mStepcountText = (TextView) view.findViewById(R.id.stepcount_number);

        // Create new graph
        mVelocityGraphView = new BreathingGraphView(getActivity(), false);

        // Inflate breathing graph into container
        FrameLayout graphFrame = (FrameLayout) view.findViewById(R.id.breathing_graph_container);
        graphFrame.addView(mVelocityGraphView);

        mVelocityGraphView.startBreathingGraphUpdates();

        return view;
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        updateAccel(data.getPhoneTimestamp(), data.getAccelX(), data.getAccelY(), data.getAccelZ());
    }

    public void updateAccel(long timeNs, float x, float y, float z) {
        float[] currentAccel = new float[3];
        currentAccel[0] = x;
        currentAccel[1] = y;
        currentAccel[2] = z;

        // First step is to update our guess of where the global z vector is.
        accelRingCounter++;
        accelRingX[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[0];
        accelRingY[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[1];
        accelRingZ[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[2];

        float[] worldZ = new float[3];
        worldZ[0] = Utils.sum(accelRingX) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
        worldZ[1] = Utils.sum(accelRingY) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
        worldZ[2] = Utils.sum(accelRingZ) / Math.min(accelRingCounter, ACCEL_RING_SIZE);

        float normalization_factor = Utils.norm(worldZ);

        worldZ[0] = worldZ[0] / normalization_factor;
        worldZ[1] = worldZ[1] / normalization_factor;
        worldZ[2] = worldZ[2] / normalization_factor;

        // Next step is to figure out the component of the current acceleration
        // in the direction of world_z and subtract gravity's contribution
        float currentZ = Utils.dot(worldZ, currentAccel) - normalization_factor;
        velRingCounter++;
        velRing[velRingCounter % VEL_RING_SIZE] = currentZ;

        float velocityEstimate = Utils.sum(velRing);

        Log.i("Step", "velocitiy: " + velocityEstimate);

        // Update the graph
        mVelocityGraphView.addToBreathingGraphQueue(
                new RESpeckLiveData(timeNs, 0, 0, 0, 0, 0, velocityEstimate, 0, 0, 0, 0));

        if (velocityEstimate > STEP_THRESHOLD && oldVelocityEstimate <= STEP_THRESHOLD
                && (timeNs - lastStepTimeNs > STEP_DELAY_MS)) {
            updateStep();
            lastStepTimeNs = timeNs;
        }
        oldVelocityEstimate = velocityEstimate;
    }

    @Override
    public void onDetach() {
        mVelocityGraphView.stopBreathingGraphUpdates();
        super.onDetach();
    }
}
