package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.adapters.ReadingItemArrayAdapter;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.models.ReadingItem;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;
import com.specknet.airrespeck.views.BreathingGraphView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment created for the Windmill project which contains the breathing rate, minute average breathing rate,
 * a graph of the breathing signal, an icon indicating the current activity type, and buttons to
 * start the rehab app and diary app.
 */

public class SupervisedRESpeckReadingsIcons extends ConnectionOverlayFragment implements RESpeckDataObserver {

    TextView breathingRateText;
    TextView averageBreathingRateText;
    TextView stepCountText;
    TextView frequencyText;
    TextView detectedAct;
//    TextView battLevelText;
//    TextView chargingText;

    ImageView activityIcon;

    private BreathingGraphView mBreathingGraphView;

    private Utils mUtils;
    private Map<String, String> mLoadedConfig;
    // for Firebase testing
    private Button crashButton;

    //ping add:
    // Breathing acceleration text values
    private ArrayList<ReadingItem> mReadingItems;
    private ReadingItemArrayAdapter mListViewAdapter;


    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SupervisedRESpeckReadingsIcons() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);

        //set action bar title
        getActivity().setTitle("Live Reading");
        getActivity().setTitleColor(0x000000);

        mReadingItems = new ArrayList<>();
        mReadingItems.add(new ReadingItem("x", "", 0f));
        mReadingItems.add(new ReadingItem("y", "", 0f));
        mReadingItems.add(new ReadingItem("z", "", 0f));
        mReadingItems.add(new ReadingItem("Act level", "", 0f));
        mListViewAdapter = new ReadingItemArrayAdapter(getActivity(), mReadingItems);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_respeck_readings_icons, container, false);

        // Load breathing textviews and icon
        breathingRateText = (TextView) view.findViewById(R.id.text_breathing);
        averageBreathingRateText = (TextView) view.findViewById(R.id.text_breathing_average);
        activityIcon = (ImageView) view.findViewById(R.id.activity_icon);
//        stepCountText = (TextView) view.findViewById(R.id.text_step_count);
        frequencyText = (TextView) view.findViewById(R.id.text_frequency);
        detectedAct = (TextView) view.findViewById(R.id.detected_activity);
