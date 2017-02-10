package com.specknet.airrespeck.activities;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.os.PowerManager;

import org.apache.commons.lang3.time.DateUtils;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.SectionsPagerAdapter;
import com.specknet.airrespeck.fragments.DaphneHomeFragment;
import com.specknet.airrespeck.fragments.DaphneValuesFragment;
import com.specknet.airrespeck.fragments.MenuFragment;
import com.specknet.airrespeck.models.RESpeckStoredSample;
import com.specknet.airrespeck.qoeuploadservice.QOERemoteUploadService;
import com.specknet.airrespeck.respeckuploadservice.RespeckRemoteUploadService;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.LocationHelper;
import com.specknet.airrespeck.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by Darius on 09.02.2017.
 */

public class DaphneMainActivity extends BaseActivity implements MenuFragment.OnMenuSelectedListener {

    // UI HANDLER
    private final static int UPDATE_RESPECK_READINGS = 0;
    private final static int UPDATE_QOE_READINGS = 1;


    /**
     * Static inner class doesn't hold an implicit reference to the outer class
     */
    private static class UIHandler extends Handler {
        // Using a weak reference means you won't prevent garbage collection
        private final WeakReference<DaphneMainActivity> mService;

        public UIHandler(DaphneMainActivity service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            final int what = msg.what;

            DaphneMainActivity service = mService.get();

            if (service != null) {
                switch (what) {
                    case UPDATE_RESPECK_READINGS:
                        service.updateRespeckReadings((HashMap<String, Float>) msg.obj);
                        break;
                    case UPDATE_QOE_READINGS:
                        service.updateQOEReadings((HashMap<String, Float>) msg.obj);
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
    public Handler getHandler() {
        return new DaphneMainActivity.UIHandler(this);
    }

    private final Handler mUIHandler = getHandler();


    // FRAGMENTS
    private static final String TAG_DAPHNE_HOME_FRAGMENT = "DAPHNE_HOME_FRAGMENT";
    private static final String TAG_DAPHNE_VALUES_FRAGMENT = "DAPHNE_VALUES_FRAGMENT";

    private static final String TAG_CURRENT_FRAGMENT = "DAPHNE_CURRENT_FRAGMENT";

    private DaphneHomeFragment mDaphneHomeFragment;
    private DaphneValuesFragment mDaphneValuesFragment;
    private Fragment mCurrentFragment;


    // UTILS
    Utils mUtils;
    //LocationUtils mLocationUtils;
    LocationHelper mLocationUtils;


    // Layout view for snack bar
    private CoordinatorLayout mCoordinatorLayout;


    // READING VALUES
    HashMap<String, Float> mRespeckSensorReadings;
    HashMap<String, Float> mQOESensorReadings;


    // UPLOAD SERVICES
    RespeckRemoteUploadService mRespeckRemoteUploadService;
    QOERemoteUploadService mQOERemoteUploadService;


    // BLUETOOTH
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings mScanSettings;
    private List<ScanFilter> mScanFilters;
    private BluetoothGatt mGattRespeck, mGattQOE;
    private BluetoothDevice mDeviceRespeck, mDeviceQOE;

    private boolean mQOEConnectionComplete;
    private boolean mRespeckConnectionComplete;
    private int REQUEST_ENABLE_BT = 1;
    private static String RESPECK_UUID;
    private static String QOE_UUID;
    private static final String QOE_CLIENT_CHARACTERISTIC = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String QOE_LIVE_CHARACTERISTIC = "00002002-e117-4bff-b00d-b20878bc3f44";

    private final static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private final static String RESPECK_LIVE_CHARACTERISTIC = "00002010-0000-1000-8000-00805f9b34fb";
    private final static String RESPECK_BREATHING_RATES_CHARACTERISTIC = "00002016-0000-1000-8000-00805f9b34fb";
    private final static String RESPECK_BREATH_INTERVALS_CHARACTERISTIC = "00002015-0000-1000-8000-00805f9b34fb";

    //QOE CODE
    private static byte[][] lastPackets_1 = new byte[4][];
    private static byte[][] lastPackets_2 = new byte[5][];
    private static int[] sampleIDs_1 = {0, 0, 0, 0};
    private static int[] sampleIDs_2 = {0, 1, 2, 3, 4};
    int lastSample = 0;

    //RESPECK CODE
    int latest_live_respeck_seq = -1;
    long live_bs_timestamp = -1;
    long live_rs_timestamp = -1;
    Queue<RESpeckStoredSample> stored_queue;

    long latestProcessedMinute = 0l;
    int live_seq = -1;
    long brav_bs_timestamp = -1;
    long brav_rs_timestamp = -1;
    int brav_seq = -1;
    int latest_stored_respeck_seq = -1;


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
        mLocationUtils = LocationHelper.getInstance(this);

        // Set activity title
        this.setTitle(getString(R.string.app_name) + ", v" + mUtils.getAppVersionName());

        // Get Bluetooth address
        QOE_UUID = mUtils.getProperties().getProperty(Constants.PFIELD_QOEUUID);
        RESPECK_UUID = mUtils.getProperties().getProperty(Constants.PFIELD_RESPECK_UUID);

        // Initialize fragments
        FragmentManager fm = getSupportFragmentManager();

        // Load fragments from saved instance state
        if (savedInstanceState != null) {
            // If we have saved something from a previous activity lifecycle, the fragments probably already exist
            mDaphneHomeFragment = (DaphneHomeFragment) fm.getFragment(savedInstanceState, TAG_DAPHNE_HOME_FRAGMENT);
            mDaphneValuesFragment = (DaphneValuesFragment) fm.getFragment(savedInstanceState,
                    TAG_DAPHNE_VALUES_FRAGMENT);
            // If they don't exist, which could happen because the activity was paused before loading the fragments,
            // create new fragments
            if (mDaphneHomeFragment == null) {
                mDaphneHomeFragment = new DaphneHomeFragment();
            }
            if (mDaphneValuesFragment == null) {
                mDaphneValuesFragment = new DaphneValuesFragment();
            }
        } else {
            // If there is no saved instance state, this means we are starting the activity for the first time
            // Create all the fragments
            mDaphneHomeFragment = new DaphneHomeFragment();
            mDaphneValuesFragment = new DaphneValuesFragment();
        }

        setContentView(R.layout.activity_main_tabs);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(),
                getApplicationContext());
        sectionsPagerAdapter.addFragment(mDaphneHomeFragment);
        sectionsPagerAdapter.addFragment(mDaphneValuesFragment);

        // Set up the ViewPager with the sections adapter.
        ViewPager viewPager = (ViewPager) findViewById(R.id.container);
        if (viewPager != null) {
            viewPager.setAdapter(sectionsPagerAdapter);
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        if (tabLayout != null) {
            tabLayout.setupWithViewPager(viewPager);
        }

        tabLayout.getTabAt(0).setIcon(Constants.MENU_ICON_HOME);
        tabLayout.getTabAt(0).setText("");
        tabLayout.getTabAt(1).setIcon(Constants.MENU_ICON_INFO);
        tabLayout.getTabAt(1).setText("");


        // Add the toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);

        // For use with snack bar
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        //Initialize Breathing Functions
        initBreathing();

        // Initialize Readings hash maps
        initReadingMaps();

        // Bluetooth initialization
        initBluetooth();

        // Initialize Upload services
        initRespeckUploadService();
        initQOEUploadService();

        // Start task which makes activity predictions every 2 seconds
        startActivityClassificationTask();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Bluetooth startup
        startBluetooth();

        // Start location manager
        //mLocationUtils.startLocationManager();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop Bluetooth scanning
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }

        // Stop location manager
        //mLocationUtils.stopLocationManager();
    }

    @Override
    public void onButtonSelected(int buttonId) {
        // Do nothing because we don't have buttons in Daphne layout
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // TODO: intentreceiver is leaking. Unregister here. (error message when closing app in android log)

        // Cleanup Bluetooth handlers
        if (mGattRespeck == null && mGattQOE == null) {
            return;
        }

        if (mGattRespeck != null) {
            mGattRespeck.close();
            mGattRespeck = null;
        }

        if (mGattQOE != null) {
            mGattQOE.close();
            mGattQOE = null;
        }

        // Stop location manager
        //mLocationUtils.stopLocationManager();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        FragmentManager fm = getSupportFragmentManager();

        if (mDaphneHomeFragment != null && mDaphneHomeFragment.isAdded()) {
            fm.putFragment(outState, TAG_DAPHNE_HOME_FRAGMENT, mDaphneHomeFragment);
        }

        if (mCurrentFragment != null && mCurrentFragment.isAdded()) {
            fm.putFragment(outState, TAG_CURRENT_FRAGMENT, mCurrentFragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu, this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_daphne, menu);
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
     * Update {@link #mRespeckSensorReadings} with the latest values sent from the Respeck sensor.
     *
     * @param newValues HashMap<String, Float> The Respeck sensor readings.
     */
    private void updateRespeckReadings(HashMap<String, Float> newValues) {
        // Update local values
        mRespeckSensorReadings = newValues;
        mDaphneValuesFragment.updateBreathing(mRespeckSensorReadings);
        // Update the UI
        //updateRespeckUI();
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
        //updateQOEUI();
    }


    //----------------------------------------------------------------------------------------------
    // UPLOAD SERVICES -----------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private void initRespeckUploadService() {
        mRespeckRemoteUploadService = new RespeckRemoteUploadService();
        mRespeckRemoteUploadService.onCreate(this);
        Intent intent = new Intent(RespeckRemoteUploadService.MSG_CONFIG);

        JSONObject json = new JSONObject();
        try {
            json.put("patient_id", mUtils.getProperties().getProperty(Constants.PFIELD_PATIENT_ID));
            json.put("respeck_key", mUtils.getProperties().getProperty(Constants.PFIELD_RESPECK_KEY));
            json.put("respeck_uuid", mUtils.getProperties().getProperty(Constants.PFIELD_RESPECK_UUID));
            json.put("qoe_uuid", mUtils.getProperties().getProperty(Constants.PFIELD_QOEUUID));
            json.put("tablet_serial", mUtils.getProperties().getProperty(Constants.PFIELD_TABLET_SERIAL));
            json.put("app_version", mUtils.getAppVersionCode());
        } catch (Exception e) {
            e.printStackTrace();
        }

        intent.putExtra(RespeckRemoteUploadService.MSG_CONFIG_JSON_HEADERS, json.toString());
        intent.putExtra(RespeckRemoteUploadService.MSG_CONFIG_URL, Constants.UPLOAD_SERVER_URL);
        intent.putExtra(RespeckRemoteUploadService.MSG_CONFIG_PATH, Constants.UPLOAD_SERVER_PATH);
        sendBroadcast(intent);
    }

    private void initQOEUploadService() {
        mQOERemoteUploadService = new QOERemoteUploadService();
        mQOERemoteUploadService.onCreate(this);
        Intent intent = new Intent(QOERemoteUploadService.MSG_CONFIG);

        JSONObject json = new JSONObject();
        try {
            json.put("patient_id", mUtils.getProperties().getProperty(Constants.PFIELD_PATIENT_ID));
            json.put("respeck_key", mUtils.getProperties().getProperty(Constants.PFIELD_RESPECK_KEY));
            json.put("respeck_uuid", mUtils.getProperties().getProperty(Constants.PFIELD_RESPECK_UUID));
            json.put("qoe_uuid", mUtils.getProperties().getProperty(Constants.PFIELD_QOEUUID));
            json.put("tablet_serial", mUtils.getProperties().getProperty(Constants.PFIELD_TABLET_SERIAL));
            json.put("app_version", mUtils.getAppVersionCode());
        } catch (Exception e) {
            e.printStackTrace();
        }

        intent.putExtra(QOERemoteUploadService.MSG_CONFIG_JSON_HEADERS, json.toString());
        intent.putExtra(QOERemoteUploadService.MSG_CONFIG_URL, Constants.UPLOAD_SERVER_URL);
        intent.putExtra(QOERemoteUploadService.MSG_CONFIG_PATH, Constants.UPLOAD_SERVER_PATH);
        sendBroadcast(intent);
    }


    //----------------------------------------------------------------------------------------------
    // BLUETOOTH METHODS ---------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    /**
     * Initiate Bluetooth adapter.
     */
    private void initBluetooth() {
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mQOEConnectionComplete = false;
        mRespeckConnectionComplete = false;
    }

    /**
     * Check Bluetooth availability and initiate devices scanning.
     */
    private void startBluetooth() {
        // Check if Bluetooth is supported on the device
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Check if Bluetooth is enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // For API level 21 and above
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mScanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            // Add the devices's addresses that we need for scanning
            mScanFilters = new ArrayList<ScanFilter>();
            mScanFilters.add(new ScanFilter.Builder().setDeviceAddress(RESPECK_UUID).build());
            mScanFilters.add(new ScanFilter.Builder().setDeviceAddress(QOE_UUID).build());

            // Start Bluetooth scanning
            scanLeDevice(true);
        }
    }

    /**
     * Start or stop scanning for devices.
     *
     * @param start boolean If true start scanning, else stop scanning.
     */
    private void scanLeDevice(final boolean start) {
        if (start) {
            mLEScanner.startScan(mScanFilters, mScanSettings, mScanCallback);
        } else {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    /**
     * Scan callback to handle found devices.
     */
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice btDevice = result.getDevice();

            if (btDevice != null) {
                Log.i("[Bluetooth]", "Device found: " + btDevice.getName());
                connectToDevice(btDevice);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("[Bluetooth]", "Scan Failed. Error Code: " + errorCode);
        }
    };

    /**
     * Connect to Bluetooth devices.
     *
     * @param device BluetoothDevice The device.
     */
    public void connectToDevice(BluetoothDevice device) {
        if (mGattRespeck == null && device.getName().contains("Respeck")) {
            Log.i("[Bluetooth]", "Connecting to " + device.getName());
            mDeviceRespeck = device;
            mGattRespeck = device.connectGatt(getApplicationContext(), true, mGattCallbackRespeck);
            mDaphneHomeFragment.updateConnectionRESpeck(true);
        }

        if (mGattQOE == null && device.getName().contains("QOE")) {
            Log.i("[Bluetooth]", "Connecting to " + device.getName());
            mDeviceQOE = device;
            mGattQOE = device.connectGatt(getApplicationContext(), true, mGattCallbackQOE);
            mDaphneHomeFragment.updateConnectionAirpeck(true);
        }

        if (mGattQOE != null && mGattRespeck != null) {
            Log.i("[Bluetooth]", "Devices connected. Scanner turned off.");
            scanLeDevice(false);
        }
    }

    /**
     * Bluetooth Gatt callback to handle data interchange with Bluetooth devices.
     */
    private final BluetoothGattCallback mGattCallbackQOE = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("[QOE]", "onConnectionStateChange - Status: " + status);

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    mQOEConnectionComplete = true;
                    Log.i("QOE-gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();

                    Snackbar.make(mCoordinatorLayout, "QOE "
                            + getString(R.string.device_connected)
                            + ". " + getString(R.string.waiting_for_data)
                            + ".", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    mQOEConnectionComplete = false;
                    Log.e("QOE-gattCallback", "STATE_DISCONNECTED");
                    Log.i("QOE-gattCallback", "reconnecting...");
                    BluetoothDevice device = gatt.getDevice();
                    mGattQOE.close();
                    mGattQOE = null;
                    connectToDevice(device);
                    break;
                default:
                    Log.e("QOE-gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                Log.i("[QOE]", "onServicesDiscovered - " + services.toString());
                //gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));

                for (BluetoothGattService s : services) {
                    BluetoothGattCharacteristic characteristic = s.getCharacteristic(
                            UUID.fromString(QOE_LIVE_CHARACTERISTIC));

                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString(QOE_CLIENT_CHARACTERISTIC));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (descriptor.getCharacteristic().getUuid().equals(UUID.fromString(QOE_LIVE_CHARACTERISTIC))) {
                Log.i("[QOE]", "onDescriptorWrite - " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            int sampleId2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            int packetNumber2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);

            lastPackets_2[packetNumber2] = characteristic.getValue();
            sampleIDs_2[packetNumber2] = sampleId2;

            if (characteristic.getUuid().equals(UUID.fromString(QOE_LIVE_CHARACTERISTIC))) {
                if ((sampleIDs_2[0] == sampleIDs_2[1]) && lastSample != sampleIDs_2[0] &&
                        (sampleIDs_2[1] == sampleIDs_2[2] && (sampleIDs_2[2] == sampleIDs_2[3]) &&
                                (sampleIDs_2[3] == sampleIDs_2[4]))) {

                    byte[] finalPacket = {};
                    int size = 5;

                    finalPacket = new byte[lastPackets_2[0].length + lastPackets_2[1].length +
                            lastPackets_2[2].length + lastPackets_2[3].length + lastPackets_2[4].length - 8];
                    int finalPacketIndex = 0;
                    for (int i = 0; i < size; i++) {
                        for (int j = 2; j < lastPackets_2[i].length; j++) {
                            finalPacket[finalPacketIndex] = lastPackets_2[i][j];
                            finalPacketIndex++;
                        }
                    }

                    ByteBuffer packetBufferLittleEnd = ByteBuffer.wrap(finalPacket).order(ByteOrder.LITTLE_ENDIAN);
                    ByteBuffer packetBufferBigEnd = ByteBuffer.wrap(finalPacket).order(ByteOrder.BIG_ENDIAN);

                    int bin0 = packetBufferLittleEnd.getShort();
                    int bin1 = packetBufferLittleEnd.getShort();
                    int bin2 = packetBufferLittleEnd.getShort();
                    int bin3 = packetBufferLittleEnd.getShort();
                    int bin4 = packetBufferLittleEnd.getShort();
                    int bin5 = packetBufferLittleEnd.getShort();
                    int bin6 = packetBufferLittleEnd.getShort();
                    int bin7 = packetBufferLittleEnd.getShort();
                    int bin8 = packetBufferLittleEnd.getShort();
                    int bin9 = packetBufferLittleEnd.getShort();
                    int bin10 = packetBufferLittleEnd.getShort();
                    int bin11 = packetBufferLittleEnd.getShort();
                    int bin12 = packetBufferLittleEnd.getShort();
                    int bin13 = packetBufferLittleEnd.getShort();
                    int bin14 = packetBufferLittleEnd.getShort();
                    int bin15 = packetBufferLittleEnd.getShort();

                    int total = bin0 + bin1 + bin2 + bin3
                            + bin4 + bin5 + bin6 + bin7
                            + bin8 + bin9 + bin10 + bin11
                            + bin12 + bin13 + bin14 + bin15;

                    String bins_data_string = bin0 + "," + bin1 + "," + bin2 + "," + bin3 + ","
                            + bin4 + "," + bin5 + "," + bin6 + "," + bin7 + ","
                            + bin8 + "," + bin9 + "," + bin10 + "," + bin11 + ","
                            + bin12 + "," + bin13 + "," + bin14 + "," + bin15;

                    double temperature = -1;
                    double hum = -1;

                    float pm1 = -1;
                    float pm2_5 = -1;
                    float pm10 = -1;

                    float opctemp = -1;

                    int o3_ae = -1;
                    int o3_we = -1;

                    int no2_ae = -1;
                    int no2_we = -1;

                    // MtoF
                    packetBufferLittleEnd.getInt();
                    // opc_temp
                    opctemp = packetBufferLittleEnd.getInt();
                    // opc_pressure
                    packetBufferLittleEnd.getInt();
                    // period count
                    packetBufferLittleEnd.getInt();
                    // uint16_t checksum ????
                    packetBufferLittleEnd.getShort();

                    pm1 = packetBufferLittleEnd.getFloat();
                    pm2_5 = packetBufferLittleEnd.getFloat();
                    pm10 = packetBufferLittleEnd.getFloat();

                    o3_ae = packetBufferBigEnd.getShort(62);
                    o3_we = packetBufferBigEnd.getShort(64);

                    no2_ae = packetBufferBigEnd.getShort(66);
                    no2_we = packetBufferBigEnd.getShort(68);

                    /* uint16_t temp */
                    int temp = packetBufferBigEnd.getShort(70) & 0xffff;

                    /* uint16_t humidity */
                    int humidity = packetBufferBigEnd.getShort(72);

                    temperature = ((temp - 3960) / 100.0);
                    hum = (-2.0468 + (0.0367 * humidity) + (-0.0000015955 * humidity * humidity));

                    Log.i("[QOE]", "PM1: " + pm1);
                    Log.i("[QOE]", "PM2.5: " + pm2_5);
                    Log.i("[QOE]", "PM10: " + pm10);

                    lastSample = sampleIDs_2[0];

                    // Get timestamp
                    long unixTimestamp = mUtils.getUnixTimestamp();

                    // Get location
                    double latitude = 0;
                    double longitude = 0;
                    double altitude = 0;
                    try {
                        latitude = mLocationUtils.getLatitude();
                        longitude = mLocationUtils.getLongitude();
                        altitude = mLocationUtils.getAltitude();
                    } catch (Exception e) {
                        Log.e("[QOE]", "Location permissions not granted.");
                    }

                    // Send message
                    JSONObject json = new JSONObject();
                    try {
                        json.put("messagetype", "qoe_data");
                        json.put(Constants.QOE_PM1, pm1);
                        json.put(Constants.QOE_PM2_5, pm2_5);
                        json.put(Constants.QOE_PM10, pm10);
                        json.put(Constants.QOE_TEMPERATURE, temperature);
                        json.put(Constants.QOE_HUMIDITY, hum);
                        json.put(Constants.QOE_S1ae_NO2, no2_ae);
                        json.put(Constants.QOE_S1we_NO2, no2_we);
                        json.put(Constants.QOE_S2ae_O3, o3_ae);
                        json.put(Constants.QOE_S2we_O3, o3_we);
                        json.put(Constants.QOE_BINS_0, bin0);
                        json.put(Constants.QOE_BINS_1, bin1);
                        json.put(Constants.QOE_BINS_2, bin2);
                        json.put(Constants.QOE_BINS_3, bin3);
                        json.put(Constants.QOE_BINS_4, bin4);
                        json.put(Constants.QOE_BINS_5, bin5);
                        json.put(Constants.QOE_BINS_6, bin6);
                        json.put(Constants.QOE_BINS_7, bin7);
                        json.put(Constants.QOE_BINS_8, bin8);
                        json.put(Constants.QOE_BINS_9, bin9);
                        json.put(Constants.QOE_BINS_10, bin10);
                        json.put(Constants.QOE_BINS_11, bin11);
                        json.put(Constants.QOE_BINS_12, bin12);
                        json.put(Constants.QOE_BINS_13, bin13);
                        json.put(Constants.QOE_BINS_14, bin14);
                        json.put(Constants.QOE_BINS_15, bin15);
                        json.put(Constants.QOE_BINS_TOTAL, total);
                        json.put(Constants.LOC_LATITUDE, latitude);
                        json.put(Constants.LOC_LONGITUDE, longitude);
                        json.put(Constants.LOC_ALTITUDE, altitude);
                        json.put(Constants.UNIX_TIMESTAMP, unixTimestamp);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Intent intent = new Intent(QOERemoteUploadService.MSG_UPLOAD);
                    intent.putExtra(QOERemoteUploadService.MSG_UPLOAD_DATA, json.toString());
                    sendBroadcast(intent);
                    Log.d("[QOE]", "Sent LIVE JSON to upload service: " + json.toString());


                    // Update the UI
                    HashMap<String, Float> values = new HashMap<String, Float>();
                    values.put(Constants.QOE_PM1, pm1);
                    values.put(Constants.QOE_PM2_5, pm2_5);
                    values.put(Constants.QOE_PM10, pm10);
                    values.put(Constants.QOE_TEMPERATURE, (float) temperature);
                    values.put(Constants.QOE_HUMIDITY, (float) hum);
                    values.put(Constants.QOE_NO2, (float) no2_ae);
                    values.put(Constants.QOE_O3, (float) o3_ae);
                    values.put(Constants.QOE_BINS_0, (float) bin0);
                    values.put(Constants.QOE_BINS_1, (float) bin1);
                    values.put(Constants.QOE_BINS_2, (float) bin2);
                    values.put(Constants.QOE_BINS_3, (float) bin3);
                    values.put(Constants.QOE_BINS_4, (float) bin4);
                    values.put(Constants.QOE_BINS_5, (float) bin5);
                    values.put(Constants.QOE_BINS_6, (float) bin6);
                    values.put(Constants.QOE_BINS_7, (float) bin7);
                    values.put(Constants.QOE_BINS_8, (float) bin8);
                    values.put(Constants.QOE_BINS_9, (float) bin9);
                    values.put(Constants.QOE_BINS_10, (float) bin10);
                    values.put(Constants.QOE_BINS_11, (float) bin11);
                    values.put(Constants.QOE_BINS_12, (float) bin12);
                    values.put(Constants.QOE_BINS_13, (float) bin13);
                    values.put(Constants.QOE_BINS_14, (float) bin14);
                    values.put(Constants.QOE_BINS_15, (float) bin15);
                    values.put(Constants.QOE_BINS_TOTAL, (float) total);

                    Message msg = Message.obtain();
                    msg.obj = values;
                    msg.what = UPDATE_QOE_READINGS;
                    msg.setTarget(mUIHandler);
                    msg.sendToTarget();
                }
            }
        }
    };

    private final BluetoothGattCallback mGattCallbackRespeck = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    mRespeckConnectionComplete = true;
                    Log.i("Respeck-gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();

                    Snackbar.make(mCoordinatorLayout, "Respeck "
                            + getString(R.string.device_connected)
                            + ". " + getString(R.string.waiting_for_data)
                            + ".", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    mRespeckConnectionComplete = false;
                    Log.e("Respeck-gattCallback", "STATE_DISCONNECTED");
                    Log.i("Respeck-gattCallback", "reconnecting...");
                    BluetoothDevice device = gatt.getDevice();
                    mGattRespeck.close();
                    mGattRespeck = null;
                    connectToDevice(device);
                    break;
                default:
                    Log.e("Respeck-gattCallback", "STATE_OTHER");
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                Log.i("Respeck", "onServicesDiscovered - " + services.toString());
                //gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));

                for (BluetoothGattService s : services) {
                    BluetoothGattCharacteristic characteristic = s.getCharacteristic(
                            UUID.fromString(RESPECK_LIVE_CHARACTERISTIC));

                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (descriptor.getCharacteristic().getUuid().equals(UUID.fromString(RESPECK_LIVE_CHARACTERISTIC))) {
                Log.i("Respeck", "RESPECK_LIVE_CHARACTERISTIC");

                BluetoothGattService s = descriptor.getCharacteristic().getService();

                BluetoothGattCharacteristic characteristic = s.getCharacteristic(
                        UUID.fromString(RESPECK_BREATHING_RATES_CHARACTERISTIC));

                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true);
                    BluetoothGattDescriptor descriptor2 = characteristic.getDescriptor(
                            UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                    descriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor2);
                }
            } else if (descriptor.getCharacteristic().getUuid().equals(
                    UUID.fromString(RESPECK_BREATH_INTERVALS_CHARACTERISTIC))) {
                Log.i("Respeck", "RESPECK_BREATH_INTERVALS_CHARACTERISTIC");

            } else if (descriptor.getCharacteristic().getUuid().equals(
                    UUID.fromString(RESPECK_BREATHING_RATES_CHARACTERISTIC))) {

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(UUID.fromString(RESPECK_LIVE_CHARACTERISTIC))) {
                final byte[] accelBytes = characteristic.getValue();
                final int len = accelBytes.length;
                final int seq = accelBytes[0] & 0xFF;

                //Check if the reading is not repeated
                if (seq == latest_live_respeck_seq) {
                    Log.e("RAT", "DUPLICATE SEQUENCE NUMBER: " + Integer.toString(seq));
                    return;
                } else {
                    latest_live_respeck_seq = seq;
                }

                for (int i = 1; i < len; i += 7) {
                    Byte start_byte = accelBytes[i];
                    if (start_byte == -1) {
                        Byte ts_1 = accelBytes[i + 1];
                        Byte ts_2 = accelBytes[i + 2];
                        Byte ts_3 = accelBytes[i + 3];
                        Byte ts_4 = accelBytes[i + 4];

                        live_bs_timestamp = System.currentTimeMillis();
                        Long new_rs_timestamp = combineTimestampBytes(ts_1, ts_2, ts_3, ts_4) * 197 / 32768;
                        if (new_rs_timestamp == live_rs_timestamp) {
                            Log.e("RAT", "DUPLICATE LIVE TIMESTAMP RECEIVED");
                            return;
                        }
                        live_rs_timestamp = new_rs_timestamp;
                        live_seq = 0;

                        while (!stored_queue.isEmpty()) {
                            RESpeckStoredSample s = stored_queue.remove();

                            Long current_time_offset = live_bs_timestamp / 1000 - live_rs_timestamp;

                            //MANDA AL SERVIDOR PERO NO ACTUALIZA EN PANTALLA
                            Log.i("1", "EXTRA_RESPECK_TIMESTAMP_OFFSET_SECS: " + current_time_offset);
                            Log.i("1", "EXTRA_RESPECK_RS_TIMESTAMP: " + s.getRs_timestamp());
                            Log.i("1", "EXTRA_RESPECK_SEQ: " + s.getSeq());
                            Log.i("1", "EXTRA_RESPECK_LIVE_AVE_BR: " + s.getMeanBr());
                            Log.i("1", "EXTRA_RESPECK_LIVE_N_BR: " + s.getNBreaths());
                            Log.i("1", "EXTRA_RESPECK_LIVE_SD_BR: " + s.getSdBr());
                            Log.i("1", "EXTRA_RESPECK_LIVE_ACTIVITY " + s.getActivity());
                        }
                    } else if (start_byte == -2 && live_seq >= 0) {
                        try {
                            final float x = combineAccelBytes(Byte.valueOf(accelBytes[i + 1]),
                                    Byte.valueOf(accelBytes[i + 2]));
                            float y = combineAccelBytes(Byte.valueOf(accelBytes[i + 3]),
                                    Byte.valueOf(accelBytes[i + 4]));
                            float z = combineAccelBytes(Byte.valueOf(accelBytes[i + 5]),
                                    Byte.valueOf(accelBytes[i + 6]));

                            updateBreathing(x, y, z);

                            float breathingRate = getBreathingRate();
                            float breathingSignal = getBreathingSignal();
                            float breathingAngle = getBreathingAngle();
                            float averageBreathingRate = getAverageBreathingRate();
                            float stdDevBreathingRate = getStdDevBreathingRate();
                            float nBreaths = getNBreaths();
                            float breathActivity = getActivityLevel();
                            float activityType = getCurrentActivityClassification();

                            Log.i("2", "BS TIMESTAMP " + String.valueOf(live_bs_timestamp));
                            Log.i("2", "RS_TIMESTAMP " + String.valueOf(live_rs_timestamp));
                            Log.i("2", "EXTRA_RESPECK_SEQ " + String.valueOf(live_seq));
                            Log.i("2", "Breathing Rate " + String.valueOf(breathingRate));
                            Log.i("2", "Breathing Signal " + String.valueOf(breathingSignal));
                            Log.i("2", "Breathing Angle " + String.valueOf(breathingAngle));
                            Log.i("2", "BRA " + String.valueOf(averageBreathingRate));
                            Log.i("2", "STDBR " + String.valueOf(stdDevBreathingRate));
                            Log.i("2", "NBreaths " + String.valueOf(nBreaths));
                            Log.i("2", "Activity " + String.valueOf(breathActivity));


                            // Get timestamp
                            long unixTimestamp = mUtils.getUnixTimestamp();

                            // Send message
                            JSONObject json = new JSONObject();
                            try {
                                json.put("messagetype", "respeck_data");
                                json.put(Constants.RESPECK_X, x);
                                json.put(Constants.RESPECK_Y, y);
                                json.put(Constants.RESPECK_Z, z);
                                json.put(Constants.RESPECK_ACTIVITY_TYPE, activityType);
                                if (Float.isNaN(breathingRate)) {
                                    json.put(Constants.RESPECK_BREATHING_RATE, null);
                                } else {
                                    json.put(Constants.RESPECK_BREATHING_RATE, breathingRate);
                                }
                                if (Float.isNaN(breathingSignal)) {
                                    json.put(Constants.RESPECK_BREATHING_SIGNAL, null);
                                } else {
                                    json.put(Constants.RESPECK_BREATHING_SIGNAL, breathingSignal);
                                }
                                if (Float.isNaN(breathingAngle)) {
                                    json.put(Constants.RESPECK_BREATHING_ANGLE, null);
                                } else {
                                    json.put(Constants.RESPECK_BREATHING_ANGLE, breathingAngle);
                                }
                                if (Float.isNaN(averageBreathingRate)) {
                                    json.put(Constants.RESPECK_AVERAGE_BREATHING_RATE, null);
                                } else {
                                    json.put(Constants.RESPECK_AVERAGE_BREATHING_RATE, averageBreathingRate);
                                }
                                if (Float.isNaN(stdDevBreathingRate)) {
                                    json.put(Constants.RESPECK_STD_DEV_BREATHING_RATE, null);
                                } else {
                                    json.put(Constants.RESPECK_STD_DEV_BREATHING_RATE, stdDevBreathingRate);
                                }
                                if (Float.isNaN(nBreaths)) {
                                    json.put(Constants.RESPECK_N_BREATHS, null);
                                } else {
                                    json.put(Constants.RESPECK_N_BREATHS, nBreaths);
                                }
                                if (Float.isNaN(breathActivity)) {
                                    json.put(Constants.RESPECK_ACTIVITY_LEVEL, null);
                                } else {
                                    json.put(Constants.RESPECK_ACTIVITY_LEVEL, breathActivity);
                                }
                                json.put(Constants.RESPECK_LIVE_SEQ, live_seq);
                                json.put(Constants.RESPECK_LIVE_RS_TIMESTAMP, live_rs_timestamp);
                                json.put(Constants.UNIX_TIMESTAMP, unixTimestamp);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Intent intent = new Intent(RespeckRemoteUploadService.MSG_UPLOAD);
                            intent.putExtra(RespeckRemoteUploadService.MSG_UPLOAD_DATA, json.toString());
                            sendBroadcast(intent);
                            Log.d("RESPECK", "Sent LIVE JSON to upload service: " + json.toString());


                            //UPDATE THE UI
                            HashMap<String, Float> values = new HashMap<String, Float>();
                            values.put(Constants.RESPECK_X, x);
                            values.put(Constants.RESPECK_Y, y);
                            values.put(Constants.RESPECK_Z, z);
                            values.put(Constants.RESPECK_ACTIVITY_TYPE, activityType);
                            if (Float.isNaN(breathingRate)) {
                                values.put(Constants.RESPECK_BREATHING_RATE, 0f);
                            } else {
                                values.put(Constants.RESPECK_BREATHING_RATE, breathingRate);
                            }
                            if (Float.isNaN(breathingSignal)) {
                                values.put(Constants.RESPECK_BREATHING_SIGNAL, 0f);
                            } else {
                                values.put(Constants.RESPECK_BREATHING_SIGNAL, breathingSignal);
                            }
                            if (Float.isNaN(averageBreathingRate)) {
                                values.put(Constants.RESPECK_AVERAGE_BREATHING_RATE, 0f);
                            } else {
                                values.put(Constants.RESPECK_AVERAGE_BREATHING_RATE, averageBreathingRate);
                            }

                            Message msg = Message.obtain();
                            msg.obj = values;
                            msg.what = UPDATE_RESPECK_READINGS;
                            msg.setTarget(mUIHandler);
                            msg.sendToTarget();

                            //RESpeckStoredSample s = stored_queue.remove();

                            live_seq += 1;

                            long ts_minute = DateUtils.truncate(new Date(live_bs_timestamp), Calendar.MINUTE).getTime();

                            if (ts_minute > latestProcessedMinute) {
                                calculateMA();

                                float ave_br = getAverageBreathingRate();
                                float sd_br = getStdDevBreathingRate();
                                //Log.d("RAT", "STD_DEV: " + Float.toString(sd_br));
                                int n_breaths = getNBreaths();
                                float act = getActivityLevel();

                                resetMA();
                                //ACTUALIZA Y MANDA AL SERVIDOR
                                Log.i("3", "RESPECK BS TIMESTAMP " + ts_minute);
                                Log.i("3", "EXTRA_RESPECK_LIVE_AVE_BR: " + ave_br);
                                Log.i("3", "EXTRA_RESPECK_LIVE_N_BR: " + n_breaths);
                                Log.i("3", "EXTRA_RESPECK_LIVE_SD_BR: " + sd_br);
                                Log.i("3", "EXTRA_RESPECK_LIVE_ACTIVITY " + act);

                                // Send message
                                JSONObject json2 = new JSONObject();
                                try {
                                    json2.put("messagetype", "respeck_processed");
                                    json2.put("timestamp", ts_minute);
                                    json2.put("activity", act);
                                    if (!Float.isNaN(ave_br)) {
                                        json2.put("breathing_rate", ave_br);
                                        json2.put("n_breaths", n_breaths);
                                        json2.put("sd_br", sd_br);
                                    }
                                    json2.put("stored", 0);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Intent intent2 = new Intent(RespeckRemoteUploadService.MSG_UPLOAD);
                                intent2.putExtra(RespeckRemoteUploadService.MSG_UPLOAD_DATA, json2.toString());
                                sendBroadcast(intent2);
                                Log.d("RAT", "Sent LIVE JSON to upload service: " + json.toString());


                                latestProcessedMinute = ts_minute;
                            }
                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (characteristic.getUuid().equals(UUID.fromString(RESPECK_BREATHING_RATES_CHARACTERISTIC))) {
                final byte[] breathAveragesBytes = characteristic.getValue();
                final int len = breathAveragesBytes.length;

                final int seq = breathAveragesBytes[0] & 0xFF;

                if (seq == latest_stored_respeck_seq) {
                    return;
                } else {
                    latest_stored_respeck_seq = seq;
                }


                for (int i = 1; i < len; i += 6) {
                    Byte start_byte = breathAveragesBytes[i];

                    if (start_byte == -1) {
                        //timestamp
                        Byte ts_1 = breathAveragesBytes[i + 1];
                        Byte ts_2 = breathAveragesBytes[i + 2];
                        Byte ts_3 = breathAveragesBytes[i + 3];
                        Byte ts_4 = breathAveragesBytes[i + 4];
                        brav_bs_timestamp = System.currentTimeMillis();
                        brav_rs_timestamp = combineTimestampBytes(ts_1, ts_2, ts_3, ts_4) * 197 / 32768;
                        brav_seq = 0;
                    } else if (start_byte == -2) {
                        //breath average
                        int n_breaths = breathAveragesBytes[i + 1] & 0xFF;
                        if (n_breaths > 5) {
                            float mean_br = (float) (breathAveragesBytes[i + 2] & 0xFF) / 5.0f;
                            float sd_br = (float) Math.sqrt((float) (breathAveragesBytes[i + 3] & 0xFF) / 10.0f);

                            Byte upper_act = breathAveragesBytes[i + 4];
                            Byte lower_act = breathAveragesBytes[i + 5];
                            float combined_act = combineActBytes(upper_act, lower_act);

                            RESpeckStoredSample ras = new RESpeckStoredSample(brav_bs_timestamp, brav_rs_timestamp,
                                    brav_seq++, n_breaths, mean_br, sd_br, combined_act);
                            Log.w("RAT", "Queueing stored sample: " + ras.toString());
                            stored_queue.add(ras);
                        }
                    }


                    //Log.i("RAT", "BREATH AVERAGES: " + Arrays.toString(breath_averages));
                    //updateTextBox("Breath Averages: " + Arrays.toString(breath_averages));
                }

            }
        }
    };

    private long combineTimestampBytes(Byte upper, Byte upper_middle, Byte lower_middle, Byte lower) {
        short unsigned_upper = (short) (upper & 0xFF);
        short unsigned_upper_middle = (short) (upper_middle & 0xFF);
        short unsigned_lower_middle = (short) (lower_middle & 0xFF);
        short unsigned_lower = (short) (lower & 0xFF);
        int value = (int) ((unsigned_upper << 24) | (unsigned_upper_middle << 16) | (unsigned_lower_middle << 8) | unsigned_lower);
        long uValue = value & 0xffffffffL;
        return uValue;
    }

    private float combineAccelBytes(Byte upper, Byte lower) {
        short unsigned_lower = (short) (lower & 0xFF);
        short value = (short) ((upper << 8) | unsigned_lower);
        float fValue = (value) / 16384.0f;
        return fValue;
    }

    private float combineActBytes(Byte upper, Byte lower) {
        short unsigned_lower = (short) (lower & 0xFF);
        short unsigned_upper = (short) (upper & 0xFF);
        short value = (short) ((unsigned_upper << 8) | unsigned_lower);
        float fValue = (value) / 1000.0f;
        return fValue;
    }

    private void startActivityClassificationTask() {
        final Handler h = new Handler();

        // We want to summarise predictions every 10 minutes.
        final int SUMMARY_COUNT_MAX = (int) (10 * 60 / 2.);

        // How often do we update the activity classification?
        // half the window size for the activity predictions, in milliseconds
        final int delay = 2000;

        // Create prediction summary directory if it doesn't exist
        final File summaryDirectory = new File(Environment.getExternalStorageDirectory(), "/ActivityPrediction");
        if (!summaryDirectory.exists()) {
            boolean created = summaryDirectory.mkdirs();
            Log.i("DF", "Directory created: " + summaryDirectory);
            if (!created) {
                throw new RuntimeException("Couldn't create folder for temporary storage of the recording");
            }
        }

        // Path to summary storage file. For now, we only use one file. Could be separated if the file gets too large.
        final String filenameSummaryStorage = summaryDirectory + "/" + "predictions_summary";


        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            // We summarise the currently stored predictions when the counter reaches max
            int summaryCounter = 0;
            int temporaryStoragePredictions[] = new int[Constants.NUM_ACT_CLASSES];

            @Override
            public void run() {
                updateActivityClassification();
                int predictionIdx = getCurrentActivityClassification();
                // an index of -1 means that the acceleration buffer has not been filled yet, so we have to wait.
                if (predictionIdx != -1) {
                    Log.i("DF", String.format("Prediction: %s", Constants.ACT_CLASS_NAMES[predictionIdx]));
                    temporaryStoragePredictions[predictionIdx]++;
                    summaryCounter++;
                }

                Log.i("DF", "summary counter: " + summaryCounter);

                // If the temporary prediction recording is full, i.e. count is MAX, we
                // write the summarised data into another external file and empty the temporary file.
                if (summaryCounter == SUMMARY_COUNT_MAX) {

                    String lineToWrite = getCurrentTimeStamp() + "\t" +
                            Math.round(temporaryStoragePredictions[0] * 100. / SUMMARY_COUNT_MAX) + "\t" +
                            Math.round(temporaryStoragePredictions[1] * 100. / SUMMARY_COUNT_MAX) + "\t" +
                            Math.round(temporaryStoragePredictions[2] * 100. / SUMMARY_COUNT_MAX) + "\n";

                    try {
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                                new FileOutputStream(filenameSummaryStorage, true));
                        outputStreamWriter.append(lineToWrite);
                        outputStreamWriter.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Reset counts to zero
                    Arrays.fill(temporaryStoragePredictions, 0);
                    summaryCounter = 0;
                }
            }
        }, 0, delay);
    }

    public static String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.UK).format(new Date());
    }


    static {
        System.loadLibrary("respeck-jni");
    }

    //public native String getMsgFromJni();
    public native void initBreathing();

    public native void updateBreathing(float x, float y, float z);

    public native float getBreathingSignal();

    public native float getBreathingRate();

    public native float getAverageBreathingRate();

    public native float getActivityLevel();

    public native int getNBreaths();

    public native float getBreathingAngle();

    //public native String stringfromJNI();
    public native void resetMA();

    public native void calculateMA();

    public native float getStdDevBreathingRate();

    public native int getCurrentActivityClassification();

    public native void updateActivityClassification();
}

