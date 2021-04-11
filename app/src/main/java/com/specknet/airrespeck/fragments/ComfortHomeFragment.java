package com.specknet.airrespeck.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.ConnectionStateObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.FileLogger;
import com.specknet.airrespeck.utils.Utils;

import java.util.Map;

public class ComfortHomeFragment extends Fragment implements RESpeckDataObserver,
        ConnectionStateObserver {

    private Button respeckPausePlayButton;
    private boolean isRespeckPaused;
    private boolean isRespeckEnabled;

    private ImageView connectedStatusRESpeck;

    public ComfortHomeFragment() {
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
        View view = inflater.inflate(R.layout.fragment_comfort_home, container, false);

        // Get options from project config
        Map<String, String> config = Utils.getInstance().getConfig(getActivity());

        // Load button, recording not paused
        connectedStatusRESpeck = (ImageView) view.findViewById(R.id.comfort_respeck);
        respeckPausePlayButton = (Button) view.findViewById(R.id.comfort_pauseplay_button);
        isRespeckPaused = false;


//        // if button is clicked
//        respeckPausePlayButton.setOnClickListener(v -> {
//            if (isRespeckPaused) {
//                resumeRecording();
//            } else if (!isRespeckPaused) {
//                pauseRecording();
//            }
//        });

        respeckPausePlayButton.setEnabled(false);
        respeckPausePlayButton.setOnClickListener(v -> {
            if (isRespeckPaused) {
                // Send CONTINUE command
                Intent intentData = new Intent(Constants.ACTION_RESPECK_RECORDING_CONTINUE);
                getActivity().sendBroadcast(intentData);

                isRespeckPaused = false;

                respeckPausePlayButton.setText("Pause");
                respeckPausePlayButton.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.md_green_300));
//                respeckPausePlayButton.setImageDrawable(
//                        ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_24dp));

                connectedStatusRESpeck.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.vec_respeck));

                FileLogger.logToFile(getActivity(), "RESpeck recording continued");
            } else {
                respeckPausePlayButton.setEnabled(false);
                showRESpeckPauseDialog();
            }
        });

        isRespeckEnabled = !config.get(Constants.Config.RESPECK_UUID).isEmpty();

        // Update connection symbol based on state stored in MainActivity
        if (isRespeckEnabled) {
            updateRESpeckConnectionSymbol(((MainActivity) getActivity()).getIsRESpeckConnected());
            ((MainActivity) getActivity()).registerRESpeckDataObserver(this);
        }

        // Register this fragment as connection state observer
        ((MainActivity) getActivity()).registerConnectionStateObserver(this);

        return view;
    }

//    private void resumeRecording() {
//        respeckPausePlayButton.setText("Pause");
////        respeckPausePlayButton.setBackgroundColor(0x47c06e);
//        respeckPausePlayButton.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.md_green_300));
//        isRespeckPaused = false;
//    }
//
//    private void pauseRecording() {
//        respeckPausePlayButton.setText("Resume");
////        respeckPausePlayButton.setBackgroundColor(0xFF0000);
//        respeckPausePlayButton.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.md_red_300));
//        isRespeckPaused = true;
//
//        // Broadcast PAUSE message
//        Intent intentData = new Intent(Constants.ACTION_RESPECK_RECORDING_PAUSE);
//        getActivity().sendBroadcast(intentData);
//    }

    @Override
    public void updateConnectionState(boolean showRESpeckConnected, boolean airspeckConnected, boolean pulseoxConnected, boolean inhalerConnected) {
        // if respeck connected, color rescpeck device icon
        if (isRespeckEnabled) {
            updateRESpeckConnectionSymbol(showRESpeckConnected);
        }
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {

    }

    private void showRESpeckPauseDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder
                .setMessage(getString(R.string.respeck_pause_message))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        isRespeckPaused = true;

                        // Change to dark respeck indicating disconnection
                        connectedStatusRESpeck.setImageDrawable(
                                ContextCompat.getDrawable(getActivity(), R.drawable.vec_respeck_off));

                        // Set button image to red
                        respeckPausePlayButton.setText("Resume");
                        respeckPausePlayButton.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.md_red_300));


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

    public void updateRESpeckConnectionSymbol(boolean isConnected) {
        if (isConnected) {
            respeckPausePlayButton.setEnabled(true);

//            // "Flash" with symbol when updating to indicate data coming in
            if (isRespeckPaused) {
                connectedStatusRESpeck.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.vec_respeck_off));
            } else {
                connectedStatusRESpeck.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.vec_respeck));
            }

        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        // Unregister this class as observer. If we haven't observed, nothing happens
        ((MainActivity) getActivity()).unregisterRESpeckDataObserver(this);
        ((MainActivity) getActivity()).unregisterConnectionStateObserver(this);
        super.onDestroy();
    }
}