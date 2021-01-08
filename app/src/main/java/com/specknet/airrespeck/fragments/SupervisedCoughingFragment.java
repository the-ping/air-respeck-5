package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.CoughingDetector;

import java.util.Locale;

/**
 * Created by Darius on 12.07.2017.
 */

public class SupervisedCoughingFragment extends ConnectionOverlayFragment implements RESpeckDataObserver {

    private CoughingDetector coughingDetector;
    private ImageView coughingIcon;
    private ImageView nonCoughingIcon;
    private boolean isCurrentlyWalking = false;
    private final Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);

        coughingDetector = new CoughingDetector();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_coughing, container, false);

        coughingIcon = (ImageView) view.findViewById(R.id.coughing_icon);
        nonCoughingIcon = (ImageView) view.findViewById(R.id.non_coughing_icon);

        startCoughingPredictionTask();
        return view;
    }

    private void startCoughingPredictionTask() {

        final int delay = 500; // milliseconds

        handler.postDelayed(new Runnable() {
            public void run() {
                double prediction = coughingDetector.predictIsCouhging();
//                Toast.makeText(getActivity(), String.format(Locale.UK, "%.8f", prediction),
//                        Toast.LENGTH_SHORT).show();
                Log.i("DF", String.format(Locale.UK, "%.8f", prediction));
                updateCoughingSymbol(prediction >= 0.5 && !isCurrentlyWalking);
                handler.postDelayed(this, delay);
            }
        }, 0);
    }

    private void stopCoughingPredictionTask() {
        handler.removeCallbacksAndMessages(null);
    }

    private void updateCoughingSymbol(boolean show) {
        if (show) {
            nonCoughingIcon.setVisibility(View.GONE);
            coughingIcon.setVisibility(View.VISIBLE);
        } else {
            coughingIcon.setVisibility(View.GONE);
            nonCoughingIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        isCurrentlyWalking = data.getActivityType() == 1;
        coughingDetector.updateCoughing(data.getAccelX(), data.getAccelY(), data.getAccelZ());
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        // Unregister this class as observer. If we haven't observed, nothing happens
        ((MainActivity) getActivity()).unregisterRESpeckDataObserver(this);
        stopCoughingPredictionTask();
        super.onDestroy();
    }
}
