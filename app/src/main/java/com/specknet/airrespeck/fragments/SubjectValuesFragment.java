package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.AirspeckDataObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.Constants;

import java.util.Locale;

/**
 * Created by Darius on 10.02.2017.
 */

public class SubjectValuesFragment extends Fragment implements RESpeckDataObserver, AirspeckDataObserver {

    TextView breathingRateText;
    TextView averageBreathingRateText;

    TextView pm10Text;
    TextView pm2_5Text;

    ImageView activityIcon;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);
        ((MainActivity) getActivity()).registerAirspeckDataObserver(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_subject_values, container, false);

        breathingRateText = (TextView) view.findViewById(R.id.text_breathing);
        averageBreathingRateText = (TextView) view.findViewById(R.id.text_breathing_average);
        activityIcon = (ImageView) view.findViewById(R.id.activity_icon);
        pm10Text = (TextView) view.findViewById(R.id.text_pm10);
        pm2_5Text = (TextView) view.findViewById(R.id.text_pm2_5);

        return view;
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        // Only update view if fragment has been created!
        // Set breathing rate text to currently calculated rates
        if (!Float.isNaN(data.getBreathingRate())) {
            breathingRateText.setText(String.format(Locale.UK, "%.2f BrPM", data.getBreathingRate()));
        }
        averageBreathingRateText.setText(String.format(Locale.UK, "%.2f BrPM", data.getAvgBreathingRate()));

        // Set activity icon to reflect currently predicted activity
        switch (data.getActivityType()) {
            case Constants.ACTIVITY_LYING:
                activityIcon.setImageResource(R.drawable.vec_lying);
                break;
            case Constants.ACTIVITY_WALKING:
                activityIcon.setImageResource(R.drawable.vec_walking);
                break;
            case Constants.ACTIVITY_STAND_SIT:
                activityIcon.setImageResource(R.drawable.vec_standing_sitting);
                break;
            case Constants.WRONG_ORIENTATION:
            default:
                activityIcon.setImageResource(R.drawable.vec_xmark);
        }
    }

    @Override
    public void updateAirspeckData(AirspeckData data) {
        pm2_5Text.setText(String.format(Locale.UK, "PM 2.5: %.2f μg/m³", data.getPm2_5()));
        pm10Text.setText(String.format(Locale.UK, "PM 10: %.2f μg/m³", data.getPm10()));
    }
}
