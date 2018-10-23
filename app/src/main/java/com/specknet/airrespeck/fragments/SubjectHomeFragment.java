package com.specknet.airrespeck.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.AirspeckDataObserver;
import com.specknet.airrespeck.activities.ConnectionStateObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.FileLogger;
import com.specknet.airrespeck.utils.Utils;

import java.util.Map;

/**
 * Home screen for subjects using the app
 */

public class SubjectHomeFragment extends Fragment implements RESpeckDataObserver, AirspeckDataObserver,
        ConnectionStateObserver {

    private ImageView connectedStatusRESpeck;
    private ImageView connectedStatusAirspeck;
    private ProgressBar progressBarRESpeck;
    private ProgressBar progressBarAirspeck;
    private ImageView airspeckOffButton;
    private ImageView respeckPausePlayButton;

    private boolean isAirspeckEnabled;
    private boolean isRespeckEnabled;
    private boolean isRespeckPaused;

    private boolean isCameraButtonEnabled;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SubjectHomeFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_subject_home, container, false);

        // Load connection symbols
        connectedStatusRESpeck = (ImageView) view.findViewById(R.id.connected_status_respeck);
        connectedStatusAirspeck = (ImageView) view.findViewById(R.id.connected_status_airspeck);

        progressBarRESpeck = (ProgressBar) view.findViewById(R.id.progress_bar_respeck);
        progressBarAirspeck = (ProgressBar) view.findViewById(R.id.progress_bar_airspeck);

        ImageView airspeckDisabledImage = (ImageView) view.findViewById(R.id.not_enabled_airspeck);
        ImageView respeckDisabledImage = (ImageView) view.findViewById(R.id.not_enabled_respeck);

        isRespeckPaused = false;

        ImageButton diaryButton = (ImageButton) view.findViewById(R.id.diary_button);
        diaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDiaryApp(getActivity(), "com.specknet.diarydaphne");
            }
        });

        ImageButton cameraButton = (ImageButton) view.findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
            }
        });

        // Initialise pause button for RESpeck
        respeckPausePlayButton = (ImageButton) view.findViewById(R.id.respeck_pause_button);
        respeckPausePlayButton.setEnabled(false);
        respeckPausePlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRespeckPaused) {
                    // Send CONTINUE command
                    Intent intentData = new Intent(Constants.ACTION_RESPECK_RECORDING_CONTINUE);
                    getActivity().sendBroadcast(intentData);

                    isRespeckPaused = false;

                    respeckPausePlayButton.setImageDrawable(
                            ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_24dp));

                    connectedStatusRESpeck.setImageDrawable(
                            ContextCompat.getDrawable(getActivity(), R.drawable.vec_wireless_active));

                    FileLogger.logToFile(getActivity(), "RESpeck recording continued");
                } else {
                    respeckPausePlayButton.setEnabled(false);
                    showRESpeckPauseDialog();
                }
            }
        });

        // Initialise turn off button for Airspeck
        airspeckOffButton = (ImageButton) view.findViewById(R.id.airspeck_off_button);
        airspeckOffButton.setEnabled(false);
        airspeckOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Send switch off message to BLE service
                airspeckOffButton.setEnabled(false);
                showAirspeckPowerOffDialog();
            }
        });

        Utils utils = Utils.getInstance();
        Map<String, String> config = utils.getConfig(getActivity());
        isAirspeckEnabled = !config.get(Constants.Config.AIRSPECKP_UUID).isEmpty();
        isRespeckEnabled = !config.get(Constants.Config.RESPECK_UUID).isEmpty();

        // Show disabled symbol if a device is not paired
        if (isAirspeckEnabled) {
            // Update connection symbol based on state stored in MainActivity
            updateAirspeckConnectionSymbol(((MainActivity) getActivity()).getIsAirspeckConnected());
            ((MainActivity) getActivity()).registerAirspeckDataObserver(this);
        } else {
            // Only show disabled symbol if Airspeck is disabled
            connectedStatusAirspeck.setVisibility(View.GONE);
            progressBarAirspeck.setVisibility(View.GONE);
            airspeckDisabledImage.setVisibility(View.VISIBLE);
        }
        if (isRespeckEnabled) {
            // Update connection symbol based on state stored in MainActivity
            updateRESpeckConnectionSymbol(((MainActivity) getActivity()).getIsRESpeckConnected());
            ((MainActivity) getActivity()).registerRESpeckDataObserver(this);
        } else {
            // Only show disabled symbol if RESpeck is disabled
            connectedStatusRESpeck.setVisibility(View.GONE);
            progressBarRESpeck.setVisibility(View.GONE);
            respeckDisabledImage.setVisibility(View.VISIBLE);
        }

        isCameraButtonEnabled = Boolean.parseBoolean(config.get(Constants.Config.SHOW_PHOTO_BUTTON));
        if (isCameraButtonEnabled){
            cameraButton.setVisibility(View.VISIBLE);
        }

        // Register this fragment as connection state observer
        ((MainActivity) getActivity()).registerConnectionStateObserver(this);

        return view;
    }

    @Override
    public void updateConnectionState(boolean showRESpeckConnected, boolean showAirspeckConnected,
                                      boolean showPulseoxConnecting, boolean showInhalerConnecting) {
        if (isRespeckEnabled) {
            updateRESpeckConnectionSymbol(showRESpeckConnected);
        }
        if (isAirspeckEnabled) {
            updateAirspeckConnectionSymbol(showAirspeckConnected);
        }
    }


    // Let connection symbol flicker if new data comes in
    @Override
    public void updateAirspeckData(AirspeckData data) {
        updateAirspeckConnectionSymbol(true);
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        updateRESpeckConnectionSymbol(true);
    }

    private void showRESpeckPauseDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder
                .setMessage(getString(R.string.respeck_pause_message))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        isRespeckPaused = true;

                        connectedStatusRESpeck.setImageDrawable(
                                ContextCompat.getDrawable(getActivity(), R.drawable.vec_wireless_pause));

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

    private void showAirspeckPowerOffDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder
                .setMessage(getString(R.string.airspeck_power_off_message))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        connectedStatusAirspeck.setVisibility(View.GONE);
                        progressBarAirspeck.setVisibility(View.VISIBLE);
                        Intent i = new Intent(Constants.AIRSPECK_OFF_ACTION);
                        getActivity().sendBroadcast(i);
                        FileLogger.logToFile(getActivity(), "Airspeck power off button pressed");
                    }
                })
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        airspeckOffButton.setEnabled(true);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        airspeckOffButton.setEnabled(true);
                    }
                });
        alertDialogBuilder.create().show();
    }

    public void startDiaryApp(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);

        if (intent == null) {
            Toast.makeText(context, "Diary app not installed. Contact researchers for further information.",
                    Toast.LENGTH_LONG).show();
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
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
                connectedStatusRESpeck.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.vec_wireless_pause));

                progressBarRESpeck.setVisibility(View.GONE);
                connectedStatusRESpeck.setVisibility(View.VISIBLE);
            } else {
                connectedStatusRESpeck.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.vec_wireless_active));

                progressBarRESpeck.setVisibility(View.GONE);
                connectedStatusRESpeck.setVisibility(View.INVISIBLE);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connectedStatusRESpeck.setVisibility(View.VISIBLE);
                    }
                }, 100);
            }

        } else {
            connectedStatusRESpeck.setVisibility(View.GONE);
            progressBarRESpeck.setVisibility(View.VISIBLE);
            respeckPausePlayButton.setEnabled(false);
        }
    }

    public void updateAirspeckConnectionSymbol(boolean isConnected) {
        if (isConnected) {
            // "Flash" with symbol when updating to indicate data coming in
            progressBarAirspeck.setVisibility(View.GONE);
            connectedStatusAirspeck.setVisibility(View.INVISIBLE);
            airspeckOffButton.setEnabled(true);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    connectedStatusAirspeck.setVisibility(View.VISIBLE);
                }
            }, 100);

        } else {
            airspeckOffButton.setEnabled(false);
            connectedStatusAirspeck.setVisibility(View.GONE);
            progressBarAirspeck.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        // Unregister this class as observer. If we haven't observed, nothing happens
        ((MainActivity) getActivity()).unregisterRESpeckDataObserver(this);
        ((MainActivity) getActivity()).unregisterAirspeckDataObserver(this);
        ((MainActivity) getActivity()).unregisterConnectionStateObserver(this);
        super.onDestroy();
    }
}


