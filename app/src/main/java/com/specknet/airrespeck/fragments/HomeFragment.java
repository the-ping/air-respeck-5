package com.specknet.airrespeck.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.specknet.airrespeck.R;

import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends Fragment {

    private ReadingsFragment mReadingsFragment;
    private ReadingFragment mReadingFragment;
    private FeedbackFragment mFeedbackFragment;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mReadingsFragment = new ReadingsFragment();
        mReadingFragment = new ReadingFragment();
        mFeedbackFragment = new FeedbackFragment();

        FragmentTransaction trans = getChildFragmentManager().beginTransaction();
        //trans.add(R.id.readings, mReadingsFragment, "READINGS");
        trans.add(R.id.readings, mReadingFragment, "READINGS");
        trans.add(R.id.feedback, mFeedbackFragment, "FEEDBACK");
        trans.commit();

        FragmentManager manager = getChildFragmentManager();
        manager.executePendingTransactions();

        // Add readings
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

        mReadingFragment.addReading(getString(R.string.respiratory_rate), getString(R.string.bpm),
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

        mReadingFragment.addReading(getString(R.string.pm10), getString(R.string.ug_m3),
                scaleVal, scaleCol);

        scaleVal.clear();
        scaleVal.add(0f);
        scaleVal.add(15f);
        scaleVal.add(35f);
        scaleVal.add(60f);

        mReadingFragment.addReading(getString(R.string.pm2_5), getString(R.string.ug_m3),
                scaleVal, scaleCol);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    public void setRespiratoryRate(final int value) {
        //mReadingsFragment.setRespiratoryRate(value);
        mReadingFragment.setReadingVal(value);
    }

    public void setPM10(final int value) {
        //mReadingsFragment.setPM10(value);
        mReadingFragment.setReadingVal(value);
    }

    public void setPM2_5(final int value) {
        //mReadingsFragment.setPM2_5(value);
        mReadingFragment.setReadingVal(value);
    }
}
