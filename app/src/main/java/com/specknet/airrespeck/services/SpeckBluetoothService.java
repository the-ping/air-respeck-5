package com.specknet.airrespeck.services;

import android.app.Service;
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
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.specknet.airrespeck.models.RESpeckStoredSample;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.LocationUtils;
import com.specknet.airrespeck.utils.Utils;

import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Service for connecting to RESpeck and Airspeck sensors, converting the data into a readable format and
 * sending the result to the interested Activities.
 */

public class SpeckBluetoothService extends Service {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings mScanSettings;
    private List<ScanFilter> mScanFilters;
    private BluetoothGatt mGattRespeck, mGattQOE;

    // Config settings
    private boolean mIsAirspeckEnabled;
    private boolean mIsStoreDataLocally;
    private boolean mIsStoreMergedFile;
    private boolean mIsStoreAllAirspeckFields;

    // Outputstreams for all the files
    private OutputStreamWriter mRespeckWriter;
    private OutputStreamWriter mAirspeckWriter;
    private OutputStreamWriter mMergedWriter;
    private OutputStreamWriter mActivitySummaryWriter;
    // Initial values for last write timestamps. Have to be > 0 so the truncating works.
    private Date mDateOfLastRESpeckWrite = new Date(0);
    private Date mDateOfLastAirspeckWrite = new Date(0);

    // Most recent Airspeck data, used for storing merged file
    private String mMostRecentAirspeckData;

    private static String RESPECK_UUID;
    private static String QOE_UUID;
    private static final String QOE_CLIENT_CHARACTERISTIC = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String QOE_LIVE_CHARACTERISTIC = "00002002-e117-4bff-b00d-b20878bc3f44";

    private final static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private final static String RESPECK_LIVE_CHARACTERISTIC = "00002010-0000-1000-8000-00805f9b34fb";
    private final static String RESPECK_BREATHING_RATES_CHARACTERISTIC = "00002016-0000-1000-8000-00805f9b34fb";
    private final static String RESPECK_BREATH_INTERVALS_CHARACTERISTIC = "00002015-0000-1000-8000-00805f9b34fb";

    // QOE CODE
    private static byte[][] lastPackets_1 = new byte[4][];
    private static byte[][] lastPackets_2 = new byte[5][];
    private static int[] sampleIDs_1 = {0, 0, 0, 0};
    private static int[] sampleIDs_2 = {0, 1, 2, 3, 4};
    int lastSample = 0;

    // RESPECK CODE
    private int latestPacketSequenceNumber = -1;
    private long currentPhoneTimestamp = -1;
    private long mRESpeckTimestampCurrentPacketReceived = -1;
    private Queue<RESpeckStoredSample> storedQueue;

    private long phoneTimestampPreviousPacketReceived = -1;
    private long phoneTimestampCurrentPacketReceived = -1;

    private long latestProcessedMinute = 0L;
    private int currentSequenceNumberInBatch = -1;
    private long breathAveragePhoneTimestamp = -1;
    private long breathAverageRESpeckTimestamp = -1;
    private int breathAverageSequenceNumber = -1;
    private int latestStoredRespeckSeq = -1;

    // BATTERY MONITORING

    private static final int PROMPT_TO_CHARGE_LEVEL = 1152;
    private static final int BATTERY_FULL_LEVEL = 1152; // was 1139
    private static final int BATTERY_EMPTY_LEVEL = 889;
    private final static String RESPECK_BATTERY_LEVEL_CHARACTERISTIC = "00002017-0000-1000-8000-00805f9b34fb";
    private float mLatestBatteryPercent = 0f;
    private boolean mLatestRequestCharge = false;

    // References to Context and Utils
    private Utils mUtils;
    private LocationUtils mLocationUtils;