//        battLevelText = (TextView) view.findViewById(R.id.text_batt_level);
//        chargingText = (TextView) view.findViewById(R.id.text_charging_status);

        // Create new graph
        mBreathingGraphView = new BreathingGraphView(getActivity());

        // Inflate breathing graph into container
        FrameLayout graphFrame = (FrameLayout) view.findViewById(R.id.breathing_graph_container);
        graphFrame.addView(mBreathingGraphView);

        mBreathingGraphView.startBreathingGraphUpdates();

        // Register this fragment as connection state observer
        ((MainActivity) getActivity()).registerConnectionStateObserver(this);

        // hide activity type icon if specified in pairing options
        mUtils = Utils.getInstance();
        mLoadedConfig = mUtils.getConfig(getActivity());
        if (mLoadedConfig.containsKey(Constants.Config.HIDE_ACTIVITY_TYPE)) {
            if (Boolean.parseBoolean(mLoadedConfig.get(Constants.Config.HIDE_ACTIVITY_TYPE))) {
                activityIcon.setVisibility(View.GONE);
            }
        }

        // Attach the adapter to a ListView for displaying the RESpeck readings
        ListView mListView = (ListView) view.findViewById(R.id.acc_readings_list);
        mListView.setAdapter(mListViewAdapter);

        return view;
    }

    private void updateReadings(RESpeckLiveData data) {
        // Update readings and activity symbol
        // Set breathing rate text to currently calculated rates

        String suffix = ((MainActivity) getActivity()).getBreathingSuffix();

        if (!Float.isNaN(data.getBreathingRate())) {
            breathingRateText.setText(String.format(Locale.UK, "%.2f " + suffix, data.getBreathingRate()));
        }
        averageBreathingRateText.setText(String.format(Locale.UK, "%.2f " + suffix, data.getAvgBreathingRate()));
//        stepCountText.setText(Integer.toString(data.getMinuteStepCount()));

        // Update the frequency once a minute
        // only if it's different than 0
        if(data.getFrequency() != 0) {
            frequencyText.setText(String.format(Locale.UK, "%.2f" + " Hz", data.getFrequency()));
//            frequencyText.setText(Float.toString(data.getFrequency()) + " Hz");
        }

        // update battery level and charging status
//        if (data.getBattLevel() != -1) {
//            battLevelText.setVisibility(View.VISIBLE);
//            battLevelText.setText(data.getBattLevel() + "%");
//        }
//        else {
//            battLevelText.setVisibility(View.INVISIBLE);
//        }
//
//        if (data.getChargingStatus()) {
//            chargingText.setVisibility(View.VISIBLE);
//        }
//        else {
//            chargingText.setVisibility(View.INVISIBLE);
//        }

        // Set activity icon to reflect currently predicted activity
        //ping add: reflect label
        switch (data.getActivityType()) {
            case Constants.ACTIVITY_LYING:
                activityIcon.setImageResource(R.drawable.vec_lying);
                detectedAct.setText("Lying");
                break;
            case Constants.ACTIVITY_LYING_DOWN_LEFT:
                activityIcon.setImageResource(R.drawable.lying_to_left);
                detectedAct.setText("Lying Left");
                break;
            case Constants.ACTIVITY_LYING_DOWN_RIGHT:
                activityIcon.setImageResource(R.drawable.lying_to_right);
                detectedAct.setText("Lying Right");
                break;
            case Constants.ACTIVITY_LYING_DOWN_STOMACH:
                activityIcon.setImageResource(R.drawable.lying_stomach);
                detectedAct.setText("Lying on Stomach");
                break;
            case Constants.ACTIVITY_SITTING_BENT_BACKWARD:
                activityIcon.setImageResource(R.drawable.sitting_backward);
                detectedAct.setText("Sitting Bent Backward");
                break;
            case Constants.ACTIVITY_SITTING_BENT_FORWARD:
                activityIcon.setImageResource(R.drawable.sitting_forward);
                detectedAct.setText("Sitting Bent Forward");
                break;
            case Constants.ACTIVITY_STAND_SIT:
                activityIcon.setImageResource(R.drawable.vec_standing_sitting);
                detectedAct.setText("Stand/Sit");
                break;
            case Constants.ACTIVITY_MOVEMENT:
                activityIcon.setImageResource(R.drawable.movement);
                detectedAct.setText("Moving");
                break;
            case Constants.ACTIVITY_WALKING:
                activityIcon.setImageResource(R.drawable.vec_walking);
                detectedAct.setText("Walking");
                break;
            case Constants.SS_COUGHING:
                activityIcon.setImageResource(R.drawable.ic_cough);
                detectedAct.setText("Coughing");
                break;
            default:
                activityIcon.setImageResource(R.drawable.vec_xmark);
        }

        // Only update readings if they are not NaN
        mReadingItems.get(0).value = data.getAccelX();
        mReadingItems.get(1).value = data.getAccelY();
        mReadingItems.get(2).value = data.getAccelZ();
        mReadingItems.get(3).value = data.getActivityLevel();

        mListViewAdapter.notifyDataSetChanged();

    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        Log.i("RESpeckReadings", "updateRESpeckData");
        Log.i("RESpeckReadings", data.toString());
        // Update the graph
        mBreathingGraphView.addToBreathingGraphQueue(data);

        // Update the other readings
        updateReadings(data);
    }

    @Override
    public void onDetach() {
        mBreathingGraphView.stopBreathingGraphUpdates();
        super.onDetach();
    }
    @Override
    public void onDestroy() {
        // Unregister this class as observer. If we haven't observed, nothing happens
        ((MainActivity) getActivity()).unregisterRESpeckDataObserver(this);
        super.onDestroy();
    }

}


