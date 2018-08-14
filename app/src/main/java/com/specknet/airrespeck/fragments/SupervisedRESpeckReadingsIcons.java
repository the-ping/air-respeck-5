package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.ConnectionStateObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.views.BreathingGraphView;

import java.util.Locale;

/**
 * Fragment created for the Windmill project which contains the breathing rate, minute average breathing rate,
 * a graph of the breathing signal, an icon indicating the current activity type, and buttons to
 * start the rehab app and diary app.
 */

public class SupervisedRESpeckReadingsIcons extends ConnectionOverlayFragment implements RESpeckDataObserver {

    TextView breathingRateText;
    TextView averageBreathingRateText;
    TextView stepCountText;

    ImageView activityIcon;

    private BreathingGraphView mBreathingGraphView;


    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SupervisedRESpeckReadingsIcons() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_respeck_readings_icons, container, false);

        // Load breathing textviews and icon
        breathingRateText = (TextView) view.findViewById(R.id.text_breathing);
        averageBreathingRateText = (TextView) view.findViewById(R.id.text_breathing_average);
        activityIcon = (ImageView) view.findViewById(R.id.activity_icon);
        stepCountText = (TextView) view.findViewById(R.id.text_step_count);

        // Create new graph
        mBreathingGraphView = new BreathingGraphView(getActivity());

        // Inflate breathing graph into container
        FrameLayout graphFrame = (FrameLayout) view.findViewById(R.id.breathing_graph_container);
        graphFrame.addView(mBreathingGraphView);

        mBreathingGraphView.startBreathingGraphUpdates();

        // Register this fragment as connection state observer
        ((MainActivity) getActivity()).registerConnectionStateObserver(this);

        return view;
    }

    private void updateReadings(RESpeckLiveData data) {
        // Update readings and activity symbol
        // Set breathing rate text to currently calculated rates
        if (!Float.isNaN(data.getBreathingRate())) {
            breathingRateText.setText(String.format(Locale.UK, "%.2f BrPM", data.getBreathingRate()));
        }
        averageBreathingRateText.setText(String.format(Locale.UK, "%.2f BrPM", data.getAvgBreathingRate()));
        stepCountText.setText(Integer.toString(data.getMinuteStepCount()));

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
        // Update the graph
        mBreathingGraphView.addToBreathingGraphQueue(data);

        // Update the other readings
        updateReadings(data);
    }

    @Override
    public void onDetach() {
        mBreathingGraphView.stopBreathingGraphUpdates();
        super.onDetach();
    }
    @Override
    public void onDestroy() {
        // Unregister this class as observer. If we haven't observed, nothing happens
        ((MainActivity) getActivity()).unregisterRESpeckDataObserver(this);
        super.onDestroy();
    }

}

