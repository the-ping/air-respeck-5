package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.utils.Constants;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Darius on 10.02.2017.
 */

public class DaphneValuesFragment extends BaseFragment {

    TextView breathingRateText;
    TextView averageBreathingRateText;
    ImageView activityIcon;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_daphne_values, container, false);

        breathingRateText = (TextView) view.findViewById(R.id.text_breathing);
        averageBreathingRateText = (TextView) view.findViewById(R.id.text_breathing_average);
        activityIcon = (ImageView) view.findViewById(R.id.activity_icon);

        return view;
    }

    public void updateBreathing(HashMap<String, Float> mRespeckSensorReadings) {
        Log.i("DF", mRespeckSensorReadings.toString());

        // Set breathing rate text to currently calculated rates
        breathingRateText.setText(
                String.format(Locale.UK, "%.2f BrPM", mRespeckSensorReadings.get(Constants.RESPECK_BREATHING_RATE)));
        averageBreathingRateText.setText(
                String.format(Locale.UK, "%.2f BrPM", mRespeckSensorReadings.get(Constants.RESPECK_AVERAGE_BREATHING_RATE)));

        // Set activity icon to reflect currently predicted activity
        int activityType = Math.round(mRespeckSensorReadings.get(Constants.RESPECK_ACTIVITY_TYPE));
        switch(activityType) {
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
}