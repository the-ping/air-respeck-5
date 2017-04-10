package com.specknet.airrespeck.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.specknet.airrespeck.utils.LocationUtils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Darius on 10.04.2017.
 */

public class PhoneGPSService extends Service {

    private Timer mTimer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread() {
            @Override
            public void run() {
                Log.i("GPS Service", "Starting GPS service...");
                startGPSService();
            }
        }.start();
        return START_STICKY;
    }

    private void startGPSService() {
        // Load location Utils
        final LocationUtils locationUtils = LocationUtils.getInstance(getApplicationContext());
        locationUtils.startLocationManager();

        final int delay = 1000;
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Get location
                double latitude = -1;
                double longitude = -1;
                double altitude = -1;
                try {
                    latitude = locationUtils.getLatitude();
                    longitude = locationUtils.getLongitude();
                    altitude = locationUtils.getAltitude();
                } catch (Exception e) {
                    Log.i("GPS Service", "Location permissions not granted or GPS turned off. Store empty values.");
                }
                Log.i("GPS Service", "Longitude: " + longitude + ", Latitude: " + latitude + ", Altitude: " + altitude);

                // Store GPS data

            }
        }, 0, delay);
    }

    @Override
    public void onDestroy() {
        mTimer.cancel();
        Log.i("GPS Service", "GPS service stopped");
        super.onDestroy();
    }
}
