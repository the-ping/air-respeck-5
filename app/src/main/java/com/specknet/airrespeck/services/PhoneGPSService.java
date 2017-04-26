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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

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
        Log.i("GPS Service", "Location Utils connected!");
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
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("GPS Service", "App doesn't have permission to access GPS!");
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        } else {
            // Request regular location updates
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            // Open GPS writer and write header line if file didn't exist before
            try {
                String filename = Constants.EXTERNAL_DIRECTORY_STORAGE_PATH + "GPS Phone.csv";
                if (!new File(filename).exists()) {
                    mGPSWriter = new OutputStreamWriter(new FileOutputStream(filename, true));
                    mGPSWriter.append(Constants.GPS_PHONE_HEADER).append("\n");
                    mGPSWriter.flush();
                } else {
                    mGPSWriter = new OutputStreamWriter(new FileOutputStream(filename, true));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        long currentTimestamp = Utils.getUnixTimestamp();
        String locationString = currentTimestamp + "," + location.getLongitude() + "," + location.getLatitude() +
                "," + location.getAltitude() + "\n";
        Log.i("GPS Service", "Location updated: " + locationString);

        // Store location
        try {
            mGPSWriter.append(locationString);
            mGPSWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("GPS Service", "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("GPS Service", "Connection Suspended");
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
        Log.i("GPS Service", "GPS service stopped");
        super.onDestroy();
    }
}