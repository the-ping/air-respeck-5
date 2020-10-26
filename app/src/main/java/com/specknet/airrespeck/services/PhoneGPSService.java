package com.specknet.airrespeck.services;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.models.LocationData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.FileLogger;
import com.specknet.airrespeck.utils.Utils;

import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Service which listens to GPS updates of the phone and stores the data on the external directory
 */

public class PhoneGPSService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private OutputStreamWriter mGPSWriter;
    // Just in case there could be a conflict with another notification, we give it a high "random" integer
    private final int SERVICE_NOTIFICATION_ID = 2148914;

    private Date mDateofLastWrite = new Date(0);

    private boolean mIsEncryptData;
    private boolean mIsStoreDataLocally;
    private String patientID;
    private String androidID;

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
                Log.i("GPSService", "Starting GPS service...");
                FileLogger.logToFile(PhoneGPSService.this, "Phone GPS service started");

                Utils utils = Utils.getInstance();
                Map<String,String> loadedConfig = utils.getConfig(PhoneGPSService.this);

                // Store whether we want to store data locally
                mIsStoreDataLocally = Boolean.parseBoolean(loadedConfig.get(Constants.Config.STORE_DATA_LOCALLY));

                mIsEncryptData = Boolean.parseBoolean(loadedConfig.get(Constants.Config.ENCRYPT_LOCAL_DATA));

                patientID = loadedConfig.get(Constants.Config.SUBJECT_ID);
                androidID = Settings.Secure.getString(PhoneGPSService.this.getContentResolver(),
                        Settings.Secure.ANDROID_ID);

                Intent notificationIntent = new Intent(PhoneGPSService.this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(PhoneGPSService.this, 0, notificationIntent, 0);

                Notification notification = new Notification.Builder(PhoneGPSService.this)
                        .setContentTitle(getText(R.string.notification_gps_title))
                        .setContentText(getText(R.string.notification_gps_text))
                        .setSmallIcon(R.drawable.vec_location)
                        .setContentIntent(pendingIntent)
                        .build();

                startForeground(SERVICE_NOTIFICATION_ID, notification);

                mGoogleApiClient = new GoogleApiClient.Builder(PhoneGPSService.this)
                        .addConnectionCallbacks(PhoneGPSService.this)
                        .addOnConnectionFailedListener(PhoneGPSService.this)
                        .addApi(LocationServices.API)
                        .build();
                mGoogleApiClient.connect();
            }
        }.start();
        return START_STICKY;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i("GPSService", "Location Utils connected!");
        createLocationRequest();
        startLocationUpdates();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Constants.GPS_UPDATE_INTERVAL_ACTIVE);
        mLocationRequest.setFastestInterval(Constants.GPS_UPDATE_INTERVAL_PASSIVE);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        // Request regular location updates
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // As this is not an Activity, we cannot request the permission from here. Instead,
            // the permission should be checked prior to starting this service
            Log.e("GPSService", "App does not have location permission. This should be checked prior to starting the" +
                    "PhoneGPSServce");
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        long currentTimestamp = Utils.getUnixTimestamp();
        String locationString = currentTimestamp + "," + location.getLongitude() + "," + location.getLatitude() +
                "," + location.getAltitude() + "," + location.getAccuracy();
        Log.i("GPSService", "Location updated: " + locationString);

        // If we have local storage enabled, write the location to a file
        if (mIsStoreDataLocally) {
            writeToFile(locationString);
        }

        // Broadcast location
        Intent intentData = new Intent(Constants.ACTION_PHONE_LOCATION_BROADCAST);
        intentData.putExtra(Constants.PHONE_LOCATION,
                new LocationData(location.getLatitude(), location.getLongitude(), location.getAltitude(),
                        location.getAccuracy()));
        sendBroadcast(intentData);
    }

    private void writeToFile(String line) {
        // Check whether we are in a new day
        Date now = new Date();
        long currentWriteDay = DateUtils.truncate(now, Calendar.DAY_OF_MONTH).getTime();
        long previousWriteDay = DateUtils.truncate(mDateofLastWrite, Calendar.DAY_OF_MONTH).getTime();
        long numberOfMillisInDay = 1000 * 60 * 60 * 24;

        String filename = Utils.getInstance().getDataDirectory(this) +
                Constants.PHONE_LOCATION_DIRECTORY_NAME + "GPSPhone " +
                patientID + " " + androidID + " " +
                new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(now) +
                ".csv";

        // If we are in a new day, create a new file if necessary
        if (currentWriteDay != previousWriteDay ||
                now.getTime() - mDateofLastWrite.getTime() > numberOfMillisInDay) {
            try {
                // Close old connection if there was one
                if (mGPSWriter != null) {
                    mGPSWriter.close();
                }

                // The file could already exist if we just started the app. If not, add the header
                if (!new File(filename).exists()) {
                    Log.i("DF", "GPS data file created with header");
                    // Open new connection to new file
                    mGPSWriter = new OutputStreamWriter(
                            new FileOutputStream(filename, true));
                    if (mIsEncryptData) {
                        mGPSWriter.append("Encrypted").append("\n");
                    }
                    mGPSWriter.append(Constants.GPS_PHONE_HEADER).append("\n");
                    mGPSWriter.flush();
                } else {
                    // Open new connection to new file
                    mGPSWriter = new OutputStreamWriter(
                            new FileOutputStream(filename, true));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mDateofLastWrite = now;

        // Write new line to file
        try {
            if (mIsEncryptData) {
                // Write new line to file. If concatenation is split up with append, the second part might not be written,
                // meaning that there will be a line without a line break in the file.
                mGPSWriter.append(Utils.encrypt(line, this) + "\n");

            } else {
                mGPSWriter.append(line + "\n");
            }
            mGPSWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("GPSService", "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("GPSService", "Connection Suspended");
    }

    @Override
    public void onDestroy() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        if (mGPSWriter != null) {
            try {
                mGPSWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.i("GPSService", "GPS service stopped");

        super.onDestroy();
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        FileLogger.logToFile(PhoneGPSService.this, "Phone GPS service stopped by Android");
        return super.onUnbind(intent);
    }
}
