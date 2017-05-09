package com.specknet.airrespeck.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.models.AirspeckMapData;
import com.specknet.airrespeck.models.LocationData;
import com.specknet.airrespeck.utils.Constants;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

public class MapsAQActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Deque<AirspeckMapData> mQueueMapData;
    private final int MAX_DISPLAYED_DATA = 200;
    private BroadcastReceiver mBroadcastReceiver;

    private LatLng mLastLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_aq);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mQueueMapData = new LinkedList<>();
    }

    /**
     * Manipulates the map once available.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Permission for location not granted", Toast.LENGTH_LONG).show();
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        startAQUpdate();
    }

    private void startAQUpdate() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("AQ Map", "Receiving broadcast");
                switch (intent.getAction()) {
                    case Constants.ACTION_AIRSPECK_LIVE_BROADCAST:
                        if (mLastLatLng != null) {
                            HashMap<String, Float> readings = (HashMap<String, Float>) intent.getSerializableExtra(
                                    Constants.AIRSPECK_ALL_MEASURES);
                            Long timestamp = (Long) intent.getSerializableExtra(Constants.INTERPOLATED_PHONE_TIMESTAMP);
                            AirspeckMapData newData = new AirspeckMapData(timestamp, mLastLatLng,
                                    readings.get(Constants.QOE_PM1), readings.get(Constants.QOE_PM2_5),
                                    readings.get(Constants.QOE_PM10));
                            mQueueMapData.addLast(newData);
                            Toast.makeText(getApplicationContext(),
                                    String.format(Locale.UK, "PM 2.5: %f, PM 10: %f", newData.getPm2_5(),
                                            newData.getPm10()), Toast.LENGTH_LONG).show();
                            while (mQueueMapData.size() > MAX_DISPLAYED_DATA) {
                                mQueueMapData.removeFirst();
                            }
                            updateMarkers();
                        }
                        break;
                    case Constants.ACTION_PHONE_LOCATION_BROADCAST:
                        LocationData loc = (LocationData) intent.getSerializableExtra(Constants.PHONE_LOCATION);
                        LatLng newLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());

                        if (mLastLatLng == null) {
                            mLastLatLng =  newLatLng;
                            CameraUpdate center = CameraUpdateFactory.newLatLng(mLastLatLng);
                            CameraUpdate zoom = CameraUpdateFactory.zoomTo(18);
                            mMap.moveCamera(center);
                            mMap.animateCamera(zoom);
                        } else {
                            mLastLatLng = newLatLng;
                        }
                        break;
                }
            }
        };
        registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.ACTION_AIRSPECK_LIVE_BROADCAST));
        registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.ACTION_PHONE_LOCATION_BROADCAST));
    }

    private int getMobileCircleColor(AirspeckMapData dataItem) {
        double pm2_5 = dataItem.getPm2_5();
        double pm10 = dataItem.getPm10();

        if (pm2_5 < 0 || pm10 < 0) {
            return Color.GREEN;
        }

        if (pm2_5 > Constants.PM2_5_EUROPEAN_YEARLY_AVERAGE_MAX || pm10 > Constants.PM10_EUROPEAN_YEARLY_AVERAGE_MAX) {
            return Color.RED;
        } else {
            // Return a gradient value with green == 0 and red == MAX of the value with the higher percentage
            // in relation to MAX
            double perc2_5 = pm2_5 / Constants.PM2_5_EUROPEAN_YEARLY_AVERAGE_MAX;
            double perc10 = pm10 / Constants.PM10_EUROPEAN_YEARLY_AVERAGE_MAX;
            double maxPerc = Math.max(perc2_5, perc10);
            return Color.rgb((int) maxPerc * 255, (int) ((1 - maxPerc) * 255), 0);
        }
    }

    public void updateMarkers() {
        Log.i("AQ Map", "Updating map");
        mMap.clear();
        for (AirspeckMapData airspeckDataItem : mQueueMapData) {
            int circleColor = getMobileCircleColor(airspeckDataItem);
            mMap.addCircle(new CircleOptions().center(airspeckDataItem.getLocation()).radius(10)
                    .fillColor(circleColor).strokeColor(circleColor).strokeWidth(1));
            Log.i("AQ Map", "Circle painted at location: " + airspeckDataItem.getLocation());
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }
}
