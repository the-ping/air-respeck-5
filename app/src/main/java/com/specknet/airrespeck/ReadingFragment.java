package com.specknet.airrespeck;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


public class ReadingFragment extends Fragment implements View.OnClickListener {

    private int mCurrentReading;
    private TextView mReadingName;
    private TextView mReadingVal;
    private TextView mReadingUnit;
    private ImageButton mPrevReading;
    private ImageButton mNextReading;

    public ReadingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentReading = -1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_reading, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mReadingName = (TextView) view.findViewById(R.id.reading_name);
        mReadingVal = (TextView) view.findViewById(R.id.reading_val);
        mReadingUnit = (TextView) view.findViewById(R.id.reading_unit);
        mPrevReading = (ImageButton) view.findViewById(R.id.prevReading);
        mNextReading = (ImageButton) view.findViewById(R.id.nextReading);

        mPrevReading.setOnClickListener(this);
        mNextReading.setOnClickListener(this);

        updateReading(0);
    }

    @Override
    public void onClick(View v) {
        int r = 0;
        if(v.getId() == R.id.prevReading) {
            r = mCurrentReading - 1;
            if (r < 0) {
                r = 2;
            }
        }
        else if(v.getId() == R.id.nextReading) {
            r = mCurrentReading + 1;
            if (r > 2) {
                r = 0;
            }
        }
        updateReading(r);
    }

    public void updateReading(int reading) {
        if (mCurrentReading == reading) {
            return;
        }

        switch (reading) {
            case 0:
                mReadingName.setText(getString(R.string.respiratory_rate));
                mReadingUnit.setText(getString(R.string.bpm));
                break;
            case 1:
                mReadingName.setText(getString(R.string.pm10));
                mReadingUnit.setText(getString(R.string.ug_m3));
                break;
            case 2:
                mReadingName.setText(getString(R.string.pm2_5));
                mReadingUnit.setText(getString(R.string.ug_m3));
                break;
        }

        mCurrentReading = reading;
    }

    public void setReadingVal(final int value, final int reading) {
        if (mCurrentReading == reading) {
            mReadingVal.setText(String.valueOf(value));
        }
    }
}
