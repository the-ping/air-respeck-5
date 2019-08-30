package com.specknet.airrespeck.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

/**
 * Home screen for subjects using the app
 */

public class SubjectHomeFragment extends Fragment implements RESpeckDataObserver, AirspeckDataObserver,
        ConnectionStateObserver {

    static final int REQUEST_IMAGE_CAPTURE = 0;
    static final int REQUEST_VIDEO_CAPTURE = 1;


    File mediaFile = null;

    private ImageView connectedStatusRESpeck;
    private ImageView connectedStatusAirspeck;
    private ProgressBar progressBarRESpeck;
    private ProgressBar progressBarAirspeck;
    private ImageView airspeckOffButton;
    private ImageView respeckPausePlayButton;

    private boolean isAirspeckEnabled;
    private boolean isRespeckEnabled;
    private boolean isRespeckPaused;

    private boolean isMediaButtonsEnabled;
    private TableLayout mediaButtonsTable;

    private Uri photoURI;

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



        Map<String, String> config = Utils.getInstance().getConfig(getActivity());
        isMediaButtonsEnabled = Boolean.parseBoolean(config.get(Constants.Config.SHOW_MEDIA_BUTTONS));

        if (isMediaButtonsEnabled){
            // Photograph button
            ImageButton cameraButton = (ImageButton) view.findViewById(R.id.camera_button);
            cameraButton.setOnClickListener(view12 -> {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {

                    try {
                        mediaFile = createMediaFile(".jpg");
                    } catch (IOException ex) {
                        // TODO: Error occurred while creating the File
                    }

                    if (mediaFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(getContext(),
                                "com.specknet.airrespeck.fileprovider",
                                mediaFile);

                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                    }
                }
            });

            // Video button
            ImageButton videoButton = (ImageButton) view.findViewById(R.id.video_button);
            videoButton.setOnClickListener(view1 -> {
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {

                    try {
                        mediaFile = createMediaFile(".mp4");
                    } catch (IOException ex) {
                        // TODO: Error occurred while creating the File
                    }

                    if (mediaFile != null) {
                        Uri videoURI = FileProvider.getUriForFile(getContext(),
                                "com.specknet.airrespeck.fileprovider",
                                mediaFile);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI);
                        startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
                    }
                }
            });

            // Text button
            ImageButton textButton = (ImageButton) view.findViewById(R.id.text_button);
            textButton.setOnClickListener(view13 -> {
                TextSubmission textFragment = new TextSubmission();
                textFragment.show(getActivity().getFragmentManager(), "text");
            });

            // Audio button
            ImageButton audioButton = (ImageButton) view.findViewById(R.id.audio_button);
            audioButton.setOnClickListener(view14 -> {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.REQUEST_RECORD_AUDIO_PERMISSION);
                }

                Fragment fragment = new AudioSubmission();
                android.support.v4.app.FragmentManager fm = getActivity().getSupportFragmentManager();
                android.support.v4.app.FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.main_frame, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            });

            // Now that the buttons have been initialised, show them.
            mediaButtonsTable = (TableLayout) view.findViewById(R.id.media_buttons_table);
            mediaButtonsTable.setVisibility(View.VISIBLE);
        }

        // Initialise pause button for RESpeck
        respeckPausePlayButton = (ImageButton) view.findViewById(R.id.respeck_pause_button);
        respeckPausePlayButton.setEnabled(false);
        respeckPausePlayButton.setOnClickListener(v -> {
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
        });

        // Initialise turn off button for Airspeck
        airspeckOffButton = (ImageButton) view.findViewById(R.id.airspeck_off_button);
        airspeckOffButton.setEnabled(false);
        airspeckOffButton.setOnClickListener(view15 -> {
            // Send switch off message to BLE service
            airspeckOffButton.setEnabled(false);
            showAirspeckPowerOffDialog();
        });

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

    public File createMediaFile(String suffix) throws IOException {
            String directory = Utils.getInstance().getDataDirectory(getActivity()) +
                    Constants.MEDIA_DIRECTORY_NAME;
            String filename = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File mediaFile = new File(new File(directory), filename.concat(suffix));
            photoURI = Uri.parse(mediaFile.toString());
            mediaFile.createNewFile();
            return mediaFile;
        }

    // Processing media after collection
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK){
            mediaFile.delete();
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE){
            Toast.makeText(getContext(), "Picture saved successfully\nPlease check image quality now by\nzooming in on the photo", Toast.LENGTH_SHORT).show();
            // Launch default viewer for the file
            Intent intent2 = new Intent();
            intent2.setAction(android.content.Intent.ACTION_VIEW);
            intent2.setDataAndType(photoURI,"image/*");
            ((Activity) getContext()).startActivity(intent2);
        }
        else if (requestCode == REQUEST_VIDEO_CAPTURE){
            Toast.makeText(getContext(), "Video saved successfully", Toast.LENGTH_SHORT).show();
        }
    }
}


