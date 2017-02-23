package com.specknet.airrespeck.services;

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
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.models.RESpeckStoredSample;
import com.specknet.airrespeck.services.qoeuploadservice.QOERemoteUploadService;
import com.specknet.airrespeck.services.respeckuploadservice.RespeckRemoteUploadService;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.LocationUtils;
import com.specknet.airrespeck.utils.Utils;

import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
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
 * Created by Darius on 13.02.2017.
 */

public class SpeckBluetoothService {

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

    // QOE CODE
    private static byte[][] lastPackets_1 = new byte[4][];
    private static byte[][] lastPackets_2 = new byte[5][];
    private static int[] sampleIDs_1 = {0, 0, 0, 0};
    private static int[] sampleIDs_2 = {0, 1, 2, 3, 4};
    int lastSample = 0;

    // RESPECK CODE
    private int latestLiveRespeckSeq = -1;
    private long currentPhoneTimestamp = -1;
    private long currentRESpeckTimestamp = -1;
    private Queue<RESpeckStoredSample> storedQueue;

    private long timestampOfPreviousSequence = -1;
    private long timestampOfCurrentSequence = -1;

    private long latestProcessedMinute = 0L;
    private int currentSequenceNumberInBatch = -1;
    private long breathAveragePhoneTimestamp = -1;
    private long breathAverageRESpeckTimestamp = -1;
    private int breathAverageSequenceNumber = -1;
    private int latestStoredRespeckSeq = -1;

    // BATTERY MONITORING

    public static final int PROMPT_TO_CHARGE_LEVEL = 1152;
    public static final int BATTERY_FULL_LEVEL = 1152; // was 1139
    public static final int BATTERY_EMPTY_LEVEL = 889;
    private final static String RESPECK_BATTERY_LEVEL_CHARACTERISTIC = "00002017-0000-1000-8000-00805f9b34fb";
    private float latestBatteryPercent = 0f;
    private float latestRequestCharge = 0f;

    // References to Context and Utils
    MainActivity mainActivity;
    Utils mUtils;
    LocationUtils mLocationUtils;

    // UPLOAD SERVICES
    RespeckRemoteUploadService mRespeckRemoteUploadService;
    QOERemoteUploadService mQOERemoteUploadService;

    public SpeckBluetoothService() {

    }

    /**
     * Initiate Bluetooth adapter.
     */
    public void initSpeckService(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        // Get references to Utils
        mUtils = Utils.getInstance(mainActivity);

        // Get reference to LocationUtils
        mLocationUtils = LocationUtils.getInstance(mainActivity);
        mLocationUtils.startLocationManager();

        // Initialise stored queue
        storedQueue = new LinkedList<>();

        //Initialize Breathing Functions
        initBreathing();

        // Get Bluetooth address
        QOE_UUID = mUtils.getProperties().getProperty(Constants.PFIELD_QOEUUID);
        RESPECK_UUID = mUtils.getProperties().getProperty(Constants.PFIELD_RESPECK_UUID);

        // Initializes a Bluetooth adapter. For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mainActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mQOEConnectionComplete = false;
        mRespeckConnectionComplete = false;

        // Initialize Upload services
        initRespeckUploadService();
        initQOEUploadService();
    }

