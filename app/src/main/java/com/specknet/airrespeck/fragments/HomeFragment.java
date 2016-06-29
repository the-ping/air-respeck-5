package com.specknet.airrespeck.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.specknet.airrespeck.R;

import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends Fragment {

    private SharedPreferences mSettings;
    private int readings_display_mode = -1;

    private ListReadingFragment mListReadingFragment;
    private CyclicReadingFragment mCyclicReadingFragment;
    private FeedbackFragment mFeedbackFragment;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mListReadingFragment = new ListReadingFragment();
        mCyclicReadingFragment = new CyclicReadingFragment();
        mFeedbackFragment = new FeedbackFragment();

        mSettings = PreferenceManager.getDefaultSharedPreferences(getContext());

        FragmentTransaction trans = getChildFragmentManager().beginTransaction();
        trans.add(R.id.feedback, mFeedbackFragment, "FEEDBACK");
        trans.commit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        readings_display_mode = Integer.valueOf(mSettings.getString("main_screen_readings", "0"));

        // Setup reading mode
        setupReadingMode(readings_display_mode);

        // Add readings
        loadData(readings_display_mode);
    }

    private void setupReadingMode(final int mode_index) {
        FragmentTransaction trans = getChildFragmentManager().beginTransaction();

        if (mode_index == 0 && !mListReadingFragment.isVisible()) {
            trans.remove(mCyclicReadingFragment);
            trans.add(R.id.readings, mListReadingFragment, "READINGS");
        }
        else if (mode_index == 1 && !mCyclicReadingFragment.isVisible()) {
            trans.remove(mListReadingFragment);
            trans.add(R.id.readings, mCyclicReadingFragment, "READINGS");
        }

        trans.commit();
    }

    private void loadData(final int mode_index) {
        // Make sure all transactions have finished
        getChildFragmentManager().executePendingTransactions();

        if (mode_index == 0) {
            mListReadingFragment.
                    addReading(getString(R.string.respiratory_rate), 0, getString(R.string.bpm));
            mListReadingFragment.
                    addReading(getString(R.string.pm10), 0, getString(R.string.ug_m3));
            mListReadingFragment.
                    addReading(getString(R.string.pm2_5), 0, getString(R.string.ug_m3));
        }
        else if (mode_index == 1) {
            List<Integer> scaleCol = new ArrayList<>();
            List<Float> scaleVal = new ArrayList<>();

            scaleCol.clear();
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorRed));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorOrange));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorGreen));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorOrange));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorRed));

            scaleVal.clear();
            scaleVal.add(0f);
            scaleVal.add(8f);
            scaleVal.add(12f);
            scaleVal.add(20f);
            scaleVal.add(35f);
            scaleVal.add(60f);

            mCyclicReadingFragment.addReading(getString(R.string.respiratory_rate), getString(R.string.bpm),
                    scaleVal, scaleCol);

            scaleCol.clear();
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorGreen));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorOrange));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorRed));

            scaleVal.clear();
            scaleVal.add(0f);
            scaleVal.add(35f);
            scaleVal.add(50f);
            scaleVal.add(60f);

            mCyclicReadingFragment.addReading(getString(R.string.pm10), getString(R.string.ug_m3),
                    scaleVal, scaleCol);

            scaleVal.clear();
            scaleVal.add(0f);
            scaleVal.add(15f);
            scaleVal.add(35f);
            scaleVal.add(60f);

            mCyclicReadingFragment.addReading(getString(R.string.pm2_5), getString(R.string.ug_m3),
                    scaleVal, scaleCol);
        }
    }

    public void setReadingValues(final List<Integer> values, final int value) {
        if (readings_display_mode == 0) {
            mListReadingFragment.setListValues(values);
        }
        else if (readings_display_mode == 1) {
            mCyclicReadingFragment.setReadingVal(value);
        }
    }
}
