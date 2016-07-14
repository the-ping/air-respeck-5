package com.specknet.airrespeck.activities;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.specknet.airrespeck.utils.ThemeUtils;


/**
 * Base Activity class to handle all mSettings related preferences.
 */
public class BaseActivity extends AppCompatActivity {

    protected SharedPreferences mSettings;
    protected boolean mTabModePref;
    protected boolean mIconsInTabsPref;
    protected boolean mRespeckAppAccessPref;
    protected boolean mAirspeckAppAccessPref;
    protected int mFontSizePref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Enclose everything in a try block so that the default view
        // can be used if anything goes wrong.
        try {
            mTabModePref = mSettings.getBoolean("main_menu_layout", false);
            mIconsInTabsPref = mSettings.getBoolean("icons_in_tabs", false);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mRespeckAppAccessPref = mSettings.getBoolean("respeck_app_access", false);
            mAirspeckAppAccessPref = mSettings.getBoolean("airspeck_app_access", false);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mFontSizePref = Integer.valueOf(mSettings.getString("font_size", "1"));

            ThemeUtils themeUtils = ThemeUtils.getInstance();
            themeUtils.setTheme(mFontSizePref);
            themeUtils.onActivityCreateSetTheme(this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        try {
            boolean newVal = mSettings.getBoolean("main_menu_layout", false);

            if (mTabModePref != newVal) {
                // Preference change requires full refresh.
                restartActivity();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            boolean newVal = mSettings.getBoolean("icons_in_tabs", false);

            if (mIconsInTabsPref != newVal) {
                // Preference change requires full refresh.
                restartActivity();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            int newVal = Integer.valueOf(mSettings.getString("font_size", "1"));

            if (mFontSizePref != newVal) {
                // Preference change requires full refresh.
                restartActivity();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restartActivity() {
        finish();
        startActivity(getIntent());
    }
}