    /**
     * Check Bluetooth availability and initiate devices scanning.
     */
    public void startServiceAndBluetoothScanning() {
        // Check if Bluetooth is supported on the device
        if (mBluetoothAdapter == null) {
            Toast.makeText(mainActivity.getApplicationContext(), "This device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Check if Bluetooth is enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mainActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
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
    }

    public boolean isConnecting() {
        // TODO: this is only commented out for testing purposes
        //return !mQOEConnectionComplete && !mRespeckConnectionComplete;
        return false;
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
        Log.i("DF", "device name: " + device.getName());
        if (mGattRespeck == null && device.getName().contains("Respeck")) {
            Log.i("[Bluetooth]", "Connecting to " + device.getName());
            mDeviceRespeck = device;
            mGattRespeck = device.connectGatt(mainActivity.getApplicationContext(), true, mGattCallbackRespeck);
        }
        if (mGattQOE == null && device.getName().contains("QOE")) {
            Log.i("[Bluetooth]", "Connecting to " + device.getName());
            mDeviceQOE = device;
            mGattQOE = device.connectGatt(mainActivity.getApplicationContext(), true, mGattCallbackQOE);
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
                    sendMessageToUIHandler(MainActivity.SHOW_AIRSPECK_CONNECTED, null);
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    mQOEConnectionComplete = false;
                    Log.e("QOE-gattCallback", "STATE_DISCONNECTED");
                    Log.i("QOE-gattCallback", "reconnecting...");
                    BluetoothDevice device = gatt.getDevice();
                    mGattQOE.close();
                    mGattQOE = null;
                    connectToDevice(device);
                    sendMessageToUIHandler(MainActivity.SHOW_AIRSPECK_DISCONNECTED, null);
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
                    mainActivity.sendBroadcast(intent);
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

                    sendMessageToUIHandler(MainActivity.UPDATE_QOE_READINGS, values);
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
                    sendMessageToUIHandler(MainActivity.SHOW_RESPECK_CONNECTED, null);

                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    mRespeckConnectionComplete = false;
                    Log.e("Respeck-gattCallback", "STATE_DISCONNECTED");
                    Log.i("Respeck-gattCallback", "reconnecting...");
                    BluetoothDevice device = gatt.getDevice();
                    mGattRespeck.close();
                    mGattRespeck = null;
                    connectToDevice(device);
                    sendMessageToUIHandler(MainActivity.SHOW_RESPECK_DISCONNECTED, null);
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
                final int sequenceNumber = accelBytes[0] & 0xFF;

                //Check if the reading is not repeated
                if (sequenceNumber == latestLiveRespeckSeq) {
                    Log.e("RAT", "DUPLICATE SEQUENCE NUMBER: " + Integer.toString(sequenceNumber));
                    return;
                } else {
                    latestLiveRespeckSeq = sequenceNumber;
                }

                for (int i = 1; i < accelBytes.length; i += 7) {
                    Byte startByte = accelBytes[i];
                    if (startByte == -1) {
                        // Timestamp packet received. Starting a new sequence
                        Log.i("DF", "Timestamp received from RESpeck");

                        // Read timestamp from packet
                        Byte ts_1 = accelBytes[i + 1];
                        Byte ts_2 = accelBytes[i + 2];
                        Byte ts_3 = accelBytes[i + 3];
                        Byte ts_4 = accelBytes[i + 4];

                        Long newRESpeckTimestamp = combineTimestampBytes(ts_1, ts_2, ts_3, ts_4) * 197 / 32768;
                        if (newRESpeckTimestamp == currentRESpeckTimestamp) {
                            Log.e("RAT", "DUPLICATE LIVE TIMESTAMP RECEIVED");
                            return;
                        }
                        currentRESpeckTimestamp = newRESpeckTimestamp;

                        // Independent of the RESpeck timestamp, we use the phone timestamp
                        currentPhoneTimestamp = System.currentTimeMillis();

                        if (timestampOfPreviousSequence == -1) {
                            // If this is our first sequence, we use the typical time difference between the
                            // RESpeck packets for determining the previous timestamp
                            timestampOfPreviousSequence = currentPhoneTimestamp -
                                    Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_PACKETS;
                        } else {
                            timestampOfPreviousSequence = timestampOfCurrentSequence;
                        }
                        // Update the current sequence timestamp to the current phone timestamp
                        timestampOfCurrentSequence = currentPhoneTimestamp;

                        currentSequenceNumberInBatch = 0;

                        // process any queued stored data
                        while (!storedQueue.isEmpty()) {
                            // TODO: this doesn't seem to be reached? Is the queue only for the case when
                            // the RESpeck was disconnected from the phone? Comment above if applicable!
                            RESpeckStoredSample s = storedQueue.remove();

                            Long currentTimeOffset = currentPhoneTimestamp / 1000 - currentRESpeckTimestamp;

                            Log.i("1", "EXTRA_RESPECK_TIMESTAMP_OFFSET_SECS: " + currentTimeOffset);
                            Log.i("1", "EXTRA_RESPECK_RS_TIMESTAMP: " + s.getRESpeckTimestamp());
                            Log.i("1", "EXTRA_RESPECK_SEQ: " + s.getSequenceNumber());
                            Log.i("1", "EXTRA_RESPECK_LIVE_AVE_BR: " + s.getMeanBr());
                            Log.i("1", "EXTRA_RESPECK_LIVE_N_BR: " + s.getNBreaths());
                            Log.i("1", "EXTRA_RESPECK_LIVE_SD_BR: " + s.getSdBr());
                            Log.i("1", "EXTRA_RESPECK_LIVE_ACTIVITY " + s.getActivityLevel());
                        }
                    } else if (startByte == -2 && currentSequenceNumberInBatch >= 0) { //OxFE - accel packet
                        // If the currentSequenceNumberInBatch is -1, this means we have received acceleration
                        // packets before the first timestamp
                        Log.i("DF", "Acceleration packet received from RESpeck");
                        try {
                            final float x = combineAccelerationBytes(accelBytes[i + 1], accelBytes[i + 2]);
                            float y = combineAccelerationBytes(accelBytes[i + 3], accelBytes[i + 4]);
                            float z = combineAccelerationBytes(accelBytes[i + 5], accelBytes[i + 6]);

                            updateBreathing(x, y, z);

                            float breathingRate = getBreathingRate();
                            float breathingSignal = getBreathingSignal();
                            float breathingAngle = getBreathingAngle();
                            float averageBreathingRate = getAverageBreathingRate();
                            float stdDevBreathingRate = getStdDevBreathingRate();
                            float numberOfBreaths = getNumberOfBreaths();
                            float activityLevel = getActivityLevel();
                            float activityType = getCurrentActivityClassification();

                            // Calculate interpolated timestamp of current sample based on sequence number
                            // There are 32 samples in each acceleration batch the RESpeck sends.
                            long interpolatedPhoneTimestampOfCurrentSample = (long) ((timestampOfCurrentSequence -
                                    timestampOfPreviousSequence) * (currentSequenceNumberInBatch * 1. /
                                    Constants.NUMBER_OF_SAMPLES_PER_BATCH)) + timestampOfPreviousSequence;
                            float cutoffInterpolatedTimestamp = onlyKeepTimeInDay(
                                    interpolatedPhoneTimestampOfCurrentSample);

                            Log.i("2", "BS_TIMESTAMP " + String.valueOf(currentPhoneTimestamp));
                            Log.i("2", "RS_TIMESTAMP " + String.valueOf(currentRESpeckTimestamp));
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

                            // Send JSON object to server
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
                                if (Float.isNaN(numberOfBreaths)) {
                                    json.put(Constants.RESPECK_N_BREATHS, null);
                                } else {
                                    json.put(Constants.RESPECK_N_BREATHS, numberOfBreaths);
                                }
                                if (Float.isNaN(activityLevel)) {
                                    json.put(Constants.RESPECK_ACTIVITY_LEVEL, null);
                                } else {
                                    json.put(Constants.RESPECK_ACTIVITY_LEVEL, activityLevel);
                                }
                                json.put(Constants.RESPECK_LIVE_SEQ, currentSequenceNumberInBatch);
                                json.put(Constants.RESPECK_LIVE_RS_TIMESTAMP, currentRESpeckTimestamp);
                                json.put(Constants.UNIX_TIMESTAMP, interpolatedPhoneTimestampOfCurrentSample);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Intent intent = new Intent(RespeckRemoteUploadService.MSG_UPLOAD);
                            intent.putExtra(RespeckRemoteUploadService.MSG_UPLOAD_DATA, json.toString());
                            mainActivity.sendBroadcast(intent);
                            Log.d("RESPECK", "Sent LIVE JSON to upload service: " + json.toString());


                            // Send RESpeck readings to UI Handler
                            HashMap<String, Float> values = new HashMap<String, Float>();
                            values.put(Constants.RESPECK_X, x);
                            values.put(Constants.RESPECK_Y, y);
                            values.put(Constants.RESPECK_Z, z);
                            values.put(Constants.RESPECK_ACTIVITY_TYPE, activityType);
                            // Only store the
                            values.put(Constants.RESPECK_LIVE_INTERPOLATED_TIMESTAMP,
                                    (float) cutoffInterpolatedTimestamp);
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
                            // add battery info
                            values.put(Constants.RESPECK_BATTERY_PERCENT, latestBatteryPercent);
                            values.put(Constants.RESPECK_REQUEST_CHARGE, latestRequestCharge);

                            sendMessageToUIHandler(MainActivity.UPDATE_RESPECK_READINGS, values);

                            // Every full minute, calculate the average breathing rate in that minute. This value will
                            // only change after a call to "calculateMA".
                            long ts_minute = DateUtils.truncate(new Date(currentPhoneTimestamp),
                                    Calendar.MINUTE).getTime();
                            if (ts_minute > latestProcessedMinute) {
                                calculateMA();

                                float ave_br = getAverageBreathingRate();
                                float sd_br = getStdDevBreathingRate();
                                //Log.d("RAT", "STD_DEV: " + Float.toString(sd_br));
                                int n_breaths = getNumberOfBreaths();
                                float act = getActivityLevel();

                                // Empty the minute average window
                                resetMA();

                                Log.i("3", "RESPECK_BS_TIMESTAMP " + ts_minute);
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
                                mainActivity.sendBroadcast(intent2);
                                Log.d("RAT", "Sent LIVE JSON to upload service: " + json.toString());

                                latestProcessedMinute = ts_minute;
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

                Log.i("RAT", "BATTERY LEVEL notification received: " + Integer.toString(battLevel));

                int chargePercentage = (100 * (battLevel - BATTERY_EMPTY_LEVEL) /
                        (BATTERY_FULL_LEVEL - BATTERY_EMPTY_LEVEL));

                if (chargePercentage < 1)
                    chargePercentage = 1;
                else if (chargePercentage > 100)
                    chargePercentage = 100;

                latestBatteryPercent = (float) chargePercentage;

                boolean requiresCharging = battLevel <= PROMPT_TO_CHARGE_LEVEL;
                if (requiresCharging) {
                    latestRequestCharge = 1f;
                } else {
                    latestRequestCharge = 0;
                }

                Log.i("RAT", "Battery level: " + Float.toString(
                        latestBatteryPercent) + ", request charge: " + Float.toString(latestRequestCharge));

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
                            Log.w("RAT", "Queueing stored sample: " + ras.toString());
                            storedQueue.add(ras);
                        }
                    }

                    //Log.i("RAT", "BREATH AVERAGES: " + Arrays.toString(breath_averages));
                    //updateTextBox("Breath Averages: " + Arrays.toString(breath_averages));
                }

            }
        }
    };

    private float onlyKeepTimeInDay(long timestamp) {
        /*
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH", Locale.UK);
            String startOfDay = dateFormat.format(new Date());
            long startOfDayMillis = dateFormat.parse(startOfDay).getTime();
            return (float) (timestamp - startOfDayMillis);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0f;
        */
        long millisInHour = 36000000;
        return (float) (timestamp % millisInHour);
    }

    private long combineTimestampBytes(Byte upper, Byte upper_middle, Byte lower_middle, Byte lower) {
        short unsigned_upper = (short) (upper & 0xFF);
        short unsigned_upper_middle = (short) (upper_middle & 0xFF);
        short unsigned_lower_middle = (short) (lower_middle & 0xFF);
        short unsigned_lower = (short) (lower & 0xFF);
        int value = (int) ((unsigned_upper << 24) | (unsigned_upper_middle << 16) | (unsigned_lower_middle << 8) | unsigned_lower);
        long uValue = value & 0xffffffffL;
        return uValue;
    }

    private float combineAccelerationBytes(Byte upper, Byte lower) {
        short unsigned_lower = (short) (lower & 0xFF);
        short value = (short) ((upper << 8) | unsigned_lower);
        float fValue = (value) / 16384.0f;
        return fValue;
    }

    private float combineActBytes(Byte upper, Byte lower) {
        short unsignedLower = (short) (lower & 0xFF);
        short unsignedUpper = (short) (upper & 0xFF);
        short value = (short) ((unsignedUpper << 8) | unsignedLower);
        float fValue = (value) / 1000.0f;
        return fValue;
    }

    private int combineBattBytes(Byte upper, Byte lower) {
        short unsignedLower = (short) (lower & 0xFF);
        short unsignedUpper = (short) (upper & 0xFF);
        if (unsignedUpper > 0xF0) {
            unsignedUpper = (short) ((0xFF - unsignedUpper) + 1);
        }
        short value = (short) ((unsignedUpper << 8) | unsignedLower);
        int iValue = value;
        return iValue;
    }

    private void sendMessageToUIHandler(int tag, Object obj) {
        Message msg = Message.obtain();
        msg.obj = obj;
        msg.what = tag;
        msg.setTarget(mainActivity.getUIHandler());
        msg.sendToTarget();
    }

    private void initRespeckUploadService() {
        mRespeckRemoteUploadService = new RespeckRemoteUploadService();
        mRespeckRemoteUploadService.onCreate(mainActivity);
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
        mainActivity.sendBroadcast(intent);
    }

    private void initQOEUploadService() {
        mQOERemoteUploadService = new QOERemoteUploadService();
        mQOERemoteUploadService.onCreate(mainActivity);
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
        mainActivity.sendBroadcast(intent);
    }

    private void startActivityClassificationTask() {
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

    // JNI methods
    public native void initBreathing();

    public native void updateBreathing(float x, float y, float z);

    public native float getBreathingSignal();

    public native float getBreathingRate();

    public native float getAverageBreathingRate();

    public native float getActivityLevel();

    public native int getNumberOfBreaths();

    public native float getBreathingAngle();

    public native void resetMA();

    public native void calculateMA();

    public native float getStdDevBreathingRate();

    public native int getCurrentActivityClassification();

    public native void updateActivityClassification();
}

