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

    private TextView mStepcountText;
    private BreathingGraphView mVelocityGraphView;

    private void updateStep(final int number) {
        // Update text
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mStepcountText.setText(Integer.toString(number));
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
        updateStep(data.getMinuteStepCount());
        float vectorLength = Utils.norm(new float[]{data.getAccelX(), data.getAccelY(), data.getAccelZ()});
        mVelocityGraphView.addToBreathingGraphQueue(
                new RESpeckLiveData(data.getPhoneTimestamp(), 0, 0, 0, 0,
                        0, vectorLength, 0, 0, 0, 0, 0));
    }

    @Override
    public void onDetach() {
        mVelocityGraphView.stopBreathingGraphUpdates();
        super.onDetach();
    }
}
