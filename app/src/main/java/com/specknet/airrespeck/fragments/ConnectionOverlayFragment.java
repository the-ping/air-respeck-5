package com.specknet.airrespeck.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.ConnectionStateObserver;


/**
 * Base Activity class to handle all settings related preferences.
 */
public class ConnectionOverlayFragment extends Fragment implements ConnectionStateObserver {

    // Connecting layout
    protected LinearLayout mConnectingLayout;
    protected TextView mTextConnectionLayout;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mConnectingLayout = (LinearLayout) view.findViewById(R.id.connecting_layout);
        mTextConnectionLayout = (TextView) view.findViewById(R.id.connection_text);
    }

    @Override
    public void updateConnectionState(boolean respeckConnected, boolean airspeckConnected, boolean pulseoxConnected) {
        int n = 0;
        if (airspeckConnected) n += 1;
        if (respeckConnected) n += 1;
        if (pulseoxConnected) n += 1;

        if (isAdded() && mConnectingLayout != null && mTextConnectionLayout != null) {
            if (n == 0) {
                mConnectingLayout.setVisibility(View.INVISIBLE);
            } else if (airspeckConnected && n == 1) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_airspeck_only));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else if (respeckConnected && n == 1) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_respeck_only));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else if (pulseoxConnected && n == 1) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_pulseox_only));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else {
                mTextConnectionLayout.setText(getString(R.string.connection_text_both_devices));
                mConnectingLayout.setVisibility(View.VISIBLE);
            }
        }
    }
}
