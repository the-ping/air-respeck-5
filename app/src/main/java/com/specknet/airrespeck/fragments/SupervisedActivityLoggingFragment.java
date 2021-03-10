package com.specknet.airrespeck.fragments;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

//import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Recording activity data
 */

public class SupervisedActivityLoggingFragment extends ConnectionOverlayFragment implements RESpeckDataObserver, AirspeckDataObserver {

    private static final String TAG = "ActivityLogging";
    private OutputStreamWriter mWriter;

    private boolean mIsRespeckRecording;
    private boolean mIsInOutRecording;
    private String mActivity;
    private String mSubjectName;

    private Button mStartStopButton;
    private Button mCancelButton;
    private Button mUploadButton;

    //ping add:
    private LinearLayout mCancelLayout;
    private LinearLayout mUploadLayout;
    private ImageView recording_image;
    private ImageView cancel_image;
    private ImageView upload_image;

    private static final int SITBREATHE = 0;
    private static final int SITBREATHEDEEP = 1;
    private static final int HYPE = 2;


    private TextView nameTextField;
//    private Spinner categorySpinner;
//    private Spinner activitySpinner;
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

    // FOR PATIENT RECORDING
    private final String SIT_BREATHE = "Sitting straight and breathing";
    private final String SIT_BREATHE_DEEP = "Sitting straight and breathing deeply";
    private final String SIT_COUGH = "Sitting straight and coughing";
    private final String SIT_TALK = "Sitting straight and talking";
    private final String LIE_BREATHE = "Lying down on back and breathing";
    private final String LIE_COUGH = "Lying down on back and coughing";
    private final String WALK = "Walking at a normal pace";
    private final String WALK_SLOW = "Walking slowly";
    private final String SWING = "Swinging front to back while sitting down";
    private final String MOVE = "Sudden movement";
    private final String SIT_HYPER = "Sitting straight and hyperventilating";

    private final String LOG_TAG = "RESpeckActivityLogging";

    private IndoorOutdoorPredictor indoorOutdoorPredictor;
    private OutputStreamWriter predictionWriter;

    private FirebaseStorage storage;

    long totalBytesForUploading = 0L;
    long totalBytesTransferred = 0L;
    int totalFilesToUpload = 0;
    int totalFilesAlreadyUploaded = 0;

//    LinearLayout progressBarContainer;
    ProgressBar progressBar;
    TextView progressBarLabel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity_logging_respeck, container, false);

        //set actionbar title
        getActivity().setTitle("Activity Logging");

        // Load config variables
        Map<String, String> config = Utils.getInstance().getConfig(getActivity());
        airspeckUUID = config.get(Constants.Config.AIRSPECKP_UUID);
        subjectID = config.get(Constants.Config.SUBJECT_ID);
        androidID = Settings.Secure.getString(getActivity().getContentResolver(),
                Settings.Secure.ANDROID_ID);

//        progressBarContainer = (LinearLayout) view.findViewById(R.id.progress_bar_container);
        progressBar = (ProgressBar) view.findViewById(R.id.upload_progress_bar_act_log);
        progressBarLabel = (TextView) view.findViewById(R.id.progress_bar_label_act_log);

//        progressBar.setVisibility(View.INVISIBLE);
//        progressBarLabel.setVisibility(View.INVISIBLE);
//
//        nameTextField = (TextView) view.findViewById(R.id.name_text_field);
//        nameTextField.setText("Subject ID: " + subjectID);

        // Setup category spinner
//        categorySpinner = (Spinner) view.findViewById(R.id.category_spinner);

//        String[] categorySpinnerElements = new String[]{ORIENTATION, INOUT, TRANSPORT, COUGHING};
//        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getActivity(),
//                android.R.layout.simple_spinner_dropdown_item, categorySpinnerElements);
//        categorySpinner.setAdapter(categoryAdapter);

        // Setup activity spinner
//        activitySpinner = (Spinner) view.findViewById(R.id.activity_spinner);
//        final String[] orientationSpinnerElements = new String[]{SITTING_STRAIGHT, SITTING_FORWARD,
//                SITTING_BACKWARD, STANDING, LYING_ON_BACK, LYING_STOMACH, LYING_RIGHT, LYING_LEFT};
//        final String[] indoorOutdoorSpinnerElements = new String[]{OUTDOOR, INDOOR, HALF_OPEN};
//        final String[] transportSpinnerElements = new String[]{BUS, BIKE, TRAIN, WALKING, WALKING_100_STEPS, WALKING_UPSTAIRS,
//                WALKING_DOWNSTAIRS, RUNNING};
//        final String[] coughingSpinnerElements = new String[]{COUGHING, NONCOUGHING};

        //ping uncommented:
