package com.specknet.airrespeck.base;

import com.activeandroid.ActiveAndroid;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.PreferencesUtils;


public class App extends android.support.multidex.MultiDexApplication {
    @Override
    public void onCreate()
    {
        super.onCreate();

        // Initialize Active Android SQLite database
        ActiveAndroid.initialize(this);

        // Initialize initial startup flag. This is used to control the user profile retrieval in
        // InitialSetup Activity
        PreferencesUtils.getInstance(getApplicationContext());
        PreferencesUtils.getInstance().put(Constants.Preferences.IS_APP_INITIAL_STARTUP, true);
    }
}
