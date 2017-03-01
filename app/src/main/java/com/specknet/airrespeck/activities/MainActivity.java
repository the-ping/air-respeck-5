package com.specknet.airrespeck.activities;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.SectionsPagerAdapter;
import com.specknet.airrespeck.fragments.SupervisedActivitySummaryFragment;
import com.specknet.airrespeck.fragments.SupervisedAirspeckReadingsFragment;
import com.specknet.airrespeck.fragments.SupervisedRESpeckReadingsFragment;
import com.specknet.airrespeck.fragments.SubjectHomeFragment;
import com.specknet.airrespeck.fragments.SubjectValuesFragment;
import com.specknet.airrespeck.fragments.SupervisedAllGraphsFragment;
import com.specknet.airrespeck.fragments.SupervisedOverviewFragment;
import com.specknet.airrespeck.models.BreathingGraphData;
import com.specknet.airrespeck.services.SpeckBluetoothService;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.LocationUtils;
import com.specknet.airrespeck.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
    private final static int UPDATE_BREATHING_GRAPH = 8;


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
                    case UPDATE_BREATHING_GRAPH:
                        service.updateBreathingGraphs((BreathingGraphData) msg.obj);
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
    private static final String TAG_DAPHNE_HOME_FRAGMENT = "DAPHNE_HOME_FRAGMENT";
    private static final String TAG_DAPHNE_VALUES_FRAGMENT = "DAPHNE_VALUES_FRAGMENT";
    private static final String TAG_ACTIVITY_SUMMARY_FRAGMENT = "ACTIVITY_SUMMARY_FRAGMENT";
    private static final String TAG_BREATHING_GRAPH_FRAGMENT = "BREATHING_GRAPH_FRAGMENT";

    private SubjectHomeFragment mSubjectHomeFragment;
    private SubjectValuesFragment mSubjectValuesFragment;
    private SupervisedOverviewFragment mSupervisedOverviewFragment;
    private SupervisedAirspeckReadingsFragment mSupervisedAirspeckReadingsFragment;
    private SupervisedAllGraphsFragment mSupervisedAllGraphsFragment;
    private SupervisedActivitySummaryFragment mSupervisedActivitySummaryFragment;
    private SupervisedRESpeckReadingsFragment mSupervisedRESpeckReadingsFragment;

    // Config loaded from RESpeck.config
    private boolean mIsSupervisedModeEnabled;
    private boolean mIsSubjectModeEnabled;
    private boolean mShowSupervisedOverview;
    private boolean mShowSupervisedAQGraphs;
    private boolean mShowSupervisedActivitySummary;
    private boolean mShowSupervisedAirspeckReadings;
    private boolean mShowSupervisedRESpeckReadings;
    private boolean mIsAirspeckEnabled;

    // UTILS
    Utils mUtils;
    LocationUtils mLocationUtils;

    // Layout view for snack bar
    private CoordinatorLayout mCoordinatorLayout;

    // READING VALUES
    HashMap<String, Float> mRespeckSensorReadings = new HashMap<>();
    HashMap<String, Float> mQOESensorReadings = new HashMap<>();
    private LinkedList<BreathingGraphData> breathingSignalchartDataQueue = new LinkedList<>();
    private int updateDelayBreathingGraph;

    // Speck service
    SpeckBluetoothService mSpeckBluetoothService;

    // Variable to switch modes: subject mode or supervised mode
    private static final String IS_SUPERVISED_MODE = "supervised_mode";
    boolean isSupervisedMode;

    TabLayout tabLayout;
    ViewPager viewPager;
    ArrayList<Fragment> supervisedFragments = new ArrayList<>();
    ArrayList<String> supervisedTitles = new ArrayList<>();
    ArrayList<Fragment> subjectFragments = new ArrayList<>();
    ArrayList<String> subjectTitles = new ArrayList<>();

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

        // Load configuration
        loadConfig();

        // Load location Utils
        mLocationUtils = LocationUtils.getInstance(this);
        mLocationUtils.startLocationManager();

        // Set activity title
        this.setTitle(getString(R.string.app_name) + ", v" + mUtils.getAppVersionName());

        setupFragments(savedInstanceState);
        setupViewPager();

        // Load mode if stored. If no mode was stored, this defaults to false, i.e. subject mode
        if (mIsSubjectModeEnabled && mIsSupervisedModeEnabled) {
            if (savedInstanceState != null) {
                isSupervisedMode = savedInstanceState.getBoolean(IS_SUPERVISED_MODE);
            } else {
                // Set mode to starting mode specified in config file
                Log.i("DF", "starting mode: " + mUtils.getProperties().getProperty(
                        Constants.Config.IS_SUPERVISED_STARTING_MODE));
                isSupervisedMode = Boolean.parseBoolean(
                        mUtils.getProperties().getProperty(Constants.Config.IS_SUPERVISED_STARTING_MODE));

            }
        } else if (mIsSubjectModeEnabled) {
            isSupervisedMode = false;
        } else if (mIsSupervisedModeEnabled) {
            isSupervisedMode = true;
        } else {
            throw new RuntimeException(
                    "Neither subject more, nor supervised mode enabled in Configs. Nothing to display!");
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
        startBreathingGraphUpdaterTask();
    }

    private void loadConfig() {
        mIsSupervisedModeEnabled = Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_SUPERVISED_MODE_ENABLED));
        mIsSubjectModeEnabled = Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_SUBJECT_MODE_ENABLED));
        if (mIsSupervisedModeEnabled) {
            mShowSupervisedOverview = Boolean.parseBoolean(
                    mUtils.getProperties().getProperty(Constants.Config.SHOW_SUPERVISED_OVERVIEW));
            mShowSupervisedAQGraphs = Boolean.parseBoolean(
                    mUtils.getProperties().getProperty(Constants.Config.SHOW_SUPERVISED_AQ_GRAPHS));
            mShowSupervisedActivitySummary = Boolean.parseBoolean(
                    mUtils.getProperties().getProperty(Constants.Config.SHOW_SUPERVISED_ACTIVITY_SUMMARY));
            mShowSupervisedAirspeckReadings = Boolean.parseBoolean(
                    mUtils.getProperties().getProperty(Constants.Config.SHOW_SUPERVISED_AIRSPECK_READINGS));
            mShowSupervisedRESpeckReadings = Boolean.parseBoolean(
                    mUtils.getProperties().getProperty(Constants.Config.SHOW_SUPERVISED_RESPECK_READINGS));
        }
        mIsAirspeckEnabled = Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_AIRSPECK_ENABLED));

        // If Airspeck is disabled, disable all fragments related to its data. Overwrite settings above.
        if (!mIsAirspeckEnabled) {
            mShowSupervisedOverview = false;
            mShowSupervisedAirspeckReadings = false;
            mShowSupervisedAQGraphs = false;
        }
    }

    private void setupViewPager() {
        if (mIsSupervisedModeEnabled) {
            // Setup supervised mode arrays
            supervisedFragments.clear();
            supervisedTitles.clear();
            // Only show each fragment if we set the config to true
            if (mShowSupervisedOverview) {
                supervisedFragments.add(mSupervisedOverviewFragment);
                supervisedTitles.add(getString(R.string.menu_home));
            }
            if (mShowSupervisedRESpeckReadings) {
                supervisedFragments.add(mSupervisedRESpeckReadingsFragment);
                supervisedTitles.add(getString(R.string.menu_breathing_graph));
            }
            if (mShowSupervisedAirspeckReadings) {
                supervisedFragments.add(mSupervisedAirspeckReadingsFragment);
                supervisedTitles.add(getString(R.string.menu_air_quality));
            }
            if (mShowSupervisedAQGraphs) {
                supervisedFragments.add(mSupervisedAllGraphsFragment);
                supervisedTitles.add(getString(R.string.menu_graphs));
            }
            if (mShowSupervisedActivitySummary) {
                supervisedFragments.add(mSupervisedActivitySummaryFragment);
                supervisedTitles.add(getString(R.string.menu_activity_summary));
            }
        }

        if (mIsSubjectModeEnabled) {
            // Setup subject mode arrays
            subjectFragments.clear();
            subjectTitles.clear();
            subjectFragments.add(mSubjectHomeFragment);
            // We don't want any text in the subject view, just the icons
            subjectTitles.add("");
            subjectFragments.add(mSubjectValuesFragment);
            subjectTitles.add("");
        }

        // Set the PagerAdapter. It will check in which mode we are and load the corresponding Fragments
        viewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));
    }

    private void setupFragments(Bundle savedInstanceState) {
        // Load or create fragments
        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState != null) {
            // If we have saved something from a previous activity lifecycle, the fragments probably already exist
            mSupervisedOverviewFragment =
                    (SupervisedOverviewFragment) fm.getFragment(savedInstanceState, TAG_HOME_FRAGMENT);
            mSupervisedAirspeckReadingsFragment =
                    (SupervisedAirspeckReadingsFragment) fm.getFragment(savedInstanceState, TAG_AQREADINGS_FRAGMENT);
            mSupervisedAllGraphsFragment =
                    (SupervisedAllGraphsFragment) fm.getFragment(savedInstanceState, TAG_GRAPHS_FRAGMENT);
            mSubjectHomeFragment = (SubjectHomeFragment) fm.getFragment(savedInstanceState, TAG_DAPHNE_HOME_FRAGMENT);
            mSubjectValuesFragment = (SubjectValuesFragment) fm.getFragment(savedInstanceState,
                    TAG_DAPHNE_VALUES_FRAGMENT);

            mSupervisedActivitySummaryFragment = (SupervisedActivitySummaryFragment) fm.getFragment(savedInstanceState,
                    TAG_ACTIVITY_SUMMARY_FRAGMENT);

            mSupervisedRESpeckReadingsFragment = (SupervisedRESpeckReadingsFragment) fm.getFragment(savedInstanceState,
                    TAG_BREATHING_GRAPH_FRAGMENT);
        }
        // If there is no saved instance state, or if the fragments haven't been created during the last activity
        // startup, create them now
        if (mSupervisedOverviewFragment == null) {
            mSupervisedOverviewFragment = new SupervisedOverviewFragment();
        }
        if (mSupervisedAirspeckReadingsFragment == null) {
            mSupervisedAirspeckReadingsFragment = new SupervisedAirspeckReadingsFragment();
        }
        if (mSupervisedAllGraphsFragment == null) {
            mSupervisedAllGraphsFragment = new SupervisedAllGraphsFragment();
        }
        if (mSubjectHomeFragment == null) {
            mSubjectHomeFragment = new SubjectHomeFragment();
        }
        if (mSubjectValuesFragment == null) {
            mSubjectValuesFragment = new SubjectValuesFragment();
        }
        if (mSupervisedActivitySummaryFragment == null) {
            mSupervisedActivitySummaryFragment = new SupervisedActivitySummaryFragment();
        }
        if (mSupervisedRESpeckReadingsFragment == null) {
            mSupervisedRESpeckReadingsFragment = new SupervisedRESpeckReadingsFragment();
        }
    }

    private void startActivitySummaryUpdaterTask() {
        final int delay = 10 * 60 * 1000;
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

    private void startBreathingGraphUpdaterTask() {
        final Handler handler = new Handler();
        final int defaultDelay = Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_PACKETS /
                Constants.NUMBER_OF_SAMPLES_PER_BATCH;
        updateDelayBreathingGraph = defaultDelay;

        // This class is used to determine the update frequency of the breathing data graph. The data from the
        // RESpeck is coming in in batches, which would make the graph hard to read. Therefore, we store the
        // incoming data in a queue and update the graph smoothly.
        class breathingUpdaterRunner implements Runnable {
            private boolean queueHadBeenFilled = false;

            @Override
            public void run() {
                //Log.i("DF", String.format(Locale.UK, "Breathing data queue length: %d",
                //        breathingSignalchartDataQueue.size()));

                if (breathingSignalchartDataQueue.isEmpty()) {
                    // If the queue is empty and there has been data in the queue previously,
                    // this means we were too fast. Wait for another delay and decrease the processing speed.
                    // Only do this if we're below a certain threshold (set with intuition here)
                    if (queueHadBeenFilled && updateDelayBreathingGraph <= 1.1 * defaultDelay) {
                        updateDelayBreathingGraph += 1;
                        Log.i("DF", String.format(Locale.UK, "Queue empty: decrease processing speed to: %d ms",
                                updateDelayBreathingGraph));
                    }

                    handler.postDelayed(this, updateDelayBreathingGraph);
                } else {
                    // Remember the fact that we have already received data
                    queueHadBeenFilled = true;

                    Message msg = new Message();
                    msg.obj = breathingSignalchartDataQueue.removeFirst();
                    msg.what = UPDATE_BREATHING_GRAPH;
                    msg.setTarget(mUIHandler);
                    msg.sendToTarget();

                    // If our queue is too long, increase processing speed. The size threshold was set intuitively and
                    // might have to be adjusted
                    if (breathingSignalchartDataQueue.size() > Constants.NUMBER_OF_SAMPLES_PER_BATCH) {
                        updateDelayBreathingGraph -= 1;
                        Log.i("DF", String.format(Locale.UK, "Queue too full: increase processing speed to: %d ms",
                                updateDelayBreathingGraph));
                    }

                    handler.postDelayed(this, updateDelayBreathingGraph);
                }
            }
        }
        handler.postDelayed(new breathingUpdaterRunner(), updateDelayBreathingGraph);
    }

    private void displaySupervisedMode() {
        isSupervisedMode = true;

        // Update displayed Fragments to reflect mode
        ((SectionsPagerAdapter) viewPager.getAdapter()).setDisplayedFragments(supervisedFragments, supervisedTitles);
        viewPager.setCurrentItem(0);

        // We only display tabs if there is more than one fragment
        if (supervisedFragments.size() > 1) {
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.setupWithViewPager(viewPager);
            // Update tab icons
            // tabLayout.getTabAt(0).setIcon(Constants.MENU_ICON_HOME);
        } else {
            tabLayout.setVisibility(View.GONE);
        }

        // Recreate options menu
        invalidateOptionsMenu();
    }

    private void displaySubjectMode() {
        isSupervisedMode = false;

        // Update displayed Fragments to reflect mode
        ((SectionsPagerAdapter) viewPager.getAdapter()).setDisplayedFragments(subjectFragments, subjectTitles);
        viewPager.setCurrentItem(0);

        tabLayout.setVisibility(View.VISIBLE);
        tabLayout.setupWithViewPager(viewPager);

        // We currently have no option to change the display in the subject mode, so the location of the tabs
        // will always be the same
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

        if (mSupervisedOverviewFragment != null && mSupervisedOverviewFragment.isAdded()) {
            fm.putFragment(outState, TAG_HOME_FRAGMENT, mSupervisedOverviewFragment);
        }
        if (mSupervisedAirspeckReadingsFragment != null && mSupervisedAirspeckReadingsFragment.isAdded()) {
            fm.putFragment(outState, TAG_AQREADINGS_FRAGMENT, mSupervisedAirspeckReadingsFragment);
        }
        if (mSupervisedAllGraphsFragment != null && mSupervisedAllGraphsFragment.isAdded()) {
            fm.putFragment(outState, TAG_GRAPHS_FRAGMENT, mSupervisedAllGraphsFragment);
        }
        if (mSubjectHomeFragment != null && mSubjectHomeFragment.isAdded()) {
            fm.putFragment(outState, TAG_DAPHNE_HOME_FRAGMENT, mSubjectHomeFragment);
        }
        if (mSubjectValuesFragment != null && mSubjectValuesFragment.isAdded()) {
            fm.putFragment(outState, TAG_DAPHNE_VALUES_FRAGMENT, mSubjectValuesFragment);
        }
        if (mSupervisedActivitySummaryFragment != null && mSupervisedActivitySummaryFragment.isAdded()) {
            fm.putFragment(outState, TAG_ACTIVITY_SUMMARY_FRAGMENT, mSupervisedActivitySummaryFragment);
        }
        if (mSupervisedRESpeckReadingsFragment != null && mSupervisedRESpeckReadingsFragment.isAdded()) {
            fm.putFragment(outState, TAG_BREATHING_GRAPH_FRAGMENT, mSupervisedRESpeckReadingsFragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu, this adds items to the action bar if it is present.
        if (isSupervisedMode) {
            // We currently only use one setting item in supervised mode, namely for enabling the subject mode.
            // If subject mode is disabled, don't load the menu
            if (mIsSubjectModeEnabled) {
                getMenuInflater().inflate(R.menu.menu_supervised, menu);
            }
        } else {
            // We currently only use one setting item in subject mode, namely for enabling the supervised mode.
            // If subject mode is disabled, don't load the menu
            if (mIsSupervisedModeEnabled) {
                getMenuInflater().inflate(R.menu.menu_subject, menu);
            }
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

        /*
        if (id == R.id.action_user_profile) {
            startActivity(new Intent(this, UserProfileActivity.class));
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.SettingsFragment.class.getName());
            intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
            startActivity(intent);
        }*/
        if (id == R.id.action_supervised_mode) {
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

        mRespeckSensorReadings.put(Constants.RESPECK_LIVE_INTERPOLATED_TIMESTAMP, 0f);
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
                // Update overview fragment
                ArrayList<Float> listValuesOverview = new ArrayList<>();

                listValuesOverview.add(
                        mUtils.roundToTwoDigits(mRespeckSensorReadings.get(Constants.RESPECK_BREATHING_RATE)));
                listValuesOverview.add(mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM2_5)));
                listValuesOverview.add(mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM10)));

                mSupervisedOverviewFragment.setReadings(listValuesOverview);

                // Update RESpeckReadings Fragment
                ArrayList<Float> listValuesRESpeckReadings = new ArrayList<>();

                listValuesRESpeckReadings.add(
                        mUtils.roundToTwoDigits(mRespeckSensorReadings.get(Constants.RESPECK_BREATHING_RATE)));
                listValuesRESpeckReadings.add(
                        mUtils.roundToTwoDigits(mRespeckSensorReadings.get(Constants.RESPECK_AVERAGE_BREATHING_RATE)));

                mSupervisedRESpeckReadingsFragment.setReadings(listValuesRESpeckReadings);

                // Add breathing data to queue. This is stored so it can be updated continuously instead of batches.
                BreathingGraphData breathingGraphData = new BreathingGraphData(
                        mRespeckSensorReadings.get(Constants.RESPECK_LIVE_INTERPOLATED_TIMESTAMP),
                        mRespeckSensorReadings.get(Constants.RESPECK_X),
                        mRespeckSensorReadings.get(Constants.RESPECK_Y),
                        mRespeckSensorReadings.get(Constants.RESPECK_Z),
                        mRespeckSensorReadings.get(Constants.RESPECK_BREATHING_SIGNAL));

                breathingSignalchartDataQueue.add(breathingGraphData);

            } else {
                mSubjectValuesFragment.updateBreathing(mRespeckSensorReadings);
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
                HashMap<String, Float> values = new HashMap<>();

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

                mSupervisedAirspeckReadingsFragment.setReadings(values);

                // Graphs fragment UI
                ArrayList<Float> listValues = new ArrayList<>();

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

                mSupervisedAllGraphsFragment.setBinsChartData(listValues);

                mSupervisedAllGraphsFragment.addPMsChartData(new SupervisedAllGraphsFragment.PMs(
                        mQOESensorReadings.get(Constants.QOE_PM1),
                        mQOESensorReadings.get(Constants.QOE_PM2_5),
                        mQOESensorReadings.get(Constants.QOE_PM10)));
            } else {
                // Daphne values fragment
                mSubjectValuesFragment.updateQOEReadings(mQOESensorReadings);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateConnectionLoadingLayout() {
        boolean isConnecting = mSpeckBluetoothService.isConnecting();
        // TODO: show loading symbol instead of X-mark in subject mode
        if (isSupervisedMode) {
            mSupervisedOverviewFragment.showConnecting(isConnecting);
            mSupervisedRESpeckReadingsFragment.showConnecting(isConnecting);
            mSupervisedAirspeckReadingsFragment.showConnecting(isConnecting);
            mSupervisedAllGraphsFragment.showConnecting(isConnecting);
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
            mSubjectHomeFragment.updateRESpeckConnectionSymbol(isConnected);
        }
    }

    private void updateAirspeckConnectionSymbol(boolean isConnected) {
        if (!isSupervisedMode) {
            mSubjectHomeFragment.updateAirspeckConnectionSymbol(isConnected);
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(mCoordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }


    private void updateActivitySummary() {
        // The activity summary is only displayed in supervised mode
        if (isSupervisedMode) {
            mSupervisedActivitySummaryFragment.updateActivitySummary();
        }
    }

    private void updateBreathingGraphs(BreathingGraphData data) {
        if (isSupervisedMode) {
            mSupervisedRESpeckReadingsFragment.updateBreathingGraphs(data);
        }
    }
}