//        final String[] patientSpinnerElements = new String[]{SIT_BREATHE, SIT_BREATHE_DEEP, SIT_COUGH, SIT_HYPER, SIT_TALK,
//                LIE_BREATHE, LIE_COUGH, WALK, WALK_SLOW, MOVE, SWING};
//        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(getActivity(),
//                android.R.layout.simple_spinner_dropdown_item, patientSpinnerElements);
//        activitySpinner.setAdapter(activityAdapter);


//        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                String[] activitySpinnerElements = new String[]{};
//                switch (position) {
//                    case 0:
//                        activitySpinnerElements = orientationSpinnerElements;
//                        break;
//                    case 1:
//                        activitySpinnerElements = indoorOutdoorSpinnerElements;
//                        break;
//                    case 2:
//                        activitySpinnerElements = transportSpinnerElements;
//                        break;
//                    case 3:
//                        activitySpinnerElements = coughingSpinnerElements;
//                        break;
//                }
//                ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(getActivity(),
//                        android.R.layout.simple_spinner_dropdown_item, activitySpinnerElements);
//                activitySpinner.setAdapter(activityAdapter);
//            }

//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Set empty activity spinner
//                activitySpinner.setAdapter(
//                        new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item,
//                                new String[]{}));
//            }
//        });


        // Load buttons
        mStartStopButton = (Button) view.findViewById(R.id.record_button);

        //ping uncommented:
        mStartStopButton.setBackgroundColor(0xffffff);
        recording_image = view.findViewById(R.id.record_imagebutton);
        mCancelButton = (Button) view.findViewById(R.id.cancel_button);
        cancel_image = view.findViewById(R.id.cancel_imagebutton);
        mUploadButton = (Button) view.findViewById(R.id.upload_button);
        upload_image = view.findViewById(R.id.upload_imagebutton);
        mCancelLayout = view.findViewById(R.id.cancel_layout);
        mUploadLayout = view.findViewById(R.id.upload_layout);

