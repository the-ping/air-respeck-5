package com.specknet.airrespeck.base;


import android.app.Application;

import com.activeandroid.ActiveAndroid;


public class App extends Application{
    @Override
    public void onCreate()
    {
        super.onCreate();

        // Initialize Active Android SQLite database
        ActiveAndroid.initialize(this);
    }
}
