package com.specknet.airrespeck.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.utils.Constants;

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

public class VolumeCalibrationRecordingActivity extends BaseActivity {

    private OutputStreamWriter mWriter;
    private BroadcastReceiver mSpeckServiceReceiver;

    private boolean mIsRecording;
    private String mBagSize;
    private String mActivity;

    private Button mStartStopButton;
    private Button mCancelButton;

    private StringBuilder outputData;

    private final String SITTING = "Sitting";
    private final String STANDING = "Standing";
    private final String LYING = "Lying down";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_volume_calibration_recording);

        // Setup spinners
        final Spinner activitySpinner = (Spinner) findViewById(R.id.activity_spinner);
        final Spinner bagSizeSpinner = (Spinner) findViewById(R.id.bag_size_spinner);

        String[] activitySpinnerElements = new String[]{SITTING, STANDING, LYING};
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, activitySpinnerElements);
        activitySpinner.setAdapter(activityAdapter);

        String[] bagSpinnerElements = new String[]{"0.35", "0.7", "1.2"};
        ArrayAdapter<String> bagSizeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, bagSpinnerElements);
        bagSizeSpinner.setAdapter(bagSizeAdapter);

        // Load buttons
        mStartStopButton = (Button) findViewById(R.id.record_button);
        mStartStopButton.setBackgroundColor(
                ContextCompat.getColor(getApplicationContext(), R.color.md_grey_300));
        mCancelButton = (Button) findViewById(R.id.cancel_button);

        outputData = new StringBuilder();

        mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsRecording) {
                    // End recording
                    try {
                        if (outputData.length() != 0) {
                            mWriter.write(outputData.toString());
                            mWriter.flush();
                            Toast.makeText(getApplicationContext(),
                                    "Recording saved",
                                    Toast.LENGTH_LONG).show();
                        }
                        outputData = new StringBuilder();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mIsRecording = false;

                    // Change button label to tell the user that the recording has stopped
                    mStartStopButton.setText(R.string.button_text_start_recording);
                    mStartStopButton.setBackgroundColor(
                            ContextCompat.getColor(getApplicationContext(), R.color.md_grey_300));

                    mCancelButton.setEnabled(false);
                } else {
                    // Start recording
                    mBagSize = bagSizeSpinner.getSelectedItem().toString();
                    mActivity = activitySpinner.getSelectedItem().toString();
                    mIsRecording = true;

                    // Change button label to tell the user that we are recording
                    mStartStopButton.setText(R.string.button_text_stop_recording);
                    mStartStopButton.setBackgroundColor(
                            ContextCompat.getColor(getApplicationContext(), R.color.md_green_300));
                    mCancelButton.setEnabled(true);
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
        File directory = new File(Constants.VOLUME_DATA_DIRECTORY_PATH);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                Log.i("DF", "Directory created: " + directory);
            } else {
                Log.e("Volume recording", "Couldn't create Volume recording folder on external storage");
            }
        }

        String filename = Constants.VOLUME_DATA_DIRECTORY_PATH +
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
                mWriter = new OutputStreamWriter(
                        new FileOutputStream(filename, true));
            }
        } catch (IOException e) {
            Log.e("Volume calibration", "Error while creating the file");
        }

        mSpeckServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // If we are currently recording (button is pressed), store the received values
                if (mIsRecording) {
                    int activityType = intent.getIntExtra(Constants.RESPECK_ACTIVITY_TYPE, Constants.WRONG_ORIENTATION);
                    if ((mActivity.equals(SITTING) || mActivity.equals(
                            STANDING)) && !(activityType == Constants.ACTIVITY_STAND_SIT)) {
                        Toast.makeText(getApplicationContext(),
                                "RESpeck registers activity other than sitting or standing, which is the selected option",
                                Toast.LENGTH_LONG).show();
                        cancelRecording();
                    } else if (mActivity.equals(LYING) && !(activityType == Constants.ACTIVITY_LYING)) {
                        Toast.makeText(getApplicationContext(),
                                "RESpeck registers activity other than lying down, which is the selected option",
                                Toast.LENGTH_LONG).show();
                        cancelRecording();
                    }

                    String output = intent.getLongExtra(Constants.INTERPOLATED_PHONE_TIMESTAMP, 0) + "," +
                            intent.getFloatExtra(Constants.RESPECK_X, Float.NaN) + "," +
                            intent.getFloatExtra(Constants.RESPECK_Y, Float.NaN) + "," +
                            intent.getFloatExtra(Constants.RESPECK_Z, Float.NaN) + "," +
                            intent.getFloatExtra(Constants.RESPECK_BREATHING_SIGNAL, Float.NaN) + "," +
                            mActivity + "," + mBagSize + "\n";
                    outputData.append(output);
                }
            }
        };

        registerReceiver(mSpeckServiceReceiver, new IntentFilter(
                Constants.ACTION_RESPECK_LIVE_BROADCAST));
    }

    private void cancelRecording() {
        // Reset output data and with that, discard previously stored data
        outputData = new StringBuilder();

        mIsRecording = false;

        // Change button label to tell the user that the recording has stopped
        mStartStopButton.setText(R.string.button_text_start_recording);
        mStartStopButton.setBackgroundColor(
                ContextCompat.getColor(getApplicationContext(), R.color.md_grey_300));

        mCancelButton.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        try {
            mWriter.close();
        } catch (IOException e) {
            Log.e("Volume calibration", "Error while creating the file");
        }
        unregisterReceiver(mSpeckServiceReceiver);
        super.onDestroy();
    }
}
