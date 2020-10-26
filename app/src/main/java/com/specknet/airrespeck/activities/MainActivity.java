package com.specknet.airrespeck.activities;


import android.annotation.SuppressLint;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.lazydroid.autoupdateapk.AutoUpdateApk;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.dialogs.SupervisedPasswordDialog;
import com.specknet.airrespeck.dialogs.TurnGPSOnDialog;
import com.specknet.airrespeck.dialogs.WrongOrientationDialog;
import com.specknet.airrespeck.fragments.SubjectHomeFragment;
import com.specknet.airrespeck.fragments.SupervisedActivityLoggingFragment;
import com.specknet.airrespeck.fragments.SupervisedActivitySummaryFragment;
import com.specknet.airrespeck.fragments.SupervisedAirspeckGraphsFragment;
import com.specknet.airrespeck.fragments.SupervisedAirspeckMapLoaderFragment;
import com.specknet.airrespeck.fragments.SupervisedAirspeckReadingsFragment;
import com.specknet.airrespeck.fragments.SupervisedIndoorPredictionFragment;
import com.specknet.airrespeck.fragments.SupervisedInhalerReadingsFragment;
import com.specknet.airrespeck.fragments.SupervisedPulseoxReadingsFragment;
import com.specknet.airrespeck.fragments.SupervisedRESpeckRawAccerelationData;
import com.specknet.airrespeck.fragments.SupervisedRESpeckReadingsIcons;
import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.InhalerData;
import com.specknet.airrespeck.models.PulseoxData;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.services.PhoneGPSService;
import com.specknet.airrespeck.services.SpeckBluetoothService;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.FileLogger;
import com.specknet.airrespeck.utils.ThemeUtils;
import com.specknet.airrespeck.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Intent.ACTION_BATTERY_LOW;
import static android.content.Intent.ACTION_BATTERY_OKAY;
import static android.content.Intent.ACTION_POWER_CONNECTED;
import static android.content.Intent.ACTION_POWER_DISCONNECTED;


public class MainActivity extends AppCompatActivity {

    // UI handler. Has to be int because Message.what object is int
    public final static int UPDATE_RESPECK_READINGS = 0;
    public final static int UPDATE_AIRSPECK_READINGS = 1;
    public final static int SHOW_SNACKBAR_MESSAGE = 2;
    public final static int SHOW_RESPECK_CONNECTED = 3;
    public final static int SHOW_RESPECK_DISCONNECTED = 4;
    public final static int SHOW_AIRSPECK_CONNECTED = 5;
    public final static int SHOW_AIRSPECK_DISCONNECTED = 6;
    public final static int UPDATE_PULSEOX_READINGS = 9;
    public final static int SHOW_PULSEOX_CONNECTED = 10;
    public final static int SHOW_PULSEOX_DISCONNECTED = 11;
    public final static int UPDATE_INHALER_READINGS = 12;
    public final static int SHOW_INHALER_CONNECTED = 13;
    public final static int SHOW_INHALER_DISCONNECTED = 14;
    private static final int AIRSPECK_NOTIFICATION_WATCHDOG = 8;
    // Variable to switch modes: subject mode or supervised mode
    private static final String SAVED_STATE_IS_SUPERVISED_MODE = "supervised_mode";
    // Speck service
    final int REQUEST_ENABLE_BLUETOOTH = 0;
    private final Handler mUIHandler = new UIHandler(this);
    DrawerLayout mNavDrawerLayout;
    FrameLayout mMainFrameLayout;
    // Config loaded from config content provider
    private boolean mIsSupervisedStartingMode;
    private boolean mShowSupervisedAQGraphs;
    private boolean mShowSupervisedActivitySummary;
    private boolean mShowSupervisedAirspeckReadings;
    private boolean mShowSupervisedPulseoxReadings;
    private boolean mShowSupervisedInhalerReadings;
    private boolean mShowSupervisedRESpeckReadings;
    private boolean mShowStepCount;
    private boolean mShowSupervisedAQMap;
    private boolean mIsAirspeckEnabled;
    private boolean mIsRESpeckEnabled;
    private boolean mIsPulseoxEnabled;
    private boolean mIsInhalerEnabled;
    private boolean mShowSubjectHome;
    private boolean mShowSubjectValues;
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
    private BroadcastReceiver mBroadcastReceiver;
    private boolean mIsRESpeckConnected;
    private boolean mIsAirspeckConnected;
    private boolean mIsPulseoxConnected;
    private boolean mIsInhalerConnected;
    private boolean mIsSupervisedModeCurrentlyShown;
    private DialogFragment mWrongOrientationDialog;
    private boolean mIsWrongOrientationDialogDisplayed = false;
    private boolean mIsGPSDialogDisplayed = false;
    private boolean mIsBluetoothRequestDialogDisplayed = false;
    private boolean mIsActivityRunning = false;
    private boolean mIsActivityInitialised = false;
    private Set<RESpeckDataObserver> respeckDataObservers = new HashSet<>();
    private Set<AirspeckDataObserver> airspeckDataObservers = new HashSet<>();
    private Set<PulseoxDataObserver> pulseoxDataObservers = new HashSet<>();
    private Set<InhalerDataObserver> inhalerDataObservers = new HashSet<>();
    private Set<ConnectionStateObserver> connectionStateObservers = new HashSet<>();
    private AutoUpdateApk aua;
    private Bundle mSavedInstanceState;
    private Map<String, String> mLoadedConfig;
    private BluetoothAdapter mBluetoothAdapter;
    private ActionBar mActionbar;
    public InhalerData lastInhalerPress = null;
    private boolean mCollectMedia = false;

