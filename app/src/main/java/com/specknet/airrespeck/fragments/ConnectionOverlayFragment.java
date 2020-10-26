package com.specknet.airrespeck.fragments;


import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.ConnectionStateObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.util.Map;


/**
 * Base Activity class to handle all settings related preferences.
 */
public class ConnectionOverlayFragment extends Fragment implements ConnectionStateObserver {

    // Connecting layout
    private LinearLayout mConnectingLayout;
    private TextView mTextConnectionLayout;

    private boolean mIsAirspeckEnabled;
    private boolean mIsRESpeckEnabled;
    private boolean mIsPulseoxEnabled;
    private boolean mIsInhalerEnabled;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Register observer at MainActivity
        ((MainActivity) getActivity()).registerConnectionStateObserver(this);

        mConnectingLayout = (LinearLayout) view.findViewById(R.id.connecting_layout);
        mTextConnectionLayout = (TextView) view.findViewById(R.id.connection_text);

        Map<String, String> config = Utils.getInstance().getConfig(getActivity());
        mIsAirspeckEnabled = !config.get(Constants.Config.AIRSPECKP_UUID).isEmpty();
        mIsRESpeckEnabled = !config.get(Constants.Config.RESPECK_UUID).isEmpty();
        mIsPulseoxEnabled = !config.get(Constants.Config.PULSEOX_UUID).isEmpty();

        // Manually pull connection state on startup. Every new change is then pushed by observer pattern.
        updateConnectionState(((MainActivity) getActivity()).getIsRESpeckConnected(),
                ((MainActivity) getActivity()).getIsAirspeckConnected(),
                ((MainActivity) getActivity()).getIsPulseoxConnected(),
                ((MainActivity) getActivity()).getIsInhalerConnected());
    }

    @Override
    public void updateConnectionState(boolean respeckConnected, boolean airspeckConnected, boolean pulseoxConnected, boolean inhalerConnected) {
        int n = 4;
        if (!mIsAirspeckEnabled || airspeckConnected) n -= 1;
        if (!mIsRESpeckEnabled || respeckConnected) n -= 1;
        if (!mIsPulseoxEnabled || pulseoxConnected) n -= 1;
        if (!mIsInhalerEnabled || inhalerConnected) n -= 1;

        if (isAdded() && mConnectingLayout != null && mTextConnectionLayout != null) {
            if (n == 0) {
                mConnectingLayout.setVisibility(View.INVISIBLE);
            } else if (mIsAirspeckEnabled && !airspeckConnected && n == 1) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_airspeck_only));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else if (mIsRESpeckEnabled && !respeckConnected && n == 1) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_respeck_only));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else if (mIsPulseoxEnabled && !pulseoxConnected && n == 1) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_pulseox_only));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else if (mIsInhalerEnabled && !inhalerConnected && n == 1) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_inhaler_only));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else {
                mTextConnectionLayout.setText(getString(R.string.connection_text_both_devices));
                mConnectingLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onDestroy() {
        ((MainActivity) getActivity()).unregisterConnectionStateObserver(this);
        super.onDestroy();
    }
}
