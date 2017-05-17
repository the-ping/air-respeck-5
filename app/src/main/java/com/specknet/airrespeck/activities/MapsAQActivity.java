package com.specknet.airrespeck.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.models.AirspeckMapData;
import com.specknet.airrespeck.models.LocationData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

public class MapsAQActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Deque<AirspeckMapData> mQueueMapData;
    private final int MAX_DISPLAYED_DATA = 400;
    private BroadcastReceiver mBroadcastReceiver;

    private LatLng mLastLatLng;

    public static final int MAP_TYPE_LIVE = 0;
    public static final int MAP_TYPE_HISTORICAL = 1;
    public static final String MAP_TYPE = "map type";
    public static final String TIMESTAMP_FROM = "ts from";
    public static final String TIMESTAMP_TO = "ts to";

    // Default is the live map
    private int mapType = MAP_TYPE_LIVE;

    ArrayList<AirspeckMapData> mLoadedData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_aq);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mapType = (int) getIntent().getExtras().get(MAP_TYPE);
    }

    /**
     * Manipulates the map once available.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mapType == MAP_TYPE_LIVE) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Permission for location not granted",
                        Toast.LENGTH_LONG).show();
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
            zoomToLastKnownLocation();
            mQueueMapData = new LinkedList<>();
            startLiveAQUpdate();
        } else if (mapType == MAP_TYPE_HISTORICAL) {
            long tsFrom = (long) getIntent().getExtras().get(TIMESTAMP_FROM);
            long tsTo = (long) getIntent().getExtras().get(TIMESTAMP_TO);
            loadStoredData(tsFrom, tsTo);
        }
    }

    // Initial zoom. This will be updated as soon as we get a location from the PhoneGPSService
    private void zoomToLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (location != null) {
            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(),
                            location.getLongitude()))      // Sets the center of the map to location user
                    .zoom(18)                   // Sets the zoom
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    private void startLiveAQUpdate() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
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
                            mLastLatLng = newLatLng;
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
            return ContextCompat.getColor(this, R.color.pollution_low);
        }

        if (pm2_5 > Constants.PM2_5_EUROPEAN_YEARLY_AVERAGE_MAX || pm10 > Constants.PM10_EUROPEAN_YEARLY_AVERAGE_MAX) {
            return ContextCompat.getColor(this, R.color.pollution_high);
        } else {
            // Return a gradient value with green == 0 and red == MAX of the value with the higher percentage
            // in relation to MAX
            double perc2_5 = Math.min(pm2_5 / Constants.PM2_5_EUROPEAN_YEARLY_AVERAGE_MAX, 1.0);
            double perc10 = Math.min(pm10 / Constants.PM10_EUROPEAN_YEARLY_AVERAGE_MAX, 1.0);
            double maxPerc = Math.max(perc2_5, perc10);

            int lowColor = ContextCompat.getColor(this, R.color.pollution_low);
            int highColor = ContextCompat.getColor(this, R.color.pollution_high);

            // Return weighted mix between low and high pollution color depending on maxPerc
            return Color.rgb((int) (Color.red(lowColor) * (1 - maxPerc) + Color.red(highColor) * maxPerc),
                    (int) (Color.green(lowColor) * (1 - maxPerc) + Color.green(highColor) * maxPerc),
                    (int) (Color.blue(lowColor) * (1 - maxPerc) + Color.blue(highColor) * maxPerc));
        }
    }

    public void updateMarkers() {
        Log.i("AQ Map", "Updating map");
        mMap.clear();
        for (AirspeckMapData airspeckDataItem : mQueueMapData) {
            drawCircleOnMap(airspeckDataItem);
        }
    }

    private void drawCircleOnMap(AirspeckMapData airspeckDataItem) {
        int circleColor = getMobileCircleColor(airspeckDataItem);
        /*
        var p = Math.pow(2, (21 - iMap.map.getZoom()));
        iMap.circle[i].setRadius(p * 1128.497220 * 0.0027);*/
        mMap.addCircle(new CircleOptions().center(airspeckDataItem.getLocation()).radius(10)
                .fillColor(circleColor).strokeColor(circleColor).strokeWidth(1));
        // Log.i("AQ Map", "Circle painted at location: " + airspeckDataItem.getLocation());
    }


    @Override
    protected void onDestroy() {
        if (mapType == MAP_TYPE_LIVE) {
            unregisterReceiver(mBroadcastReceiver);
        }
        super.onDestroy();
    }

    private void loadStoredData(long tsFrom, long tsTo) {
        // Create new task for loading data
        new LoadStoredDataTask().execute(tsFrom, tsTo);
    }

    private class LoadStoredDataTask extends AsyncTask<Long, Integer, Void> {

        protected Void doInBackground(Long... timestamps) {
            Log.i("Map", "Started loading stored data task");
            mLoadedData = new ArrayList<>();

            long tsFrom = timestamps[0];
            long tsTo = timestamps[1];

            long dayFrom = Utils.roundToDay(tsFrom);
            long dayTo = Utils.roundToDay(tsTo);

            Log.i("Map", "Day from: " + dayFrom);
            Log.i("Map", "Day to: " + dayTo);

            // Go through filenames in Airspeck directory
            File dir = new File(Constants.AIRSPECK_DATA_DIRECTORY_PATH);
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (File file : directoryListing) {
                    String fileDate = file.getName().split(" ")[0];
                    try {
                        // If file lies in specified time period, open it and read content
                        long tsFile = Utils.timestampFromString(fileDate, "yyyy-MM-dd");

                        if (tsFile >= dayFrom && tsFile <= dayTo) {
                            BufferedReader reader = new BufferedReader(new FileReader(file));
                            // Skip first line as that's the header
                            reader.readLine();
                            String currentLine;
                            while ((currentLine = reader.readLine()) != null) {
                                String[] row = currentLine.split(",");
                                long tsRow = Long.parseLong(row[1]);
                                // Only if the timestamp of the currently read line is in specified time period,
                                // do we draw a circle on the map corresponding to the measurements
                                if (tsRow >= tsFrom && tsRow <= tsTo) {
                                    LatLng circleLocation = new LatLng(Double.parseDouble(row[27]),
                                            Double.parseDouble(row[26]));
                                    AirspeckMapData readSample = new AirspeckMapData(tsRow, circleLocation,
                                            Float.parseFloat(row[2]), Float.parseFloat(row[3]),
                                            Float.parseFloat(row[4]));

                                    mLoadedData.add(readSample);
                                }
                            }
                            reader.close();
                        }
                    } catch (IOException | ParseException e) {
                        e.printStackTrace();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.i("Map", "Incomplete Airspeck data row (might be because location is missing");
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            updateCircles();
        }
    }

    private void updateCircles() {
        // Check if there is something to draw
        if (mLoadedData.size() > 0) {
            // Bounds builder to calculate zoom location and factor so that all markers are in view
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            // Iterate through data and draw on map
            for (AirspeckMapData data : mLoadedData) {
                drawCircleOnMap(data);
                builder.include(data.getLocation());
            }

            LatLngBounds bounds = builder.build();
            int padding = 30; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.moveCamera(cu);
            cu = CameraUpdateFactory.zoomTo(18);
            mMap.moveCamera(cu);
        } else {
            Toast.makeText(getApplicationContext(), "No data in selected time period",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
