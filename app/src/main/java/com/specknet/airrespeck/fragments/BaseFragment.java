package com.specknet.airrespeck.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.LinearLayout;

import com.specknet.airrespeck.datamodels.User;
import com.specknet.airrespeck.models.MenuButton;
import com.specknet.airrespeck.utils.PreferencesUtils;


/**
 * Base Activity class to handle all settings related preferences.
 */
public class BaseFragment extends Fragment {

    // Preferences
    protected User mCurrentUser;
    protected int mReadingsModeHomeScreen;
    protected int mReadingsModeAQReadingsScreen;
    protected boolean mGraphsScreen;
    protected int mButtonsPadding;

    // Connecting layout
    protected LinearLayout mConnectingLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesUtils.getInstance(getContext());

        mCurrentUser = User.getUserByUniqueId(PreferencesUtils.getInstance()
                .getString(PreferencesUtils.Key.USER_ID));

        mReadingsModeHomeScreen = Integer.valueOf(PreferencesUtils.getInstance()
                .getString(PreferencesUtils.Key.READINGS_MODE_HOME_SCREEN, "0"));
        mReadingsModeAQReadingsScreen = Integer.valueOf(PreferencesUtils.getInstance()
                .getString(PreferencesUtils.Key.READINGS_MODE_AQREADINGS_SCREEN, "0"));

        mGraphsScreen = PreferencesUtils.getInstance()
                .getBoolean(PreferencesUtils.Key.MENU_GRAPHS_SCREEN, false);

        mButtonsPadding = Integer.valueOf(PreferencesUtils.getInstance()
                .getString(PreferencesUtils.Key.MENU_BUTTONS_PADDING, "5"));
    }

    @Override
    public void onStart() {
        super.onStart();

        if (this instanceof HomeFragment) {
            int newVal = Integer.valueOf(PreferencesUtils.getInstance()
                    .getString(PreferencesUtils.Key.READINGS_MODE_HOME_SCREEN, "0"));

            if (mReadingsModeHomeScreen != newVal) {
                restartFragment();
            }
        }
        else if (this instanceof AQReadingsFragment) {
            int newVal = Integer.valueOf(PreferencesUtils.getInstance()
                    .getString(PreferencesUtils.Key.READINGS_MODE_AQREADINGS_SCREEN, "0"));

            if (mReadingsModeAQReadingsScreen != newVal) {
                restartFragment();
            }
        }
        else if (this instanceof MenuFragment) {
            int newVal = Integer.valueOf(PreferencesUtils.getInstance()
                    .getString(PreferencesUtils.Key.MENU_BUTTONS_PADDING, "5"));

            if (mButtonsPadding != newVal) {
                restartFragment();
            }
        }
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


    /***********************************************************************************************
     * CONNECTING LAYOUT (for Bluetooth connection only)
     **********************************************************************************************/
    public void showConnecting(final boolean enable) {
        if (mConnectingLayout != null) {
            mConnectingLayout.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
