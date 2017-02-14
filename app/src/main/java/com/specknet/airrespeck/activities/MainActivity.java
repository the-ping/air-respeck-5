package com.specknet.airrespeck.activities;


import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceActivity;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.SectionsPagerAdapter;
import com.specknet.airrespeck.fragments.AQReadingsFragment;
import com.specknet.airrespeck.fragments.GraphsFragment;
import com.specknet.airrespeck.fragments.HomeFragment;
import com.specknet.airrespeck.fragments.MenuFragment;
import com.specknet.airrespeck.services.SpeckBluetoothService;
import com.specknet.airrespeck.services.qoeuploadservice.QOERemoteUploadService;
import com.specknet.airrespeck.services.respeckuploadservice.RespeckRemoteUploadService;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.LocationUtils;
import com.specknet.airrespeck.utils.Utils;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


public class MainActivity extends BaseActivity implements MenuFragment.OnMenuSelectedListener {

    // UI HANDLER
    public final static int UPDATE_RESPECK_READINGS = 0;
    public final static int UPDATE_QOE_READINGS = 1;
    public final static int SHOW_SNACKBAR_MESSAGE = 2;

    /**
     * Static inner class doesn't hold an implicit reference to the outer class
     */
    private static class UIHandler extends Handler {
        // Using a weak reference means you won't prevent garbage collection
        private final WeakReference<MainActivity> mService;

        public UIHandler(MainActivity service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            final int what = msg.what;

            MainActivity service = mService.get();

            if (service != null) {
                switch (what) {
                    case UPDATE_RESPECK_READINGS:
                        service.updateRespeckReadings((HashMap<String, Float>) msg.obj);
                        break;
                    case UPDATE_QOE_READINGS:
                        service.updateQOEReadings((HashMap<String, Float>) msg.obj);
                        break;
                    case SHOW_SNACKBAR_MESSAGE:
                        service.showSnackbar((String) msg.obj);
                }
            }
        }
    }

    /**
     * A getter for the UI handler
     *
     * @return UIHandler The handler.
     */
    public Handler getUIHandler() {
        return mUIHandler;
    }

    private final Handler mUIHandler = new UIHandler(this);

    // FRAGMENTS
    private static final String TAG_HOME_FRAGMENT = "HOME_FRAGMENT";
    private static final String TAG_AQREADINGS_FRAGMENT = "AQREADINGS_FRAGMENT";
    private static final String TAG_GRAPHS_FRAGMENT = "GRAPHS_FRAGMENT";
    private static final String TAG_CURRENT_FRAGMENT = "CURRENT_FRAGMENT";

    private HomeFragment mHomeFragment;
    private AQReadingsFragment mAQReadingsFragment;
    private GraphsFragment mGraphsFragment;
    private Fragment mCurrentFragment;

    // UTILS
    Utils mUtils;
    LocationUtils mLocationUtils;

    // Layout view for snack bar
    private CoordinatorLayout mCoordinatorLayout;

    // READING VALUES
    HashMap<String, Float> mRespeckSensorReadings;
    HashMap<String, Float> mQOESensorReadings;

    // Speck service
    SpeckBluetoothService mSpeckBluetoothService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Keep CPU running. TODO: Do we really need this? From Android developers: Creating and holding wake locks
        can have a dramatic impact on the host device's battery life. Thus you should use wake locks only when
        strictly necessary and hold them for as short a time as possible. For example, you should never need to
        use a wake lock in an activity. As described above, if you want to keep the screen on in your activity,
        use FLAG_KEEP_SCREEN_ON.
         */
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();

        // Utils
        mUtils = Utils.getInstance(this);

        // Load location Utils
        mLocationUtils = LocationUtils.getInstance(this);
        mLocationUtils.startLocationManager();

        // Set activity title
        this.setTitle(getString(R.string.app_name) + ", v" + mUtils.getAppVersionName());

