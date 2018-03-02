package com.specknet.airrespeck.activities;


import android.app.AlertDialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

//import com.crashlytics.android.ndk.CrashlyticsNdk;
//import com.crashlytics.android.Crashlytics;
import com.lazydroid.autoupdateapk.AutoUpdateApk;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.SectionsPagerAdapter;
import com.specknet.airrespeck.dialogs.SupervisedPasswordDialog;
import com.specknet.airrespeck.dialogs.TurnGPSOnDialog;
import com.specknet.airrespeck.dialogs.WrongOrientationDialog;
import com.specknet.airrespeck.fragments.BaseFragment;
import com.specknet.airrespeck.fragments.SubjectHomeFragment;
import com.specknet.airrespeck.fragments.SubjectValuesFragment;
import com.specknet.airrespeck.fragments.SubjectWindmillFragment;
import com.specknet.airrespeck.fragments.SupervisedActivitySummaryFragment;
import com.specknet.airrespeck.fragments.SupervisedAirspeckGraphsFragment;
import com.specknet.airrespeck.fragments.SupervisedAirspeckMapLoaderFragment;
import com.specknet.airrespeck.fragments.SupervisedAirspeckReadingsFragment;
import com.specknet.airrespeck.fragments.SupervisedRESpeckReadingsFragment;
import com.specknet.airrespeck.fragments.SupervisedStepCounterFragment;
import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.services.PhoneGPSService;
import com.specknet.airrespeck.services.SpeckBluetoothService;
import com.specknet.airrespeck.services.airspeckuploadservice.AirspeckRemoteUploadService;
import com.specknet.airrespeck.services.respeckuploadservice.RespeckAndDiaryRemoteUploadService;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.FileLogger;
import com.specknet.airrespeck.utils.ThemeUtils;
import com.specknet.airrespeck.utils.Utils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

//import io.fabric.sdk.android.Fabric;


public class MainActivity extends AppCompatActivity {

    // UI handler. Has to be int because Message.what object is int
    public final static int UPDATE_RESPECK_READINGS = 0;
    public final static int UPDATE_AIRSPECK_READINGS = 1;
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

