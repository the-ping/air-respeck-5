package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.AirspeckDataObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.CountUpTimer;
import com.specknet.airrespeck.utils.IndoorOutdoorPredictor;
import com.specknet.airrespeck.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Recording activity data
 */

public class SupervisedActivityLoggingFragment extends ConnectionOverlayFragment implements RESpeckDataObserver, AirspeckDataObserver {

    private OutputStreamWriter mWriter;

    private boolean mIsRespeckRecording;
    private boolean mIsInOutRecording;
    private String mActivity;
    private String mSubjectName;

    private Button mStartStopButton;
    private Button mCancelButton;

    private EditText nameTextField;
    private Spinner categorySpinner;
    private Spinner activitySpinner;
    private TextView timerText;
    private CountUpTimer countUpTimer;

    private StringBuilder outputData;

    private String androidID;
    private String airspeckUUID;
    private String subjectID;

    private final String OUTDOOR = "Outdoor";
    private final String INDOOR = "Indoor";
    private final String HALF_OPEN = "Half-open";
    private final String BUS = "Riding a bus";
    private final String BIKE = "Riding a bicycle";
    private final String TRAIN = "Riding a train";
    private final String RUNNING = "Running (at different speeds)";
    private final String WALKING = "Walking (without stops)";
    private final String WALKING_UPSTAIRS = "Walking upstairs";
    private final String WALKING_DOWNSTAIRS = "Walking downstairs";
    private final String WALKING_100_STEPS = "Walking 100 steps";
    private final String SITTING_STRAIGHT = "Sitting straight";
    private final String SITTING_FORWARD = "Sitting bent forward";
    private final String SITTING_BACKWARD = "Sitting bent backward";
    private final String STANDING = "Standing";
    private final String LYING_ON_BACK = "Lying down normal on back";
    private final String LYING_STOMACH = "Lying down on stomach";
    private final String LYING_RIGHT = "Lying down to the right";
    private final String LYING_LEFT = "Lying down to the left";
    private final String ORIENTATION = "Orientation";
    private final String INOUT = "Indoor / Outdoor";
    private final String TRANSPORT = "Movement / Modes of transport";
    private final String COUGHING = "Coughing";
    private final String NONCOUGHING = "Noncoughing";

    private final String LOG_TAG = "RESpeckActivityLogging";

