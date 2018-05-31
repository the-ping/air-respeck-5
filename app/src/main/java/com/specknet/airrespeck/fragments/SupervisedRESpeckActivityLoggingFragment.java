package com.specknet.airrespeck.fragments;

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
import android.widget.TextView;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.CountUpTimer;
import com.specknet.airrespeck.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for recording volume data with zipper bags and sealing rings.
 */

public class SupervisedRESpeckActivityLoggingFragment extends ConnectionOverlayFragment implements RESpeckDataObserver {

    private OutputStreamWriter mWriter;

    private boolean mIsRecording;
    private String mActivity;
    private String mSubjectName;

    private Button mStartStopButton;
    private Button mCancelButton;

    private EditText nameTextField;
    private Spinner activitySpinner;
    private TextView timerText;
    private CountUpTimer countUpTimer;

    private StringBuilder outputData;

    private final String BUS = "Driving on a bus";
    private final String BIKE = "Driving bicycle";
    private final String WALKING = "Walking (without stops)";
    private final String SITTING_STRAIGHT = "Sitting straight";
    private final String SITTING_FORWARD = "Sitting bent forward";
    private final String SITTING_BACKWARD = "Sitting bent backward";
    private final String STANDING = "Standing";
    private final String LYING_ON_BACK = "Lying down normal on back";
    private final String LYING_STOMACH = "Lying down on stomach";
    private final String LYING_RIGHT = "Lying down to the right";
    private final String LYING_LEFT = "Lying down to the left";

    private final String LOG_TAG = "RESpeckActivityLogging";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity_logging_respeck, container, false);

        nameTextField = (EditText) view.findViewById(R.id.name_text_field);

        // Setup spinner
        activitySpinner = (Spinner) view.findViewById(R.id.activity_spinner);

        String[] activitySpinnerElements = new String[]{BUS, BIKE, WALKING, SITTING_STRAIGHT, SITTING_FORWARD,
                SITTING_BACKWARD, STANDING, LYING_ON_BACK, LYING_STOMACH, LYING_RIGHT, LYING_LEFT};
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, activitySpinnerElements);
        activitySpinner.setAdapter(activityAdapter);

        // Load buttons
        mStartStopButton = (Button) view.findViewById(R.id.record_button);
        mStartStopButton.setBackgroundColor(
                ContextCompat.getColor(getActivity(), R.color.md_grey_300));
        mCancelButton = (Button) view.findViewById(R.id.cancel_button);

        timerText = (TextView) view.findViewById(R.id.count_up_timer);
        countUpTimer = new CountUpTimer(1000) {
            @Override
            public void onTick(long elapsedTime) {
                Date date = new Date(elapsedTime);
                DateFormat formatter = new SimpleDateFormat("mm:ss");
                String dateFormatted = formatter.format(date);
                timerText.setText(dateFormatted);
            }
        };

        outputData = new StringBuilder();

        mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsRecording) {
                    stopRecording();
                } else {
                    startRecording();
                }
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelRecording();
            }
        });

        initialiseFileLogger();

        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);

        return view;
    }

    private void initialiseFileLogger() {
        String filename = Utils.getInstance().getDataDirectory(getActivity()) + Constants.LOGGING_DIRECTORY_NAME +
                new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(new Date()) +
                " Activity RESpeck Logs " + Utils.getInstance().getConfig(getActivity()).get(
                Constants.Config.RESPECK_UUID) +
                ".csv";

        // Create file for current day and append header, if it doesn't exist yet
        try {
            if (!new File(filename).exists()) {
                mWriter = new OutputStreamWriter(
                        new FileOutputStream(filename, true));
                mWriter.append(Constants.ACTIVITY_RECORDING_HEADER).append("\n");
                mWriter.flush();
            } else {
                mWriter = new OutputStreamWriter(new FileOutputStream(filename, true));
            }
            Log.i(LOG_TAG, "Logging file created");
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error while creating the file");
        }
    }

    private void startRecording() {
        // Start recording
        mSubjectName = nameTextField.getText().toString();
        if (!mSubjectName.equals("")) {
            mActivity = activitySpinner.getSelectedItem().toString();

            mIsRecording = true;

            // Change button label to tell the user that we are recording
            mStartStopButton.setText(R.string.button_text_stop_recording);
            mStartStopButton.setBackgroundColor(
                    ContextCompat.getColor(getActivity(), R.color.md_green_300));
            mCancelButton.setEnabled(true);

            countUpTimer.start();
        } else {
            Toast.makeText(getActivity(), "Enter subject name", Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording() {
        countUpTimer.stop();
        countUpTimer.reset();
        timerText.setText("00:00");

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
                    } else {
                        Toast.makeText(getActivity(),
                                "No data received from RESpeck in recording period",
                                Toast.LENGTH_LONG).show();
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
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        // If we are currently recording (button is pressed), store the received values
        if (mIsRecording) {
            String output = mSubjectName + "," + data.getPhoneTimestamp() + "," +
                    data.getAccelX() + "," + data.getAccelY() + "," + data.getAccelZ() + "," +
                    data.getBreathingSignal() + "," + mActivity + "," + data.getActivityLevel() + "," +
                    data.getActivityType() + "\n";
            outputData.append(output);
        }
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
            if (mWriter != null) {
                mWriter.close();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error while closing the file");
        }
        ((MainActivity) getActivity()).unregisterRESpeckDataObserver(this);
        super.onDestroy();
    }

}
