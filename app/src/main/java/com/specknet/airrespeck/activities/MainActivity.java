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
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
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

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.SectionsPagerAdapter;
import com.specknet.airrespeck.fragments.GraphsFragment;
import com.specknet.airrespeck.fragments.HomeFragment;
import com.specknet.airrespeck.fragments.AQReadingsFragment;
import com.specknet.airrespeck.fragments.MenuFragment;
import com.specknet.airrespeck.qoeuploadservice.QOERemoteUploadService;
import com.specknet.airrespeck.respeckuploadservice.RespeckRemoteUploadService;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class MainActivity extends BaseActivity implements MenuFragment.OnMenuSelectedListener {

    // UTILS
    Utils mUtils;


    // FRAGMENTS
    private static final String TAG_HOME_FRAGMENT = "HOME_FRAGMENT";
    private static final String TAG_AQREADINGS_FRAGMENT = "AQREADINGS_FRAGMENT";
    private static final String TAG_GRAPHS_FRAGMENT = "GRAPHS_FRAGMENT";
    private static final String TAG_CURRENT_FRAGMENT = "CURRENT_FRAGMENT";

    private HomeFragment mHomeFragment;
    private AQReadingsFragment mAQReadingsFragment;
    private GraphsFragment mGraphsFragment;
    private Fragment mCurrentFragment;


    // READING VALUES
    HashMap<String, Float> mRespeckSensorReadings;
    HashMap<String, Float> mQOESensorReadings;


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
    private static final String RESPECK_UUID = "F5:85:7D:EA:61:F9";
    private static final String QOE_UUID = "FC:A6:33:A2:A4:5A";
    private static final String QOE_CLIENT_CHARACTERISTIC = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String QOE_LIVE_CHARACTERISTIC = "00002002-e117-4bff-b00d-b20878bc3f44";

    // CODIGO MANTAS
    private static byte[][] lastPackets_1  = new byte[4][];
    private static byte[][] lastPackets_2  = new byte[5][];
    private static int[] sampleIDs_1 = {0, 0, 0, 0};
    private static int[] sampleIDs_2 = {0, 1, 2, 3, 4};
    int lastSample = 0;


    // UPLOAD SERVICES
    RespeckRemoteUploadService mRespeckRemoteUploadService;
    QOERemoteUploadService mQOERemoteUploadService;


    // UI HANDLER
    private final static int UPDATE_RESPECK_READINGS = 0;
    private final static int UPDATE_QOE_READINGS = 1;

    /**
     * Static inner class doesn't hold an implicit reference to the outer class
     */
    private static class UIHandler extends Handler {
        // Using a weak reference means you won't prevent garbage collection
        private final WeakReference<MainActivity> mService;

        public UIHandler(MainActivity service) {
            mService = new WeakReference<MainActivity>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            final int what = msg.what;

            MainActivity service = mService.get();

            if (service != null) {
                switch(what) {
                    case UPDATE_RESPECK_READINGS:
                        service.updateRespeckReadings( (HashMap<String, Float>) msg.obj );
                        break;
                    case UPDATE_QOE_READINGS:
                        service.updateQOEReadings( (HashMap<String, Float>) msg.obj );
                        break;
                }
            }
        }
    }

    /**
     * A getter for the UI handler
     * @return UIHandler The handler.
     */
    public Handler getHandler() {
        return new UIHandler(this);
    }

    private final Handler mUIHandler = getHandler();


    /**
     * Update {@link #mRespeckSensorReadings} with the latest values sent from the Respeck sensor.
     * @param newValues HashMap<String, Float> The Respeck sensor readings.
     */
    private void updateRespeckReadings(HashMap<String, Float> newValues) {
        // Update local values
        mRespeckSensorReadings = newValues;

        // Update the UI
        updateUI();
    }

    /**
     * Update {@link #mQOESensorReadings} with the latest values sent from the QOE sensor.
     * @param newValues HashMap<String, Float> The QOE sensor readings.
     */
    private void updateQOEReadings(HashMap<String, Float> newValues) {
        // Update local values
        mQOESensorReadings = newValues;

        // Update the UI
        updateUI();
    }


    /**
     * Update reading values in fragments' UIs.
     */
    private void updateUI() {
        // Update connection loading layout
        mHomeFragment.showConnecting(!mQOEConnectionComplete);
        mAQReadingsFragment.showConnecting(!mQOEConnectionComplete);
        mGraphsFragment.showConnecting(!mQOEConnectionComplete);

        // Home fragment UI
        try {
            ArrayList<Float> listValues = new ArrayList<Float>();

            listValues.add(mUtils.roundToTwoDigits(15f));//mRespeckSensorReadings.get(RESPECK_BREATHING_RATE)));
            listValues.add(mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM2_5)));
            listValues.add(mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM10)));

            mHomeFragment.setReadings(listValues);
        }
        catch (Exception e) { e.printStackTrace(); }

        // Air Quality fragment UI
        try {
            HashMap<String, Float> values = new HashMap<String, Float>();

            values.put(Constants.QOE_TEMPERATURE, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_TEMPERATURE)));
            values.put(Constants.QOE_HUMIDITY, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_HUMIDITY)));
            values.put(Constants.QOE_O3, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_O3)));
            values.put(Constants.QOE_NO2, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_NO2)));
            values.put(Constants.QOE_PM1, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM1)));
            values.put(Constants.QOE_PM2_5, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM2_5)));
            values.put(Constants.QOE_PM10, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_PM10)));
            values.put(Constants.QOE_BINS_TOTAL, mUtils.roundToTwoDigits(mQOESensorReadings.get(Constants.QOE_BINS_TOTAL)));

            mAQReadingsFragment.setReadings(values);
        }
        catch (Exception e) { e.printStackTrace(); }

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
        }
        catch (Exception e) { e.printStackTrace(); }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Utils
        mUtils = Utils.getInstance(this);

        // Initialize fragments
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState != null) {
            mHomeFragment =
                    (HomeFragment) fm.getFragment(savedInstanceState, TAG_HOME_FRAGMENT);
            mAQReadingsFragment =
                    (AQReadingsFragment) fm.getFragment(savedInstanceState, TAG_AQREADINGS_FRAGMENT);
            mGraphsFragment =
                    (GraphsFragment) fm.getFragment(savedInstanceState, TAG_GRAPHS_FRAGMENT);
            mCurrentFragment = fm.getFragment(savedInstanceState, TAG_CURRENT_FRAGMENT);

            if (mHomeFragment == null) {
                mHomeFragment = new HomeFragment();
            }
            if (mAQReadingsFragment == null) {
                mAQReadingsFragment = new AQReadingsFragment();
            }
            if (mGraphsFragment == null) {
                mGraphsFragment = new GraphsFragment();
            }
        }
        else {
            mHomeFragment = new HomeFragment();
            mAQReadingsFragment = new AQReadingsFragment();
            mGraphsFragment = new GraphsFragment();
            mCurrentFragment = mHomeFragment;
        }

        // Choose layout
        if (mMenuModePref == 0) {
            setContentView(R.layout.activity_main_buttons);

            FragmentTransaction trans = fm.beginTransaction();

            if (mCurrentFragment instanceof HomeFragment) {
                trans.replace(R.id.content, mCurrentFragment, TAG_HOME_FRAGMENT);
            }
            else if (mCurrentFragment instanceof AQReadingsFragment) {
                trans.replace(R.id.content, mCurrentFragment, TAG_AQREADINGS_FRAGMENT);
            }
            else if (mCurrentFragment instanceof GraphsFragment) {
                trans.replace(R.id.content, mCurrentFragment, TAG_GRAPHS_FRAGMENT);
            }
            else {
                trans.replace(R.id.content, mHomeFragment, TAG_HOME_FRAGMENT);
                mCurrentFragment = mHomeFragment;
            }

            trans.commit();
        }
        else if (mMenuModePref == 1) {
            setContentView(R.layout.activity_main_tabs);

            // Create the adapter that will return a fragment for each of the three
            // primary sections of the activity.
            SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), getApplicationContext());
            sectionsPagerAdapter.addFragment(mHomeFragment);
            sectionsPagerAdapter.addFragment(mAQReadingsFragment);
            sectionsPagerAdapter.addFragment(mGraphsFragment);

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
                tabLayout.getTabAt(0).setIcon(Constants.menuIconsResId[0]);
                tabLayout.getTabAt(1).setIcon(Constants.menuIconsResId[2]);
                tabLayout.getTabAt(2).setIcon(Constants.menuIconsResId[3]);
            }
        }

        // Add the toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // Initialize Readings hash maps
        mRespeckSensorReadings = new HashMap<String, Float>();
        mQOESensorReadings = new HashMap<String, Float>();

        // Bluetooth initialization
        initBluetooth();

        // Initialize Upload services
        initRespeckUploadService();
        initQOEUploadService();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Bluetooth startup
        startBluetooth();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop Bluetooth scanning
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

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
        }
        else if (Objects.equals(mCurrentUser.getGender(), "F")) {
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
        }
        else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.SettingsFragment.class.getName());
            intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
            startActivity(intent);
        }
        else if (id == R.id.action_airspeck) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.airquality.sepa",
                        "com.airquality.sepa.DataCollectionActivity"));
                startActivity(intent);
            }
            catch (Exception e) {
                Toast.makeText(this, R.string.airspeck_not_found, Toast.LENGTH_LONG).show();
            }
        }
        else if (id == R.id.action_respeck) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.pulrehab",
                        "com.pulrehab.fragments.MainActivity"));
                startActivity(intent);
            }
            catch (Exception e) {
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
     * @param fragment Fragment New fragment.
     * @param tag String Tag for the new fragment.
     */
    public void replaceFragment(Fragment fragment, String tag) {
        replaceFragment(fragment, tag, false);
    }

    /**
     * Replace the current fragment with the given one.
     * @param fragment Fragment New fragment.
     * @param tag String Tag for the new fragment.
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
    // UPLOAD SERVICES -----------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private void initRespeckUploadService() {
        mRespeckRemoteUploadService = new RespeckRemoteUploadService();
        mRespeckRemoteUploadService.onCreate(this);
        Intent intent = new Intent(RespeckRemoteUploadService.MSG_CONFIG);

        JSONObject json = new JSONObject();
        try {
            /*json.put("patient_id", mUtils.getProperties().getProperty("PatientID"));
            json.put("respeck_uuid", mUtils.getProperties().getProperty("RESpeckUUID"));
            json.put("qoe_uuid", mUtils.getProperties().getProperty("QOEUUID"));
            json.put("airrespeck_key", mUtils.getProperties().getProperty("AirRESpeckKey"));
            json.put("tablet_serial", mUtils.getProperties().getProperty("TabletSerial"));
            json.put("app_version", mUtils.getAppVersionCode());*/

            json.put("patient_id", "test_id");
            json.put("respeck_uuid", "test_respeck_uuid");
            json.put("qoe_uuid", "test_qoe_uuid");
            json.put("airrespeck_key", "test_airrespeck_key");
            json.put("tablet_serial", "test_tablet_serial");
            json.put("app_version", mUtils.getAppVersionCode());
        }
        catch (JSONException e) {
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
            /*json.put("patient_id", mUtils.getProperties().getProperty("PatientID"));
            json.put("respeck_uuid", mUtils.getProperties().getProperty("RESpeckUUID"));
            json.put("qoe_uuid", mUtils.getProperties().getProperty("QOEUUID"));
            json.put("airrespeck_key", mUtils.getProperties().getProperty("AirRESpeckKey"));
            json.put("tablet_serial", mUtils.getProperties().getProperty("TabletSerial"));
            json.put("app_version", mUtils.getAppVersionCode());*/

            json.put("patient_id", "test_id");
            json.put("respeck_uuid", "test_respeck_uuid");
            json.put("qoe_uuid", "test_qoe_uuid");
            json.put("airrespeck_key", "test_airrespeck_key");
            json.put("tablet_serial", "test_tablet_serial");
            json.put("app_version", mUtils.getAppVersionCode());
        }
        catch (JSONException e) {
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
        }
        else {
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
     * @param start boolean If true start scanning, else stop scanning.
     */
    private void scanLeDevice(final boolean start) {
        if (start) {
            mLEScanner.startScan(mScanFilters, mScanSettings, mScanCallback);
        }
        else {
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
     * @param device BluetoothDevice The device.
     */
    public void connectToDevice(BluetoothDevice device) {
        /*if (mGattRespeck == null && device.getName().equals("Respeck_LNT18")) {
            Log.i("[Bluetooth]", "Connecting to " + device.getName());
            mDeviceRespeck = device;
            mGattRespeck = device.connectGatt(getApplicationContext(), true, mGattCallbackRespeck);
        }*/

        if (mGattQOE == null && device.getName().equals("QOE")) {
            Log.i("[Bluetooth]", "Connecting to " + device.getName());
            mDeviceQOE = device;
            mGattQOE = device.connectGatt(getApplicationContext(), true, mGattCallbackQOE);
        }

        if (mGattQOE != null /*&& mGattRespeck != null*/) {
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
                    Log.i("[QOE] - gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    mQOEConnectionComplete = false;
                    Log.e("[QOE] - gattCallback", "STATE_DISCONNECTED");
                    Log.i("[QOE] - gattCallback", "reconnecting...");
                    BluetoothDevice device = gatt.getDevice();
                    mGattQOE.close();
                    mGattQOE = null;
                    connectToDevice(device);
                    break;
                default:
                    Log.e("[QOE] - gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                Log.i("[QOE]", "onServicesDiscovered - " + services.toString());
                //gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));

                for (BluetoothGattService s : services) {
                    BluetoothGattCharacteristic characteristic = s.getCharacteristic(UUID.fromString(QOE_LIVE_CHARACTERISTIC));

                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(QOE_CLIENT_CHARACTERISTIC));
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
                if ((sampleIDs_2[0] == sampleIDs_2[1]) && lastSample != sampleIDs_2[0] && (sampleIDs_2[1] == sampleIDs_2[2] && (sampleIDs_2[2] == sampleIDs_2[3]) && (sampleIDs_2[3] == sampleIDs_2[4]))) {

                    byte[] finalPacket = {};
                    int size = 5;

                    finalPacket = new byte[lastPackets_2[0].length + lastPackets_2[1].length + lastPackets_2[2].length + lastPackets_2[3].length + lastPackets_2[4].length - 8];
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
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }

                    /*Intent intent = new Intent(QOERemoteUploadService.MSG_UPLOAD);
                    intent.putExtra(QOERemoteUploadService.MSG_UPLOAD_DATA, json.toString());
                    sendBroadcast(intent);*/
                    Log.d("[QOE]", "Sent LIVE JSON to upload service: " + json.toString());


                    // Update the UI
                    HashMap<String, Float> values = new HashMap<String, Float>();
                    values.put(Constants.QOE_PM1, pm1);
                    values.put(Constants.QOE_PM2_5, pm2_5);
                    values.put(Constants.QOE_PM10, pm10);
                    values.put(Constants.QOE_TEMPERATURE, (float)temperature);
                    values.put(Constants.QOE_HUMIDITY, (float)hum);
                    values.put(Constants.QOE_NO2, (float)no2_ae);
                    values.put(Constants.QOE_O3, (float)o3_ae);
                    values.put(Constants.QOE_BINS_0, (float)bin0);
                    values.put(Constants.QOE_BINS_1, (float)bin1);
                    values.put(Constants.QOE_BINS_2, (float)bin2);
                    values.put(Constants.QOE_BINS_3, (float)bin3);
                    values.put(Constants.QOE_BINS_4, (float)bin4);
                    values.put(Constants.QOE_BINS_5, (float)bin5);
                    values.put(Constants.QOE_BINS_6, (float)bin6);
                    values.put(Constants.QOE_BINS_7, (float)bin7);
                    values.put(Constants.QOE_BINS_8, (float)bin8);
                    values.put(Constants.QOE_BINS_9, (float)bin9);
                    values.put(Constants.QOE_BINS_10, (float)bin10);
                    values.put(Constants.QOE_BINS_11, (float)bin11);
                    values.put(Constants.QOE_BINS_12, (float)bin12);
                    values.put(Constants.QOE_BINS_13, (float)bin13);
                    values.put(Constants.QOE_BINS_14, (float)bin14);
                    values.put(Constants.QOE_BINS_15, (float)bin15);
                    values.put(Constants.QOE_BINS_TOTAL, (float)total);

                    Message msg = Message.obtain();
                    msg.obj = values;
                    msg.what = UPDATE_QOE_READINGS;
                    msg.setTarget(mUIHandler);
                    msg.sendToTarget();
                }
            }
        }
    };
}
