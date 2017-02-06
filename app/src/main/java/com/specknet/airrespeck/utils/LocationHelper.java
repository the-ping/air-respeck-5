package com.specknet.airrespeck.utils;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;


/**
 * Location Helper Class.
 * Handles creation of the Location Manager and Location Listener.
 * Created by Santiago Balladares on 14/07/2014.
 */
public class LocationHelper {

    private static LocationHelper mLocationHelper;
    private final Context mContext;

    /**
     * Lat and Long variables.
     */
    private double mLatitude = 0;
    private double mLongitude = 0;
    private double mAltitude = 0;

    /**
     * Indicates whether or not a location has been acquired.
     */
    private boolean mGotLocation = false;

    /**
     * Location Manager and Location Listener.
     */
    private LocationManager mLocationManager;
    private MyLocationListener mLocationListener;

    /**
     * Constructor.
     * @param context - The context of the calling activity.
     */
    public LocationHelper(Context context) {
        mContext = context;

        // Setup the Location Manager.
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Create the Location Listener
        mLocationListener = new MyLocationListener();

        // Setup a callback for when the GRPS/WiFi gets a lock and we receive data.
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //mLocationManager.requestLocationUpdates(
        //        LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);

        // Setup a callback for when the GPS gets a lock and we receive data.
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
    }

    public static LocationHelper getInstance(Context context) {
        if (mLocationHelper == null) {
            mLocationHelper = new LocationHelper(context);
        }
        return mLocationHelper;
    }

    /***
     * Used to receive notifications from the Location Manager when they are sent.
     * These methods are called when the Location Manager is registered with the
     * Location Service and a state changes.
     */
    public class MyLocationListener implements LocationListener {
        // Called when the location service reports a change in location.
        public void onLocationChanged(Location location) {
            // Store lat and long.
            mLatitude = location.getLatitude();
            mLongitude = location.getLongitude();
            mAltitude = location.hasAltitude() ? location.getAltitude() : 0;

            // Now we have our location we can stop the service from sending updates.
            // Comment out this line if you want the service to continue updating the users location
            //killLocationServices();

            // Change the flag to indicate that we now have a location.
            mGotLocation = true;
        }

        // Called when the provider is disabled
        public void onProviderDisabled(String provider) {
        }

        // Called when the provider is enabled
        public void onProviderEnabled(String provider) {
        }

        // Called when the provider changes state
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    /***
     * Stop updates from the Location Service.
     */
    public void killLocationServices() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.removeUpdates(mLocationListener);
    }

    /***
     * Get Latitude from GPS Helper.
     * @return - The current Latitude.
     */
    public double getLatitude() {
        return gotLocation() ? mLatitude : 0;
    }

    /***
     * Get Longitude from GPS Helper.
     * @return - The current Longitude.
     */
    public double getLongitude() {
        return gotLocation() ? mLongitude : 0;
    }

    /**
     * Get Altitude from GPS helper.
     * @return The current Altitude.
     */
    public double getAltitude() {
        return gotLocation() ? mAltitude : 0;
    }

    /***
     * Check if a location has been found yet.
     * @return - True if a location has been acquired. False otherwise.
     */
    public Boolean gotLocation() {
        return mGotLocation;
    }

    /**
     *
     * Checks whether or not location services have been enabled.
     * @param context - The context of the calling activity.
     * @return - True if location services are enabled. False otherwise.
     */
    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            }
            catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }
        else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }
}