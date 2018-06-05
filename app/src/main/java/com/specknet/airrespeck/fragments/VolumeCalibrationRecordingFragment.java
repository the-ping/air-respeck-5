package com.specknet.airrespeck.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for recording volume data with zipper bags and sealing rings.
 */

public class VolumeCalibrationRecordingFragment extends ConnectionOverlayFragment {

    private OutputStreamWriter mWriter;
    private BroadcastReceiver mSpeckServiceReceiver;

    private boolean mIsRecording;
    private String mBagSize;
    private String mActivity;
    private String mSubjectName;

    private Button mStartStopButton;
    private Button mCancelButton;

    private StringBuilder outputData;

    private final String SITTING_STRAIGHT = "Sitting straight"; // normal straight sitting
    private final String SITTING_FORWARD = "Sitting forward";
    private final String SITTING_BACKWARD = "Sitting backward";
    private final String STANDING = "Standing";
    private final String LYING_NORMAL = "Lying down normal"; // normal lying
    private final String LYING_RIGHT = "Lying down right";
    private final String LYING_LEFT = "Lying down left";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_volume_calibration_recording, container, false);

        // Setup spinners
        final Spinner activitySpinner = (Spinner) view.findViewById(R.id.category_spinner);
        final Spinner bagSizeSpinner = (Spinner) view.findViewById(R.id.bag_size_spinner);

        final EditText nameTextField = (EditText) view.findViewById(R.id.name_text_field);

        //String[] activitySpinnerElements = new String[]{SITTING_STRAIGHT, STANDING, LYING_NORMAL};
        String[] activitySpinnerElements = new String[]{SITTING_STRAIGHT, SITTING_FORWARD, SITTING_BACKWARD, STANDING,
                LYING_NORMAL, LYING_RIGHT, LYING_LEFT};
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, activitySpinnerElements);
        activitySpinner.setAdapter(activityAdapter);

        String[] bagSpinnerElements = new String[]{"0.35", "0.5", "0.7", "1.2"};
        ArrayAdapter<String> bagSizeAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, bagSpinnerElements);
        bagSizeSpinner.setAdapter(bagSizeAdapter);

        // Load buttons
        mStartStopButton = (Button) view.findViewById(R.id.record_button);
        mStartStopButton.setBackgroundColor(
                ContextCompat.getColor(getActivity(), R.color.md_grey_300));
        mCancelButton = (Button) view.findViewById(R.id.cancel_button);

        outputData = new StringBuilder();

        mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsRecording) {
                    // Wait 1 second before actually ending the run, to factor in bluetooth lag
                    // Disable start button until then
                    mStartStopButton.setEnabled(false);
                    mCancelButton.setEnabled(false);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // End recording
                                mIsRecording = false;

                                if (outputData.length() != 0) {
                                    mWriter.write(outputData.toString());
                                    mWriter.flush();
                                    Toast.makeText(getActivity(),
                                            "Recording saved",
                                            Toast.LENGTH_LONG).show();

                                    // Add a line which indicates end of recording
                                    mWriter.append("end of record,,,,,,,\n").flush();
                                }

                                outputData = new StringBuilder();

                                // Change button label to tell the user that the recording has stopped
                                mStartStopButton.setText(R.string.button_text_start_recording);
                                mStartStopButton.setEnabled(true);
                                mStartStopButton.setBackgroundColor(
                                        ContextCompat.getColor(getActivity(), R.color.md_grey_300));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, 1000);
                } else {
                    // Start recording

                    mSubjectName = nameTextField.getText().toString();
                    if (!mSubjectName.equals("")) {
                        mBagSize = bagSizeSpinner.getSelectedItem().toString();
                        mActivity = activitySpinner.getSelectedItem().toString();

                        mIsRecording = true;

                        // Change button label to tell the user that we are recording
                        mStartStopButton.setText(R.string.button_text_stop_recording);
                        mStartStopButton.setBackgroundColor(
                                ContextCompat.getColor(getActivity(), R.color.md_green_300));
                        mCancelButton.setEnabled(true);
                    } else {
                        Toast.makeText(getActivity(), "Enter subject name", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelRecording();
            }
        });

        // Create volume directory if it doesn't exist
        File directory = new File(Utils.getInstance().getDataDirectory(getActivity()) +
                Constants.VOLUME_DATA_DIRECTORY_NAME);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                Log.i("DF", "Directory created: " + directory);
            } else {
                Log.e("Volume recording", "Couldn't create Volume recording folder on external storage");
            }
        }

        String filename = directory.getAbsolutePath() +
                new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(new Date()) +
                " Volume calibration.csv";
        try {
            // Create file for current day and append header, if it doesn't exist yet
            if (!new File(filename).exists()) {
                mWriter = new OutputStreamWriter(
                        new FileOutputStream(filename, true));
                mWriter.append(Constants.VOLUME_DATA_HEADER).append("\n");
                mWriter.flush();
            } else {
                mWriter = new OutputStreamWriter(new FileOutputStream(filename, true));
            }
        } catch (IOException e) {
            Log.e("Volume calibration", "Error while creating the file");
        }

        mSpeckServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                RESpeckLiveData data = (RESpeckLiveData) intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA);

                // If we are currently recording (button is pressed), store the received values
                if (mIsRecording) {
                    if ((mActivity.equals(SITTING_STRAIGHT) || mActivity.equals(SITTING_BACKWARD) || mActivity.equals(
                            SITTING_FORWARD) || mActivity.equals(
                            STANDING)) && !(data.getActivityType() == Constants.ACTIVITY_STAND_SIT)) {
                        Toast.makeText(getActivity(),
                                "RESpeck registers activity other than sitting or standing, which is the selected option",
                                Toast.LENGTH_LONG).show();
                        cancelRecording();
                    } else if ((mActivity.equals(LYING_NORMAL) || mActivity.equals(LYING_LEFT) || mActivity.equals(
                            LYING_RIGHT)) && !(data.getActivityType() == Constants.ACTIVITY_LYING)) {
                        Toast.makeText(getActivity(),
                                "RESpeck registers activity other than lying down, which is the selected option",
                                Toast.LENGTH_LONG).show();
                        cancelRecording();
                    }

                    String output = mSubjectName + "," + data.getPhoneTimestamp() + "," +
                            data.getAccelX() + "," + data.getAccelY() + "," + data.getAccelZ() + "," +
                            data.getBreathingSignal() + "," + mActivity + "," + mBagSize + "\n";
                    outputData.append(output);
                }
            }
        };

        getActivity().registerReceiver(mSpeckServiceReceiver, new IntentFilter(
                Constants.ACTION_RESPECK_LIVE_BROADCAST));

        return view;
    }

    private void cancelRecording() {
        // Reset output data and with that, discard previously stored data
        outputData = new StringBuilder();

        mIsRecording = false;

        // Change button label to tell the user that the recording has stopped
        mStartStopButton.setText(R.string.button_text_start_recording);
        mStartStopButton.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.md_grey_300));

        mCancelButton.setEnabled(false);
    }

    @Override
    public void onDestroy() {
        try {
            mWriter.close();
        } catch (IOException e) {
            Log.e("Volume calibration", "Error while creating the file");
        }
        getActivity().unregisterReceiver(mSpeckServiceReceiver);
        super.onDestroy();
    }

}