    public SpeckBluetoothService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        /*
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getText(R.string.service_notification_title))
                .setContentText(getText(R.string.service_notification_message))
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.service_notification_message))
                .build();

        startForeground(1, notification);
        */
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        new Thread() {
            @Override
            public void run() {
                Log.i("SpeckService", "Starting SpeckService...");
                initSpeckService();
                startServiceAndBluetoothScanning();
            }
        }.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopBluetoothScanning();
        stopSpeckService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // We don't allow threads to bind to this service. Once the service is started, it sends updates
        // via broadcasts and there is no need for calls from outside
        return null;
    }

    /**
     * Initiate Bluetooth adapter.
     */
    public void initSpeckService() {

        // Get references to Utils
        mUtils = Utils.getInstance(getApplicationContext());

        // Look whether Airspeck is enabled in config
        mIsAirspeckEnabled = Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_AIRSPECK_ENABLED));

        // Do we want all Airspeck fields, or only the reliable data, i.e. only bin0, temperature, humidity etc.?
        mIsStoreAllAirspeckFields = Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_STORE_ALL_AIRSPECK_FIELDS));

        // Do we store data locally?
        mIsStoreDataLocally = Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_STORE_DATA_LOCALLY));

        // Do we store a merged Airspeck-RESpeck file in addition to the individual files? Only
        // works if Airspeck is enabled
        mIsStoreMergedFile = (Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_STORE_MERGED_FILE)) && mIsAirspeckEnabled);

        // Get reference to LocationUtils
        mLocationUtils = LocationUtils.getInstance(getApplicationContext());
        mLocationUtils.startLocationManager();

        // Initialise stored queue
        storedQueue = new LinkedList<>();

        //Initialize Breathing Functions
        initBreathing();

        // Get Bluetooth address
        QOE_UUID = mUtils.getProperties().getProperty(Constants.Config.QOEUUID);
        RESPECK_UUID = mUtils.getProperties().getProperty(Constants.Config.RESPECK_UUID);

        // Initializes a Bluetooth adapter. For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(
                Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Initialise ActivitySummaryWriter. The other writers are initialised on demand
        try {
            mActivitySummaryWriter = new OutputStreamWriter(
                    new FileOutputStream(Constants.ACTIVITY_SUMMARY_FILE_PATH, true));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Set most recent Airspeck data to be empty
        if (mIsStoreAllAirspeckFields) {
            mMostRecentAirspeckData = ",,,,,,,,,,,,,,,,,,,,,,,,,,,";
        } else {
            mMostRecentAirspeckData = ",,,,,,,,";
        }
    }

    /**
     * Check Bluetooth availability and initiate devices scanning.
     */
    public void startServiceAndBluetoothScanning() {
        // Check if Bluetooth is supported on the device
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // For API level 21 and above
        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mScanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        // Add the devices's addresses that we need for scanning
        mScanFilters = new ArrayList<>();
        mScanFilters.add(new ScanFilter.Builder().setDeviceAddress(RESPECK_UUID).build());
        if (mIsAirspeckEnabled) {
            if (QOE_UUID != null && !QOE_UUID.equals("")) {
                mScanFilters.add(new ScanFilter.Builder().setDeviceAddress(QOE_UUID).build());
            } else {
                Log.e("Bluetooth", "Airspeck enabled but QOE_UUID not set in config");
            }
        }

        // Start Bluetooth scanning
        BluetoothManager mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(
                Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            Log.i("Bluetooth", "Bluetooth scanning started");
            scanLeDevice(true);
        }

        // Start task which makes activity predictions every 2 seconds
        startActivityClassificationTask();
    }

    public void stopBluetoothScanning() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    public void stopSpeckService() {
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

        // Close the OutputWritingStreams
        try {
            if (mRespeckWriter != null) {
                mRespeckWriter.close();
            }
            if (mAirspeckWriter != null) {
                mAirspeckWriter.close();
            }
            if (mMergedWriter != null) {
                mMergedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
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

            if (btDevice != null && btDevice.getName() != null) {
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
        if (mGattRespeck == null && device.getAddress().equals(RESPECK_UUID)) {
            Log.i("[Bluetooth]", "Connecting to " + device.getName() + " Address: " + device.getAddress());
            mGattRespeck = device.connectGatt(getApplicationContext(), true, mGattCallbackRespeck);
            // Log.i("[Bluetooth]", "Connection status: " + mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT));
        }

        if (mIsAirspeckEnabled) {
            if (mGattQOE == null && device.getName().contains("QOE")) {
                Log.i("[Bluetooth]", "Connecting to " + device.getName());
                mGattQOE = device.connectGatt(getApplicationContext(), true, mGattCallbackQOE);
            }
        }

        if (mGattRespeck != null && (!mIsAirspeckEnabled || mGattQOE != null)) {
            Log.i("[Bluetooth]", "Device(s) connected. Scanner turned off.");
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
                    Log.i("QOE-gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();

                    Intent intentConnected = new Intent(Constants.ACTION_AIRSPECK_CONNECTED);
                    sendBroadcast(intentConnected);

                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("QOE-gattCallback", "STATE_DISCONNECTED");
                    Log.i("QOE-gattCallback", "reconnecting...");
                    BluetoothDevice device = gatt.getDevice();
                    mGattQOE.close();
                    mGattQOE = null;
                    connectToDevice(device);

                    Intent intentDisconnected = new Intent(Constants.ACTION_AIRSPECK_DISCONNECTED);
                    sendBroadcast(intentDisconnected);

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

                    byte[] finalPacket;
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

                    // MtoF
                    packetBufferLittleEnd.getInt();
                    // opc_temp
                    float opctemp = packetBufferLittleEnd.getInt();
                    // opc_pressure
                    packetBufferLittleEnd.getInt();
                    // period count
                    packetBufferLittleEnd.getInt();
                    // uint16_t checksum ????
                    packetBufferLittleEnd.getShort();

                    float pm1 = packetBufferLittleEnd.getFloat();
                    float pm2_5 = packetBufferLittleEnd.getFloat();
                    float pm10 = packetBufferLittleEnd.getFloat();

                    int o3_ae = packetBufferBigEnd.getShort(62);
                    int o3_we = packetBufferBigEnd.getShort(64);

                    int no2_ae = packetBufferBigEnd.getShort(66);
                    int no2_we = packetBufferBigEnd.getShort(68);

                    /* uint16_t temperature */
                    int unconvertedTemperature = packetBufferBigEnd.getShort(70) & 0xffff;

                    /* uint16_t humidity */
                    int unconvertedHumidity = packetBufferBigEnd.getShort(72);

                    double temperature = ((unconvertedTemperature - 3960) / 100.0);
                    double humidity = (-2.0468 + (0.0367 * unconvertedHumidity) +
                            (-0.0000015955 * unconvertedHumidity * unconvertedHumidity));

                    /*
                    Log.i("[QOE]", "PM1: " + pm1);
                    Log.i("[QOE]", "PM2.5: " + pm2_5);
                    Log.i("[QOE]", "PM10: " + pm10);
                    */

                    lastSample = sampleIDs_2[0];

                    // Get timestamp
                    long currentPhoneTimestamp = mUtils.getUnixTimestamp();

                    // Get location
                    double latitude = -1;
                    double longitude = -1;
                    double altitude = -1;
                    try {
                        latitude = mLocationUtils.getLatitude();
                        longitude = mLocationUtils.getLongitude();
                        altitude = mLocationUtils.getAltitude();
                    } catch (Exception e) {
                        Log.i("[QOE]", "Location permissions not granted or GPS turned off. Store empty values.");
                    }

                    //Log.i("[GPS]",
                    //        String.format("Gps signal: lat %.4f, long %.4f, alt %.4f", latitude, longitude, altitude));

                    // Send data in broadcast
                    HashMap<String, Float> readings = new HashMap<>();
                    readings.put(Constants.QOE_PM1, pm1);
                    readings.put(Constants.QOE_PM2_5, pm2_5);
                    readings.put(Constants.QOE_PM10, pm10);
                    readings.put(Constants.QOE_TEMPERATURE, (float) temperature);
                    readings.put(Constants.QOE_HUMIDITY, (float) humidity);
                    readings.put(Constants.QOE_NO2, (float) no2_ae);
                    readings.put(Constants.QOE_O3, (float) o3_ae);
                    readings.put(Constants.QOE_BINS_0, (float) bin0);
                    readings.put(Constants.QOE_BINS_1, (float) bin1);
                    readings.put(Constants.QOE_BINS_2, (float) bin2);
                    readings.put(Constants.QOE_BINS_3, (float) bin3);
                    readings.put(Constants.QOE_BINS_4, (float) bin4);
                    readings.put(Constants.QOE_BINS_5, (float) bin5);
                    readings.put(Constants.QOE_BINS_6, (float) bin6);
                    readings.put(Constants.QOE_BINS_7, (float) bin7);
                    readings.put(Constants.QOE_BINS_8, (float) bin8);
                    readings.put(Constants.QOE_BINS_9, (float) bin9);
                    readings.put(Constants.QOE_BINS_10, (float) bin10);
                    readings.put(Constants.QOE_BINS_11, (float) bin11);
                    readings.put(Constants.QOE_BINS_12, (float) bin12);
                    readings.put(Constants.QOE_BINS_13, (float) bin13);
                    readings.put(Constants.QOE_BINS_14, (float) bin14);
                    readings.put(Constants.QOE_BINS_15, (float) bin15);
                    readings.put(Constants.QOE_BINS_TOTAL, (float) total);

                    Intent intentData = new Intent(Constants.ACTION_AIRSPECK_LIVE_BROADCAST);
                    intentData.putExtra(Constants.QOE_TIMESTAMP, currentPhoneTimestamp);
                    intentData.putExtra(Constants.AIRSPECK_ALL_MEASURES, readings);
                    sendBroadcast(intentData);

                    // Store the important data in the external storage if set in config
                    if (mIsStoreDataLocally) {
                        String storedLine;

                        String location;
                        if (longitude == -1) {
                            location = ",,";
                        } else {
                            location = longitude + "," + latitude + "," + altitude;
                        }
                        Log.i("[GPS]", "Gps signal: " + location);

                        if (mIsStoreAllAirspeckFields) {
                            storedLine = currentPhoneTimestamp + "," + pm1 + "," + pm2_5 + "," + pm10 + "," +
                                    temperature + "," + humidity + "," + no2_ae +
                                    "," + o3_ae + "," + bin0 + "," + bin1 + "," + bin2 + "," + bin3 + "," + bin4 +
                                    "," + bin5 + "," + bin6 + "," + bin7 + "," + bin8 + "," + bin9 + "," + bin10 +
                                    "," + bin11 + "," + bin12 + "," + bin13 + "," + bin14 + "," + bin15 + "," + total +
                                    "," + location;
                        } else {
                            storedLine = currentPhoneTimestamp + "," + temperature + "," + humidity + "," + no2_ae +
                                    "," + o3_ae + "," + bin0 + "," + location;
                        }
                        writeToAirspeckFile(storedLine);

                        // If we want to store a merged file, store the most recent Airspeck data without
                        // timestamp as a string
                        if (mIsStoreMergedFile) {
                            mMostRecentAirspeckData = storedLine;
                        }
                    }
                }
            }
        }
    };

    private final BluetoothGattCallback mGattCallbackRespeck = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("Respeck-gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();

                    Intent intentConnected = new Intent(Constants.ACTION_RESPECK_CONNECTED);
                    intentConnected.putExtra(Constants.RESPECK_UUID, gatt.getDevice().getAddress());
                    sendBroadcast(intentConnected);

                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("Respeck-gattCallback", "STATE_DISCONNECTED");
                    Log.i("Respeck-gattCallback", "reconnecting...");
                    BluetoothDevice device = gatt.getDevice();
                    mGattRespeck.close();
                    mGattRespeck = null;
                    connectToDevice(device);

                    Intent intentDisconnected = new Intent(Constants.ACTION_RESPECK_DISCONNECTED);
                    sendBroadcast(intentDisconnected);

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
                        UUID.fromString(RESPECK_BATTERY_LEVEL_CHARACTERISTIC));

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
                Log.i("Respeck", "RESPECK_BREATHING_RATES_CHARACTERISTIC");
            } else if (descriptor.getCharacteristic().getUuid().equals(
                    UUID.fromString(RESPECK_BATTERY_LEVEL_CHARACTERISTIC))) {
                Log.i("Respeck", "RESPECK_BATTERY_LEVEL_CHARACTERISTIC");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(UUID.fromString(RESPECK_LIVE_CHARACTERISTIC))) {
                final byte[] accelBytes = characteristic.getValue();
                final int packetSequenceNumber = accelBytes[0] & 0xFF;

                //Check if the reading is not repeated
                if (packetSequenceNumber == latestPacketSequenceNumber) {
                    Log.e("RAT", "DUPLICATE SEQUENCE NUMBER: " + Integer.toString(packetSequenceNumber));
                    return;
                } else {
                    latestPacketSequenceNumber = packetSequenceNumber;
                }

                for (int i = 1; i < accelBytes.length; i += 7) {
                    Byte startByte = accelBytes[i];
                    if (startByte == -1) {
                        // Timestamp packet received. Starting a new sequence
                        // Log.i("DF", "Timestamp received from RESpeck");

                        // Read timestamp from packet
                        Byte ts_1 = accelBytes[i + 1];
                        Byte ts_2 = accelBytes[i + 2];
                        Byte ts_3 = accelBytes[i + 3];
                        Byte ts_4 = accelBytes[i + 4];

                        Long respeckTimestamp = combineTimestampBytes(ts_1, ts_2, ts_3,
                                ts_4) * 197 / 32768;
                        if (respeckTimestamp == mRESpeckTimestampCurrentPacketReceived) {
                            Log.e("RAT", "DUPLICATE LIVE TIMESTAMP RECEIVED");
                            return;
                        }
                        mRESpeckTimestampCurrentPacketReceived = respeckTimestamp;

                        // Independent of the RESpeck timestamp, we use the phone timestamp
                        currentPhoneTimestamp = mUtils.getUnixTimestamp();

                        if (phoneTimestampPreviousPacketReceived == -1) {
                            // If this is our first sequence, we use the typical time difference between the
                            // RESpeck packets for determining the previous timestamp
                            phoneTimestampPreviousPacketReceived = currentPhoneTimestamp -
                                    Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_PACKETS;
                        } else {
                            phoneTimestampPreviousPacketReceived = phoneTimestampCurrentPacketReceived;
                        }

                        phoneTimestampCurrentPacketReceived = currentPhoneTimestamp;

                        currentSequenceNumberInBatch = 0;

                        // process any queued stored data. TODO:test this if the stored mode is activated again in the
                        // RESpeck.
                        while (!storedQueue.isEmpty()) {
                            // TODO: this doesn't seem to be reached? Is the queue only for the case when
                            // the RESpeck was disconnected from the phone? Comment above if applicable!
                            RESpeckStoredSample s = storedQueue.remove();

                            Long currentTimeOffset = currentPhoneTimestamp / 1000 -
                                    mRESpeckTimestampCurrentPacketReceived;

                            Log.i("stored", "EXTRA_RESPECK_TIMESTAMP_OFFSET_SECS: " + currentTimeOffset);
                            Log.i("stored", "EXTRA_RESPECK_RS_TIMESTAMP: " + s.getRESpeckTimestamp());
                            Log.i("stored", "EXTRA_RESPECK_SEQ: " + s.getSequenceNumber());
                            Log.i("stored", "EXTRA_RESPECK_LIVE_AVE_BR: " + s.getAverageBreathingRate());
                            Log.i("stored", "EXTRA_RESPECK_LIVE_N_BR: " + s.getNumberOfBreaths());
                            Log.i("stored", "EXTRA_RESPECK_LIVE_SD_BR: " + s.getStdBreathingRate());

                            // Send stored data broadcast
                            Intent liveDataIntent = new Intent(Constants.ACTION_RESPECK_AVG_STORED_BROADCAST);
                            liveDataIntent.putExtra(Constants.RESPECK_STORED_TIMESTAMP_OFFSET, currentTimeOffset);
                            liveDataIntent.putExtra(Constants.RESPECK_STORED_SENSOR_TIMESTAMP, s.getRESpeckTimestamp());
                            liveDataIntent.putExtra(Constants.RESPECK_MINUTE_AVG_BREATHING_RATE,
                                    s.getAverageBreathingRate());
                            liveDataIntent.putExtra(Constants.RESPECK_MINUTE_STD_BREATHING_RATE,
                                    s.getStdBreathingRate());
                            liveDataIntent.putExtra(Constants.RESPECK_MINUTE_NUMBER_OF_BREATHS, s.getNumberOfBreaths());
                            sendBroadcast(liveDataIntent);
                        }

                    } else if (startByte == -2 && currentSequenceNumberInBatch >= 0) { //OxFE - accel packet
                        //Log.v("DF", "Acceleration packet received from RESpeck");
                        // If the currentSequenceNumberInBatch is -1, this means we have received acceleration
                        // packets before the first timestamp
                        try {
                            final float x = combineAccelerationBytes(accelBytes[i + 1], accelBytes[i + 2]);
                            final float y = combineAccelerationBytes(accelBytes[i + 3], accelBytes[i + 4]);
                            final float z = combineAccelerationBytes(accelBytes[i + 5], accelBytes[i + 6]);

                            updateBreathing(x, y, z);

                            final float breathingRate = getBreathingRate();
                            final float breathingSignal = getBreathingSignal();
                            final float breathingAngle = getBreathingAngle();
                            final float activityLevel = getActivityLevel();
                            final int activityType = getCurrentActivityClassification();

                            // Calculate interpolated timestamp of current sample based on sequence number
                            // There are 32 samples in each acceleration batch the RESpeck sends.
                            long interpolatedPhoneTimestampOfCurrentSample = (long)
                                    ((phoneTimestampCurrentPacketReceived - phoneTimestampPreviousPacketReceived) *
                                            (currentSequenceNumberInBatch * 1. /
                                                    Constants.NUMBER_OF_SAMPLES_PER_BATCH)) +
                                    phoneTimestampPreviousPacketReceived;
                            /*
                            Log.i("2", "BS_TIMESTAMP " + String.valueOf(currentPhoneTimestamp));
                            Log.i("2", "RS_TIMESTAMP " + String.valueOf(mRESpeckTimestampCurrentPacketReceived));
                            Log.i("2", "Interpolated timestamp " + String.valueOf(
                                    cutoffInterpolatedTimestamp));
                            Log.i("2", "EXTRA_RESPECK_SEQ " + String.valueOf(currentSequenceNumberInBatch));
                            Log.i("2", "Breathing Rate " + String.valueOf(breathingRate));
                            Log.i("2", "Breathing Signal " + String.valueOf(breathingSignal));
                            Log.i("2", "Breathing Angle " + String.valueOf(breathingAngle));
                            Log.i("2", "BRA " + String.valueOf(averageBreathingRate));
                            Log.i("2", "STDBR " + String.valueOf(stdDevBreathingRate));
                            Log.i("2", "NBreaths " + String.valueOf(numberOfBreaths));
                            Log.i("2", "Activity Level " + String.valueOf(activityLevel));
                            */

                            // Store the important data in the external storage if set in config
                            if (mIsStoreDataLocally) {
                                String storedLine = interpolatedPhoneTimestampOfCurrentSample + "," +
                                        mRESpeckTimestampCurrentPacketReceived + "." + currentSequenceNumberInBatch +
                                        "," + x + "," + y + "," + z + "," + breathingSignal +
                                        "," + breathingRate + "," + activityLevel + "," + activityType;
                                writeToRESpeckAndMergedFile(storedLine);
                            }

                            // Send live broadcast intent
                            Intent liveDataIntent = new Intent(Constants.ACTION_RESPECK_LIVE_BROADCAST);
                            liveDataIntent.putExtra(Constants.RESPECK_INTERPOLATED_PHONE_TIMESTAMP,
                                    interpolatedPhoneTimestampOfCurrentSample);
                            liveDataIntent.putExtra(Constants.RESPECK_SENSOR_TIMESTAMP,
                                    mRESpeckTimestampCurrentPacketReceived);
                            liveDataIntent.putExtra(Constants.RESPECK_X, x);
                            liveDataIntent.putExtra(Constants.RESPECK_Y, y);
                            liveDataIntent.putExtra(Constants.RESPECK_Z, z);
                            liveDataIntent.putExtra(Constants.RESPECK_BREATHING_SIGNAL, breathingSignal);
                            liveDataIntent.putExtra(Constants.RESPECK_BREATHING_RATE, breathingRate);
                            liveDataIntent.putExtra(Constants.RESPECK_BREATHING_ANGLE, breathingAngle);
                            liveDataIntent.putExtra(Constants.RESPECK_SEQUENCE_NUMBER, currentSequenceNumberInBatch);
                            liveDataIntent.putExtra(Constants.RESPECK_ACTIVITY_LEVEL, activityLevel);
                            liveDataIntent.putExtra(Constants.RESPECK_ACTIVITY_TYPE, activityType);
                            liveDataIntent.putExtra(Constants.RESPECK_BATTERY_PERCENT, mLatestBatteryPercent);
                            liveDataIntent.putExtra(Constants.RESPECK_REQUEST_CHARGE, mLatestRequestCharge);
                            sendBroadcast(liveDataIntent);

                            // Every full minute, calculate the average breathing rate in that minute. This value will
                            // only change after a call to "calculateMedianAverageBreathing".
                            long currentProcessedMinute = DateUtils.truncate(new Date(currentPhoneTimestamp),
                                    Calendar.MINUTE).getTime();
                            if (currentProcessedMinute != latestProcessedMinute) {
                                calculateMedianAverageBreathing();

                                final float averageBreathingRate = getAverageBreathingRate();
                                final float stdDevBreathingRate = getStdDevBreathingRate();
                                final int numberOfBreaths = getNumberOfBreaths();

                                // Empty the minute average window
                                resetMedianAverageBreathing();

                                // Send average broadcast intent
                                Intent avgDataIntent = new Intent(Constants.ACTION_RESPECK_AVG_BROADCAST);
                                // The averaged data is not attached to a particular sensor record, so we only
                                // store the interpolated phone timestamp
                                avgDataIntent.putExtra(Constants.RESPECK_TIMESTAMP_MINUTE_AVG,
                                        currentProcessedMinute);
                                avgDataIntent.putExtra(Constants.RESPECK_MINUTE_AVG_BREATHING_RATE,
                                        averageBreathingRate);
                                avgDataIntent.putExtra(Constants.RESPECK_MINUTE_STD_BREATHING_RATE,
                                        stdDevBreathingRate);
                                avgDataIntent.putExtra(Constants.RESPECK_MINUTE_NUMBER_OF_BREATHS, numberOfBreaths);
                                sendBroadcast(avgDataIntent);

                                /*
                                Log.i("3", "RESPECK_BS_TIMESTAMP " + ts_minute);
                                Log.i("3", "EXTRA_RESPECK_LIVE_AVE_BR: " + updatedAverageBreathingRate);
                                Log.i("3", "EXTRA_RESPECK_LIVE_N_BR: " + updatedNumberOfBreaths);
                                Log.i("3", "EXTRA_RESPECK_LIVE_SD_BR: " + updatedStdDevBreathingRate);
                                Log.i("3", "EXTRA_RESPECK_LIVE_ACTIVITY " + activityLevel);
                                */

                                latestProcessedMinute = currentProcessedMinute;
                            }

                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }

                        // Increase sequence number for next incoming sample
                        currentSequenceNumberInBatch += 1;
                    }
                }
            } else if (characteristic.getUuid().equals(UUID.fromString(RESPECK_BATTERY_LEVEL_CHARACTERISTIC))) {
                // Battery packet received which contains the charging level of the battery
                final byte[] batteryLevelBytes = characteristic.getValue();
                int battLevel = combineBattBytes(batteryLevelBytes[0], batteryLevelBytes[1]);

                // Log.i("RAT", "BATTERY LEVEL notification received: " + Integer.toString(battLevel));

                int chargePercentage = (100 * (battLevel - BATTERY_EMPTY_LEVEL) /
                        (BATTERY_FULL_LEVEL - BATTERY_EMPTY_LEVEL));

                if (chargePercentage < 1) {
                    chargePercentage = 1;
                } else if (chargePercentage > 100) {
                    chargePercentage = 100;
                }

                mLatestBatteryPercent = (float) chargePercentage;
                mLatestRequestCharge = battLevel <= PROMPT_TO_CHARGE_LEVEL;

                // Log.i("RAT", "Battery level: " + Float.toString(
                //         mLatestBatteryPercent) + ", request charge: " + Float.toString(mLatestRequestCharge));

            } else if (characteristic.getUuid().equals(UUID.fromString(RESPECK_BREATHING_RATES_CHARACTERISTIC))) {
                // Breathing rates packet received. This only happens when the RESpeck was disconnected and
                // therefore only stored the minute averages
                final byte[] breathAveragesBytes = characteristic.getValue();

                final int sequenceNumber = breathAveragesBytes[0] & 0xFF;

                // Duplicate sent?
                if (sequenceNumber == latestStoredRespeckSeq) {
                    return;
                } else {
                    latestStoredRespeckSeq = sequenceNumber;
                }

                for (int i = 1; i < breathAveragesBytes.length; i += 6) {
                    Byte startByte = breathAveragesBytes[i];

                    if (startByte == -1) {
                        // timestamp
                        Byte ts_1 = breathAveragesBytes[i + 1];
                        Byte ts_2 = breathAveragesBytes[i + 2];
                        Byte ts_3 = breathAveragesBytes[i + 3];
                        Byte ts_4 = breathAveragesBytes[i + 4];
                        breathAveragePhoneTimestamp = System.currentTimeMillis();
                        breathAverageRESpeckTimestamp = combineTimestampBytes(ts_1, ts_2, ts_3, ts_4) * 197 / 32768;
                        breathAverageSequenceNumber = 0;
                    } else if (startByte == -2) {
                        // breath average
                        int numberOfBreaths = breathAveragesBytes[i + 1] & 0xFF;
                        if (numberOfBreaths > 5) {
                            float meanBreathingRate = (float) (breathAveragesBytes[i + 2] & 0xFF) / 5.0f;
                            float sdBreathingRate = (float) Math.sqrt((float) (breathAveragesBytes[i + 3] & 0xFF) /
                                    10.0f);

                            Byte upperActivityLevel = breathAveragesBytes[i + 4];
                            Byte lowerActivityLevel = breathAveragesBytes[i + 5];
                            float combinedActivityLevel = combineActBytes(upperActivityLevel, lowerActivityLevel);

                            RESpeckStoredSample ras = new RESpeckStoredSample(breathAveragePhoneTimestamp,
                                    breathAverageRESpeckTimestamp,
                                    breathAverageSequenceNumber++, numberOfBreaths, meanBreathingRate,
                                    sdBreathingRate,
                                    combinedActivityLevel);
                            // Log.w("RAT", "Queueing stored sample: " + ras.toString());
                            storedQueue.add(ras);
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
        int value = ((unsigned_upper << 24) | (unsigned_upper_middle << 16) | (unsigned_lower_middle << 8) | unsigned_lower);
        return value & 0xffffffffL;
    }

    private float combineAccelerationBytes(Byte upper, Byte lower) {
        short unsigned_lower = (short) (lower & 0xFF);
        short value = (short) ((upper << 8) | unsigned_lower);
        return (value) / 16384.0f;
    }

    private float combineActBytes(Byte upper, Byte lower) {
        short unsignedLower = (short) (lower & 0xFF);
        short unsignedUpper = (short) (upper & 0xFF);
        short value = (short) ((unsignedUpper << 8) | unsignedLower);
        return (value) / 1000.0f;
    }

    private int combineBattBytes(Byte upper, Byte lower) {
        short unsignedLower = (short) (lower & 0xFF);
        short unsignedUpper = (short) (upper & 0xFF);
        if (unsignedUpper > 0xF0) {
            unsignedUpper = (short) ((0xFF - unsignedUpper) + 1);
        }
        return (int) (short) ((unsignedUpper << 8) | unsignedLower);
    }

    private void startActivityClassificationTask() {
        // We want to summarise predictions every 10 minutes.
        final int SUMMARY_COUNT_MAX = (int) (10 * 60 / 2.);

        // How often do we update the activity classification?
        // half the window size for the activity predictions, in milliseconds
        final int delay = 2000;

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            // We summarise the currently stored predictions when the counter reaches max
            int summaryCounter = 0;
            int temporaryStoragePredictions[] = new int[Constants.NUMBER_OF_ACTIVITY_TYPES];

            @Override
            public void run() {
                updateActivityClassification();
                int predictionIdx = getCurrentActivityClassification();
                // an index of -1 means that the acceleration buffer has not been filled yet, so we have to wait.
                if (predictionIdx != -1) {
                    // Log.i("DF", String.format("Prediction: %s", Constants.ACT_CLASS_NAMES[predictionIdx]));
                    temporaryStoragePredictions[predictionIdx]++;
                    summaryCounter++;
                }

                // If the temporary prediction recording is full, i.e. count is MAX, we
                // write the summarised data into another external file and empty the temporary file.
                if (summaryCounter == SUMMARY_COUNT_MAX) {

                    String lineToWrite = getCurrentTimeStamp() + "," +
                            Math.round(temporaryStoragePredictions[0] * 100. / SUMMARY_COUNT_MAX) + "," +
                            Math.round(temporaryStoragePredictions[1] * 100. / SUMMARY_COUNT_MAX) + "," +
                            Math.round(temporaryStoragePredictions[2] * 100. / SUMMARY_COUNT_MAX) + "\n";
                    try {
                        mActivitySummaryWriter.append(lineToWrite);
                    } catch (IOException e) {
                        Log.e("DF", "Activity summary file write failed: " + e.toString());
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

    private void writeToRESpeckAndMergedFile(String line) {
        // Check whether we are in a new day
        Date now = new Date();
        long currentWriteDay = DateUtils.truncate(now, Calendar.DAY_OF_MONTH).getTime();
        long previousWriteDay = DateUtils.truncate(mDateOfLastRESpeckWrite, Calendar.DAY_OF_MONTH).getTime();
        long numberOfMillisInDay = 1000 * 60 * 60 * 24;

        String filenameRESpeck = Constants.RESPECK_DATA_DIRECTORY_PATH +
                new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(now) +
                " RESpeck.csv";

        String filenameMerged = Constants.MERGED_DATA_DIRECTORY_PATH +
                new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(now) +
                " Merged.csv";

        // If we are in a new day, create a new file if necessary
        if (currentWriteDay != previousWriteDay ||
                now.getTime() - mDateOfLastRESpeckWrite.getTime() > numberOfMillisInDay) {
            try {
                /*
                 * RESpeck writer
                 */
                // Close old connection if there was one
                if (mRespeckWriter != null) {
                    mRespeckWriter.close();
                }

                // The file could already exist if we just started the app. If not, add the header
                if (!new File(filenameRESpeck).exists()) {
                    Log.i("DF", "RESpeck data file created with header");
                    // Open new connection to file (which creates file)
                    mRespeckWriter = new OutputStreamWriter(
                            new FileOutputStream(filenameRESpeck, true));

                    mRespeckWriter.append(Constants.RESPECK_DATA_HEADER).append("\n");
                } else {
                    mRespeckWriter = new OutputStreamWriter(
                            new FileOutputStream(filenameRESpeck, true));
                }

                /*
                 * Merged writer
                 */
                if (mIsStoreMergedFile) {
                    // Close old connection if there was one
                    if (mMergedWriter != null) {
                        mMergedWriter.close();
                    }

                    // The file could already exist if we just started the app. If not, add the header
                    if (!new File(filenameMerged).exists()) {
                        Log.i("DF", "Merged data file created with header");
                        // Open new connection to new file
                        mMergedWriter = new OutputStreamWriter(
                                new FileOutputStream(filenameMerged, true));
                        if (mIsStoreAllAirspeckFields) {
                            mMergedWriter.append(Constants.MERGED_DATA_HEADER_ALL).append("\n");
                        } else {
                            mMergedWriter.append(Constants.MERGED_DATA_HEADER_SUBSET).append("\n");
                        }
                    } else {
                        // Open new connection to new file
                        mMergedWriter = new OutputStreamWriter(
                                new FileOutputStream(filenameMerged, true));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mDateOfLastRESpeckWrite = now;

        try {
            // Write new line to file
            mRespeckWriter.append(line).append("\n");

            // If we want to store a merged file of Airspeck and RESpeck data, append the most
            // recent Airspeck data to the current RESpeck data
            if (mIsStoreMergedFile) {
                mMergedWriter.append(line).append(",").append(mMostRecentAirspeckData).append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeToAirspeckFile(String line) {
        // Check whether we are in a new day
        Date now = new Date();
        long currentWriteDay = DateUtils.truncate(now, Calendar.DAY_OF_MONTH).getTime();
        long previousWriteDay = DateUtils.truncate(mDateOfLastAirspeckWrite, Calendar.DAY_OF_MONTH).getTime();
        long numberOfMillisInDay = 1000 * 60 * 60 * 24;

        String filenameAirspeck = Constants.AIRSPECK_DATA_DIRECTORY_PATH +
                new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(now) +
                " Airspeck.csv";

        // If we are in a new day, create a new file if necessary
        if (currentWriteDay != previousWriteDay ||
                now.getTime() - mDateOfLastAirspeckWrite.getTime() > numberOfMillisInDay) {
            try {
                // Close old connection if there was one
                if (mAirspeckWriter != null) {
                    mAirspeckWriter.close();
                }

                // The file could already exist if we just started the app. If not, add the header
                if (!new File(filenameAirspeck).exists()) {
                    Log.i("DF", "Airspeck data file created with header");
                    // Open new connection to new file
                    mAirspeckWriter = new OutputStreamWriter(
                            new FileOutputStream(filenameAirspeck, true));
                    if (mIsStoreAllAirspeckFields) {
                        mAirspeckWriter.append(Constants.AIRSPECK_DATA_HEADER_ALL).append("\n");
                    } else {
                        mAirspeckWriter.append(Constants.AIRSPECK_DATA_HEADER_SUBSET).append("\n");
                    }
                } else {
                    // Open new connection to new file
                    mAirspeckWriter = new OutputStreamWriter(
                            new FileOutputStream(filenameAirspeck, true));

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mDateOfLastAirspeckWrite = now;

        // Write new line to file
        try {
            mAirspeckWriter.append(line).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static {
        System.loadLibrary("respeck-jni");
    }

    // JNI methods
    public native void initBreathing();

    public native void updateBreathing(float x, float y, float z);

    public native float getBreathingSignal();

    public native float getBreathingRate();

    public native float getAverageBreathingRate();

    public native float getActivityLevel();

    public native int getNumberOfBreaths();

    public native float getBreathingAngle();

    public native void resetMedianAverageBreathing();

    public native void calculateMedianAverageBreathing();

    public native float getStdDevBreathingRate();

    public native int getCurrentActivityClassification();

    public native void updateActivityClassification();

    public native float getUpperThreshold();

    public native float getLowerThreshold();
}