//        mCancelButton.setTextColor(0x065A61);
//        mCancelLayout.setBackgroundResource(R.drawable.background_rounded_lightgrey);


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

        mStartStopButton.setOnClickListener(v -> {
            if (mIsRespeckRecording || mIsInOutRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelRecording();
            }
        });

        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadRecording();
            }
        });

        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);
        ((MainActivity) getActivity()).registerAirspeckDataObserver(this);

        indoorOutdoorPredictor = new IndoorOutdoorPredictor(getActivity());

        storage = FirebaseStorage.getInstance();

        // load radio buttons
        RadioButton r_sitbreathe = view.findViewById(R.id.sitbreathe_rad);
        RadioButton r_sitbreathedeep = view.findViewById(R.id.sitbreathedeep_rad);
        RadioButton r_sithyper = view.findViewById(R.id.sithyper_rad);
        RadioButton r_sitcough = view.findViewById(R.id.sitcough_rad);
        RadioButton r_sittalk = view.findViewById(R.id.sittalk_rad);
        RadioButton r_sitfrontback = view.findViewById(R.id.sitfrontback_rad);
        RadioButton r_liebreathe = view.findViewById(R.id.liebreathe_rad);
        RadioButton r_liecough = view.findViewById(R.id.liecough_rad);
        RadioButton r_walknorm = view.findViewById(R.id.walknormal_rad);
        RadioButton r_walkslow = view.findViewById(R.id.walkslow_rad);
        RadioButton r_movesudden = view.findViewById(R.id.movesudden_rad);

        // set up onClick radio buttons
        r_sitbreathe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r_sitbreathe.setChecked(true);
                r_sitbreathedeep.setChecked(false);
                r_sithyper.setChecked(false);
                r_sitcough.setChecked(false);
                r_sittalk.setChecked(false);
                r_sitfrontback.setChecked(false);
                r_liebreathe.setChecked(false);
                r_liecough.setChecked(false);
                r_walknorm.setChecked(false);
                r_walkslow.setChecked(false);
                r_movesudden.setChecked(false);
                mActivity = SIT_BREATHE;
            }
        });

        r_sitbreathedeep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r_sitbreathe.setChecked(false);
                r_sitbreathedeep.setChecked(true);
                r_sithyper.setChecked(false);
                r_sitcough.setChecked(false);
                r_sittalk.setChecked(false);
                r_sitfrontback.setChecked(false);
                r_liebreathe.setChecked(false);
                r_liecough.setChecked(false);
                r_walknorm.setChecked(false);
                r_walkslow.setChecked(false);
                r_movesudden.setChecked(false);
                mActivity = SIT_BREATHE_DEEP;
            }
        });

        r_sithyper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r_sitbreathe.setChecked(false);
                r_sitbreathedeep.setChecked(false);
                r_sithyper.setChecked(true);
                r_sitcough.setChecked(false);
                r_sittalk.setChecked(false);
                r_sitfrontback.setChecked(false);
                r_liebreathe.setChecked(false);
                r_liecough.setChecked(false);
                r_walknorm.setChecked(false);
                r_walkslow.setChecked(false);
                r_movesudden.setChecked(false);
                mActivity = SIT_HYPER;
            }
        });
        r_sitcough.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r_sitbreathe.setChecked(false);
                r_sitbreathedeep.setChecked(false);
                r_sithyper.setChecked(false);
                r_sitcough.setChecked(true);
                r_sittalk.setChecked(false);
                r_sitfrontback.setChecked(false);
                r_liebreathe.setChecked(false);
                r_liecough.setChecked(false);
                r_walknorm.setChecked(false);
                r_walkslow.setChecked(false);
                r_movesudden.setChecked(false);
                mActivity = SIT_COUGH;
            }
        });

        r_sittalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r_sitbreathe.setChecked(false);
                r_sitbreathedeep.setChecked(false);
                r_sithyper.setChecked(false);
                r_sitcough.setChecked(false);
                r_sittalk.setChecked(true);
                r_sitfrontback.setChecked(false);
                r_liebreathe.setChecked(false);
                r_liecough.setChecked(false);
                r_walknorm.setChecked(false);
                r_walkslow.setChecked(false);
                r_movesudden.setChecked(false);
                mActivity = SIT_TALK;
            }
        });

        r_sitfrontback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r_sitbreathe.setChecked(false);
                r_sitbreathedeep.setChecked(false);
                r_sithyper.setChecked(false);
                r_sitcough.setChecked(false);
                r_sittalk.setChecked(false);
                r_sitfrontback.setChecked(true);
                r_liebreathe.setChecked(false);
                r_liecough.setChecked(false);
                r_walknorm.setChecked(false);
                r_walkslow.setChecked(false);
                r_movesudden.setChecked(false);
                mActivity = SWING;
            }
        });
        r_liebreathe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r_sitbreathe.setChecked(false);
                r_sitbreathedeep.setChecked(false);
                r_sithyper.setChecked(false);
                r_sitcough.setChecked(false);
                r_sittalk.setChecked(false);
                r_sitfrontback.setChecked(false);
                r_liebreathe.setChecked(true);
                r_liecough.setChecked(false);
                r_walknorm.setChecked(false);
                r_walkslow.setChecked(false);
                r_movesudden.setChecked(false);
                mActivity = LIE_BREATHE;
            }
        });
        r_liecough.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r_sitbreathe.setChecked(false);
                r_sitbreathedeep.setChecked(false);
                r_sithyper.setChecked(false);
                r_sitcough.setChecked(false);
                r_sittalk.setChecked(false);
                r_sitfrontback.setChecked(false);
                r_liebreathe.setChecked(false);
                r_liecough.setChecked(true);
                r_walknorm.setChecked(false);
                r_walkslow.setChecked(false);
                r_movesudden.setChecked(false);
                mActivity = LIE_COUGH;
            }
        });
        r_walknorm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r_sitbreathe.setChecked(false);
                r_sitbreathedeep.setChecked(false);
                r_sithyper.setChecked(false);
                r_sitcough.setChecked(false);
                r_sittalk.setChecked(false);
                r_sitfrontback.setChecked(false);
                r_liebreathe.setChecked(false);
                r_liecough.setChecked(false);
                r_walknorm.setChecked(true);
                r_walkslow.setChecked(false);
                r_movesudden.setChecked(false);
                mActivity = WALK;
            }
        });
        r_walkslow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r_sitbreathe.setChecked(false);
                r_sitbreathedeep.setChecked(false);
                r_sithyper.setChecked(false);
                r_sitcough.setChecked(false);
                r_sittalk.setChecked(false);
                r_sitfrontback.setChecked(false);
                r_liebreathe.setChecked(false);
                r_liecough.setChecked(false);
                r_walknorm.setChecked(false);
                r_walkslow.setChecked(true);
                r_movesudden.setChecked(false);
                mActivity = WALK_SLOW;
            }
        });
        r_movesudden.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r_sitbreathe.setChecked(false);
                r_sitbreathedeep.setChecked(false);
                r_sithyper.setChecked(false);
                r_sitcough.setChecked(false);
                r_sittalk.setChecked(false);
                r_sitfrontback.setChecked(false);
                r_liebreathe.setChecked(false);
                r_liecough.setChecked(false);
                r_walknorm.setChecked(false);
                r_walkslow.setChecked(false);
                r_movesudden.setChecked(true);
                mActivity = MOVE;
            }
        });


        return view;
    }


    private void startRecording() {
        // Start recording
//        mSubjectName = nameTextField.getText().toString();
        progressBarLabel.setVisibility(View.INVISIBLE);
        mSubjectName = subjectID;

        if (!mSubjectName.equals("")) {
//            mActivity = activitySpinner.getSelectedItem().toString();

            mIsInOutRecording = false;
            mIsRespeckRecording = true;

            // Change button label and color to tell the user that we are recording
            mStartStopButton.setText(R.string.button_text_stop_recording);
            mStartStopButton.setBackgroundColor(0x00ffff);
//            mStartStopButton.setBackgroundColor(
//                    ContextCompat.getColor(getActivity(), R.color.md_green_300));
            recording_image.setImageResource(R.drawable.ic_diskette);
            mCancelButton.setEnabled(true);
            mCancelLayout.setBackgroundResource(R.drawable.rounded_button);
//            mCancelButton.setTextColor(0xFFFFFF);
            cancel_image.setImageResource(R.drawable.ic_baseline_delete_24);
            mUploadButton.setEnabled(false);
            upload_image.setImageResource(R.drawable.ic_baseline_cloud_upload_clicked);
            mUploadLayout.setBackgroundResource(R.drawable.background_rounded_lightgrey_lined);
//            activitySpinner.setEnabled(false);
//            activitySpinner.setClickable(false);


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
        mStartStopButton.setBackgroundColor(0x00ffff);
        mCancelButton.setEnabled(false);
//        mCancelButton.setTextColor(0x065A61);
        cancel_image.setImageResource(R.drawable.ic_baseline_delete_clicked);
        mCancelLayout.setBackgroundResource(R.drawable.background_rounded_lightgrey_lined);
        mUploadButton.setEnabled(true);
        upload_image.setImageResource(R.drawable.ic_baseline_cloud_upload_24);
        mUploadLayout.setBackgroundResource(R.drawable.rounded_button);
        progressBarLabel.setVisibility(View.INVISIBLE);

        final Handler handler = new Handler();
        handler.postDelayed(() -> {

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
            mStartStopButton.setBackgroundColor(0x00ffff);
            recording_image.setImageResource(R.drawable.ic_baseline_fiber_manual_record_24);
//            mStartStopButton.setBackgroundColor(
//                    ContextCompat.getColor(getActivity(), R.color.md_grey_300));

        }, 2000);
    }

    private void saveRespeckRecording() {
        //ping add:
        String currentActivity = mActivity;
//        String currentActivity = activitySpinner.getSelectedItem().toString();
        String filename = Utils.getInstance().getDataDirectory(getActivity()) + Constants.LOGGING_DIRECTORY_NAME +
                new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.UK).format(new Date()) +
                " Activity RESpeck Logs " + currentActivity + " " + Utils.getInstance().getConfig(getActivity()).get(
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
        recording_image.setImageResource(R.drawable.ic_baseline_fiber_manual_record_24);
//        mStartStopButton.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.md_grey_300));

        mCancelButton.setEnabled(false);
//        mCancelButton.setTextColor(0x065A61);
        cancel_image.setImageResource(R.drawable.ic_baseline_delete_clicked);
        mCancelLayout.setBackgroundResource(R.drawable.background_rounded_lightgrey_lined);

        mUploadButton.setEnabled(true);
    }

    private void updateProgressBar(long bytesTransferred) {

        if(progressBar.getVisibility() == View.INVISIBLE) {
            progressBar.setVisibility(View.VISIBLE);
            progressBarLabel.setVisibility(View.VISIBLE);
            progressBarLabel.setText("Upload progress");
        }
        totalBytesTransferred += bytesTransferred;

        double progress = (100.0 * totalBytesTransferred) / totalBytesForUploading;

        Log.d(TAG, "updateProgressBar: progress = " + progress);
        progressBar.setProgress((int) progress);

        if(progress >= 100) {
            displaySuccessMessage();
        }
    }

    private void displaySuccessMessage() {
        progressBar.setVisibility(View.INVISIBLE);
        progressBarLabel.setText("Upload complete!");
        mUploadButton.setEnabled(true);
        upload_image.setImageResource(R.drawable.ic_baseline_cloud_upload_24);
        mUploadLayout.setBackgroundResource(R.drawable.rounded_button);
    }

    private void displayAlreadyUploaded() {
        progressBar.setVisibility(View.INVISIBLE);

        progressBarLabel.setText("Data already uploaded!");
        progressBarLabel.setVisibility(View.VISIBLE);
    }


    private void uploadRecording() {

        mUploadButton.setEnabled(false);
        upload_image.setImageResource(R.drawable.ic_baseline_cloud_upload_clicked);
        mUploadLayout.setBackgroundResource(R.drawable.background_rounded_lightgrey_lined);

        StorageReference storageRef = storage.getReferenceFromUrl("gs://specknet-pyramid-test.appspot.com");

        // upload content
        String folderName = Utils.getInstance().getDataDirectory(getActivity()) + "/" + Constants.LOGGING_DIRECTORY_NAME;
        Log.d(TAG, "uploadRecording: folderName = " + folderName);

        File folderFile = new File(folderName);
        File[] filesInFolder = folderFile.listFiles();

        if (filesInFolder != null) {
            Log.d(TAG, "uploadRecording: Folder size = " + filesInFolder.length);

            for (int i = 0; i < filesInFolder.length; i++) {
               File currentFile = filesInFolder[i];
                Log.d(TAG, "uploadRecording: currentFile = " + currentFile.getName());

                // check if file contains the substring 'Activity RESpeck Logs' in which case it is a calibration file
                if(currentFile.getName().contains("Activity RESpeck Logs")) {
                    Log.d(TAG, "uploadRecording: file " + currentFile.getName() + " is an activity recording file");
                    totalFilesToUpload += 1;
                    
                    // upload this file
                    String currentFileAbsPath = currentFile.getAbsolutePath();
                    Uri fileUri = Uri.fromFile(currentFile);
                    String fileExtension = currentFileAbsPath.substring(currentFileAbsPath.lastIndexOf(".") + 1);
                
                    StorageMetadata metadata = new StorageMetadata.Builder()
                            .setContentType("AirRespeck/" + fileExtension)
                            .build();
                    
                    // upload file and metadata to the path
                    int indexOfAirRespeck = fileUri.getPath().indexOf("AirRespeck");
                    String pathFromAirRespeck = fileUri.getPath().substring(indexOfAirRespeck + "AirRespeck".length());

                    Task<StorageMetadata> uploadRef = storageRef.child("AirRespeck/" + pathFromAirRespeck)
                            .getMetadata()
                            .addOnSuccessListener(storageMetadata -> {
                                Log.d(TAG, "uploadRecording: File already exists");
                                totalFilesAlreadyUploaded += 1;

                                if(totalFilesAlreadyUploaded == totalFilesToUpload) {
                                    displayAlreadyUploaded();
                                }
                            })
                            .addOnFailureListener(exception -> {
                                Log.d(TAG, "uploadRecording: File does not exist, must upload");

                                UploadTask uploadTask = storageRef.child("AirRespeck/" + pathFromAirRespeck).putFile(fileUri, metadata);

                                totalBytesForUploading += currentFile.length();
                                AtomicLong lastBytesUploaded = new AtomicLong(0L);

                                uploadTask
                                        .addOnProgressListener(snapshot -> {

                                            if (lastBytesUploaded.get() == 0L) {
                                                Log.d(TAG, "uploadRecording: uploaded " + snapshot.getBytesTransferred());
                                                updateProgressBar(snapshot.getBytesTransferred());
                                            }
                                            else {
                                                long bytesTransferredSinceLastTime = snapshot.getBytesTransferred() - lastBytesUploaded.get();
                                                updateProgressBar(bytesTransferredSinceLastTime);
                                            }

                                            lastBytesUploaded.set(snapshot.getBytesTransferred());

                                            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                                            Log.d(TAG, "Upload for " + currentFile.getName() + " is " + progress + "% done");
                                        })
                                        .addOnPausedListener(snapshot -> Log.d(TAG, "onPaused: Upload is paused"))
                                        .addOnFailureListener(e -> {
                                            Log.d(TAG, "uploadRecording: failure when uploading");
                                            exception.printStackTrace();
                                        })
                                        .addOnSuccessListener(taskSnapshot -> Log.d(TAG, "uploadRecording: Upload Succesful!"));
                            });
                    
                }

            }

        }

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
//                    Utils.getUnixTimestamp() + ";" + mSubjectName + ";" + indoorOutdoorPredictor.toFileString() + ";" +
//                            activitySpinner.getSelectedItem().toString() + "\n");
                    Utils.getUnixTimestamp() + ";" + mSubjectName + ";" + indoorOutdoorPredictor.toFileString() + ";" +
                            mActivity + "\n");


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
