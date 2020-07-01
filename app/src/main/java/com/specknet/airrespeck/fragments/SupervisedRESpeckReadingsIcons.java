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
import com.specknet.airrespeck.utils.Utils;
import com.specknet.airrespeck.views.BreathingGraphView;

import java.util.Locale;
import java.util.Map;

/**
 * Fragment created for the Windmill project which contains the breathing rate, minute average breathing rate,
 * a graph of the breathing signal, an icon indicating the current activity type, and buttons to
 * start the rehab app and diary app.
 */

public class SupervisedRESpeckReadingsIcons extends ConnectionOverlayFragment implements RESpeckDataObserver {

    TextView breathingRateText;
    TextView averageBreathingRateText;
    TextView stepCountText;
    TextView frequencyText;
    TextView battLevelText;
    TextView chargingText;

    ImageView activityIcon;

    private BreathingGraphView mBreathingGraphView;

    private Utils mUtils;
    private Map<String, String> mLoadedConfig;


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
        frequencyText = (TextView) view.findViewById(R.id.text_frequency);
        battLevelText = (TextView) view.findViewById(R.id.text_batt_level);
        chargingText = (TextView) view.findViewById(R.id.text_charging_status);

        // Create new graph
        mBreathingGraphView = new BreathingGraphView(getActivity());

        // Inflate breathing graph into container
        FrameLayout graphFrame = (FrameLayout) view.findViewById(R.id.breathing_graph_container);
        graphFrame.addView(mBreathingGraphView);

        mBreathingGraphView.startBreathingGraphUpdates();

        // Register this fragment as connection state observer
        ((MainActivity) getActivity()).registerConnectionStateObserver(this);

        // hide activity type icon if specified in pairing options
        mUtils = Utils.getInstance();
        mLoadedConfig = mUtils.getConfig(getActivity());
        if (mLoadedConfig.containsKey(Constants.Config.HIDE_ACTIVITY_TYPE)) {
            if (Boolean.parseBoolean(mLoadedConfig.get(Constants.Config.HIDE_ACTIVITY_TYPE))) {
                activityIcon.setVisibility(View.GONE);
            }
        }

        return view;
    }

    private void updateReadings(RESpeckLiveData data) {
        // Update readings and activity symbol
        // Set breathing rate text to currently calculated rates

        String suffix = ((MainActivity) getActivity()).getBreathingSuffix();

        if (!Float.isNaN(data.getBreathingRate())) {
            breathingRateText.setText(String.format(Locale.UK, "%.2f " + suffix, data.getBreathingRate()));
        }
        averageBreathingRateText.setText(String.format(Locale.UK, "%.2f " + suffix, data.getAvgBreathingRate()));
        stepCountText.setText(Integer.toString(data.getMinuteStepCount()));

        // Update the frequency once a minute
        // only if it's different than 0
        if(data.getFrequency() != 0) {
            frequencyText.setText(String.format(Locale.UK, "%.2f" + " Hz", data.getFrequency()));
//            frequencyText.setText(Float.toString(data.getFrequency()) + " Hz");
        }

        // update battery level and charging status
        if (data.getBattLevelval() != -1) {
            battLevelText.setVisibility(View.VISIBLE);
            battLevelText.setText(data.getBattLevelval() + "%");
        }
        else {
            battLevelText.setVisibility(View.INVISIBLE);
        }

        if (data.getChargingStatus()) {
            chargingText.setVisibility(View.VISIBLE);
        }
        else {
            chargingText.setVisibility(View.INVISIBLE);
        }

        // Set activity icon to reflect currently predicted activity
        switch (data.getActivityType()) {
            case Constants.ACTIVITY_LYING:
                activityIcon.setImageResource(R.drawable.vec_lying);
                break;
            case Constants.ACTIVITY_LYING_DOWN_LEFT:
                activityIcon.setImageResource(R.drawable.lying_to_left);
                break;
            case Constants.ACTIVITY_LYING_DOWN_RIGHT:
                activityIcon.setImageResource(R.drawable.lying_to_right);
                break;
            case Constants.ACTIVITY_LYING_DOWN_STOMACH:
                activityIcon.setImageResource(R.drawable.lying_stomach);
                break;
            case Constants.ACTIVITY_SITTING_BENT_BACKWARD:
                activityIcon.setImageResource(R.drawable.sitting_backward);
                break;
            case Constants.ACTIVITY_SITTING_BENT_FORWARD:
                activityIcon.setImageResource(R.drawable.sitting_forward);
                break;
            case Constants.ACTIVITY_STAND_SIT:
                activityIcon.setImageResource(R.drawable.vec_standing_sitting);
                break;
            case Constants.ACTIVITY_MOVEMENT:
                activityIcon.setImageResource(R.drawable.movement);
                break;
            case Constants.ACTIVITY_WALKING:
                activityIcon.setImageResource(R.drawable.vec_walking);
                break;
            default:
                activityIcon.setImageResource(R.drawable.vec_xmark);
        }
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        Log.i("RESpeckReadings", "updateRESpeckData");
        Log.i("RESpeckReadings", data.toString());
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


