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
    private int mReadingsDisplayMode = -1;

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
        mReadingsDisplayMode = Integer.valueOf(mSettings.getString("home_screen_readings_display_mode", "0"));

        FragmentTransaction trans = getChildFragmentManager().beginTransaction();
        trans.add(R.id.feedback, mFeedbackFragment, "FEEDBACK");
        trans.commit();

        // Setup readings display mode
        init();

        // Add readings
        loadData();
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

        mReadingsDisplayMode = Integer.valueOf(mSettings.getString("home_screen_readings_display_mode", "0"));

        switchReadingMode(mReadingsDisplayMode);
    }

    /**
     * Add the fragments for the reading display mode: {@link #mListReadingFragment} and
     * {@link #mCyclicReadingFragment}.
     */
    private void init() {
        FragmentTransaction trans = getChildFragmentManager().beginTransaction();

        trans.add(R.id.readings, mListReadingFragment, "READINGS");
        trans.add(R.id.readings, mCyclicReadingFragment, "READINGS");

        if (mReadingsDisplayMode == 0) {
            trans.hide(mCyclicReadingFragment);
        }
        else if (mReadingsDisplayMode == 1) {
            trans.hide(mListReadingFragment);
        }

        trans.commit();
    }

    /**
     * Set the readings data in the corresponding fragments: {@link #mListReadingFragment} and
     * {@link #mCyclicReadingFragment}.
     */
    private void loadData() {
        // Make sure needed fragments are in place
        getChildFragmentManager().executePendingTransactions();

        mListReadingFragment.addReading(getString(R.string.reading_respiratory_rate), 0, getString(R.string.reading_unit_bpm));
        mListReadingFragment.addReading(getString(R.string.reading_pm10), 0, getString(R.string.reading_unit_ug_m3));
        mListReadingFragment.addReading(getString(R.string.reading_pm2_5), 0, getString(R.string.reading_unit_ug_m3));


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

        mCyclicReadingFragment.addReading(getString(R.string.reading_respiratory_rate), getString(R.string.reading_unit_bpm), scaleVal, scaleCol);

        scaleCol.clear();
        scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorGreen));
        scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorOrange));
        scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorRed));

        scaleVal.clear();
        scaleVal.add(0f);
        scaleVal.add(35f);
        scaleVal.add(50f);
        scaleVal.add(60f);

        mCyclicReadingFragment.addReading(getString(R.string.reading_pm10), getString(R.string.reading_unit_ug_m3), scaleVal, scaleCol);

        scaleVal.clear();
        scaleVal.add(0f);
        scaleVal.add(15f);
        scaleVal.add(35f);
        scaleVal.add(60f);

        mCyclicReadingFragment.addReading(getString(R.string.reading_pm2_5), getString(R.string.reading_unit_ug_m3), scaleVal, scaleCol);
    }

    /**
     * Switch the readings display mode.
     * @param mode int Readings display mode index.
     */
    private void switchReadingMode(final int mode) {
        FragmentTransaction trans = getChildFragmentManager().beginTransaction();

        if (mode == 0) {
            trans.show(mListReadingFragment);
            trans.hide(mCyclicReadingFragment);
        }
        else if (mode == 1) {
            trans.hide(mListReadingFragment);
            trans.show(mCyclicReadingFragment);
        }

        trans.commit();
    }

    /**
     * Set the current value / values for the readings.
     * @param values List<Integer> List with all the current reading values for the
     *               {@link #mListReadingFragment} fragment.
     * @param value int Current value for the current reading for the
     *               {@link #mCyclicReadingFragment} fragment.
     */
    public void setReadingValues(final List<Integer> values, final int value) {
        if (mReadingsDisplayMode == 0) {
            mListReadingFragment.setListValues(values);
        }
        else if (mReadingsDisplayMode == 1) {
            mCyclicReadingFragment.setReadingVal(value);
        }
    }
}
