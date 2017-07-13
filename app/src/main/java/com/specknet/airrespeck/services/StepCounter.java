package com.specknet.airrespeck.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.specknet.airrespeck.fragments.SupervisedStepCounterFragment;
import com.specknet.airrespeck.utils.Utils;

import java.nio.Buffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Created by Darius on 12.07.2017.
 */

public class StepCounter {
    private static final float STEP_THRESHOLD = 1.02f;
    private static final int STEP_MIN_DELAY_MS = 40;

    private long mlastTimestamp = 0;
    private float mOldVectorLength = 0;

    private final int NUM_STEPS_UNTIL_COUNT_WALKING = 4;
    private final int MAX_ALLOWED_DEVIATION_FROM_MEAN_DISTANCE = 6;
    private final int NUM_SAMPLES_UNTIL_STATIC = 20;
    private final int FILTER_LENGTH = 3;
    private int[] mStepDistances = new int[NUM_STEPS_UNTIL_COUNT_WALKING - 1];
    private int mSamplesSinceLastStep = 0;
    private int mNumValidSteps = 0;
    private int mNumSamplesUntilStatic = 0;

    private int mMinuteStepCount = 0;

    private Deque<Float> mVectorLengthFilter = new ArrayDeque<>();

    private enum State {
        STATIC, MOVING, WALKING
    }

    private State mState = State.STATIC;

    public void resetMinuteCount() {
        mMinuteStepCount = 0;
    }

    public int getMinuteCount() {
        return mMinuteStepCount;
    }

    public void updateAccel(long currentTimestamp, float x, float y, float z) {

        float unfilteredVectorLength = Utils.norm(new float[]{x, y, z});
        mVectorLengthFilter.addLast(unfilteredVectorLength);

        if (mVectorLengthFilter.size() > FILTER_LENGTH) {
            mVectorLengthFilter.removeFirst();
            float vectorLength = Utils.mean(mVectorLengthFilter.toArray(new Float[FILTER_LENGTH]));

            mSamplesSinceLastStep++;

            if (vectorLength > STEP_THRESHOLD && mOldVectorLength <= STEP_THRESHOLD
                    && (currentTimestamp - mlastTimestamp > STEP_MIN_DELAY_MS)) {

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
                    mStepDistances[mNumValidSteps - 2] = mSamplesSinceLastStep;

                    if (mNumValidSteps == NUM_STEPS_UNTIL_COUNT_WALKING) {
                        // We have the number of steps required for walking. Only register walking if the steps were
                        // in regular time intervals
                        float meanDistance = Utils.mean(mStepDistances);
                        boolean validWalking = true;
                        for (int distance : mStepDistances) {
                            if (Math.abs(meanDistance - distance) > MAX_ALLOWED_DEVIATION_FROM_MEAN_DISTANCE) {
                                validWalking = false;
                            }
                            Log.i("Step", "Distance: " + distance);
                            Log.i("Step", "Diff from mean distance: " + Math.abs(meanDistance - distance));
                        }
                        if (validWalking) {
                            mState = State.WALKING;
                            mMinuteStepCount += mNumValidSteps;
                        } else {
                            // Reset to static state
                            mState = State.STATIC;
                            Log.i("Step", "Irregular steps. Fall back to static and don't count them");
                        }
                    }
                } else {
                    // State is walking
                    mMinuteStepCount += 1;
                }

                mlastTimestamp = currentTimestamp;
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
            mOldVectorLength = vectorLength;
        }
    }
}
