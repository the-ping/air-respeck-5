package com.specknet.airrespeck.activities;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.specknet.airrespeck.datamodels.User;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.PreferencesUtils;
import com.specknet.airrespeck.utils.ThemeUtils;


/**
 * Base Activity class to handle all settings related preferences. This class is extended by all the other activities.
 * This means that each time we start another activity, the settings are correctly loaded from the preferences defined
 * by the RESpeck.config.
 */
public class BaseActivity extends AppCompatActivity {

    protected User mCurrentUser;
    protected int mMenuModePref;
    protected boolean mMenuTabIconsPref;
    protected boolean mGraphsScreen;
    protected boolean mRespeckAppAccessPref;
    protected boolean mAirspeckAppAccessPref;
    protected int mFontSizePref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesUtils.getInstance(getApplicationContext());

        if (!(this instanceof InitialSetupActivity) && !(this instanceof NewUserActivity)) {
            mCurrentUser = User.getUserByUniqueId(PreferencesUtils.getInstance().
                    getString(PreferencesUtils.Key.USER_ID));
        }

        // Get preference settings depending on user type defined in RESpeck.config.
        // Enclose everything in a try block so that the default view
        // can be used if anything goes wrong.
        try {
            mMenuModePref = PreferencesUtils.getInstance()
                    .getInt(PreferencesUtils.Key.MENU_MODE, Constants.MENU_MODE_BUTTONS);
            mMenuTabIconsPref = PreferencesUtils.getInstance()
                    .getBoolean(PreferencesUtils.Key.MENU_TAB_ICONS, false);
            mGraphsScreen = PreferencesUtils.getInstance()
                    .getBoolean(PreferencesUtils.Key.MENU_GRAPHS_SCREEN, false);

            mRespeckAppAccessPref = PreferencesUtils.getInstance()
                    .getBoolean(PreferencesUtils.Key.RESPECK_APP_ACCESS, false);
            mAirspeckAppAccessPref = PreferencesUtils.getInstance()
                    .getBoolean(PreferencesUtils.Key.AIRSPECK_APP_ACCESS, false);

            mFontSizePref = PreferencesUtils.getInstance()
                    .getInt(PreferencesUtils.Key.FONT_SIZE, Constants.FONT_SIZE_NORMAL);

            ThemeUtils themeUtils = ThemeUtils.getInstance();
            themeUtils.setTheme(mFontSizePref);
            themeUtils.onActivityCreateSetTheme(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        try {
            int newVal = PreferencesUtils.getInstance()
                    .getInt(PreferencesUtils.Key.MENU_MODE, Constants.MENU_MODE_BUTTONS);

            if (mMenuModePref != newVal) {
                restartActivity();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            boolean newVal = PreferencesUtils.getInstance()
                    .getBoolean(PreferencesUtils.Key.MENU_TAB_ICONS, false);

            if (mMenuTabIconsPref != newVal) {
                restartActivity();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            boolean newVal = PreferencesUtils.getInstance()
                    .getBoolean(PreferencesUtils.Key.MENU_GRAPHS_SCREEN, false);

            if (mGraphsScreen != newVal) {
                restartActivity();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            int newVal = Integer.valueOf(PreferencesUtils.getInstance()
                    .getString(PreferencesUtils.Key.FONT_SIZE, "1"));

            if (mFontSizePref != newVal) {
                restartActivity();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restartActivity() {
        finish();
        startActivity(getIntent());
    }
}