    private PowerManager.WakeLock wakeLock;
    private boolean doFullAppClose = false;

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

        Log.i("MainActivity", "Pairing info: " + new Gson().toJson(mLoadedConfig));

        if (mLoadedConfig.size() <= 1 || mLoadedConfig.get("SubjectID").compareTo("") == 0) {
            showDoPairingDialog();
            return;
        }

        if (mLoadedConfig.containsKey(Constants.Config.SHOW_MEDIA_BUTTONS)) {
            mCollectMedia = Boolean.parseBoolean(mLoadedConfig.get(Constants.Config.SHOW_MEDIA_BUTTONS));
        }

        // First, we have to make sure that we have permission to access storage. We need this for loading the config.
        checkPermissionsAndInitMainActivity();
    }

    private void showDoPairingDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);
        alertDialogBuilder
                .setMessage("No pairing detected. Please run Pairing app before starting this app!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.doFullAppClose = true;
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

        if (mCollectMedia) {
            boolean isMicPermissionGranted = Utils.checkAndRequestMicPermission(MainActivity.this);
            if (!isMicPermissionGranted) {
                return;
            }
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
        startGPSCheckTask();
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return;
        }
        initMainActivity();
    }

    private boolean checkIfSecurityKeyExists() {
        String securityKey = Utils.getSecurityKey(this);
        String projectIDKey = Utils.getProjectIDForKey(this);

        // If either the key hasn't been set yet, or if it wasn't created with the currently used project ID,
        // create a new key.

        String sid = mLoadedConfig.get(Constants.Config.SUBJECT_ID);

        if (securityKey.equals("") || !projectIDKey.equals(sid.substring(0, 2))) {
            // Open SecurityKeyActivity
            Intent intent = new Intent(this, SecurityKeySetupActivity.class);
            startActivity(intent);
            return false;
        } else {
            return true;
        }
    }

    @SuppressLint("MissingPermission")
    public void initMainActivity() {
        mIsActivityInitialised = true;
        FileLogger.logToFile(this, "App started and initialised");
        Log.i("MainActivity", "Initialising main activity");

        aua = new AutoUpdateApk(getApplicationContext(), true);
        AutoUpdateApk.enableMobileUpdates();

        // Setup the part of the layout which is the same for both modes
        setContentView(R.layout.activity_main);
        mNavDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        mMainFrameLayout = (FrameLayout) findViewById(R.id.main_frame);

        aquireWakeLockToKeepAppRunning();

        loadConfigInstanceVariables();

        setupNavigationDrawer(navigationView);

        if (mIsStoreDataLocally) {
            Utils.createExternalDirectory(this);
        }

        if (mIsStorePhoneGPS) {
            startPhoneGPSService();
        }

        // Add the toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mActionbar = getSupportActionBar();
        mActionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        // Set activity title
        this.setTitle(getString(R.string.app_name) + ", v" + mUtils.getAppVersionName());

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
            mIsPulseoxConnected = mSavedInstanceState.getBoolean(Constants.IS_PULSEOX_CONNECTED);
            mIsInhalerConnected = mSavedInstanceState.getBoolean(Constants.IS_INHALER_CONNECTED);
            updateRESpeckConnection(mIsRESpeckConnected);
            updateAirspeckConnection(mIsAirspeckConnected);
            updatePulseoxConnection(mIsPulseoxConnected);
            updatePulseoxConnection(mIsInhalerConnected);
        }

        // For use with snack bar (notification bar at the bottom of the screen)
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(
                Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        startBluetoothCheckTask();

        startSpeckService();

        // Initialise broadcast receiver which receives data from the speck service
        initBroadcastReceiver();

        startAirspeckWatchdogUpdaterTask();
    }

    private void aquireWakeLockToKeepAppRunning() {
        // Request wake lock to keep CPU running for all services of the app
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();
    }

    public void setupNavigationDrawer(NavigationView navigationView) {
        Menu navigationMenu = navigationView.getMenu();

        // Hide parts of the navigation menu we don't need
        if (!mIsAirspeckEnabled) {
            navigationMenu.findItem(R.id.menu_airspeck_subgroup).setVisible(false);
        }
        if (!mIsRESpeckEnabled) {
            navigationMenu.findItem(R.id.menu_respeck_subgroup).setVisible(false);
        }
        if (!mIsPulseoxEnabled) {
            navigationMenu.findItem(R.id.menu_pulseox_subgroup).setVisible(false);
        }
        if (!mIsInhalerEnabled) {
            navigationMenu.findItem(R.id.menu_inhaler_subgroup).setVisible(false);
        }

        // Setup nav drawer menu
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        mNavDrawerLayout.closeDrawers();

                        switch (menuItem.getItemId()) {
                            case R.id.nav_airspeck_readings:
                                displayFragment(new SupervisedAirspeckReadingsFragment());
                                break;
                            case R.id.nav_airspeck_graphs:
                                displayFragment(new SupervisedAirspeckGraphsFragment());
                                break;
                            case R.id.nav_airspeck_map:
                                displayFragment(new SupervisedAirspeckMapLoaderFragment());
                                break;
                            case R.id.nav_respeck_readings:
                                displayFragment(new SupervisedRESpeckReadingsIcons());
                                break;
                            case R.id.nav_activity_summary:
                                displayFragment(new SupervisedActivitySummaryFragment());
                                break;
                            case R.id.nav_activity_logging:
                                displayFragment(new SupervisedActivityLoggingFragment());
                                break;
                            case R.id.nav_activity_raw_accel_respeck:
                                displayFragment(new SupervisedRESpeckRawAccerelationData(), "RAW");
                                break;
                            case R.id.nav_inout_prediction:
                                displayFragment(new SupervisedIndoorPredictionFragment());
                                break;
                            case R.id.nav_pulseox:
                                displayFragment(new SupervisedPulseoxReadingsFragment());
                                break;
                            case R.id.nav_inhaler:
                                displayFragment(new SupervisedInhalerReadingsFragment());
                                break;
                        }
                        return true;
                    }
                });
    }

    private void displayFragment(Fragment newFragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_frame, newFragment);
        transaction.commit();
    }

    private void displayFragment(Fragment newFragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_frame, newFragment, tag);
        transaction.commit();
    }

    private void startPhoneGPSService() {
        // Start the service (if it's not already running) which will regularly check GPS and store the data
        if (!Utils.isServiceRunning(PhoneGPSService.class, this)) {
            Intent startGPSServiceIntent = new Intent(this, PhoneGPSService.class);
            startService(startGPSServiceIntent);
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == Constants.REQUEST_CODE_LOCATION_PERMISSION) {
            Log.i("AirspeckMap", "onRequestPermissionResult: " + grantResults[0]);
            // If request is cancelled, the result arrays are empty.
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
        } else if (requestCode == Constants.REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted. Initialise this activity.
                checkPermissionsAndInitMainActivity();
            } else {
                // Permission was not granted. Explain to the user why we need permission and ask again
                Utils.showMicRequestDialog(MainActivity.this);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Do nothing.
    }

    @Override
    protected void onResume() {
        super.onResume();
        FileLogger.logToFile(this, "App was brought into foreground (Main Activity) resumed");
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
        // Only start service if it is not already running.
        if (!Utils.isServiceRunning(SpeckBluetoothService.class, this)) {
            FileLogger.logToFile(this, "Started Speck Bluetooth service");
            Intent intentStartService = new Intent(this, SpeckBluetoothService.class);
            startService(intentStartService);
        } else {
            FileLogger.logToFile(this, "Speck Bluetooth service already running. Don't start it again.");
        }
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

    private void initBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
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
                    case Constants.ACTION_PULSEOX_BROADCAST:
                        PulseoxData pd = (PulseoxData) intent.getSerializableExtra(Constants.PULSEOX_DATA);
                        pd.toStringForFile();
                        //Toast.makeText(context,
                        //        "Pulseox: " + pd.toStringForFile(),
                        //        Toast.LENGTH_LONG).show();
                        sendMessageToHandler(UPDATE_PULSEOX_READINGS, pd);
                        break;
                    case Constants.ACTION_PULSEOX_CONNECTED:
                        String pulseoxUUID = "00:1C:05:FF:F0:0F";
                        sendMessageToHandler(SHOW_PULSEOX_CONNECTED, pulseoxUUID);
                        break;
                    case Constants.ACTION_PULSEOX_DISCONNECTED:
                        sendMessageToHandler(SHOW_PULSEOX_DISCONNECTED, null);
                        break;
                    case Constants.ACTION_INHALER_BROADCAST:
                        InhalerData ind = (InhalerData) intent.getSerializableExtra(Constants.INHALER_DATA);
                        ind.toStringForFile();
                        sendMessageToHandler(UPDATE_INHALER_READINGS, ind);
                        Log.i("MainActivity", "Inhaler pressed: " + ind.getPhoneTimestamp());
                        lastInhalerPress = ind;
                        break;
                    case Constants.ACTION_INHALER_CONNECTED:
                        String inhalerUUID = intent.getStringExtra(Constants.Config.INHALER_UUID);
                        sendMessageToHandler(SHOW_INHALER_CONNECTED, inhalerUUID);
                        break;
                    case Constants.ACTION_INHALER_DISCONNECTED:
                        sendMessageToHandler(SHOW_INHALER_DISCONNECTED, null);
                        break;
                    case ACTION_BATTERY_LOW:
                        FileLogger.logToFile(MainActivity.this, "Battery level low");
                        break;
                    case ACTION_BATTERY_OKAY:
                        FileLogger.logToFile(MainActivity.this, "Battery level ok");
                        break;
                    case ACTION_POWER_CONNECTED:
                        FileLogger.logToFile(MainActivity.this, "Phone is being charged");
                        break;
                    case ACTION_POWER_DISCONNECTED:
                        FileLogger.logToFile(MainActivity.this, "Phone was removed from charger");
                        break;
                }
            }
        };

        // Register receivers
        if (mIsRESpeckEnabled) {
            registerReceiver(mBroadcastReceiver, new IntentFilter(
                    Constants.ACTION_RESPECK_LIVE_BROADCAST));
            registerReceiver(mBroadcastReceiver, new IntentFilter(
                    Constants.ACTION_RESPECK_CONNECTED));
            registerReceiver(mBroadcastReceiver, new IntentFilter(
                    Constants.ACTION_RESPECK_DISCONNECTED));
        }
        if (mIsAirspeckEnabled) {
            registerReceiver(mBroadcastReceiver, new IntentFilter(
                    Constants.ACTION_AIRSPECK_LIVE_BROADCAST));
            registerReceiver(mBroadcastReceiver, new IntentFilter(
                    Constants.ACTION_AIRSPECK_CONNECTED));
            registerReceiver(mBroadcastReceiver, new IntentFilter(
                    Constants.ACTION_AIRSPECK_DISCONNECTED));
        }
        if (mIsPulseoxEnabled) {
            registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.ACTION_PULSEOX_BROADCAST));
            registerReceiver(mBroadcastReceiver, new IntentFilter(
                    Constants.ACTION_PULSEOX_CONNECTED));
            registerReceiver(mBroadcastReceiver, new IntentFilter(
                    Constants.ACTION_PULSEOX_DISCONNECTED));
        }

        if (mIsInhalerEnabled) {
            registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.ACTION_INHALER_BROADCAST));
            registerReceiver(mBroadcastReceiver, new IntentFilter(
                    Constants.ACTION_INHALER_CONNECTED));
            registerReceiver(mBroadcastReceiver, new IntentFilter(
                    Constants.ACTION_INHALER_DISCONNECTED));
        }

        registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_BATTERY_LOW));
        registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_BATTERY_OKAY));
        registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_POWER_CONNECTED));
        registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_POWER_DISCONNECTED));
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
        mIsPulseoxEnabled = mLoadedConfig.containsKey(Constants.Config.PULSEOX_UUID) && !mLoadedConfig.get(
                Constants.Config.PULSEOX_UUID).isEmpty();
        mIsInhalerEnabled = mLoadedConfig.containsKey(Constants.Config.INHALER_UUID) && !mLoadedConfig.get(
                Constants.Config.INHALER_UUID).isEmpty();

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

        mShowSupervisedPulseoxReadings = mIsPulseoxEnabled;
        mShowSupervisedInhalerReadings = mIsInhalerEnabled;

        mShowStepCount = false;
        mShowSubjectHome = true;

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

    private void startAirspeckWatchdogUpdaterTask() {
        final int delay = 15 * 1000;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = AIRSPECK_NOTIFICATION_WATCHDOG;
                msg.setTarget(mUIHandler);
                msg.sendToTarget();
            }
        }, 0, delay);
    }

    public void displaySupervisedMode() {
        mIsSupervisedModeCurrentlyShown = true;

        // Enable navigation drawer
        mActionbar.setDisplayHomeAsUpEnabled(true);
        mNavDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        // Replace fragment
        if (mIsRESpeckEnabled) displayFragment(new SupervisedRESpeckReadingsIcons());
        else if (mIsAirspeckEnabled) displayFragment(new SupervisedAirspeckReadingsFragment());
        else if (mIsPulseoxEnabled) displayFragment(new SupervisedPulseoxReadingsFragment());
        else if (mIsInhalerEnabled) displayFragment(new SupervisedInhalerReadingsFragment());

        // Recreate options menu
        invalidateOptionsMenu();
    }

    public void displaySubjectMode() {
        mIsSupervisedModeCurrentlyShown = false;

        // Disable navigation drawerF
        mActionbar.setDisplayHomeAsUpEnabled(false);
        mNavDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        // Replace fragment
        displayFragment(new SubjectHomeFragment());

        // Recreate options menu
        invalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        // Unregister receivers
        try {
            unregisterReceiver(mBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            // Intent receivers have not been registered yet. Skip unregistration in this case.
        }
        Log.i("DF", "App is being destroyed");
        FileLogger.logToFile(this, "App destroyed (stopped)");

        try {
            wakeLock.release();
        } catch (NullPointerException e) {

        }

        super.onDestroy();

        if (doFullAppClose) {
            System.exit(0);
        }
    }

    private void stopServices() {
        Log.i("DF", "Services are being stopped");
        Intent intentStopSpeckService = new Intent(this, SpeckBluetoothService.class);
        stopService(intentStopSpeckService);
        Intent intentStopGPSService = new Intent(this, PhoneGPSService.class);
        stopService(intentStopGPSService);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(SAVED_STATE_IS_SUPERVISED_MODE, mIsSupervisedModeCurrentlyShown);

        // Save connection state
        outState.putBoolean(Constants.IS_RESPECK_CONNECTED, mIsRESpeckConnected);
        outState.putBoolean(Constants.IS_AIRSPECK_CONNECTED, mIsAirspeckConnected);
        outState.putBoolean(Constants.IS_PULSEOX_CONNECTED, mIsPulseoxConnected);
        outState.putBoolean(Constants.IS_INHALER_CONNECTED, mIsInhalerConnected);

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu, this adds items to the action bar if it is present.
        if (mIsSupervisedModeCurrentlyShown) {
            getMenuInflater().inflate(R.menu.menu_supervised, menu);
        } else {
            // We currently only use one setting item in subject mode, namely for enabling the supervised mode.
            getMenuInflater().inflate(R.menu.menu_subject, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_supervised_mode:
                DialogFragment supervisedPasswordDialog = new SupervisedPasswordDialog();
                supervisedPasswordDialog.show(getFragmentManager(), "password_dialog");
                return true;
            case R.id.action_subject_mode:
                displaySubjectMode();
                return true;
            case R.id.action_close_app:
                stopServices();
                doFullAppClose = true;
                finish();
                return true;
            case R.id.action_view_config:
                startActivity(new Intent(this, ConfigViewActivity.class));
                return true;
            case R.id.action_check_for_updates:
                Toast.makeText(this,
                        "Checking for updates in background. You will be notified when an update is available in about a minute.",
                        Toast.LENGTH_LONG).show();
                aua.checkUpdatesManually();
                return true;
            case android.R.id.home:
                mNavDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateRespeckReadings(RESpeckLiveData newData) {
        Fragment rawFragment = getSupportFragmentManager().findFragmentByTag("RAW");
        // If the sensor is in the wrong orientation, show a dialog
        if (!(rawFragment != null && rawFragment.isVisible()) && mShowRESpeckWrongOrientationEnabled) {
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
                    if (mWrongOrientationDialog.isAdded()) {
                        mWrongOrientationDialog.dismiss();
                    }
                }
            }
        }

        notifyNewRESpeckReading(newData);
    }

    public String getWrongOrientationText() {
        String sid = "";
        // Needs to be in Dutch for Windmill
        if (mLoadedConfig.containsKey(Constants.Config.SUBJECT_ID)) {
            sid = mLoadedConfig.get(Constants.Config.SUBJECT_ID);
        }
        Log.i("Rat", "SID " + sid);
        if (sid.contains("WI") && !sid.contains("WIF")){
            return getString(R.string.wrong_orientation_message_dutch);
        }
        return getString(R.string.wrong_orientation_message);
    }

    public String getBreathingSuffix() {
        String sid = "";
        // Needs to be in Dutch for Windmill
        if (mLoadedConfig.containsKey(Constants.Config.SUBJECT_ID)) {
            sid = mLoadedConfig.get(Constants.Config.SUBJECT_ID);
        }
        Log.i("Rat", "SID " + sid);
        if (sid.contains("WI") && !sid.contains("WIF")){
            return "Ah/min";
        }
        return "BrPM";
    }



    private void notifyNewRESpeckReading(RESpeckLiveData newData) {
        for (RESpeckDataObserver observer : respeckDataObservers) {
            observer.updateRESpeckData(newData);
        }
    }

    public void registerRESpeckDataObserver(RESpeckDataObserver observer) {
        respeckDataObservers.add(observer);
    }

    public void unregisterRESpeckDataObserver(RESpeckDataObserver observer) {
        respeckDataObservers.remove(observer);
    }

    private void notifyNewAirspeckReading(AirspeckData newData) {
        for (AirspeckDataObserver observer : airspeckDataObservers) {
            observer.updateAirspeckData(newData);
        }
    }

    public void registerAirspeckDataObserver(AirspeckDataObserver observer) {
        airspeckDataObservers.add(observer);
    }

    public void unregisterAirspeckDataObserver(AirspeckDataObserver observer) {
        airspeckDataObservers.remove(observer);
    }

    private void updatePulseoxReadings(PulseoxData newValues) {
        notifyNewPulseoxReading(newValues);
    }

    private void notifyNewPulseoxReading(PulseoxData newData) {
        for (PulseoxDataObserver observer : pulseoxDataObservers) {
            observer.updatePulseoxData(newData);
        }
    }

    public void registerPulseoxDataObserver(PulseoxDataObserver observer) {
        pulseoxDataObservers.add(observer);
    }

    public void unregisterPulseoxDataObserver(PulseoxDataObserver observer) {
        pulseoxDataObservers.remove(observer);
    }

    private void updateInhalerReadings(InhalerData newValues) {
        notifyNewInhalerReading(newValues);
    }

    private void notifyNewInhalerReading(InhalerData newData) {
        for (InhalerDataObserver observer : inhalerDataObservers) {
            observer.updateInhalerData(newData);
        }
    }

    public void registerInhalerDataObserver(InhalerDataObserver observer) {
        inhalerDataObservers.add(observer);
    }

    public void unregisterInhalerDataObserver(InhalerDataObserver observer) {
        inhalerDataObservers.remove(observer);
    }

    public void registerConnectionStateObserver(ConnectionStateObserver observer) {
        connectionStateObservers.add(observer);
    }

    public void unregisterConnectionStateObserver(ConnectionStateObserver observer) {
        connectionStateObservers.remove(observer);
    }

    private void notifyNewConnectionState() {
        for (ConnectionStateObserver observer : connectionStateObservers) {
            observer.updateConnectionState(mIsRESpeckConnected, mIsAirspeckConnected, mIsPulseoxConnected,
                    mIsInhalerConnected);
        }
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
        notifyNewConnectionState();
    }

    private void updateAirspeckConnection(boolean isConnected) {
        mIsAirspeckConnected = isConnected;
        notifyNewConnectionState();
    }

    private void updatePulseoxConnection(boolean isConnected) {
        mIsPulseoxConnected = isConnected;
        notifyNewConnectionState();
    }

    private void updateInhalerConnection(boolean isConnected) {
        mIsInhalerConnected = isConnected;
        notifyNewConnectionState();
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

    public boolean getIsRESpeckConnected() {
        return mIsRESpeckConnected;
    }

    public boolean getIsAirspeckConnected() {
        return mIsAirspeckConnected;
    }

    public boolean getIsPulseoxConnected() {
        return mIsPulseoxConnected;
    }

    public boolean getIsInhalerConnected() {
        return mIsInhalerConnected;
    }

    /**
     * Static inner class doesn't hold an implicit reference to the outer class
     */
    private static class UIHandler extends Handler {
        // Using a weak reference means you won't prevent garbage collection
        private final WeakReference<MainActivity> mService;
        private boolean mAirspeckConnected = false;
        private long mLastAirspeckNotificationTime = System.currentTimeMillis();

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
                        }
                        break;
                    case UPDATE_AIRSPECK_READINGS:
                        service.notifyNewAirspeckReading((AirspeckData) msg.obj);
                        mAirspeckConnected = true;
                        mLastAirspeckNotificationTime = System.currentTimeMillis();
                        break;
                    case UPDATE_PULSEOX_READINGS:
                        service.updatePulseoxReadings((PulseoxData) msg.obj);
                        break;
                    case UPDATE_INHALER_READINGS:
                        service.updateInhalerReadings((InhalerData) msg.obj);
                        break;
                    case SHOW_SNACKBAR_MESSAGE:
                        service.showSnackbarFromHandler((String) msg.obj);
                        break;
                    case SHOW_AIRSPECK_CONNECTED:
                        String messageAir = "AIRspeck "
                                + msg.obj + " "
                                + service.getString(R.string.device_found)
                                + ". " + service.getString(R.string.waiting_for_data)
                                + ".";
                        service.updateAirspeckConnection(true);
                        service.showSnackbarFromHandler(messageAir);
                        mAirspeckConnected = true;
                        mLastAirspeckNotificationTime = System.currentTimeMillis();
                        break;
                    case SHOW_AIRSPECK_DISCONNECTED:
                        service.updateAirspeckConnection(false);
                        mAirspeckConnected = false;
                        break;
                    case SHOW_PULSEOX_CONNECTED:
                        String messagePulse = "Pulseox "
                                + msg.obj + " "
                                + service.getString(R.string.device_found)
                                + ". " + service.getString(R.string.waiting_for_data)
                                + ".";
                        service.updatePulseoxConnection(true);
                        service.showSnackbarFromHandler(messagePulse);
                        break;
                    case SHOW_PULSEOX_DISCONNECTED:
                        service.updatePulseoxConnection(false);
                        break;
                    case SHOW_INHALER_CONNECTED:
                        String messageInhaler = "Inhaler "
                                + msg.obj + " "
                                + service.getString(R.string.device_found)
                                + ". " + service.getString(R.string.waiting_for_data)
                                + ".";
                        service.updateInhalerConnection(true);
                        service.showSnackbarFromHandler(messageInhaler);
                        break;
                    case SHOW_INHALER_DISCONNECTED:
                        service.updateInhalerConnection(false);
                        break;
                    case SHOW_RESPECK_CONNECTED:
                        String messageRE = "RESpeck "
                                + msg.obj + " "
                                + service.getString(R.string.device_found)
                                + ". " + service.getString(R.string.waiting_for_data)
                                + ".";
                        service.updateRESpeckConnection(true);
                        service.showSnackbarFromHandler(messageRE);
                        break;
                    case SHOW_RESPECK_DISCONNECTED:
                        service.updateRESpeckConnection(false);
                        break;
                    case AIRSPECK_NOTIFICATION_WATCHDOG:
                        if (mAirspeckConnected) {
                            long t = System.currentTimeMillis() - mLastAirspeckNotificationTime;
                            //service.showSnackbarFromHandler(Long.toString(t));
                            if (t > 60 * 1000) {
                                service.showSnackbarFromHandler(
                                        "Waiting for Air Quality readings...\nAirspeck may be in standby mode.");
                            }
                        }
                        break;
                }
            }
        }
    }
}
