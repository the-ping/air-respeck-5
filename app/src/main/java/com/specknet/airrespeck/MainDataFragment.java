package com.specknet.airrespeck;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class MainDataFragment extends Fragment {

    private TextView mRespRate, mPM10, mPM2_5;

    public MainDataFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    public void setPM10(final String value) {
        mPM10.setText(value);
    }

    public void setPM2_5(final String value) {
        mPM2_5.setText(value);
    }
}
