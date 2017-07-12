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

import java.util.LinkedList;

/**
 * Created by Darius on 12.07.2017.
 */

public class SupervisedStepCounterFragment extends BaseFragment implements RESpeckDataObserver {

    private static final float STEP_THRESHOLD = 1.02f;
    private static final int STEP_MIN_DELAY_MS = 250;

    private long lastTimestamp = 0;
    private float oldVectorLength = 0;

    private TextView mStepcountText;
    private BreathingGraphView mVelocityGraphView;

    private final int NUM_STEPS_UNTIL_COUNT_WALKING = 4;
    private final int MAX_ALLOWED_DEVIATION_FROM_MEAN_DISTANCE = 6;
    private final int NUM_SAMPLES_UNTIL_STATIC = 20;
    private final int FILTER_LENGTH = 3;
    private int[] stepDistances = new int[NUM_STEPS_UNTIL_COUNT_WALKING - 1];
    private int mSamplesSinceLastStep = 0;
    private int mNumValidSteps = 0;
    private int mNumSamplesUntilStatic = 0;

    private LinkedList<Float> mVectorLengthFilter = new LinkedList<>();

    private enum State {
        STATIC, MOVING, WALKING
    }

    private State mState = State.STATIC;

    private void updateStep(final int number) {
        // Update text
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mStepcountText.setText(
                        Integer.toString(Integer.parseInt(mStepcountText.getText().toString()) + number));
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

    public void updateAccel(long currentTimestamp, float x, float y, float z) {

        float unfilteredVectorLength = Utils.norm(new float[]{x, y, z});
        mVectorLengthFilter.addLast(unfilteredVectorLength);

        if (mVectorLengthFilter.size() > FILTER_LENGTH) {
            mVectorLengthFilter.removeFirst();
            float vectorLength = Utils.mean(mVectorLengthFilter.toArray(new Float[FILTER_LENGTH]));

            mVelocityGraphView.addToBreathingGraphQueue(
                    new RESpeckLiveData(currentTimestamp, 0, 0, 0, 0, 0, vectorLength, 0, 0, 0, 0));

            mSamplesSinceLastStep++;

            if (vectorLength > STEP_THRESHOLD && oldVectorLength <= STEP_THRESHOLD
                    && (currentTimestamp - lastTimestamp > STEP_MIN_DELAY_MS)) {

                mNumSamplesUntilStatic = NUM_SAMPLES_UNTIL_STATIC;

                // We have a potentially valid step. If we were static before, start moving and keep track of valid steps
                if (mState == State.STATIC) {
                    mNumValidSteps = 1;
                    mState = State.MOVING;
                } else if (mState == State.MOVING) {
                    // If we were in moving state, take note of this valid step
                    mNumValidSteps++;
                    Log.i("Step", "Count step in movement state");

                    // Keep track of step time differences after second step
                    stepDistances[mNumValidSteps - 2] = mSamplesSinceLastStep;

                    if (mNumValidSteps == NUM_STEPS_UNTIL_COUNT_WALKING) {
                        // We have the number of steps required for walking. Only register walking if the steps were
                        // in regular time intervals
                        float meanDistance = Utils.mean(stepDistances);
                        boolean validWalking = true;
                        for (int distance : stepDistances) {
                            if (Math.abs(meanDistance - distance) > MAX_ALLOWED_DEVIATION_FROM_MEAN_DISTANCE) {
                                validWalking = false;
                            }
                            Log.i("Step", "Distance: " + distance);
                            Log.i("Step", "Diff from mean distance: " + Math.abs(meanDistance - distance));
                        }
                        if (validWalking) {
                            mState = State.WALKING;
                            updateStep(mNumValidSteps);
                        } else {
                            // Reset to static state
                            mState = State.STATIC;
                            Log.i("Step", "Irregular steps. Fall back to static and don't count them");
                        }
                    }
                } else {
                    // State is walking
                    updateStep(1);
                }

                lastTimestamp = currentTimestamp;
                mSamplesSinceLastStep = 0;
            } else {
                if (mState != State.STATIC) {
                    // We are not above the threshold. If this happens often, fall back to static state
                    mNumSamplesUntilStatic--;
                    if (mNumSamplesUntilStatic == 0) {
                        Log.i("Step", "Didn't move for 20 samples. Fall back to static state");
                        mState = State.STATIC;
                    }
                }
            }
            oldVectorLength = vectorLength;
        }
    }

    @Override
    public void onDetach() {
        mVelocityGraphView.stopBreathingGraphUpdates();
        super.onDetach();
    }
}
