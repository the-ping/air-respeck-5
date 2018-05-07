package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.AirspeckDataObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.utils.IndoorOutdoorPredictor;

import java.util.Locale;

public class ActivityPredictionFragment extends BaseFragment implements AirspeckDataObserver {

    private IndoorOutdoorPredictor indoorOutdoorPredictor;
    private TextView indoorLikelihoodText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register observer for getting Airspeck data
        ((MainActivity) getActivity()).registerAirspeckDataObserver(this);

        indoorOutdoorPredictor = new IndoorOutdoorPredictor();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_activity_prediction, container, false);

        indoorLikelihoodText = (TextView) view.findViewById(R.id.indoor_likelihood);

        return view;
    }


    @Override
    public void updateAirspeckData(AirspeckData data) {
        float indoorLikelihood = indoorOutdoorPredictor.getIndoorLikelihood(data);
        indoorLikelihoodText.setText(String.format(Locale.UK, "Indoor Likelihood: %.2f", indoorLikelihood));
    }
}
