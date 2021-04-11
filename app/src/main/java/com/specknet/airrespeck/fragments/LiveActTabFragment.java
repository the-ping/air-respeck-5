package com.specknet.airrespeck.fragments;

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.ConnectionStateObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.activities.SubjectActivity;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.models.ReadingItem;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.security.auth.Subject;

///**
// * A simple {@link Fragment} subclass.
// * Use the {@link LiveActTabFragment#newInstance} factory method to
// * create an instance of this fragment.
// */
public class LiveActTabFragment extends ConnectionOverlayFragment implements RESpeckDataObserver {

    // Roboto font
    TextView t1,t2,t4,t5,t6;

    // Time activity spent on
    private ArrayList<ReadingItem> mReadingItems;
    private final String KEY_HOUR = "hour";
    private final String KEY_DAY = "day";
    private final String KEY_WEEK = "week";

    private String mHourStatsString = "Loading data";
    private String mDayStatsString = "Loading data";
    private String mWeekStatsString = "Loading data";

    private TextView sittime_text;
    private TextView walktime_text;
    private TextView lietime_text;
    private int sit_timeval;
    private int walk_timeval;
    private int lie_timeval;
    private int day_sit;
    private int day_walk;
    private int day_lie;

    // Icons
    TextView sbj_averageBreathingRateText;
    ImageView sbj_activityIcon;
    TextView detectedAct;

    public LiveActTabFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);

        //set action bar title
        getActivity().setTitle("Live Readings");
        getActivity().setTitleColor(0x000000);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_live_act_tab, container, false);

        // setting roboto font
        t1 = (TextView) view.findViewById(R.id.live_act_t1);
        setRobotoFont(t1);
        t2 = (TextView) view.findViewById(R.id.activity_label);
        setRobotoFont(t2);
        sbj_averageBreathingRateText = (TextView) view.findViewById(R.id.sbj_text_breathing_average);
        setRobotoFont(sbj_averageBreathingRateText);
//        t4 = (TextView) view.findViewById(R.id.live_act_t4);
//        setRobotoFont(t4);
        t5 = (TextView) view.findViewById(R.id.live_act_t5);
        setRobotoFont(t5);


        // Load breathing textviews and icon
        sbj_averageBreathingRateText = (TextView) view.findViewById(R.id.sbj_text_breathing_average);
        sbj_activityIcon = (ImageView) view.findViewById(R.id.sbj_activity_icon);
        detectedAct = (TextView) view.findViewById(R.id.activity_label);

        // Register this fragment as connection state observer
        ((MainActivity) getActivity()).registerConnectionStateObserver((ConnectionStateObserver) this);



        return view;

    }




    public void setRobotoFont(TextView t1) {
        Typeface roboto = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Medium.ttf");
        t1.setTypeface(roboto);
    }

    private void updateTimeReadings() {
        if (mReadingItems != null) {
            mReadingItems.get(0).stringValue = mHourStatsString;
            mReadingItems.get(1).stringValue = mDayStatsString;
            mReadingItems.get(2).stringValue = mWeekStatsString;
        }
    }

    public void updateReadings(RESpeckLiveData data) {

        // Only update view if fragment has been created!
        // Set breathing rate text to currently calculated rates
        String suffix = ((MainActivity) getActivity()).getBreathingSuffix();
//        if (!Float.isNaN(data.getBreathingRate())) {
//            sbj_breathingRateText.setText(String.format(Locale.UK, "%.2f " + suffix, data.getBreathingRate()));
//        }
        sbj_averageBreathingRateText.setText(String.format(Locale.UK, "%.2f ", data.getAvgBreathingRate()));

        // Set activity icon to reflect currently predicted activity
        switch (data.getActivityType()) {
            case Constants.ACTIVITY_LYING:
                sbj_activityIcon.setImageResource(R.drawable.vec_lying);
                detectedAct.setText("Lying");
                break;
            case Constants.ACTIVITY_LYING_DOWN_LEFT:
                sbj_activityIcon.setImageResource(R.drawable.lying_to_left);
                detectedAct.setText("Lying Left");
                break;
            case Constants.ACTIVITY_LYING_DOWN_RIGHT:
                sbj_activityIcon.setImageResource(R.drawable.lying_to_right);
                detectedAct.setText("Lying Right");
                break;
            case Constants.ACTIVITY_LYING_DOWN_STOMACH:
                sbj_activityIcon.setImageResource(R.drawable.lying_stomach);
                detectedAct.setText("Lying on Stomach");
                break;
            case Constants.ACTIVITY_SITTING_BENT_BACKWARD:
                sbj_activityIcon.setImageResource(R.drawable.sitting_backward);
                detectedAct.setText("Sitting Bent Backward");
                break;
            case Constants.ACTIVITY_SITTING_BENT_FORWARD:
                sbj_activityIcon.setImageResource(R.drawable.sitting_forward);
                detectedAct.setText("Sitting Bent Forward");
                break;
            case Constants.ACTIVITY_STAND_SIT:
                sbj_activityIcon.setImageResource(R.drawable.vec_standing_sitting);
                detectedAct.setText("Stand/Sit");
                break;
            case Constants.ACTIVITY_MOVEMENT:
                sbj_activityIcon.setImageResource(R.drawable.movement);
                detectedAct.setText("Moving");
                break;
            case Constants.ACTIVITY_WALKING:
                sbj_activityIcon.setImageResource(R.drawable.vec_walking);
                detectedAct.setText("Walking");
                break;
            case Constants.SS_COUGHING:
                sbj_activityIcon.setImageResource(R.drawable.ic_cough);
                detectedAct.setText("Coughing");
                break;
            default:
                sbj_activityIcon.setImageResource(R.drawable.vec_xmark);
        }
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        Log.i("RESpeckReadings", "updateRESpeckData");
        Log.i("RESpeckReadings", data.toString());

        // Update the other readings
        updateReadings(data);
    }

    @Override
    public void onDestroy() {
        // Unregister this class as observer. If we haven't observed, nothing happens
        ((MainActivity) getActivity()).unregisterRESpeckDataObserver(this);
        super.onDestroy();
    }
}

