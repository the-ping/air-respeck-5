package com.specknet.airrespeck.activities;

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
import android.support.annotation.NonNull;
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
import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.AirspeckMapData;
import com.specknet.airrespeck.models.LocationData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Locale;

public class MapsAQActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Deque<AirspeckMapData> mQueueMapData;
    private final int MAX_DISPLAYED_DATA = 10000;
    private BroadcastReceiver mBroadcastReceiver;

    private LatLng mLastLatLng;

    public static final int MAP_TYPE_LIVE = 0;
    public static final int MAP_TYPE_HISTORICAL = 1;
    public static final String MAP_TYPE = "map type";
    public static final String TIMESTAMP_FROM = "ts from";
    public static final String TIMESTAMP_TO = "ts to";


    // Default is the live map
    private int mapType = MAP_TYPE_LIVE;

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
        mQueueMapData = new LinkedList<>();

        if (mapType == MAP_TYPE_LIVE) {
            boolean locationPermissionGranted = Utils.checkAndRequestLocationPermission(MapsAQActivity.this);
            if (locationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                zoomToLastKnownLocation();

                // Load last 6 hours of data
                long tsTo = new Date().getTime();
                long tsFrom = (long) (tsTo - 1000. * 60 * 60 * 6);
                loadStoredData(tsFrom, tsTo);
                startLiveAQUpdate();
            }
        } else if (mapType == MAP_TYPE_HISTORICAL) {
            long tsFrom = (long) getIntent().getExtras().get(TIMESTAMP_FROM);
            long tsTo = (long) getIntent().getExtras().get(TIMESTAMP_TO);
            loadStoredData(tsFrom, tsTo);
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == Constants.REQUEST_CODE_LOCATION_PERMISSION) {
            Log.i("AirspeckMap", "onRequestPermissionResult: " + grantResults[0]);
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted. Initialise map position
                onMapReady(mMap);
            } else {
                // Permission was not granted. Explain to the user why we need permission and ask again
                Utils.showLocationRequestDialog(MapsAQActivity.this);
            }
        }
    }

    // Initial zoom. This will be updated as soon as we get a location from the PhoneGPSService
    private void zoomToLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        boolean permissionGranted = Utils.checkAndRequestLocationPermission(MapsAQActivity.this);
        if (permissionGranted) {
            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
            if (location != null) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(),
                                location.getLongitude()))      // Sets the center of the map to location user
                        .zoom(18)                   // Sets the zoom
                        .build();                   // Creates a CameraPosition from the builder
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    private void startLiveAQUpdate() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case Constants.ACTION_AIRSPECK_LIVE_BROADCAST:
                        if (mLastLatLng != null) {
                            AirspeckData allData = (AirspeckData) intent.getSerializableExtra(Constants.AIRSPECK_DATA);
                            LatLng dataLatLng = new LatLng(allData.getLocation().getLatitude(),
                                    allData.getLocation().getLongitude());
                            AirspeckMapData newData = new AirspeckMapData(dataLatLng, allData.getPm1(),
                                    allData.getPm2_5(), allData.getPm10());
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

                        mMap.animateCamera(CameraUpdateFactory.newLatLng(newLatLng));

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

            int colorIdx = (int) (maxPerc * 100.);
            // Return weighted mix between low and high pollution color depending on maxPerc
            String[] cmap = getResources().getStringArray(R.array.viridis);
            return Color.parseColor(cmap[colorIdx]);
        }
    }

    public void updateMarkers() {
        mMap.clear();
        for (AirspeckMapData airspeckDataItem : mQueueMapData) {
            drawCircleOnMap(airspeckDataItem);
        }
    }

    private void drawCircleOnMap(AirspeckMapData airspeckDataItem) {
        int circleColor = getMobileCircleColor(airspeckDataItem);
        mMap.addCircle(new CircleOptions().center(airspeckDataItem.getLocation()).radius(10)
                .fillColor(circleColor).strokeColor(circleColor).strokeWidth(1));
        // Log.i("AirspeckMap", "Circle painted at location: " + airspeckDataItem.getLocation());
    }


    @Override
    protected void onDestroy() {
        try {
            if (mapType == MAP_TYPE_LIVE) {
                unregisterReceiver(mBroadcastReceiver);
            }
        } catch (Exception e) {
            // Receiver might not be registered. Do nothing in this case
        }
        super.onDestroy();
    }

    private void loadStoredData(long tsFrom, long tsTo) {
        // Create new task for loading data
        new LoadStoredDataTask().execute(tsFrom, tsTo);
    }

    private class LoadStoredDataTask extends AsyncTask<Long, Integer, Void> {

        protected Void doInBackground(Long... timestamps) {
            Log.i("AirspeckMap", "Started loading stored data task");

            long tsFrom = timestamps[0];
            long tsTo = timestamps[1];

            long dayFrom = Utils.roundToDay(tsFrom);
            long dayTo = Utils.roundToDay(tsTo);

            Log.i("AirspeckMap", "Day from: " + dayFrom);
            Log.i("AirspeckMap", "Day to: " + dayTo);

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
                                    LatLng circleLocation = new LatLng(Double.parseDouble(row[29]),
                                            Double.parseDouble(row[28]));
                                    AirspeckMapData readSample = new AirspeckMapData(circleLocation,
                                            Float.parseFloat(row[2]), Float.parseFloat(row[3]),
                                            Float.parseFloat(row[4]));

                                    mQueueMapData.add(readSample);
                                }
                            }
                            reader.close();
                        }
                    } catch (IOException | ParseException e) {
                        e.printStackTrace();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.i("AirspeckMap", "Incomplete Airspeck data row (might be because location is missing");
                    }
                }
            }
            Log.i("AirspeckMap", "Updating map with " + mQueueMapData.size() + " number of elements");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            updateCircles();
        }
    }

    private void updateCircles() {
        // Check if there is something to draw
        if (mQueueMapData.size() > 0) {
            // Bounds builder to calculate zoom location and factor so that all markers are in view
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            // Iterate through data and draw on map
            for (AirspeckMapData data : mQueueMapData) {
                drawCircleOnMap(data);
                builder.include(data.getLocation());
            }

            // Only move camera to historical data if we're in historical mode
            if (mapType == MAP_TYPE_HISTORICAL) {
                LatLngBounds bounds = builder.build();
                int padding = 30; // offset from edges of the map in pixels
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                mMap.moveCamera(cu);
            }
        } else {
            // Close activity if we wanted to display historical data and there is none
            if (mapType == MAP_TYPE_HISTORICAL) {
                Toast.makeText(getApplicationContext(), "No data in selected time period",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