    private IndoorOutdoorPredictor indoorOutdoorPredictor;
    private OutputStreamWriter predictionWriter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity_logging_respeck, container, false);

        // Load config variables
        Map<String, String> config = Utils.getInstance().getConfig(getActivity());
        airspeckUUID = config.get(Constants.Config.AIRSPECKP_UUID);
        subjectID = config.get(Constants.Config.SUBJECT_ID);
        androidID = Settings.Secure.getString(getActivity().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        nameTextField = (EditText) view.findViewById(R.id.name_text_field);

        // Setup category spinner
        categorySpinner = (Spinner) view.findViewById(R.id.category_spinner);

        String[] categorySpinnerElements = new String[]{ORIENTATION, INOUT, TRANSPORT, COUGHING};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, categorySpinnerElements);
        categorySpinner.setAdapter(categoryAdapter);

        // Setup activity spinner
        activitySpinner = (Spinner) view.findViewById(R.id.activity_spinner);
        final String[] orientationSpinnerElements = new String[]{SITTING_STRAIGHT, SITTING_FORWARD,
                SITTING_BACKWARD, STANDING, LYING_ON_BACK, LYING_STOMACH, LYING_RIGHT, LYING_LEFT};
        final String[] indoorOutdoorSpinnerElements = new String[]{OUTDOOR, INDOOR, HALF_OPEN};
        final String[] transportSpinnerElements = new String[]{BUS, BIKE, TRAIN, WALKING, WALKING_100_STEPS, WALKING_UPSTAIRS,
                WALKING_DOWNSTAIRS, RUNNING};
        final String[] coughingSpinnerElements = new String[]{COUGHING, NONCOUGHING};

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] activitySpinnerElements = new String[]{};
                switch (position) {
                    case 0:
                        activitySpinnerElements = orientationSpinnerElements;
                        break;
                    case 1:
                        activitySpinnerElements = indoorOutdoorSpinnerElements;
                        break;
                    case 2:
                        activitySpinnerElements = transportSpinnerElements;
                        break;
                    case 3:
                        activitySpinnerElements = coughingSpinnerElements;
                }
                ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_spinner_dropdown_item, activitySpinnerElements);
                activitySpinner.setAdapter(activityAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Set empty activity spinner
                activitySpinner.setAdapter(
                        new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item,
                                new String[]{}));
            }
        });

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
                if (mIsRespeckRecording || mIsInOutRecording) {
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

        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);
        ((MainActivity) getActivity()).registerAirspeckDataObserver(this);

        indoorOutdoorPredictor = new IndoorOutdoorPredictor(getActivity());

        return view;
    }

    private void startRecording() {
        // Start recording
        mSubjectName = nameTextField.getText().toString();
        if (!mSubjectName.equals("")) {
            mActivity = activitySpinner.getSelectedItem().toString();

            if (categorySpinner.getSelectedItem().toString().equals(INOUT)) {
                mIsInOutRecording = true;
            } else {
                mIsRespeckRecording = true;
            }

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

                if (mIsRespeckRecording) {
                    mIsRespeckRecording = false;
                    saveRespeckRecording();
                } else if (mIsInOutRecording) {
                    mIsInOutRecording = false;
                    saveInOutRecording();
                }

                // Change button label to tell the user that the recording has stopped
                mStartStopButton.setText(R.string.button_text_start_recording);
                mStartStopButton.setEnabled(true);
                mStartStopButton.setBackgroundColor(
                        ContextCompat.getColor(getActivity(), R.color.md_grey_300));

            }
        }, 2000);
    }

    private void saveRespeckRecording() {
        String filename = Utils.getInstance().getDataDirectory(getActivity()) + Constants.LOGGING_DIRECTORY_NAME +
                new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(new Date()) +
                " Activity RESpeck Logs " + Utils.getInstance().getConfig(getActivity()).get(
                Constants.Config.RESPECK_UUID).replace(":", "") +
                ".csv";

        OutputStreamWriter respeckWriter;

        // Create file for current day and append header, if it doesn't exist yet
        try {
            if (!new File(filename).exists()) {
                respeckWriter = new OutputStreamWriter(
                        new FileOutputStream(filename, true));
                respeckWriter.append(Constants.ACTIVITY_RECORDING_HEADER).append("\n");
                respeckWriter.flush();
            } else {
                respeckWriter = new OutputStreamWriter(new FileOutputStream(filename, true));
            }

            if (outputData.length() != 0) {
                respeckWriter.write(outputData.toString());
                Toast.makeText(getActivity(),
                        "Recording saved",
                        Toast.LENGTH_LONG).show();

                // Add a line which indicates end of recording
                respeckWriter.append("end of record\n").flush();
            } else {
                Toast.makeText(getActivity(),
                        "No data received from RESpeck in recording period",
                        Toast.LENGTH_LONG).show();
            }
            respeckWriter.close();

            outputData = new StringBuilder();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error while writing to RESpeck logging file: " +
                    e.getMessage());
        }
    }

    private void saveInOutRecording() {
        String filename = Utils.getInstance().getDataDirectory(
                getActivity()) + Constants.LOGGING_DIRECTORY_NAME +
                new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(new Date()) +
                " IndoorPrediction " + airspeckUUID.replace(":", "") +
                ".csv";

        OutputStreamWriter predictionWriter;

        // If file doesn't exist, create a new one and add header
        try {
            if (!new File(filename).exists()) {
                // Close old connection if there was one

                Log.i("AirspeckPacketHandler", "Indoor prediction data file created with header");
                // Open new connection to new file
                predictionWriter = new OutputStreamWriter(new FileOutputStream(filename, true));
                predictionWriter.append(Constants.INDOOR_PREDICTION_HEADER + "\n");
                predictionWriter.flush();
            } else {
                predictionWriter = new OutputStreamWriter(new FileOutputStream(filename, true));
            }

            if (outputData.length() != 0) {
                predictionWriter.write(outputData.toString());
                Toast.makeText(getActivity(),
                        "Recording saved",
                        Toast.LENGTH_LONG).show();

                // Add a line which indicates end of recording
                predictionWriter.append("end of record\n").flush();
            } else {
                Toast.makeText(getActivity(),
                        "No data received from Airspeck in recording period",
                        Toast.LENGTH_LONG).show();
            }
            predictionWriter.close();
            outputData = new StringBuilder();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error while writing to indoor prediction file: " +
                    e.getMessage());
        }
    }


    private void cancelRecording() {
        // Reset timer
        countUpTimer.stop();
        countUpTimer.reset();
        timerText.setText("00:00");

        // Reset output data and with that, discard previously stored data
        outputData = new StringBuilder();

        mIsRespeckRecording = false;
        mIsInOutRecording = false;

        // Change button label to tell the user that the recording has stopped
        mStartStopButton.setText(R.string.button_text_start_recording);
        mStartStopButton.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.md_grey_300));

        mCancelButton.setEnabled(false);
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        // If we are currently recording (button is pressed), store the received values
        if (mIsRespeckRecording) {
            String output = mSubjectName + "," + data.getPhoneTimestamp() + "," +
                    data.getAccelX() + "," + data.getAccelY() + "," + data.getAccelZ() + "," +
                    data.getBreathingSignal() + "," + mActivity + "," + data.getActivityLevel() + "," +
                    data.getActivityType() + "\n";
            outputData.append(output);
        }
    }

    @Override
    public void updateAirspeckData(AirspeckData data) {
        indoorOutdoorPredictor.updateScores(data, getActivity());
        if (mIsInOutRecording) {
            outputData.append(
                    Utils.getUnixTimestamp() + ";" + mSubjectName + ";" + indoorOutdoorPredictor.toFileString() + ";" +
                            activitySpinner.getSelectedItem().toString() + "\n");
        }
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
        ((MainActivity) getActivity()).unregisterAirspeckDataObserver(this);
        super.onDestroy();
    }

}
