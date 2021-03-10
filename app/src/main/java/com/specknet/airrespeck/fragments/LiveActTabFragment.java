package com.specknet.airrespeck.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.ConnectionStateObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.activities.SubjectActivity;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.util.Locale;

import javax.security.auth.Subject;

///**
// * A simple {@link Fragment} subclass.
// * Use the {@link LiveActTabFragment#newInstance} factory method to
// * create an instance of this fragment.
// */
public class LiveActTabFragment extends ConnectionOverlayFragment implements RESpeckDataObserver {

    //
    TextView sbj_averageBreathingRateText;
    ImageView sbj_activityIcon;

    public LiveActTabFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_live_act_tab, container, false);

        // Load breathing textviews and icon
        sbj_averageBreathingRateText = (TextView) view.findViewById(R.id.sbj_text_breathing_average);
        sbj_activityIcon = (ImageView) view.findViewById(R.id.sbj_activity_icon);

        // Register this fragment as connection state observer
        ((MainActivity) getActivity()).registerConnectionStateObserver((ConnectionStateObserver) this);

        return view;

    }

    public void updateReadings(RESpeckLiveData data) {

        // Only update view if fragment has been created!
        // Set breathing rate text to currently calculated rates
        String suffix = ((MainActivity) getActivity()).getBreathingSuffix();
//        if (!Float.isNaN(data.getBreathingRate())) {
//            sbj_breathingRateText.setText(String.format(Locale.UK, "%.2f " + suffix, data.getBreathingRate()));
//        }
        sbj_averageBreathingRateText.setText(String.format(Locale.UK, "%.2f " + suffix, data.getAvgBreathingRate()));

        // Set activity icon to reflect currently predicted activity
        switch (data.getActivityType()) {
            case Constants.ACTIVITY_LYING:
                sbj_activityIcon.setImageResource(R.drawable.vec_lying);
                break;
            case Constants.ACTIVITY_LYING_DOWN_LEFT:
                sbj_activityIcon.setImageResource(R.drawable.lying_to_left);
                break;
            case Constants.ACTIVITY_LYING_DOWN_RIGHT:
                sbj_activityIcon.setImageResource(R.drawable.lying_to_right);
                break;
            case Constants.ACTIVITY_LYING_DOWN_STOMACH:
                sbj_activityIcon.setImageResource(R.drawable.lying_stomach);
                break;
            case Constants.ACTIVITY_SITTING_BENT_BACKWARD:
                sbj_activityIcon.setImageResource(R.drawable.sitting_backward);
                break;
            case Constants.ACTIVITY_SITTING_BENT_FORWARD:
                sbj_activityIcon.setImageResource(R.drawable.sitting_forward);
                break;
            case Constants.ACTIVITY_STAND_SIT:
                sbj_activityIcon.setImageResource(R.drawable.vec_standing_sitting);
                break;
            case Constants.ACTIVITY_MOVEMENT:
                sbj_activityIcon.setImageResource(R.drawable.movement);
                break;
            case Constants.ACTIVITY_WALKING:
                sbj_activityIcon.setImageResource(R.drawable.vec_walking);
                break;
            case Constants.SS_COUGHING:
                sbj_activityIcon.setImageResource(R.drawable.ic_cough);
                break;
            default:
                sbj_activityIcon.setImageResource(R.drawable.vec_xmark);
        }
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        Log.i("RESpeckReadings", "updateRESpeckData");
        Log.i("RESpeckReadings", data.toString());
        // Update the graph
//        mBreathingGraphView.addToBreathingGraphQueue(data);

        // Update the other readings
        updateReadings(data);
    }

    @Override
    public void onDestroy() {
        // Unregister this class as observer. If we haven't observed, nothing happens
        ((MainActivity) getActivity()).unregisterRESpeckDataObserver(this);
        super.onDestroy();
    }
}