        UIHandler(MainActivity service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            final int what = msg.what;

            MainActivity service = mService.get();

            if (service != null) {
                switch (what) {
                    case UPDATE_RESPECK_READINGS:
                        if (msg.obj instanceof RESpeckLiveData) {
                            service.updateRespeckReadings((RESpeckLiveData) msg.obj);
                            service.updateRESpeckConnection(true);
                        }
                        break;
                    case UPDATE_AIRSPECK_READINGS:
                        service.updateAirspeckReadings((AirspeckData) msg.obj);
                        service.updateAirspeckConnection(true);
                        break;
                    case ACTIVITY_SUMMARY_UPDATE:
                        service.updateActivitySummary();
                        break;
                    case SHOW_SNACKBAR_MESSAGE:
                        service.showSnackbarFromHandler((String) msg.obj);
                        break;
                    case SHOW_AIRSPECK_CONNECTED:
                        String messageAir = "QOE "
                                + service.getString(R.string.device_found)
                                + " UUID: " + msg.obj
                                + ". " + service.getString(R.string.waiting_for_data)
                                + ".";
                        service.updateAirspeckConnection(true);
                        service.showSnackbarFromHandler(messageAir);
                        break;
                    case SHOW_AIRSPECK_DISCONNECTED:
                        service.updateAirspeckConnection(false);
                        break;
                    case SHOW_RESPECK_CONNECTED:
                        String messageRE = "Respeck "
                                + service.getString(R.string.device_found)
                                + " UUID: " + msg.obj
                                + ". " + service.getString(R.string.waiting_for_data)
                                + ".";
                        service.updateRESpeckConnection(true);
                        service.showSnackbarFromHandler(messageRE);
                        break;
                    case SHOW_RESPECK_DISCONNECTED:
                        service.updateRESpeckConnection(false);
                        break;
                }
            }
        }
    }

    private final Handler mUIHandler = new UIHandler(this);

    // FRAGMENTS
    private static final String TAG_AQREADINGS_FRAGMENT = "AQREADINGS_FRAGMENT";
    private static final String TAG_GRAPHS_FRAGMENT = "GRAPHS_FRAGMENT";
    private static final String TAG_SUBJECT_HOME_FRAGMENT = "SUBJECT_HOME_FRAGMENT";
    private static final String TAG_SUBJECT_VALUES_FRAGMENT = "SUBJECT_VALUES_FRAGMENT";
    private static final String TAG_SUBJECT_WINDMILL_FRAGMENT = "SUBJECT_WINDMILL_FRAGMENT";
    private static final String TAG_ACTIVITY_SUMMARY_FRAGMENT = "ACTIVITY_SUMMARY_FRAGMENT";
    private static final String TAG_BREATHING_GRAPH_FRAGMENT = "BREATHING_GRAPH_FRAGMENT";
    private static final String TAG_AQ_MAP_FRAGMENT = "AQ_MAP_FRAGMENT";
    private static final String TAG_STEPCOUNT_FRAGMENT = "STEPCOUNT_FRAGMENT";

    private SubjectHomeFragment mSubjectHomeFragment;
    private SubjectValuesFragment mSubjectValuesFragment;
    private SubjectWindmillFragment mSubjectWindmillFragment;
    private SupervisedAirspeckReadingsFragment mSupervisedAirspeckReadingsFragment;
    private SupervisedAirspeckGraphsFragment mSupervisedAirspeckGraphsFragment;
    private SupervisedActivitySummaryFragment mSupervisedActivitySummaryFragment;
    private SupervisedRESpeckReadingsFragment mSupervisedRESpeckReadingsFragment;
    private SupervisedAirspeckMapLoaderFragment mSupervisedAirspeckMapLoaderFragment;
    private SupervisedStepCounterFragment mSupervisedStepCounterFragment;

    // Config loaded from config content provider
    private boolean mIsSupervisedStartingMode;
    private boolean mShowSupervisedAQGraphs;
    private boolean mShowSupervisedActivitySummary;
    private boolean mShowSupervisedAirspeckReadings;
    private boolean mShowSupervisedRESpeckReadings;
    private boolean mShowStepCount;
    private boolean mShowSupervisedAQMap;
    private boolean mIsAirspeckEnabled;
    private boolean mIsRESpeckEnabled;
    private boolean mShowSubjectHome;
    private boolean mShowSubjectValues;
    private boolean mShowSubjectWindmill;
    private boolean mIsUploadDataToServer;
    private boolean mIsEncryptLocalData;
    private boolean mShowVolumeBagCalibrationView;
    private boolean mDisableBreathingPostFiltering;
    private boolean mIsStorePhoneGPS;
    private boolean mIsStoreDataLocally;
    private boolean mShowRESpeckWrongOrientationEnabled;

    // Utils
    private Utils mUtils;

    // Layout view for snack bar
    private CoordinatorLayout mCoordinatorLayout;

    // Speck service
    final int REQUEST_ENABLE_BLUETOOTH = 0;
    private BroadcastReceiver mSpeckServiceReceiver;
    private boolean mIsRESpeckConnected;
    private boolean mIsAirspeckConnected;

    // Variable to switch modes: subject mode or supervised mode
    private static final String SAVED_STATE_IS_SUPERVISED_MODE = "supervised_mode";
    private boolean mIsSupervisedModeCurrentlyShown;

    TabLayout tabLayout;
    ViewPager viewPager;
    ArrayList<Fragment> supervisedFragments = new ArrayList<>();
    ArrayList<String> supervisedTitles = new ArrayList<>();
    ArrayList<Fragment> subjectFragments = new ArrayList<>();
    ArrayList<String> subjectTitles = new ArrayList<>();

    private DialogFragment mWrongOrientationDialog;
    private boolean mIsWrongOrientationDialogDisplayed = false;
    private boolean mIsGPSDialogDisplayed = false;
    private boolean mIsBluetoothRequestDialogDisplayed = false;

    private boolean mIsActivityRunning = false;
    private boolean mIsActivityInitialised = false;

    private Set<RESpeckDataObserver> respeckDataObservers = new HashSet<>();
    private Set<AirspeckDataObserver> airspeckDataObservers = new HashSet<>();

    private AutoUpdateApk aua;

    private Bundle mSavedInstanceState;

    private Map<String, String> mLoadedConfig;

    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsActivityRunning = true;

        mSavedInstanceState = savedInstanceState;

        ThemeUtils themeUtils = ThemeUtils.getInstance();
        themeUtils.setTheme(ThemeUtils.NORMAL_FONT_SIZE);
        themeUtils.onActivityCreateSetTheme(this);

        // Check whether we can load the configuration
        // Load configuration
        mUtils = Utils.getInstance();
        mLoadedConfig = mUtils.getConfig(this);

        if (mLoadedConfig.isEmpty()) {
            showDoPairingDialog();
            return;
        }

        // First, we have to make sure that we have permission to access storage. We need this for loading the config.
        checkPermissionsAndInitMainActivity();
    }

    private boolean checkIfSecurityKeyExists() {
        String securityKey = Utils.getSecurityKey(this);
        String projectIDKey = Utils.getProjectIDForKey(this);

        // If either the key hasn't been set yet, or if it wasn't created with the currently used project ID,
        // create a new key.
        if (securityKey.equals("") || !projectIDKey.equals(
                mLoadedConfig.get(Constants.Config.SUBJECT_ID).substring(0, 2))) {
            // Open SecurityKeyActivity
            Intent intent = new Intent(this, SecurityKeySetupActivity.class);
            startActivity(intent);
            return false;
        } else {
            return true;
        }
    }

    private void showDoPairingDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);
        alertDialogBuilder
                .setMessage("No pairing detected. Please run Pairing app before starting this app!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.finish();
                    }
                });
        alertDialogBuilder.create().show();
    }

    private void checkPermissionsAndInitMainActivity() {
        boolean isStoragePermissionGranted = Utils.checkAndRequestStoragePermission(MainActivity.this);
        if (!isStoragePermissionGranted) {
            return;
        }

        boolean isLocationPermissionGranted = Utils.checkAndRequestLocationPermission(MainActivity.this);
        if (!isLocationPermissionGranted) {
            return;
        }

        // Check whether this is the first app start. If yes, a security key needs to be created
        boolean keyExists = checkIfSecurityKeyExists();
        if (!keyExists) {
            finish();
            return;
        }

        // Create data directories
        mUtils.createDataDirectoriesIfTheyDontExist(this);

        // Start task checking whether GPS is on every 5 seconds. Only needed on non-Redmi devices, as Redmi can turn
        // on GPS automatically
        // Also check whether GPS is turned on now. This is needed for Bluetooth connection.
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.M) {
            startGPSCheckTask();
            LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                return;
            }
        }

        initMainActivity();
    }

    public void initMainActivity() {
        mIsActivityInitialised = true;
        FileLogger.logToFile(this, "App started and initialised");
        Log.i("MainActivity", "Initialising main activity");
        aua = new AutoUpdateApk(getApplicationContext(), true);
        AutoUpdateApk.enableMobileUpdates();

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

        // Load configuration variables
        loadConfigInstanceVariables();

        // Create the external directories for storing the data if storage is enabled
        if (mIsStoreDataLocally) {
            createExternalDirectory();
        }

        // Start GPS phone storage service
        if (mIsStorePhoneGPS) {
            startPhoneGPSService();
        }

        // Set activity title
        this.setTitle(getString(R.string.app_name) + ", v" + mUtils.getAppVersionName());

        setupFragments();
        setupViewPager();

        // Load current mode if stored. If no mode was stored, use starting mode.
        if (mSavedInstanceState != null) {
            mIsSupervisedModeCurrentlyShown = mSavedInstanceState.getBoolean(SAVED_STATE_IS_SUPERVISED_MODE);
        } else {
            // Set mode to starting mode specified in config file
            mIsSupervisedModeCurrentlyShown = mIsSupervisedStartingMode;
        }


        // Call displayMode methods so the tabs are set correctly
        if (mIsSupervisedModeCurrentlyShown) {
            displaySupervisedMode();
        } else {
            displaySubjectMode();
        }

        // Load connection state
        if (mSavedInstanceState != null) {
            mIsRESpeckConnected = mSavedInstanceState.getBoolean(Constants.IS_RESPECK_CONNECTED);
            mIsAirspeckConnected = mSavedInstanceState.getBoolean(Constants.IS_AIRSPECK_CONNECTED);
            updateRESpeckConnection(mIsRESpeckConnected);
            updateAirspeckConnection(mIsAirspeckConnected);
        }

        // Add the toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // For use with snack bar (notification bar at the bottom of the screen)
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(
                Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        startBluetoothCheckTask();

        startSpeckService();

        // Initialise upload services if desired
        if (mIsUploadDataToServer) {
            if (mIsRESpeckEnabled) {
                Intent startUploadRESpeckIntent = new Intent(this, RespeckAndDiaryRemoteUploadService.class);
                startService(startUploadRESpeckIntent);
            }

            if (mIsAirspeckEnabled) {
                Intent startUploadAirspeckIntent = new Intent(this, AirspeckRemoteUploadService.class);
                startService(startUploadAirspeckIntent);
            }
        }

        // Initialise broadcast receiver which receives data from the speck service
        initSpeckServiceReceiver();

        startActivitySummaryUpdaterTask();
    }

    private void startPhoneGPSService() {
        // Start the service which will regularly check GPS and store the data
        Intent startGPSServiceIntent = new Intent(this, PhoneGPSService.class);
        startService(startGPSServiceIntent);
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == Constants.REQUEST_CODE_LOCATION_PERMISSION) {
            Log.i("AirspeckMap", "onRequestPermissionResult: " + grantResults[0]);
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted. Start the phone service.
                checkPermissionsAndInitMainActivity();
            } else {
                // Permission was not granted. Explain to the user why we need permission and ask again
                Utils.showLocationRequestDialog(MainActivity.this);
            }
        } else if (requestCode == Constants.REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted. Initialise this activity.
                checkPermissionsAndInitMainActivity();
            } else {
                // Permission was not granted. Explain to the user why we need permission and ask again
                Utils.showStorageRequestDialog(MainActivity.this);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Do nothing.
    }

    private void createExternalDirectory() {
        // Create directories on external storage if they don't exist
        File directory = new File(Constants.EXTERNAL_DIRECTORY_STORAGE_PATH);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            // The following is used as the directory sometimes doesn't show as it is not indexed by the system yet
            // scanFile should force the indexation of the new directory.
            MediaScannerConnection.scanFile(this, new String[]{directory.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
            if (created) {
                Log.i("DF", "Directory created: " + directory);
            } else {
                throw new RuntimeException("Couldn't create app root folder on external storage");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FileLogger.logToFile(this, "App was brought into foreground (Main Activity) resumed)");
        mIsActivityRunning = true;
    }

    @Override
    protected void onPause() {
        mIsActivityRunning = false;
        super.onPause();
    }

    private void startGPSCheckTask() {
        final Handler h = new Handler();
        final int delay = 5000; // milliseconds

        h.postDelayed(new Runnable() {
            public void run() {
                if (checkGPSAndShowDialog() && !mIsActivityInitialised) {
                    initMainActivity();
                }
                h.postDelayed(this, delay);
            }
        }, 0);
    }

    private boolean checkGPSAndShowDialog() {
        // Check if GPS is turned on
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!mIsGPSDialogDisplayed && !manager.isProviderEnabled(
                LocationManager.GPS_PROVIDER)) {
            if (mIsActivityRunning) {
                mIsGPSDialogDisplayed = true;
                DialogFragment turnGPSOnDialog = new TurnGPSOnDialog();
                turnGPSOnDialog.show(getFragmentManager(), "turn_gps_on_dialog");
            }
            return false;
        } else {
            return true;
        }
    }

    private void startBluetoothCheckTask() {
        final Handler h = new Handler();
        final int delay = 5000; // milliseconds

        h.postDelayed(new Runnable() {
            public void run() {
                // Open request for bluetooth if turned off
                showBluetoothRequest();
                h.postDelayed(this, delay);
            }
        }, 0);
    }

    private void showBluetoothRequest() {
        if (!mBluetoothAdapter.isEnabled() && !mIsBluetoothRequestDialogDisplayed) {
            mIsBluetoothRequestDialogDisplayed = true;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        }
    }

    public void setIsGPSDialogDisplayed(boolean isDisplayed) {
        mIsGPSDialogDisplayed = isDisplayed;
    }

    private void startSpeckService() {
        Intent intentStartService = new Intent(this, SpeckBluetoothService.class);
        startService(intentStartService);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            mIsBluetoothRequestDialogDisplayed = false;
            if (resultCode == RESULT_OK) {
                startSpeckService();
            } else {
                showBluetoothRequest();
            }
        }
    }

    private void initSpeckServiceReceiver() {
        mSpeckServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case Constants.ACTION_RESPECK_LIVE_BROADCAST:
                        RESpeckLiveData liveRESpeckData = (RESpeckLiveData) intent.getSerializableExtra(
                                Constants.RESPECK_LIVE_DATA);
                        sendMessageToHandler(UPDATE_RESPECK_READINGS, liveRESpeckData);
                        break;
                    case Constants.ACTION_RESPECK_CONNECTED:
                        String respeckUUID = intent.getStringExtra(Constants.Config.RESPECK_UUID);
                        sendMessageToHandler(SHOW_RESPECK_CONNECTED, respeckUUID);
                        break;
                    case Constants.ACTION_RESPECK_DISCONNECTED:
                        sendMessageToHandler(SHOW_RESPECK_DISCONNECTED, null);
                        break;
                    case Constants.ACTION_AIRSPECK_LIVE_BROADCAST:
                        AirspeckData liveAirspeckData = (AirspeckData) intent.getSerializableExtra(
                                Constants.AIRSPECK_DATA);
                        sendMessageToHandler(UPDATE_AIRSPECK_READINGS, liveAirspeckData);
                        break;
                    case Constants.ACTION_AIRSPECK_CONNECTED:
                        String airspeckUUID = intent.getStringExtra(Constants.Config.AIRSPECKP_UUID);
                        sendMessageToHandler(SHOW_AIRSPECK_CONNECTED, airspeckUUID);
                        break;
                    case Constants.ACTION_AIRSPECK_DISCONNECTED:
                        sendMessageToHandler(SHOW_AIRSPECK_DISCONNECTED, null);
                        break;
                }
            }
        };

        // Register receivers
        if (mIsRESpeckEnabled) {
            registerReceiver(mSpeckServiceReceiver, new IntentFilter(
                    Constants.ACTION_RESPECK_LIVE_BROADCAST));
            registerReceiver(mSpeckServiceReceiver, new IntentFilter(
                    Constants.ACTION_RESPECK_CONNECTED));
            registerReceiver(mSpeckServiceReceiver, new IntentFilter(
                    Constants.ACTION_RESPECK_DISCONNECTED));
        }
        if (mIsAirspeckEnabled) {
            registerReceiver(mSpeckServiceReceiver, new IntentFilter(
                    Constants.ACTION_AIRSPECK_LIVE_BROADCAST));
            registerReceiver(mSpeckServiceReceiver, new IntentFilter(
                    Constants.ACTION_AIRSPECK_CONNECTED));
            registerReceiver(mSpeckServiceReceiver, new IntentFilter(
                    Constants.ACTION_AIRSPECK_DISCONNECTED));
        }
    }

    private void sendMessageToHandler(int what, Object obj) {
        Message msg = Message.obtain();
        msg.obj = obj;
        msg.what = what;
        msg.setTarget(mUIHandler);
        msg.sendToTarget();
    }

    private void loadConfigInstanceVariables() {
        // Check whether RESpeck and/or Airspeck have been paired
        mIsRESpeckEnabled = !mLoadedConfig.get(Constants.Config.RESPECK_UUID).isEmpty();
        mIsAirspeckEnabled = !mLoadedConfig.get(Constants.Config.AIRSPECKP_UUID).isEmpty();

        // Options which are fixed for now
        mIsStoreDataLocally = true;

        if (mIsRESpeckEnabled) {
            mShowSupervisedActivitySummary = true;
            mShowSupervisedRESpeckReadings = true;
        } else {
            mShowSupervisedActivitySummary = false;
            mShowSupervisedRESpeckReadings = false;
        }

        if (mIsAirspeckEnabled) {
            mShowSupervisedAQGraphs = true;
            mShowSupervisedAirspeckReadings = true;
            mShowSupervisedAQMap = true;
        } else {
            mShowSupervisedAQGraphs = false;
            mShowSupervisedAirspeckReadings = false;
            mShowSupervisedAQMap = false;
        }

        mShowStepCount = false;
        mShowSubjectHome = true;
        mShowSubjectWindmill = false;

        // Load custom config which can be changed in pairing app
        mShowRESpeckWrongOrientationEnabled = !Boolean.parseBoolean(mLoadedConfig.get(
                Constants.Config.DISABLE_WRONG_ORIENTATION_DIALOG));
        mIsUploadDataToServer = Boolean.parseBoolean(mLoadedConfig.get(Constants.Config.UPLOAD_TO_SERVER));
        mIsEncryptLocalData = Boolean.parseBoolean(mLoadedConfig.get(Constants.Config.ENCRYPT_LOCAL_DATA));
        mIsSupervisedStartingMode = Boolean.parseBoolean(
                mLoadedConfig.get(Constants.Config.IS_SUPERVISED_STARTING_MODE));
        mIsStorePhoneGPS = Boolean.parseBoolean(mLoadedConfig.get(Constants.Config.ENABLE_PHONE_LOCATION_STORAGE));
        mShowVolumeBagCalibrationView = Boolean.parseBoolean(mLoadedConfig.get(
                Constants.Config.ENABLE_VOLUME_BAG_CALIBRATION_VIEW));
        mDisableBreathingPostFiltering = Boolean.parseBoolean(mLoadedConfig.get(
                Constants.Config.DISABLE_POST_FILTERING_BREATHING));
        mShowSubjectValues = Boolean.parseBoolean(mLoadedConfig.get(
                Constants.Config.SHOW_SUBJECT_VALUES_SCREEN));
    }

    private void setupViewPager() {
        // Setup supervised mode arrays
        supervisedFragments.clear();
        supervisedTitles.clear();
        // Only show each fragment if we set the config to true
        if (mShowStepCount) {
            supervisedFragments.add(mSupervisedStepCounterFragment);
            supervisedTitles.add("Steps");
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
            supervisedFragments.add(mSupervisedAirspeckGraphsFragment);
            supervisedTitles.add(getString(R.string.menu_aq_graphs));
        }
        if (mShowSupervisedActivitySummary) {
            supervisedFragments.add(mSupervisedActivitySummaryFragment);
            supervisedTitles.add(getString(R.string.menu_activity_summary));
        }
        if (mShowSupervisedAQMap) {
            supervisedFragments.add(mSupervisedAirspeckMapLoaderFragment);
            supervisedTitles.add(getString(R.string.menu_aq_map));
        }

        // Setup subject mode arrays
        subjectFragments.clear();
        subjectTitles.clear();

        if (mShowSubjectHome) {
            subjectFragments.add(mSubjectHomeFragment);
            // Emtpy title as we display icons
            subjectTitles.add("");
        }
        if (mShowSubjectValues) {
            subjectFragments.add(mSubjectValuesFragment);
            subjectTitles.add("");
        }
        if (mShowSubjectWindmill) {
            subjectFragments.add(mSubjectWindmillFragment);
            subjectTitles.add("");
        }

        // Set the PagerAdapter. It will check in which mode we are and load the corresponding Fragments
        viewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));
    }

    private void setupFragments() {
        // Load or create fragments
        FragmentManager fm = getSupportFragmentManager();
        if (mSavedInstanceState != null) {
            // If we have saved something from a previous activity lifecycle, the fragments probably already exist
            mSupervisedAirspeckReadingsFragment =
                    (SupervisedAirspeckReadingsFragment) fm.getFragment(mSavedInstanceState, TAG_AQREADINGS_FRAGMENT);
            mSupervisedAirspeckGraphsFragment =
                    (SupervisedAirspeckGraphsFragment) fm.getFragment(mSavedInstanceState, TAG_GRAPHS_FRAGMENT);
            mSupervisedActivitySummaryFragment = (SupervisedActivitySummaryFragment) fm.getFragment(mSavedInstanceState,
                    TAG_ACTIVITY_SUMMARY_FRAGMENT);
            mSupervisedRESpeckReadingsFragment = (SupervisedRESpeckReadingsFragment) fm.getFragment(mSavedInstanceState,
                    TAG_BREATHING_GRAPH_FRAGMENT);
            mSupervisedAirspeckMapLoaderFragment = (SupervisedAirspeckMapLoaderFragment) fm.getFragment(
                    mSavedInstanceState,
                    TAG_AQ_MAP_FRAGMENT);
            mSubjectHomeFragment = (SubjectHomeFragment) fm.getFragment(mSavedInstanceState, TAG_SUBJECT_HOME_FRAGMENT);
            mSubjectValuesFragment = (SubjectValuesFragment) fm.getFragment(mSavedInstanceState,
                    TAG_SUBJECT_VALUES_FRAGMENT);
            mSubjectWindmillFragment = (SubjectWindmillFragment) fm.getFragment(mSavedInstanceState,
                    TAG_SUBJECT_WINDMILL_FRAGMENT);
            mSupervisedStepCounterFragment = (SupervisedStepCounterFragment) fm.getFragment(mSavedInstanceState,
                    TAG_STEPCOUNT_FRAGMENT);
        }
        // If there is no saved instance state, or if the fragments haven't been created during the last activity
        // startup, create them now
        if (mSupervisedAirspeckReadingsFragment == null) {
            mSupervisedAirspeckReadingsFragment = new SupervisedAirspeckReadingsFragment();
        }
        if (mSupervisedAirspeckGraphsFragment == null) {
            mSupervisedAirspeckGraphsFragment = new SupervisedAirspeckGraphsFragment();
        }
        if (mSupervisedActivitySummaryFragment == null) {
            mSupervisedActivitySummaryFragment = new SupervisedActivitySummaryFragment();
        }
        if (mSupervisedRESpeckReadingsFragment == null) {
            mSupervisedRESpeckReadingsFragment = new SupervisedRESpeckReadingsFragment();
        }
        if (mSupervisedAirspeckMapLoaderFragment == null) {
            mSupervisedAirspeckMapLoaderFragment = new SupervisedAirspeckMapLoaderFragment();
        }
        if (mSubjectHomeFragment == null) {
            mSubjectHomeFragment = new SubjectHomeFragment();
        }
        if (mSubjectValuesFragment == null) {
            mSubjectValuesFragment = new SubjectValuesFragment();
        }
        if (mSubjectWindmillFragment == null) {
            mSubjectWindmillFragment = new SubjectWindmillFragment();
        }
        if (mSupervisedStepCounterFragment == null) {
            mSupervisedStepCounterFragment = new SupervisedStepCounterFragment();
        }
    }

    private void startActivitySummaryUpdaterTask() {
        final int delay = 5 * 60 * 1000;
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

    public void displaySupervisedMode() {
        mIsSupervisedModeCurrentlyShown = true;

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

    public void displaySubjectMode() {
        mIsSupervisedModeCurrentlyShown = false;

        // Update displayed Fragments to reflect mode
        ((SectionsPagerAdapter) viewPager.getAdapter()).setDisplayedFragments(subjectFragments, subjectTitles);
        viewPager.setCurrentItem(0);

        if (subjectFragments.size() > 1) {
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.setupWithViewPager(viewPager);

            for (int i = 0; i < subjectFragments.size(); i++) {
                tabLayout.getTabAt(i).setIcon(((BaseFragment) subjectFragments.get(i)).getIcon());
                tabLayout.getTabAt(i).setText("");
            }
        } else {
            tabLayout.setVisibility(View.GONE);
        }
        // Recreate options menu
        invalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        // Unregister receivers
        try {
            unregisterReceiver(mSpeckServiceReceiver);
        } catch (IllegalArgumentException e) {
            // Intent receivers have not been registered yet. Skip unregistration in this case.
        }
        Log.i("DF", "App is being destroyed");
        FileLogger.logToFile(this, "App destroyed (stopped)");
        super.onDestroy();
    }

    private void stopServices() {
        Log.i("DF", "Services are being stopped");
        Intent intentStopSpeckService = new Intent(this, SpeckBluetoothService.class);
        stopService(intentStopSpeckService);
        Intent intentStopUploadRespeck = new Intent(this, RespeckAndDiaryRemoteUploadService.class);
        stopService(intentStopUploadRespeck);
        Intent intentStopUploadAirspeck = new Intent(this, AirspeckRemoteUploadService.class);
        stopService(intentStopUploadAirspeck);
        Intent intentStopGPSService = new Intent(this, PhoneGPSService.class);
        stopService(intentStopGPSService);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(SAVED_STATE_IS_SUPERVISED_MODE, mIsSupervisedModeCurrentlyShown);
        super.onSaveInstanceState(outState);

        FragmentManager fm = getSupportFragmentManager();
        if (mSupervisedAirspeckReadingsFragment != null && mSupervisedAirspeckReadingsFragment.isAdded()) {
            fm.putFragment(outState, TAG_AQREADINGS_FRAGMENT, mSupervisedAirspeckReadingsFragment);
        }
        if (mSupervisedAirspeckGraphsFragment != null && mSupervisedAirspeckGraphsFragment.isAdded()) {
            fm.putFragment(outState, TAG_GRAPHS_FRAGMENT, mSupervisedAirspeckGraphsFragment);
        }
        if (mSupervisedActivitySummaryFragment != null && mSupervisedActivitySummaryFragment.isAdded()) {
            fm.putFragment(outState, TAG_ACTIVITY_SUMMARY_FRAGMENT, mSupervisedActivitySummaryFragment);
        }
        if (mSupervisedRESpeckReadingsFragment != null && mSupervisedRESpeckReadingsFragment.isAdded()) {
            fm.putFragment(outState, TAG_BREATHING_GRAPH_FRAGMENT, mSupervisedRESpeckReadingsFragment);
        }
        if (mSubjectHomeFragment != null && mSubjectHomeFragment.isAdded()) {
            fm.putFragment(outState, TAG_SUBJECT_HOME_FRAGMENT, mSubjectHomeFragment);
        }
        if (mSubjectValuesFragment != null && mSubjectValuesFragment.isAdded()) {
            fm.putFragment(outState, TAG_SUBJECT_VALUES_FRAGMENT, mSubjectValuesFragment);
        }
        if (mSubjectWindmillFragment != null && mSubjectWindmillFragment.isAdded()) {
            fm.putFragment(outState, TAG_SUBJECT_WINDMILL_FRAGMENT, mSubjectWindmillFragment);
        }

        // Save connection state
        outState.putBoolean(Constants.IS_RESPECK_CONNECTED, mIsRESpeckConnected);
        outState.putBoolean(Constants.IS_AIRSPECK_CONNECTED, mIsAirspeckConnected);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu, this adds items to the action bar if it is present.
        if (mIsSupervisedModeCurrentlyShown) {
            if (mShowVolumeBagCalibrationView) {
                getMenuInflater().inflate(R.menu.menu_supervised_volume_recording, menu);
            } else {
                getMenuInflater().inflate(R.menu.menu_supervised, menu);
            }
        } else {
            // We currently only use one setting item in subject mode, namely for enabling the supervised mode.
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
        if (id == R.id.action_supervised_mode) {
            DialogFragment supervisedPasswordDialog = new SupervisedPasswordDialog();
            supervisedPasswordDialog.show(getFragmentManager(), "password_dialog");
        } else if (id == R.id.action_subject_mode) {
            displaySubjectMode();
        } else if (id == R.id.action_close_app) {
            stopServices();
            finish();
        } else if (id == R.id.action_volume_recording) {
            startActivity(new Intent(this, VolumeCalibrationRecordingActivity.class));
        } else if (id == R.id.action_view_config) {
            startActivity(new Intent(this, ConfigViewActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateConnectionLoadingLayout() {
        boolean showAirspeckConnecting = mIsAirspeckEnabled && !mIsAirspeckConnected;
        boolean showRESpeckConnecting = mIsRESpeckEnabled && !mIsRESpeckConnected;

        if (mIsSupervisedModeCurrentlyShown) {
            mSupervisedRESpeckReadingsFragment.showConnecting(showAirspeckConnecting, showRESpeckConnecting);
            mSupervisedAirspeckReadingsFragment.showConnecting(showAirspeckConnecting, showRESpeckConnecting);
            mSupervisedAirspeckGraphsFragment.showConnecting(showAirspeckConnecting, showRESpeckConnecting);
        }
    }

    private void updateRespeckReadings(RESpeckLiveData newData) {
        // If the sensor is in the wrong orientation, show a dialog
        if (mShowRESpeckWrongOrientationEnabled) {
            if (!mIsWrongOrientationDialogDisplayed) {
                if (newData.getActivityType() == Constants.WRONG_ORIENTATION && mIsActivityRunning) {
                    mIsWrongOrientationDialogDisplayed = true;
                    mWrongOrientationDialog = new WrongOrientationDialog();
                    mWrongOrientationDialog.show(getFragmentManager(), "wrong_orientation_dialog");
                }
            } else {
                // If the current activity is sitting or standing the sensor was put into the correct orientation,
                // so we can dismiss the dialog
                if (newData.getActivityType() == Constants.ACTIVITY_STAND_SIT) {
                    mWrongOrientationDialog.dismiss();
                }
            }
        }

        notifyNewRESpeckReading(newData);
    }

    private void notifyNewRESpeckReading(RESpeckLiveData newData) {
        for (RESpeckDataObserver observer : respeckDataObservers) {
            observer.updateRESpeckData(newData);
        }
    }

    public void registerRESpeckDataObserver(RESpeckDataObserver observer) {
        respeckDataObservers.add(observer);
        Log.i("MainActivity", "Number of RESpeck observers: " + respeckDataObservers.size());
    }


    private void updateAirspeckReadings(AirspeckData newValues) {
        notifyNewAirspeckReading(newValues);
    }

    private void notifyNewAirspeckReading(AirspeckData newData) {
        for (AirspeckDataObserver observer : airspeckDataObservers) {
            observer.updateAirspeckData(newData);
        }
    }

    public void registerAirspeckDataObserver(AirspeckDataObserver observer) {
        airspeckDataObservers.add(observer);
    }


    public void setWrongOrientationDialogDisplayed(boolean isDisplayed) {
        if (!isDisplayed) {
            // Release the "lock" on the dialog only after 3 seconds so that the activity type buffer can adapt to the
            // new orientation
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mIsWrongOrientationDialogDisplayed = false;
                }
            }, 3000);
        } else {
            mIsWrongOrientationDialogDisplayed = true;
        }
    }

    private void updateRESpeckConnection(boolean isConnected) {
        mIsRESpeckConnected = isConnected;
        if (!mIsSupervisedModeCurrentlyShown && mShowSubjectHome) {
            mSubjectHomeFragment.updateRESpeckConnectionSymbol(isConnected);
        }
        if (!mIsSupervisedModeCurrentlyShown && mShowSubjectWindmill) {
            mSubjectWindmillFragment.updateRESpeckConnectionSymbol(isConnected);
        }
        updateConnectionLoadingLayout();
    }

    private void updateAirspeckConnection(boolean isConnected) {
        mIsAirspeckConnected = isConnected;
        if (!mIsSupervisedModeCurrentlyShown && mShowSubjectHome) {
            mSubjectHomeFragment.updateAirspeckConnectionSymbol(isConnected);
        }
        updateConnectionLoadingLayout();
    }

    private void showSnackbarFromHandler(String message) {
        Snackbar.make(mCoordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    public void showOnSnackbar(String text) {
        Message msg = new Message();
        msg.what = SHOW_SNACKBAR_MESSAGE;
        msg.obj = text;
        msg.setTarget(mUIHandler);
        msg.sendToTarget();
    }

    private void updateActivitySummary() {
        // The activity summary is only displayed in supervised mode
        if (mIsSupervisedModeCurrentlyShown && mShowSupervisedActivitySummary) {
            mSupervisedActivitySummaryFragment.updateActivitySummary();
        }
    }

    public boolean getIsRESpeckConnected() {
        return mIsRESpeckConnected;
    }

    public boolean getIsAirspeckConnected() {
        return mIsAirspeckConnected;
    }
}
