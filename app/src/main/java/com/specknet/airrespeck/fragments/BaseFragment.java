package com.specknet.airrespeck.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.PreferencesUtils;


/**
 * Base Activity class to handle all settings related preferences.
 */
public class BaseFragment extends Fragment {

    // Preferences
    protected String mReadingsModeHomeScreen;
    protected String mReadingsModeAQReadingsScreen;
    protected boolean mGraphsScreen;
    protected int mButtonsPadding;

    // Connecting layout
    protected LinearLayout mConnectingLayout;
    protected TextView mTextConnectionLayout;

    protected boolean mIsCreated = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesUtils.getInstance(getContext());

        mReadingsModeHomeScreen = PreferencesUtils.getInstance().getString(
                Constants.Preferences.READINGS_MODE_HOME_SCREEN, Constants.READINGS_MODE_HOME_SCREEN_LIST);

        mGraphsScreen = PreferencesUtils.getInstance()
                .getBoolean(Constants.Preferences.MENU_GRAPHS_SCREEN, false);

        mButtonsPadding = Integer.parseInt(PreferencesUtils.getInstance()
                .getString(Constants.Preferences.MENU_BUTTONS_PADDING, Constants.MENU_BUTTONS_PADDING_NORMAL));

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mConnectingLayout = (LinearLayout) view.findViewById(R.id.connecting_layout);
        if (mConnectingLayout != null) {
            mTextConnectionLayout = (TextView) mConnectingLayout.findViewById(R.id.connection_text);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Update connection symbol. Calls MainActivity to update all Fragments
        ((MainActivity) getActivity()).updateConnectionLoadingLayout();
    }

    @Override
    public void onDestroyView() {
        mIsCreated = false;
        super.onDestroyView();
    }

    /**
     * Refresh fragment view by detaching it and attaching it again from its parent activity.
     */
    private void restartFragment() {
        // Destroy and Re-create this fragment's view.
        final FragmentManager fm = this.getActivity().getSupportFragmentManager();
        fm.beginTransaction().
                detach(this).
                attach(this).
                commit();
    }

    public int getIcon() {
        return Constants.MENU_ICON_HOME;
    }


    /***********************************************************************************************
     * CONNECTING LAYOUT (for Bluetooth connection only)
     **********************************************************************************************/
    public void showConnecting(final boolean showAirspeckConnecting, final boolean showRESpeckConnecting, final boolean showPulseoxConnecting) {
        int n = 0;
        if (showAirspeckConnecting) n += 1;
        if (showRESpeckConnecting) n += 1;
        if (showPulseoxConnecting) n += 1;

        Log.d("RAT", new Integer(n).toString() + ": " + showAirspeckConnecting + "," + showRESpeckConnecting + ", " + showPulseoxConnecting);

        if (isAdded() && mConnectingLayout != null && mTextConnectionLayout != null) {
            if (n == 0) {
                mConnectingLayout.setVisibility(View.INVISIBLE);
            } else if (showAirspeckConnecting && n == 1) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_airspeck_only));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else if (showRESpeckConnecting && n == 1) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_respeck_only));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else if (showPulseoxConnecting && n == 1) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_pulseox_only));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else {
                mTextConnectionLayout.setText(getString(R.string.connection_text_both_devices));
                mConnectingLayout.setVisibility(View.VISIBLE);
            }
        }
    }
}
