package com.specknet.airrespeck.base;


import android.app.Application;

import com.activeandroid.ActiveAndroid;
import com.specknet.airrespeck.utils.PreferencesUtils;


public class App extends Application{
    @Override
    public void onCreate()
    {
        super.onCreate();

        // Initialize Active Android SQLite database
        ActiveAndroid.initialize(this);

        // Initialize initial startup flag. Thi is used to control the user profile retrieval in
        // InitialSetup Activity
        PreferencesUtils.getInstance(getApplicationContext());
        PreferencesUtils.getInstance().put(PreferencesUtils.Key.IS_APP_INITIAL_STARTUP, true);
    }
}
