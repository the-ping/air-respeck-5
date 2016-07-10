package com.specknet.airrespeck.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;


/**
 * Base Activity class to handle all settings related preferences.
 */
public class BaseFragment extends Fragment {

    // Preferences
    protected SharedPreferences mSettings;
    protected int mReadingsDisplayMode = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = PreferenceManager.getDefaultSharedPreferences(getContext());
        mReadingsDisplayMode = Integer.valueOf(mSettings.getString("readings_display_mode", "0"));
    }

    @Override
    public void onStart() {
        super.onStart();

        int newVal = Integer.valueOf(mSettings.getString("readings_display_mode", "0"));

        if (mReadingsDisplayMode != newVal) {
            mReadingsDisplayMode = newVal;

            // Preference change requires full refresh.
            restartFragment();
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
