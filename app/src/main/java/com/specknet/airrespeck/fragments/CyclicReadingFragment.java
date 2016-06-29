package com.specknet.airrespeck.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.utils.ReadingView;

import java.util.ArrayList;
import java.util.List;


public class CyclicReadingFragment extends Fragment implements View.OnClickListener {

    private FrameLayout mReadingContainer;
    private ReadingView mCurrentReading;
    private ArrayList<ReadingView> mReadings;

    private ImageButton mPrevReading;
    private ImageButton mNextReading;

    public CyclicReadingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentReading = null;
        mReadings = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.cyclic_fragment_reading, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mReadingContainer = (FrameLayout) view.findViewById(R.id.reading_container);

        mPrevReading = (ImageButton) view.findViewById(R.id.prev_reading);
        mNextReading = (ImageButton) view.findViewById(R.id.next_reading);

        mPrevReading.setOnClickListener(this);
        mNextReading.setOnClickListener(this);

        mReadingContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mReadingContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // Can get container dimensions here
                updateReading(0);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (mReadings.isEmpty()) {
            return;
        }

        int index = mReadings.indexOf(mCurrentReading);

        if(v.getId() == R.id.prev_reading) {
            index--;
            if (index < 0) {
                index = mReadings.size()-1;
            }
        }
        else if(v.getId() == R.id.next_reading) {
            index++;
            if (index > mReadings.size()-1) {
                index = 0;
            }
        }
        updateReading(index);
    }

    public void addReading(final String title, final String units,
                           final List<Float> scaleVal, final List<Integer> scaleCol) {
        mCurrentReading = new ReadingView(getContext());
        //mCurrentReading.setLayoutParams(new ViewGroup.LayoutParams(600, 200));

        mCurrentReading.setTitle(title);
        mCurrentReading.setValueUnits(units);
        mCurrentReading.setScale(scaleVal);
        mCurrentReading.setColours(scaleCol);
        mCurrentReading.setGradientColours(true);

        mReadings.add(mCurrentReading);

        mCurrentReading = null;
    }

    public void updateReading(int index) {
        if (mReadings.isEmpty()) {
            return;
        }

        mCurrentReading = mReadings.get(index);

        mReadingContainer.removeAllViews();
        mReadingContainer.addView(mCurrentReading);
    }

    public void setReadingVal(final int value) {
        mCurrentReading.setValue(value);
    }
}
