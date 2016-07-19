package com.specknet.airrespeck.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.specknet.airrespeck.datamodels.User;
import com.specknet.airrespeck.utils.PreferencesUtils;


/**
 * Base Activity class to handle all settings related preferences.
 */
public class BaseFragment extends Fragment {

    // Preferences
    protected User mCurrentUser;
    protected int mReadingsModeHomeScreen;
    protected int mReadingsModeAQReadingsScreen;

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
    }

    private void restartFragment() {
        // Destroy and Re-create this fragment's view.
        final FragmentManager fm = this.getActivity().getSupportFragmentManager();
        fm.beginTransaction().
                detach(this).
                attach(this).
                commit();
    }
}