        // Initialize fragments
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState != null) {
            // If we have saved something from a previous activity lifecycle, the fragments probably already exist
            mHomeFragment =
                    (HomeFragment) fm.getFragment(savedInstanceState, TAG_HOME_FRAGMENT);
            mAQReadingsFragment =
                    (AQReadingsFragment) fm.getFragment(savedInstanceState, TAG_AQREADINGS_FRAGMENT);
            mGraphsFragment =
                    (GraphsFragment) fm.getFragment(savedInstanceState, TAG_GRAPHS_FRAGMENT);
            mCurrentFragment = fm.getFragment(savedInstanceState, TAG_CURRENT_FRAGMENT);

            // If they don't exist, which could happen because the activity was paused before loading the fragments,
            // create new fragments
            if (mHomeFragment == null) {
                mHomeFragment = new HomeFragment();
            }
            if (mAQReadingsFragment == null) {
                mAQReadingsFragment = new AQReadingsFragment();
            }
            if (mGraphsFragment == null) {
                mGraphsFragment = new GraphsFragment();
            }
            if (mCurrentFragment == null) {
                mCurrentFragment = mHomeFragment;
            }
        } else {
            // If there is no saved instance state, this means we are starting the activity for the first time
            // Create all the fragments
            mHomeFragment = new HomeFragment();
            mAQReadingsFragment = new AQReadingsFragment();
            mGraphsFragment = new GraphsFragment();
            // Set home fragment to be the currently displayed one
            mCurrentFragment = mHomeFragment;
        }

        // Display currently selected fragment layout
        if (mMenuModePref.equals(Constants.MENU_MODE_BUTTONS)) {
            setContentView(R.layout.activity_main_buttons);

            FragmentTransaction trans = fm.beginTransaction();

            if (mCurrentFragment instanceof HomeFragment) {
                trans.replace(R.id.content, mCurrentFragment, TAG_HOME_FRAGMENT);
            } else if (mCurrentFragment instanceof AQReadingsFragment) {
                trans.replace(R.id.content, mCurrentFragment, TAG_AQREADINGS_FRAGMENT);
            } else if (mCurrentFragment instanceof GraphsFragment) {
                trans.replace(R.id.content, mCurrentFragment, TAG_GRAPHS_FRAGMENT);
            } else {
                trans.replace(R.id.content, mHomeFragment, TAG_HOME_FRAGMENT);
                mCurrentFragment = mHomeFragment;
            }

            trans.commit();
        } else if (mMenuModePref.equals(Constants.MENU_MODE_TABS)) {
            setContentView(R.layout.activity_main_tabs);

            // Create the adapter that will return a fragment for each of the three
            // primary sections of the activity.
            SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(),
                    getApplicationContext());
            sectionsPagerAdapter.addFragment(mHomeFragment);
            sectionsPagerAdapter.addFragment(mAQReadingsFragment);
            if (mGraphsScreen) {
                sectionsPagerAdapter.addFragment(mGraphsFragment);
            }
            // Set up the ViewPager with the sections adapter.
            ViewPager viewPager = (ViewPager) findViewById(R.id.container);
            if (viewPager != null) {
                viewPager.setAdapter(sectionsPagerAdapter);
            }

            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            if (tabLayout != null) {
                tabLayout.setupWithViewPager(viewPager);
            }

            if (mMenuTabIconsPref) {
                tabLayout.getTabAt(0).setIcon(Constants.MENU_ICON_HOME);
                tabLayout.getTabAt(1).setIcon(Constants.MENU_ICON_AIR);
                if (mGraphsScreen) {
                    tabLayout.getTabAt(2).setIcon(Constants.MENU_ICON_GRAPHS);
                }
            }
        }

        // Add the toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // For use with snack bar
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);


        // Initialize Readings hash maps
        initReadingMaps();

        // Start Speck Service
        mSpeckBluetoothService = new SpeckBluetoothService();
        mSpeckBluetoothService.initSpeckService(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Bluetooth startup
        mSpeckBluetoothService.startServiceAndBluetoothScanning();

        // Start location manager
        //mLocationUtils.startLocationManager();
    }

    @Override
    public void onPause() {
        super.onPause();

        mSpeckBluetoothService.stopBluetoothScanning();

        // Stop location manager
        // mLocationUtils.stopLocationManager();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSpeckBluetoothService.stopSpeckService();
        // Stop location manager
        //mLocationUtils.stopLocationManager();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        FragmentManager fm = getSupportFragmentManager();

        if (mHomeFragment != null && mHomeFragment.isAdded()) {
            fm.putFragment(outState, TAG_HOME_FRAGMENT, mHomeFragment);
        }

        if (mAQReadingsFragment != null && mAQReadingsFragment.isAdded()) {
            fm.putFragment(outState, TAG_AQREADINGS_FRAGMENT, mAQReadingsFragment);
        }

        if (mGraphsFragment != null && mGraphsFragment.isAdded()) {
            fm.putFragment(outState, TAG_GRAPHS_FRAGMENT, mGraphsFragment);
        }

        if (mCurrentFragment != null && mCurrentFragment.isAdded()) {
            fm.putFragment(outState, TAG_CURRENT_FRAGMENT, mCurrentFragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu, this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setVisible(mRespeckAppAccessPref);
        menu.getItem(1).setVisible(mAirspeckAppAccessPref);

        if (Objects.equals(mCurrentUser.getGender(), "M")) {
            menu.getItem(2).setIcon(R.drawable.ic_user_male);
        } else if (Objects.equals(mCurrentUser.getGender(), "F")) {
            menu.getItem(2).setIcon(R.drawable.ic_user_female);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStattement
        if (id == R.id.action_user_profile) {
            startActivity(new Intent(this, UserProfileActivity.class));
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.SettingsFragment.class.getName());
            intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
            startActivity(intent);
        } else if (id == R.id.action_airspeck) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.airquality.sepa",
                        "com.airquality.sepa.DataCollectionActivity"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, R.string.airspeck_not_found, Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.action_respeck) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.pulrehab",
                        "com.pulrehab.fragments.MainActivity"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, R.string.respeck_not_found, Toast.LENGTH_LONG).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onButtonSelected(int buttonId) {
        switch (buttonId) {
            // Home
            case 0:
                replaceFragment(mHomeFragment, TAG_HOME_FRAGMENT);
                break;
            // Air Quality
            case 1:
                replaceFragment(mAQReadingsFragment, TAG_AQREADINGS_FRAGMENT);
                break;
            // Dashboard
            case 2:
                replaceFragment(mGraphsFragment, TAG_GRAPHS_FRAGMENT);
                break;
        }
    }

    /**
     * Replace the current fragment with the given one.
     *
     * @param fragment Fragment New fragment.
     * @param tag      String Tag for the new fragment.
     */

    public void replaceFragment(Fragment fragment, String tag) {
        replaceFragment(fragment, tag, false);
    }

    /**
     * Replace the current fragment with the given one.
     *
     * @param fragment       Fragment New fragment.
     * @param tag            String Tag for the new fragment.
     * @param addToBackStack boolean Whether to add the previous fragment to the Back Stack, or not.
     */
    public void replaceFragment(Fragment fragment, String tag, boolean addToBackStack) {
        if (fragment.isVisible()) {
            return;
        }

        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        //trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        trans.replace(R.id.content, fragment, tag);
        if (addToBackStack) {
            trans.addToBackStack(null);
        }
        trans.commit();

        mCurrentFragment = fragment;
    }


    //----------------------------------------------------------------------------------------------
    // UI ------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    /**
     * Initialize hashmaps for sensor reading values
     */
    private void initReadingMaps() {
        mQOESensorReadings = new HashMap<String, Float>();
        mQOESensorReadings.put(Constants.QOE_PM1, 0f);
        mQOESensorReadings.put(Constants.QOE_PM2_5, 0f);
        mQOESensorReadings.put(Constants.QOE_PM10, 0f);
        mQOESensorReadings.put(Constants.QOE_TEMPERATURE, 0f);
        mQOESensorReadings.put(Constants.QOE_HUMIDITY, 0f);
        mQOESensorReadings.put(Constants.QOE_NO2, 0f);
        mQOESensorReadings.put(Constants.QOE_O3, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_0, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_1, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_2, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_3, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_4, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_5, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_6, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_7, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_8, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_9, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_10, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_11, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_12, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_13, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_14, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_15, 0f);
        mQOESensorReadings.put(Constants.QOE_BINS_TOTAL, 0f);

        mRespeckSensorReadings = new HashMap<String, Float>();
        mRespeckSensorReadings.put(Constants.RESPECK_X, 0f);
        mRespeckSensorReadings.put(Constants.RESPECK_Y, 0f);
        mRespeckSensorReadings.put(Constants.RESPECK_Z, 0f);
        mRespeckSensorReadings.put(Constants.RESPECK_BREATHING_RATE, 0f);
        mRespeckSensorReadings.put(Constants.RESPECK_BREATHING_SIGNAL, 0f);
    }

    /**
     * Update Respeck reading values
     * We need to separate Respeck and QOE values as both update at different rates
     */
    private void updateRespeckUI() {
        // Home fragment UI
        try {
            ArrayList<Float> listValues = new ArrayList<Float>();

            listValues.add(mUtils.roundToTwoDigits(mRespeckSensorReadings.get(Constants.RESPECK_BREATHING_RATE)));
            listValues.add(mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM2_5)));
            listValues.add(mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM10)));
            //listValues.add(mQOESensorReadings.get(Constants.LOC_LATITUDE));
            //listValues.add(mQOESensorReadings.get(Constants.LOC_LONGITUDE));

            mHomeFragment.setReadings(listValues);
        } catch (Exception e) { e.printStackTrace(); }

        // Graphs fragment UI
        try {
            mGraphsFragment.addBreathingSignalData(
                    mUtils.roundToTwoDigits(mRespeckSensorReadings.get(Constants.RESPECK_BREATHING_SIGNAL)));
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Update QOE reading values
     * We need to separate Respeck and QOE values as both update at different rates
     */
    private void updateQOEUI() {
        // Update connection loading layout
        boolean isConnecting = mSpeckBluetoothService.isConnecting();
        mHomeFragment.showConnecting(isConnecting);
        mAQReadingsFragment.showConnecting(isConnecting);
        mGraphsFragment.showConnecting(isConnecting);


        // Air Quality fragment UI
        try {
            HashMap<String, Float> values = new HashMap<String, Float>();

            values.put(Constants.QOE_TEMPERATURE,
                    mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_TEMPERATURE)));
            values.put(Constants.QOE_HUMIDITY, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_HUMIDITY)));
            values.put(Constants.QOE_O3, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_O3)));
            values.put(Constants.QOE_NO2, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_NO2)));
            values.put(Constants.QOE_PM1, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM1)));
            values.put(Constants.QOE_PM2_5, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM2_5)));
            values.put(Constants.QOE_PM10, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM10)));
            values.put(Constants.QOE_BINS_TOTAL,
                    mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_BINS_TOTAL)));

            mAQReadingsFragment.setReadings(values);
        } catch (Exception e) { e.printStackTrace(); }

        // Graphs fragment UI
        try {
            ArrayList<Float> listValues = new ArrayList<Float>();

            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_0));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_1));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_2));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_3));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_4));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_5));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_6));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_7));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_8));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_9));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_10));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_11));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_12));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_13));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_14));
            listValues.add(mQOESensorReadings.get(Constants.QOE_BINS_15));

            mGraphsFragment.setBinsChartData(listValues);

            mGraphsFragment.addPMsChartData(new GraphsFragment.PMs(
                    mQOESensorReadings.get(Constants.QOE_PM1),
                    mQOESensorReadings.get(Constants.QOE_PM2_5),
                    mQOESensorReadings.get(Constants.QOE_PM10)));
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Update {@link #mRespeckSensorReadings} with the latest values sent from the Respeck sensor.
     *
     * @param newValues HashMap<String, Float> The Respeck sensor readings.
     */
    private void updateRespeckReadings(HashMap<String, Float> newValues) {
        // Update local values
        mRespeckSensorReadings = newValues;

        // Update the UI
        updateRespeckUI();
    }

    /**
     * Update {@link #mQOESensorReadings} with the latest values sent from the QOE sensor.
     *
     * @param newValues HashMap<String, Float> The QOE sensor readings.
     */
    private void updateQOEReadings(HashMap<String, Float> newValues) {
        // Update local values
        mQOESensorReadings = newValues;

        // Update the UI
        updateQOEUI();
    }

    private void showSnackbar(String message) {
        Snackbar.make(mCoordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    //----------------------------------------------------------------------------------------------
    // UPLOAD SERVICES -----------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------


}
