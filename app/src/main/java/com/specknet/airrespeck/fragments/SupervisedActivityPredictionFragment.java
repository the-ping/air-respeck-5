package com.specknet.airrespeck.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.AirspeckDataObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.LocationData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.IndoorOutdoorPredictor;

import java.util.Locale;

public class SupervisedActivityPredictionFragment extends BaseFragment {

    private IndoorOutdoorPredictor indoorOutdoorPredictor;
    private TextView indoorLikelihoodText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_activity_prediction, container, false);

        indoorLikelihoodText = (TextView) view.findViewById(R.id.indoor_likelihood);

        BroadcastReceiver predictionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                indoorLikelihoodText.setText(intent.getStringExtra(Constants.INDOOR_PREDICTION_STRING));
            }
        };
        getActivity().registerReceiver(predictionReceiver,
                new IntentFilter(Constants.ACTION_INDOOR_PREDICTION_BROADCAST));

        return view;
    }
}