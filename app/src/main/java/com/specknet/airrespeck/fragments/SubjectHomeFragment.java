package com.specknet.airrespeck.fragments;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

//import com.crashlytics.android.Crashlytics;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.dialogs.TurnGPSOnDialog;
import com.specknet.airrespeck.utils.Constants;

/**
 * Home screen for subjects using the app
 */

public class SubjectHomeFragment extends BaseFragment {

    private ImageView connectedStatusRESpeck;
    private ImageView connectedStatusAirspeck;
    private ProgressBar progressBarRESpeck;
    private ProgressBar progressBarAirspeck;
    private ImageView airspeckOffButton;

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

        mConnectingLayout = (LinearLayout) view.findViewById(R.id.connecting_layout);

        ImageButton diaryButton = (ImageButton) view.findViewById(R.id.diary_button);
        diaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDiaryApp(getActivity(), "com.specknet.diarydaphne");
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
                showPowerOffDialog();
            }
        });


        mIsCreated = true;

        // Update connection symbol based on state stored in MainActivity
        updateRESpeckConnectionSymbol(((MainActivity) getActivity()).getIsRESpeckConnected());
        updateAirspeckConnectionSymbol(((MainActivity) getActivity()).getIsAirspeckConnected());

        return view;
    }

    private void showPowerOffDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder
                .setMessage(getString(R.string.airspeck_power_off_message))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        connectedStatusAirspeck.setVisibility(View.GONE);
                        progressBarAirspeck.setVisibility(View.VISIBLE);
                        Intent i = new Intent(Constants.AIRSPECK_OFF_ACTION);
                        getActivity().sendBroadcast(i);
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
    public int getIcon() {
        return Constants.MENU_ICON_HOME;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void updateRESpeckConnectionSymbol(boolean isConnected) {
        if (mIsCreated) {
            if (isConnected) {
                // "Flash" with symbol when updating to indicate data coming in
                progressBarRESpeck.setVisibility(View.GONE);
                connectedStatusRESpeck.setVisibility(View.INVISIBLE);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connectedStatusRESpeck.setVisibility(View.VISIBLE);
                    }
                }, 100);

            } else {
                connectedStatusRESpeck.setVisibility(View.GONE);
                progressBarRESpeck.setVisibility(View.VISIBLE);
            }
        }
    }

    public void updateAirspeckConnectionSymbol(boolean isConnected) {
        if (mIsCreated) {
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
    }
}


