package com.specknet.airrespeck.activities;


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
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.SectionsPagerAdapter;
import com.specknet.airrespeck.fragments.AQReadingsFragment;
import com.specknet.airrespeck.fragments.ActivitySummaryFragment;
import com.specknet.airrespeck.fragments.DaphneHomeFragment;
import com.specknet.airrespeck.fragments.DaphneValuesFragment;
import com.specknet.airrespeck.fragments.GraphsFragment;
import com.specknet.airrespeck.fragments.HomeFragment;
import com.specknet.airrespeck.services.SpeckBluetoothService;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.LocationUtils;
import com.specknet.airrespeck.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends BaseActivity {

    // UI HANDLER
    public final static int UPDATE_RESPECK_READINGS = 0;
    public final static int UPDATE_QOE_READINGS = 1;
    public final static int SHOW_SNACKBAR_MESSAGE = 2;
    public final static int SHOW_RESPECK_CONNECTED = 3;
    public final static int SHOW_RESPECK_DISCONNECTED = 4;
    public final static int SHOW_AIRSPECK_CONNECTED = 5;
    public final static int SHOW_AIRSPECK_DISCONNECTED = 6;
    private static final int ACTIVITY_SUMMARY_UPDATE = 7;


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
                        // We also update the connection symbol in case it hasn't been updated yet
                        service.updateRESpeckConnectionSymbol(true);
                        break;
                    case UPDATE_QOE_READINGS:
                        service.updateQOEReadings((HashMap<String, Float>) msg.obj);
                        // We also update the connection symbol in case it hasn't been updated yet
                        service.updateAirspeckConnectionSymbol(true);
                        break;

                    case ACTIVITY_SUMMARY_UPDATE:
                        service.updateActivitySummary();
                        break;
                    case SHOW_SNACKBAR_MESSAGE:
                        service.showSnackbar((String) msg.obj);
                        break;
                    case SHOW_AIRSPECK_CONNECTED:
                        String messageAir = String.format(Locale.UK, "QOE "
                                + service.getString(R.string.device_connected)
                                + ". " + service.getString(R.string.waiting_for_data)
                                + ".");
                        service.showSnackbar(messageAir);
                        service.updateAirspeckConnectionSymbol(true);
                        break;
                    case SHOW_AIRSPECK_DISCONNECTED:
                        service.updateAirspeckConnectionSymbol(false);
                        break;
                    case SHOW_RESPECK_CONNECTED:
                        String messageRE = String.format(Locale.UK, "Respeck "
                                + service.getString(R.string.device_connected)
                                + ". " + service.getString(R.string.waiting_for_data)
                                + ".");
                        service.showSnackbar(messageRE);
                        service.updateRESpeckConnectionSymbol(true);
                        break;
                    case SHOW_RESPECK_DISCONNECTED:
                        service.updateRESpeckConnectionSymbol(false);
                        break;
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
    private static final String TAG_DAPHNE_HOME_FRAGMENT = "DAPHNE_HOME_FRAGMENT";
    private static final String TAG_DAPHNE_VALUES_FRAGMENT = "DAPHNE_VALUES_FRAGMENT";
    private static final String TAG_ACTIVITY_SUMMARY_FRAGMENT = "ACTIVITY_SUMMARY_FRAGMENT";

    private DaphneHomeFragment mDaphneHomeFragment;
    private DaphneValuesFragment mDaphneValuesFragment;
    private HomeFragment mHomeFragment;
    private AQReadingsFragment mAQReadingsFragment;
    private GraphsFragment mGraphsFragment;
    private Fragment mCurrentFragment;
    private ActivitySummaryFragment mActivitySummaryFragment;

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

    // Variable to switch modes: subject mode or supervised mode
    private static final String IS_SUPERVISED_MODE = "supervised_mode";
    boolean isSupervisedMode;

    TabLayout tabLayout;
    ViewPager viewPager;
    ArrayList<Fragment> supervisedFragments;
    ArrayList<String> supervisedTitles;
    ArrayList<Fragment> subjectFragments;
    ArrayList<String> subjectTitles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the part of the layout which is the same for both modes
        setContentView(R.layout.activity_main_tabs);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        viewPager = (ViewPager) findViewById(R.id.view_pager);

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

        setupFragments(savedInstanceState);
        setupViewPager();

        // Load mode if stored. If no mode was stored, this defaults to false, i.e. subject mode
        if (savedInstanceState != null) {
            isSupervisedMode = savedInstanceState.getBoolean(IS_SUPERVISED_MODE);
        } else {
            isSupervisedMode = false;
        }

        // Call displayMode methods so the tabs are set correctly
        if (isSupervisedMode) {
            displaySupervisedMode();
        } else {
            displaySubjectMode();
        }

        // Add the toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // For use with snack bar (notification bar at the bottom of the screen)
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        // Initialize Readings hash maps
        initReadingMaps();

        // Start Speck Service
        mSpeckBluetoothService = new SpeckBluetoothService();
        mSpeckBluetoothService.initSpeckService(this);

        startActivitySummaryUpdaterTask();
    }

    private void setupViewPager() {
        // Setup supervised mode arrays
        supervisedFragments = new ArrayList<>();
        supervisedTitles = new ArrayList<>();
        supervisedFragments.add(mHomeFragment);
        supervisedTitles.add(getString(R.string.menu_home));
        supervisedFragments.add(mAQReadingsFragment);
        supervisedTitles.add(getString(R.string.menu_air_quality));
        supervisedFragments.add(mActivitySummaryFragment);
        supervisedTitles.add(getString(R.string.menu_activity_summary));
        if (mGraphsScreen) {
            supervisedFragments.add(mGraphsFragment);
            supervisedTitles.add(getString(R.string.menu_graphs));
        }

        // Setup subject mode arrays
        subjectFragments = new ArrayList<>();
        subjectTitles = new ArrayList<>();
        subjectFragments.add(mDaphneHomeFragment);
        // We don't want any text in the subject view, just the icons
        subjectTitles.add("");
        subjectFragments.add(mDaphneValuesFragment);
        subjectTitles.add("");

        // Set the PagerAdapter. It will check in which mode we are and load the corresponding Fragments
        viewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));
    }

    private void setupFragments(Bundle savedInstanceState) {
        // Load or create fragments
        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState != null) {
            // If we have saved something from a previous activity lifecycle, the fragments probably already exist
            mHomeFragment =
                    (HomeFragment) fm.getFragment(savedInstanceState, TAG_HOME_FRAGMENT);
            mAQReadingsFragment =
                    (AQReadingsFragment) fm.getFragment(savedInstanceState, TAG_AQREADINGS_FRAGMENT);
            mGraphsFragment =
                    (GraphsFragment) fm.getFragment(savedInstanceState, TAG_GRAPHS_FRAGMENT);
            mDaphneHomeFragment = (DaphneHomeFragment) fm.getFragment(savedInstanceState, TAG_DAPHNE_HOME_FRAGMENT);
            mDaphneValuesFragment = (DaphneValuesFragment) fm.getFragment(savedInstanceState,
                    TAG_DAPHNE_VALUES_FRAGMENT);

            mCurrentFragment = fm.getFragment(savedInstanceState, TAG_CURRENT_FRAGMENT);

            mActivitySummaryFragment = (ActivitySummaryFragment) fm.getFragment(savedInstanceState,
                    TAG_ACTIVITY_SUMMARY_FRAGMENT);

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
            if (mDaphneHomeFragment == null) {
                mDaphneHomeFragment = new DaphneHomeFragment();
            }
            if (mDaphneValuesFragment == null) {
                mDaphneValuesFragment = new DaphneValuesFragment();
            }
            if (mCurrentFragment == null) {
                mCurrentFragment = mDaphneHomeFragment;
            }
            if (mActivitySummaryFragment == null) {
                mActivitySummaryFragment = new ActivitySummaryFragment();
            }
        } else {
            // If there is no saved instance state, this means we are starting the activity for the first time
            // Create all the fragments
            mHomeFragment = new HomeFragment();
            mAQReadingsFragment = new AQReadingsFragment();
            mGraphsFragment = new GraphsFragment();
            mDaphneHomeFragment = new DaphneHomeFragment();
            mDaphneValuesFragment = new DaphneValuesFragment();
            mActivitySummaryFragment = new ActivitySummaryFragment();

            // Set Daphne home fragment to be the currently displayed one
            mCurrentFragment = mDaphneHomeFragment;
        }
    }

    private void startActivitySummaryUpdaterTask() {
        final int delay = 2000;//10*60*1000;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = ACTIVITY_SUMMARY_UPDATE;
                msg.setTarget(mUIHandler);
                msg.sendToTarget();
            }
        }, 0, delay);
    }


    private void updateActivitySummary() {
        mActivitySummaryFragment.updateActivitySummary();
    }

    private void displaySupervisedMode() {
        isSupervisedMode = true;

        // Update displayed Fragments to reflect mode
        ((SectionsPagerAdapter) viewPager.getAdapter()).setDisplayedFragments(supervisedFragments, supervisedTitles);
        viewPager.setCurrentItem(0);

        // Update tab icons
        tabLayout.setupWithViewPager(viewPager);

        if (mMenuTabIconsPref) {
            tabLayout.getTabAt(0).setIcon(Constants.MENU_ICON_HOME);
            tabLayout.getTabAt(1).setIcon(Constants.MENU_ICON_AIR);
            tabLayout.getTabAt(2).setIcon(Constants.MENU_ICON_ACTIVITY);

            if (mGraphsScreen) {
                tabLayout.getTabAt(3).setIcon(Constants.MENU_ICON_GRAPHS);
            }

        }

        // Recreate options menu
        invalidateOptionsMenu();
    }

    private void displaySubjectMode() {
        isSupervisedMode = false;

        // Update displayed Fragments to reflect mode
        ((SectionsPagerAdapter) viewPager.getAdapter()).setDisplayedFragments(subjectFragments, subjectTitles);
        viewPager.setCurrentItem(0);

        tabLayout.setupWithViewPager(viewPager);

        tabLayout.getTabAt(0).setIcon(Constants.MENU_ICON_HOME);
        tabLayout.getTabAt(0).setText("");
        tabLayout.getTabAt(1).setIcon(Constants.MENU_ICON_INFO);
        tabLayout.getTabAt(1).setText("");

        // Recreate options menu
        invalidateOptionsMenu();
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
        outState.putBoolean(IS_SUPERVISED_MODE, isSupervisedMode);
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
        if (mDaphneHomeFragment != null && mDaphneHomeFragment.isAdded()) {
            fm.putFragment(outState, TAG_DAPHNE_HOME_FRAGMENT, mDaphneHomeFragment);
        }
        if (mDaphneValuesFragment != null && mDaphneValuesFragment.isAdded()) {
            fm.putFragment(outState, TAG_DAPHNE_VALUES_FRAGMENT, mDaphneValuesFragment);
        }
        if (mActivitySummaryFragment != null && mActivitySummaryFragment.isAdded()) {
            fm.putFragment(outState, TAG_ACTIVITY_SUMMARY_FRAGMENT, mActivitySummaryFragment);
        }
        if (mCurrentFragment != null && mCurrentFragment.isAdded()) {
            fm.putFragment(outState, TAG_CURRENT_FRAGMENT, mCurrentFragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu, this adds items to the action bar if it is present.
        if (isSupervisedMode) {
            getMenuInflater().inflate(R.menu.menu_supervised, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_subject, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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
        } else if (id == R.id.action_supervised_mode) {
            displaySupervisedMode();
        } else if (id == R.id.action_subject_mode) {
            displaySubjectMode();
        }

        return super.onOptionsItemSelected(item);
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
        mRespeckSensorReadings.put(Constants.RESPECK_BATTERY_PERCENT, 0f);
        mRespeckSensorReadings.put(Constants.RESPECK_REQUEST_CHARGE, 0f);
    }

    /**
     * Update Respeck reading values
     * We need to separate Respeck and QOE values as both update at different rates
     */
    private void updateRespeckUI() {
        updateConnectionLoadingLayout();
        try {
            if (isSupervisedMode) {
                ArrayList<Float> listValues = new ArrayList<Float>();

                listValues.add(mUtils.roundToTwoDigits(mRespeckSensorReadings.get(Constants.RESPECK_BREATHING_RATE)));
                listValues.add(mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM2_5)));
                listValues.add(mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM10)));
                //listValues.add(mQOESensorReadings.get(Constants.LOC_LATITUDE));
                //listValues.add(mQOESensorReadings.get(Constants.LOC_LONGITUDE));

                mHomeFragment.setReadings(listValues);

                // Graphs fragment UI
                mGraphsFragment.addBreathingSignalData(
                        mUtils.roundToTwoDigits(mRespeckSensorReadings.get(Constants.RESPECK_BREATHING_SIGNAL)));
            } else {
                mDaphneValuesFragment.updateBreathing(mRespeckSensorReadings);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Update QOE reading values
     * We need to separate Respeck and QOE values as both update at different rates
     */
    private void updateQOEUI() {
        updateConnectionLoadingLayout();

        // Air Quality fragment UI
        try {
            if (isSupervisedMode) {
                HashMap<String, Float> values = new HashMap<String, Float>();

                values.put(Constants.QOE_TEMPERATURE,
                        mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_TEMPERATURE)));
                values.put(Constants.QOE_HUMIDITY,
                        mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_HUMIDITY)));
                values.put(Constants.QOE_O3, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_O3)));
                values.put(Constants.QOE_NO2, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_NO2)));
                values.put(Constants.QOE_PM1, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM1)));
                values.put(Constants.QOE_PM2_5, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM2_5)));
                values.put(Constants.QOE_PM10, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM10)));
                values.put(Constants.QOE_BINS_TOTAL,
                        mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_BINS_TOTAL)));

                mAQReadingsFragment.setReadings(values);

                // Graphs fragment UI
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
            } else {
                // Daphne values fragment
                mDaphneValuesFragment.updateQOEReadings(mQOESensorReadings);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateConnectionLoadingLayout() {
        boolean isConnecting = mSpeckBluetoothService.isConnecting();
        // TODO: show loading symbol instead of X-mark in subject mode
        if (isSupervisedMode) {
            mHomeFragment.showConnecting(isConnecting);
            mAQReadingsFragment.showConnecting(isConnecting);
            mGraphsFragment.showConnecting(isConnecting);
        }
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

    private void updateRESpeckConnectionSymbol(boolean isConnected) {
        if (!isSupervisedMode) {
            mDaphneHomeFragment.updateRESpeckConnectionSymbol(isConnected);
        }
    }

    private void updateAirspeckConnectionSymbol(boolean isConnected) {
        if (!isSupervisedMode) {
            mDaphneHomeFragment.updateAirspeckConnectionSymbol(isConnected);
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(mCoordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }
}
