package com.specknet.airrespeck.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.datamodels.User;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.PreferencesUtils;


/**
 * Base Activity class to handle all settings related preferences.
 */
public class BaseFragment extends Fragment {

    // Preferences
    protected User mCurrentUser;
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

        mCurrentUser = User.getUserByUniqueId(PreferencesUtils.getInstance()
                .getString(Constants.Preferences.USER_ID));

        mReadingsModeHomeScreen = PreferencesUtils.getInstance().getString(
                Constants.Preferences.READINGS_MODE_HOME_SCREEN, Constants.READINGS_MODE_HOME_SCREEN_LIST);
        mReadingsModeAQReadingsScreen = PreferencesUtils.getInstance()
                .getString(Constants.Preferences.READINGS_MODE_AQREADINGS_SCREEN,
                        Constants.READINGS_MODE_AQREADINGS_SCREEN_LIST);

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

        // Restart fragments if the type of display does not correspond to the preferences set by the user
        if (this instanceof SupervisedOverviewFragment) {
            String newVal = PreferencesUtils.getInstance().getString(Constants.Preferences.READINGS_MODE_HOME_SCREEN,
                    Constants.READINGS_MODE_HOME_SCREEN_LIST);

            if (!mReadingsModeHomeScreen.equals(newVal)) {
                restartFragment();
            }
        } else if (this instanceof SupervisedAirspeckReadingsFragment) {
            String newVal = PreferencesUtils.getInstance().getString(
                    Constants.Preferences.READINGS_MODE_AQREADINGS_SCREEN,
                    Constants.READINGS_MODE_AQREADINGS_SCREEN_LIST);

            if (!mReadingsModeAQReadingsScreen.equals(newVal)) {
                restartFragment();
            }
        } else if (this instanceof MenuFragment) {
            int newVal = Integer.parseInt(PreferencesUtils.getInstance()
                    .getString(Constants.Preferences.MENU_BUTTONS_PADDING, Constants.MENU_BUTTONS_PADDING_NORMAL));

            if (mButtonsPadding != newVal) {
                restartFragment();
            }
        }

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
    public void showConnecting(final boolean showAirspeckConnecting, final boolean showRESpeckConnecting) {
        if (isAdded() && mConnectingLayout != null && mTextConnectionLayout != null) {
            if (showAirspeckConnecting && showRESpeckConnecting) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_both_devices));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else if (showAirspeckConnecting) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_airspeck_only));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else if (showRESpeckConnecting) {
                mTextConnectionLayout.setText(getString(R.string.connection_text_respeck_only));
                mConnectingLayout.setVisibility(View.VISIBLE);
            } else {
                mConnectingLayout.setVisibility(View.INVISIBLE);
            }
        }
    }
}
