package com.specknet.airrespeck.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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

public class SubjectWindmillFragment extends Fragment implements RESpeckDataObserver, ConnectionStateObserver {

    TextView breathingRateText;
    TextView averageBreathingRateText;

    ImageView activityIcon;
    private ImageView connectedStatusRESpeck;


    private BreathingGraphView mBreathingGraphView;


    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SubjectWindmillFragment() {

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
        View view = inflater.inflate(R.layout.fragment_windmill, container, false);

        // Load connection symbols
        connectedStatusRESpeck = (ImageView) view.findViewById(R.id.connected_status_respeck);

        // Load breathing textviews and icon
        breathingRateText = (TextView) view.findViewById(R.id.text_breathing);
        averageBreathingRateText = (TextView) view.findViewById(R.id.text_breathing_average);
        activityIcon = (ImageView) view.findViewById(R.id.activity_icon);

        // Setup onClick handler for buttons. We can't define them in the xml as that would search in MainActivity
        ImageButton diaryButton = (ImageButton) view.findViewById(R.id.image_button_diary);
        ImageButton rehabButton = (ImageButton) view.findViewById(R.id.image_button_exercise);

        diaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDiary();
            }
        });

        rehabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchRehab();
            }
        });

        // Update connection symbol based on state stored in MainActivity
        updateRESpeckConnectionSymbol(((MainActivity) getActivity()).getIsRESpeckConnected());

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

    @Override
    public void updateConnectionState(boolean showRESpeckConnected, boolean showAirspeckConnected,
                                      boolean showPulseoxConnecting) {
        updateRESpeckConnectionSymbol(showRESpeckConnected);
    }

    // This method gets called from the rehab button in this fragment.
    private void launchRehab() {
        Intent LaunchIntent = getContext().getPackageManager().getLaunchIntentForPackage("com.specknet.rehab2");
        try {
            startActivity(LaunchIntent);
        } catch (NullPointerException e) {
            ((MainActivity) getActivity()).showOnSnackbar("Unable to start Rehab app. Is app installed?");
        }
    }

    // This method gets called from the diary button in this fragment.
    private void launchDiary() {
        Intent LaunchIntent = getContext().getPackageManager().getLaunchIntentForPackage("com.specknet.rehabdiary");
        try {
            startActivity(LaunchIntent);
        } catch (NullPointerException e) {
            ((MainActivity) getActivity()).showOnSnackbar("Unable to start Diary app. Is app installed?");
        }
    }

    public void updateRESpeckConnectionSymbol(boolean isConnected) {
        if (connectedStatusRESpeck != null) {
            if (isConnected) {
                // "Flash" with symbol when updating to indicate data coming in
                connectedStatusRESpeck.setImageResource(R.drawable.vec_wireless);
                connectedStatusRESpeck.setVisibility(View.INVISIBLE);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connectedStatusRESpeck.setVisibility(View.VISIBLE);
                    }
                }, 100);

            } else {
                connectedStatusRESpeck.setImageResource(R.drawable.vec_xmark);
            }
        }
    }

    private void updateReadings(RESpeckLiveData data) {
        // Update readings and activity symbol
        // Set breathing rate text to currently calculated rates
        breathingRateText.setText(String.format(Locale.UK, "%.2f BrPM", data.getBreathingRate()));
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
}


