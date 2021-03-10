package com.specknet.airrespeck.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.ConnectionStateObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.FileLogger;
import com.specknet.airrespeck.utils.Utils;

//copied from SubjectHomeFragment.java
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Map;


///**
// * A simple {@link Fragment} subclass.
// * Use the {@link HomeTabFragment#newInstance} factory method to
// * create an instance of this fragment.
// */
public class HomeTabFragment extends Fragment implements RESpeckDataObserver, ConnectionStateObserver {

    private ImageView statusRESpeck;
    private ImageButton respeckPausePlayButton;
    private boolean isRespeckPaused;
    private ProgressBar waitingRESpeck;
    private ImageView respeckDisabledImage;

    private boolean isRespeckEnabled;

    private LinearLayout batteryContainer;
    private TextView respeckBatteryLevel;
    private ImageView batteryImage;

//
    public HomeTabFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home_tab, container, false);

        // Get options from project config
        Map<String, String> config = Utils.getInstance().getConfig(getActivity());

        // Load connection symbols
        statusRESpeck = (ImageView) view.findViewById(R.id.hometab_status_respeck); //when respeck is connected
        waitingRESpeck = (ProgressBar) view.findViewById(R.id.hometab_waitingrespeck); //when respeck is connecting
        respeckDisabledImage = (ImageView) view.findViewById(R.id.hometab_disabled_respeck); //when no respeck is found

        // Load battery status
        respeckBatteryLevel = (TextView) view.findViewById(R.id.hometab_battery_level);
        batteryImage = (ImageView) view.findViewById(R.id.hometab_battery_image);
        batteryContainer = (LinearLayout) view.findViewById(R.id.hometab_battery_container);

        // Initialise pause button for RESpeck
        respeckPausePlayButton = (ImageButton) view.findViewById(R.id.hometab_pause_button);
        isRespeckPaused = false;

        //clicking the pause play button
        respeckPausePlayButton.setOnClickListener(v -> {
            if (isRespeckPaused) {
                // Send CONTINUE command
                Intent intentData = new Intent(Constants.ACTION_RESPECK_RECORDING_CONTINUE);
                getActivity().sendBroadcast(intentData);

                isRespeckPaused = false;

                respeckPausePlayButton.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_24dp));

                statusRESpeck.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.vec_respeck));

                FileLogger.logToFile(getActivity(), "RESpeck recording continued");
            } else {
                respeckPausePlayButton.setEnabled(false);
                showRESpeckPauseDialog();
            }
        });

        isRespeckEnabled = !config.get(Constants.Config.RESPECK_UUID).isEmpty();

//        // Show disabled symbol if a device is not paired
//        if (isRespeckEnabled) {
//            // Update connection symbol based on state stored in MainActivity
//            updateRESpeckConnectionSymbol(((MainActivity) getActivity()).getIsRESpeckConnected());
//            ((MainActivity) getActivity()).registerRESpeckDataObserver(this);
//        } else {
//            // Only show disabled symbol if RESpeck is disabled
//            statusRESpeck.setVisibility(View.GONE);
//            waitingRESpeck.setVisibility(View.GONE);
//            respeckDisabledImage.setVisibility(View.VISIBLE);
//        }
//
//        // Register this fragment as connection state observer
//        ((MainActivity) getActivity()).registerConnectionStateObserver(this);

        return view;

    }

    @Override
    public void updateConnectionState(boolean showRESpeckConnected, boolean showAirspeckConnected,
                                  boolean showPulseoxConnecting, boolean showInhalerConnecting) {
    if (isRespeckEnabled) {
        updateRESpeckConnectionSymbol(showRESpeckConnected);
    }
}

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        updateRESpeckConnectionSymbol(true);

        // update battery level and charging status
        if (data.getBattLevel() != -1) {
            batteryContainer.setVisibility(View.VISIBLE);
            respeckBatteryLevel.setText(data.getBattLevel() + "%");
        }
        else {
            batteryContainer.setVisibility(View.INVISIBLE);
        }

        if (data.getChargingStatus()) {
            batteryImage.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.vec_battery));
        }
        else {
            batteryImage.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_battery_full));
        }
    }

    private void showRESpeckPauseDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder
                .setMessage(getString(R.string.respeck_pause_message))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        isRespeckPaused = true;

                        statusRESpeck.setImageDrawable(
                                ContextCompat.getDrawable(getActivity(), R.drawable.vec_respeck_off));

                        // Set button image to play
                        respeckPausePlayButton.setImageDrawable(
                                ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_black_24dp));

                        // Enable button again
                        respeckPausePlayButton.setEnabled(true);

                        // Broadcast PAUSE message
                        Intent intentData = new Intent(Constants.ACTION_RESPECK_RECORDING_PAUSE);
                        getActivity().sendBroadcast(intentData);

                        FileLogger.logToFile(getActivity(), "RESpeck recording paused");
                    }
                })
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        respeckPausePlayButton.setEnabled(true);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        respeckPausePlayButton.setEnabled(true);
                    }
                });
        alertDialogBuilder.create().show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void updateRESpeckConnectionSymbol(boolean isConnected) {
        if (isConnected) {
            respeckPausePlayButton.setEnabled(true);

            // "Flash" with symbol when updating to indicate data coming in
            if (isRespeckPaused) {
                statusRESpeck.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.vec_respeck_off));

                waitingRESpeck.setVisibility(View.GONE);
                statusRESpeck.setVisibility(View.VISIBLE);
            } else {
                statusRESpeck.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.vec_respeck));

                waitingRESpeck.setVisibility(View.GONE);
                statusRESpeck.setVisibility(View.INVISIBLE);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        statusRESpeck.setVisibility(View.VISIBLE);
                    }
                }, 100);
            }

        } else {
            statusRESpeck.setVisibility(View.GONE);
            waitingRESpeck.setVisibility(View.VISIBLE);
            respeckPausePlayButton.setEnabled(false);
        }
    }



//    @Override
//    public void onDestroy() {
//        // Unregister this class as observer. If we haven't observed, nothing happens
//        ((MainActivity) getActivity()).unregisterRESpeckDataObserver(this);
//        super.onDestroy();
//    }


}