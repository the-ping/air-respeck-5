package com.specknet.airrespeck;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;


/**
 * Base Activity class to handle all settings related preferences.
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
            // Get the font size option.
            // We specify "Normal" with key "1" as the default value, if it does not exist.
            mFontSizePref = Integer.valueOf(mSettings.getString("font_size", "1"));

            // Select the proper theme ID.
            // These will correspond to the theme names as defined in themes.xml.
            int themeID = R.style.FontSizeNormal;
            if (mFontSizePref == 0) {
                themeID = R.style.FontSizeSmall;
            }
            else if (mFontSizePref == 2) {
                themeID = R.style.FontSizeLarge;
            }
            else if (mFontSizePref == 3) {
                themeID = R.style.FontSizeHuge;
            }

            // Set the theme for the activity.
            setTheme(themeID);
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
                mTabModePref = newVal;

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
                mIconsInTabsPref = newVal;

                // Preference change requires full refresh.
                restartActivity();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            // Get the font size option.
            // We specify "Normal" with key "1" as the default value, if it does not exist.
            int newVal = Integer.valueOf(mSettings.getString("font_size", "1"));

            if (mFontSizePref != newVal) {
                mFontSizePref = newVal;

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
