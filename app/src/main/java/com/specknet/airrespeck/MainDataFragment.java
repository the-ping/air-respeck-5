package com.specknet.airrespeck;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Properties;


public class MainDataFragment extends Fragment {

    enum mReadings {
        RESP_RATE, PM10, PM2_5
    }

    private FragmentActivity mListener;
    private Properties mProperties;
    private TextView mRespRate, mPM10, mPM2_5;

    public MainDataFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mListener = (FragmentActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProperties = Utils.getProperties(mListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_data, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mRespRate = (TextView) view.findViewById(R.id.respiratory_rate_val);
        mPM10 = (TextView) view.findViewById(R.id.pm10_val);
        mPM2_5 = (TextView) view.findViewById(R.id.pm2_5_val);
    }

    public void setRespiratoryRate(final String value) {
        mRespRate.setText(value);
        updateTextColor(Integer.parseInt(value), mReadings.RESP_RATE, mRespRate);
    }

    public void setPM10(final String value) {
        mPM10.setText(value);
        updateTextColor(Integer.parseInt(value), mReadings.PM10, mPM10);
    }

    public void setPM2_5(final String value) {
        mPM2_5.setText(value);
        updateTextColor(Integer.parseInt(value), mReadings.PM2_5, mPM2_5);
    }

    private void updateTextColor(final int value, final mReadings reading, TextView tv) {
        String key = "";
        switch (reading) {
            case RESP_RATE:
                key = "resp_rate";
                break;
            case PM10:
                key = "pm10";
                break;
            case PM2_5:
                key = "pm2_5";
                break;
        }

        if (value <= Integer.parseInt(mProperties.getProperty(key + "_green"))) {
            tv.setTextColor(ContextCompat.getColor(mListener, R.color.colorGreen));
        }
        else if (value > Integer.parseInt(mProperties.getProperty(key + "_orange"))) {
            tv.setTextColor(ContextCompat.getColor(mListener, R.color.colorRed));
        }
        else {
            tv.setTextColor(ContextCompat.getColor(mListener, R.color.colorOrange));
        }
    }
}
