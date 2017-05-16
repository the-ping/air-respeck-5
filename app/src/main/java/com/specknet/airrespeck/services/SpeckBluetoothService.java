package com.specknet.airrespeck.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.models.LocationData;
import com.specknet.airrespeck.models.RESpeckStoredSample;
import com.specknet.airrespeck.utils.Constants;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import rx.Subscription;

/**
 * Service for connecting to RESpeck and Airspeck sensors, converting the data into a readable format and
 * sending the result to the interested Activities.
 */

public class SpeckBluetoothService extends Service {

    // Bluetooth connection
    private BluetoothAdapter mBluetoothAdapter;
    public static RxBleClient rxBleClient;
    private boolean mIsAirspeckFound;
    private boolean mIsRESpeckFound;
    private Subscription scanSubscription;
    private Subscription airspeckSubscription;
    private Subscription respeckLiveSubscription;

    // Config settings
    private boolean mIsAirspeckEnabled;
    private boolean mIsRESpeckEnabled;
    private boolean mIsStoreDataLocally;
    private boolean mIsStoreMergedFile;
    private boolean mIsStoreAllAirspeckFields;

    // Outputstreamwriters for all the files
    private OutputStreamWriter mRespeckWriter;
    private OutputStreamWriter mAirspeckWriter;
    private OutputStreamWriter mMergedWriter;
    private OutputStreamWriter mActivitySummaryWriter;

    // Initial values for last write timestamps
    private Date mDateOfLastRESpeckWrite = new Date(0);
    private Date mDateOfLastAirspeckWrite = new Date(0);

    // Most recent Airspeck data, used for storing merged file
    private String mMostRecentAirspeckData;

    // The UUIDs will be loaded from Config
    private static String RESPECK_UUID;
    private static String AIRSPECK_UUID;

    // Characteristics which determine packet type
    private static final String QOE_CLIENT_CHARACTERISTIC = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String AIRSPECK_LIVE_CHARACTERISTIC = "00001524-1212-efde-1523-784feabcd123";
    private final static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private final static String RESPECK_LIVE_CHARACTERISTIC = "00002010-0000-1000-8000-00805f9b34fb";
    private final static String RESPECK_BREATHING_RATES_CHARACTERISTIC = "00002016-0000-1000-8000-00805f9b34fb";
    private final static String RESPECK_BREATH_INTERVALS_CHARACTERISTIC = "00002015-0000-1000-8000-00805f9b34fb";

    // Airspeck
    private byte[] opcData;
    private boolean[] opcPacketsReceived;
    private float mLastTemperatureAirspeck = -1f;
    private float mLastHumidityAirspeck = -1f;
    private float mLastNO2 = -1f;
    private float mLastO3 = -1f;

    // RESpeck
    private int latestPacketSequenceNumber = -1;
    private long mPhoneTimestampCurrentPacketReceived = -1;
    private long mPhoneTimestampLastPacketReceived = -1;
    private long mRESpeckTimestampCurrentPacketReceived = -1;
    private Queue<RESpeckStoredSample> storedQueue;
    private long latestProcessedMinute = 0L;
    private int currentSequenceNumberInBatch = -1;
    private int latestStoredRespeckSeq = -1;

    // Battery monitoring
    private static final int PROMPT_TO_CHARGE_LEVEL = 1152;
    private static final int BATTERY_FULL_LEVEL = 1152; // was 1139
    private static final int BATTERY_EMPTY_LEVEL = 889;
    private final static String RESPECK_BATTERY_LEVEL_CHARACTERISTIC = "00002017-0000-1000-8000-00805f9b34fb";
    private float mLatestBatteryPercent = 0f;
    private boolean mLatestRequestCharge = false;

    // Minute average breathing stats
    private float mAverageBreathingRate;
    private float mStdDevBreathingRate;
    private int mNumberOfBreaths;

    // GPS
    private LocationData mLastPhoneLocation;
    private BroadcastReceiver mLocationReceiver;

    public SpeckBluetoothService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        new Thread() {
            @Override
            public void run() {
                Log.i("SpeckService", "Starting SpeckService...");
                startInForeground();
                initSpeckService();
                startServiceAndBluetoothScanning();
            }
        }.start();
        return START_STICKY;
    }

    private void startInForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getText(R.string.notification_speck_title))
                .setContentText(getText(R.string.notification_speck_text))
                .setSmallIcon(R.drawable.vec_wireless)
                .setContentIntent(pendingIntent)
                .build();

        // Just use a "random" service ID
        final int SERVICE_NOTIFICATION_ID = 8598001;
        startForeground(SERVICE_NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        stopSpeckService();
        Log.i("SpeckService", "SpeckService has been stopped");
        super.onDestroy();
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
        Utils mUtils = Utils.getInstance(getApplicationContext());

        // Look whether Airspeck is enabled in config
        mIsAirspeckEnabled = Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_AIRSPECK_ENABLED));

        // Is RESpeck enabled?
        mIsRESpeckEnabled = !Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_RESPECK_DISABLED));

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

        // Look whether Airspeck is enabled in config
        mIsAirspeckEnabled = Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_AIRSPECK_ENABLED));

        // Initialise stored queue
        storedQueue = new LinkedList<>();

        //Initialize Breathing Functions
        initBreathing();

        // Get Bluetooth address
        AIRSPECK_UUID = mUtils.getProperties().getProperty(Constants.Config.QOE_UUID);
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

        // Start broadcastreceiver for phone location
        mLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mLastPhoneLocation = (LocationData) intent.getSerializableExtra(Constants.PHONE_LOCATION);
            }
        };
        registerReceiver(mLocationReceiver, new IntentFilter(Constants.ACTION_PHONE_LOCATION_BROADCAST));
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

        mIsAirspeckFound = false;
        mIsRESpeckFound = false;

        rxBleClient = RxBleClient.create(this);
        Log.i("SpeckService", "Scanning..");

        // Initialise opc data arrays
        opcData = new byte[62];
        opcPacketsReceived = new boolean[]{false, false, false, false};

        scanSubscription = rxBleClient.scanBleDevices()
                .subscribe(
                        rxBleScanResult -> {
                            Log.i("SpeckService",
                                    "FOUND :" + rxBleScanResult.getBleDevice().getName() + ", " +
                                            rxBleScanResult.getBleDevice().getMacAddress());
                            if (mIsAirspeckEnabled && !mIsAirspeckFound) {
                                // Process scan result here.
                                if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(AIRSPECK_UUID)) {
                                    mIsAirspeckFound = true;
                                    connectAndNotifyAirspeck();
                                }
                            }
                            if (mIsRESpeckEnabled && !mIsRESpeckFound) {
                                if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(RESPECK_UUID)) {
                                    mIsRESpeckFound = true;
                                    connectAndNotifyRespeck();
                                }
                            }
                        },
                        throwable -> {
                            // Handle an error here.
                            Log.e("SpeckService", throwable.toString());
                        }
                );

        // Start task which makes activity predictions every 2 seconds
        startActivityClassificationTask();
    }

    private void connectAndNotifyAirspeck() {
        if ((mIsAirspeckFound && mIsRESpeckFound) || !mIsRESpeckEnabled)
            scanSubscription.unsubscribe();

        RxBleDevice device = rxBleClient.getBleDevice(AIRSPECK_UUID);
        airspeckSubscription = device.establishConnection(true)
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString(
                        AIRSPECK_LIVE_CHARACTERISTIC)))
                .doOnNext(notificationObservable -> {
                    // Notification has been set up
                    Log.i("SpeckService", "Subscribed to AIRSPECK");
                })
                .flatMap(
                        notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .subscribe(
                        bytes -> {
                            // Given characteristic has been changes, here is the value.
                            processAirspeckPacket(bytes);
                        },
                        throwable -> {
                            // Handle an error here.
                            Log.i("SpeckService", "AIRSPECK DISCONNECTED: " + throwable.toString());
                            reconnectAirspeck();
                        }
                );
    }

    private void reconnectAirspeck() {
        airspeckSubscription.unsubscribe();
        connectAndNotifyAirspeck();
    }

    void processAirspeckPacket(byte[] bytes) {
        // Packet Format

        // Each packet has a header as follows, followed by the sensor-specific data:

        // 1B Length
        // 1B Packet Type
        // 2B Mini Timestamp

        int headerLength = 4;
        int payloadLength = bytes[0];
        int packetLength = bytes.length;
        Log.i("SpeckService", "Payload length: " + payloadLength);

        if (packetLength != payloadLength + headerLength) {
            Log.e("SpeckService", "Unexpected packet length: " + packetLength);
            return;
        }

        byte packetType = bytes[1];
        Log.i("MainActicity", "Packet type: " + String.format("0x%02X ", packetType));

        byte[] miniTimestamp = Arrays.copyOfRange(bytes, 2, 3);

        // The packet type is one of the following:
        final byte SENSOR_TYPE_UUID = 0x01;
        final byte SENSOR_TYPE_TEMPERATURE_HUMIDITY = 0x02;
        final byte SENSOR_TYPE_GAS = 0x03;
        final byte SENSOR_TYPE_OPC1 = 0x04;
        final byte SENSOR_TYPE_OPC2 = 0x05;
        final byte SENSOR_TYPE_OPC3 = 0x06;
        final byte SENSOR_TYPE_OPC4 = 0x07;
        final byte SENSOR_TYPE_GPS = 0x08;
        final byte SENSOR_TYPE_TIMESTAMP = 0x09;

        byte[] payload = Arrays.copyOfRange(bytes, 4, bytes.length);

        StringBuilder sb = new StringBuilder();
        for (byte b : payload) {
            sb.append(String.format("%02X ", b));
        }
        Log.i("SpeckService", "Payload: " + sb.toString());

        switch (packetType) {
            case SENSOR_TYPE_OPC1:
                Log.i("SpeckService", "OPC1 packet received");
                if (payloadLength == 16) {
                    System.arraycopy(payload, 0, opcData, 0, 16);
                    opcPacketsReceived[0] = true;
                } else {
                    Log.e("SpeckService", "OPC1 packet with wrong length received: " + payloadLength);
                }
                break;
            case SENSOR_TYPE_OPC2:
                Log.i("SpeckService", "OPC2 packet received");
                if (payloadLength == 16) {
                    System.arraycopy(payload, 0, opcData, 16, 16);
                    opcPacketsReceived[1] = true;
                } else {
                    Log.e("SpeckService", "OPC2 packet with wrong length received: " + payloadLength);
                }
                break;
            case SENSOR_TYPE_OPC3:
                Log.i("SpeckService", "OPC3 packet received");
                if (payloadLength == 16) {
                    System.arraycopy(payload, 0, opcData, 32, 16);
                    opcPacketsReceived[2] = true;
                } else {
                    Log.e("SpeckService", "OPC3 packet with wrong length received: " + payloadLength);
                }
                break;
            case SENSOR_TYPE_OPC4:
                Log.i("SpeckService", "OPC4 packet received");
                if (payloadLength == 14) {
                    System.arraycopy(payload, 0, opcData, 48, 14);
                    opcPacketsReceived[3] = true;
                } else {
                    Log.e("SpeckService", "OPC4 packet with wrong length received: " + payloadLength);
                }
                break;
            case SENSOR_TYPE_TEMPERATURE_HUMIDITY:
                Log.i("SpeckService", "Temperature/humidity packet received");
                processTempHumidity(payload);
                break;
            case SENSOR_TYPE_GAS:
                processGasData(payload);
                break;
            default:
                Log.e("SpeckService", "Unknown packet type received: " + String.format("0x%02X ", packetType));
                break;
        }

        if (!Arrays.asList(opcPacketsReceived).contains(false)) {
            // All opc packets have been sent. Process them and store all other fields (gas, temp, humidity)
            // with this data as well.
            processOPC(ByteBuffer.wrap(opcData));
            opcData = new byte[62];
            opcPacketsReceived = new boolean[]{false, false, false, false};
        }
    }

    private void processGasData(byte[] data) {
        // TODO: implement
        mLastNO2 = -1;
        mLastO3 = -1;
    }

    void processOPC(ByteBuffer buffer) {
        Log.i("SpeckService", "Processing OPC data: " + buffer.toString());

        buffer.position(0);

        int[] bins = new int[16];
        for (int i = 0; i < bins.length; i++) {
            bins[i] = buffer.getShort();
            Log.i("SpeckService", "Bin " + i + ": " + bins[i]);
        }

        int binsTotal = Utils.sum(bins);

        // 4 8-bit values denoting average of how long particles were observed in laser beam for bin 1,3,5,7
        buffer.getInt();
        // OPC Temperature
        buffer.getInt();
        // OPC Pressure
        buffer.getInt();
        // Temperature and pressure are used to determine airflow in OPC
        // Period count: number of 12 MHz clock cycles in recording period (of processor in OPC), from beginning of
        // recording period
        buffer.getInt();
        // Checksum: count of all bin, keep last 16 bits.
        buffer.getShort();

        float pm1 = buffer.getFloat();
        float pm2_5 = buffer.getFloat();
        float pm10 = buffer.getFloat();

        Log.i("SpeckService", "PM values: " + pm1 + ", " + pm2_5 + ", " + pm10);

        long currentPhoneTimestamp = Utils.getUnixTimestamp();

        //Log.i("SpeckService",
        //        String.format("Gps signal: lat %.4f, long %.4f, alt %.4f", latitude, longitude, altitude));

        // Send data in broadcast
        HashMap<String, Float> readings = new HashMap<>();
        readings.put(Constants.QOE_PM1, pm1);
        readings.put(Constants.QOE_PM2_5, pm2_5);
        readings.put(Constants.QOE_PM10, pm10);
        readings.put(Constants.QOE_TEMPERATURE, mLastTemperatureAirspeck);
        readings.put(Constants.QOE_HUMIDITY, mLastHumidityAirspeck);
        // TODO: read these from sensor
        readings.put(Constants.QOE_NO2, mLastNO2);
        readings.put(Constants.QOE_O3, mLastO3);
        readings.put(Constants.QOE_BINS_0, (float) bins[0]);
        readings.put(Constants.QOE_BINS_1, (float) bins[1]);
        readings.put(Constants.QOE_BINS_2, (float) bins[2]);
        readings.put(Constants.QOE_BINS_3, (float) bins[3]);
        readings.put(Constants.QOE_BINS_4, (float) bins[4]);
        readings.put(Constants.QOE_BINS_5, (float) bins[5]);
        readings.put(Constants.QOE_BINS_6, (float) bins[6]);
        readings.put(Constants.QOE_BINS_7, (float) bins[7]);
        readings.put(Constants.QOE_BINS_8, (float) bins[8]);
        readings.put(Constants.QOE_BINS_9, (float) bins[9]);
        readings.put(Constants.QOE_BINS_10, (float) bins[10]);
        readings.put(Constants.QOE_BINS_11, (float) bins[11]);
        readings.put(Constants.QOE_BINS_12, (float) bins[12]);
        readings.put(Constants.QOE_BINS_13, (float) bins[13]);
        readings.put(Constants.QOE_BINS_14, (float) bins[14]);
        readings.put(Constants.QOE_BINS_15, (float) bins[15]);

        readings.put(Constants.QOE_BINS_TOTAL, (float) binsTotal);


        Intent intentData = new Intent(Constants.ACTION_AIRSPECK_LIVE_BROADCAST);
        intentData.putExtra(Constants.INTERPOLATED_PHONE_TIMESTAMP, currentPhoneTimestamp);
        intentData.putExtra(Constants.AIRSPECK_ALL_MEASURES, readings);
        sendBroadcast(intentData);

        // Store the important data in the external storage if set in config
        if (mIsStoreDataLocally) {
            String storedLine;

            // TODO: get real location from new Airspeck
            String locationString = ",,";

            // For now, as we don't have the Airspeck GPS value, use the last phone location
            if (mLastPhoneLocation != null) {
                locationString = mLastPhoneLocation.getLongitude() + "," +
                        mLastPhoneLocation.getLatitude() + "," + mLastPhoneLocation.getAltitude();
            }

            if (mIsStoreAllAirspeckFields) {
                storedLine = AIRSPECK_UUID + "," + currentPhoneTimestamp + "," + pm1 + "," + pm2_5 + "," + pm10 + "," +
                        mLastTemperatureAirspeck + "," + mLastHumidityAirspeck + "," + mLastNO2 +
                        "," + mLastO3 + "," + bins[0] + "," + bins[1] + "," + bins[2] + "," + bins[3] + "," + bins[4] +
                        "," + bins[5] + "," + bins[6] + "," + bins[7] + "," + bins[8] + "," + bins[9] + "," + bins[10] +
                        "," + bins[11] + "," + bins[12] + "," + bins[13] + "," + bins[14] + "," + bins[15] + "," +
                        binsTotal + "," + locationString;
            } else {
                storedLine = AIRSPECK_UUID + "," + currentPhoneTimestamp + "," + mLastTemperatureAirspeck + "," +
                        mLastHumidityAirspeck + "," + mLastNO2 + "," + mLastO3 + "," + bins[0] + "," + locationString;
            }
            Log.i("QOE", "Airspeck data received: " + storedLine);
            writeToAirspeckFile(storedLine);

            // If we want to store a merged file, store the most recent Airspeck data without
            // timestamp as a string
            if (mIsStoreMergedFile) {
                mMostRecentAirspeckData = storedLine;
            }
        }
    }

    void processTempHumidity(byte[] bytes) {
        if (bytes.length != 4) {
            Log.i("SpeckService", "Temp/humidity packet length incorrect: " + bytes.length);
            return;
        }

        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        mLastTemperatureAirspeck = ((float) buf.getShort()) * 0.1f;
        Log.i("SpeckService", "Temp: " + mLastTemperatureAirspeck);
        mLastHumidityAirspeck = ((float) buf.getShort()) * 0.1f;
        Log.i("SpeckService", "Humidity: " + mLastHumidityAirspeck);
    }

    private void connectAndNotifyRespeck() {
        if ((mIsAirspeckFound && mIsRESpeckFound) || !mIsAirspeckEnabled)
            scanSubscription.unsubscribe();

        RxBleDevice device = rxBleClient.getBleDevice(RESPECK_UUID);
        respeckLiveSubscription = device.establishConnection(true)
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(
                        UUID.fromString(RESPECK_LIVE_CHARACTERISTIC)))
                .doOnNext(notificationObservable -> {
                    // Notification has been set up
                    Log.i("SpeckService", "Subscribed to RESPECK");
                })
                .flatMap(
                        notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .subscribe(
                        characteristicValue -> {
                            // Given characteristic has been changes, here is the value.
                            processRESpeckPacket(RESPECK_LIVE_CHARACTERISTIC, characteristicValue);
                        },
                        throwable -> {
                            // Handle an error here.
                            if (throwable instanceof BleDisconnectedException) {
                                Log.i("SpeckService", "RESPECK DISCONNECTED: " + throwable.toString());
                                reconnectRespeck();
                            } else {
                                Log.e("SpeckService", "Notification handling error: " + throwable.toString());
                            }
                        }
                );
    }

    private void reconnectRespeck() {
        respeckLiveSubscription.unsubscribe();
        connectAndNotifyRespeck();
    }

    public void stopSpeckService() {
        Log.i("SpeckService", "Stopping Speck Service");

        // Close bluetooth scanning
        if (scanSubscription != null) {
            scanSubscription.unsubscribe();
        }
        if (respeckLiveSubscription != null) {
            respeckLiveSubscription.unsubscribe();
        }
        if (airspeckSubscription != null) {
            airspeckSubscription.unsubscribe();
        }

        unregisterReceiver(mLocationReceiver);

        // Close the OutputWritingStreams
        try {
            mActivitySummaryWriter.close();
            if (mRespeckWriter != null) {
                Log.i("SpeckService", "Respeck writer was closed");
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

    private void processRESpeckPacket(final String characteristic, final byte[] values) {

        switch (characteristic) {
            case RESPECK_LIVE_CHARACTERISTIC:
                final int packetSequenceNumber = values[0] & 0xFF;

                //Check if the reading is not repeated
                if (packetSequenceNumber == latestPacketSequenceNumber) {
                    Log.e("RAT", "DUPLICATE SEQUENCE NUMBER: " + Integer.toString(packetSequenceNumber));
                    return;
                } else {
                    latestPacketSequenceNumber = packetSequenceNumber;
                }

                for (int i = 1; i < values.length; i += 7) {
                    Byte startByte = values[i];
                    if (startByte == -1) {
                        // Timestamp packet received. Starting a new sequence
                        // Log.i("DF", "Timestamp received from RESpeck");

                        // Read timestamp from packet
                        Byte ts_1 = values[i + 1];
                        Byte ts_2 = values[i + 2];
                        Byte ts_3 = values[i + 3];
                        Byte ts_4 = values[i + 4];

                        long uncorrectedRESpeckTimestamp = combineTimestampBytes(ts_1, ts_2, ts_3, ts_4);
                        // Convert the timestamp of the RESpeck to correspond to milliseconds
                        long newRESpeckTimestamp = (long) (uncorrectedRESpeckTimestamp * 197 / 32768. * 1000);
                        if (newRESpeckTimestamp == mRESpeckTimestampCurrentPacketReceived) {
                            Log.e("SpeckService", "RESpeck: duplicate live timestamp received");
                            return;
                        }
                        long lastRESpeckTimestamp = mRESpeckTimestampCurrentPacketReceived;
                        mRESpeckTimestampCurrentPacketReceived = newRESpeckTimestamp;

                        // Log.i("SpeckService", "respeck ts: " + newRESpeckTimestamp);
                        // Log.i("SpeckService", "respeck ts diff: " + (newRESpeckTimestamp - lastRESpeckTimestamp));

                        // Independent of the RESpeck timestamp, we use the phone timestamp
                        long actualPhoneTimestamp = Utils.getUnixTimestamp();

                        if (mPhoneTimestampCurrentPacketReceived == -1) {
                            // If this is our first sequence, we use the typical time difference between the
                            // RESpeck packets for determining the previous timestamp
                            mPhoneTimestampLastPacketReceived = actualPhoneTimestamp -
                                    Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS;
                        } else {
                            mPhoneTimestampLastPacketReceived = mPhoneTimestampCurrentPacketReceived;
                        }

                        long extrapolatedPhoneTimestamp = mPhoneTimestampLastPacketReceived +
                                Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS;

                        //Log.i("SpeckService",
                        //        "Diff phone respeck: " + (extrapolatedPhoneTimestamp - newRESpeckTimestamp));

                        // If the last timestamp plus the average time difference is more than
                        // x seconds apart, we use the actual phone timestamp. Otherwise, we use the
                        // last plus the average time difference.
                        if (Math.abs(extrapolatedPhoneTimestamp - actualPhoneTimestamp) >
                                Constants.MAXIMUM_MILLISECONDS_DEVIATION_ACTUAL_AND_CORRECTED_TIMESTAMP) {
                            // Log.i("SpeckService", "correction!");
                            mPhoneTimestampCurrentPacketReceived = actualPhoneTimestamp;
                        } else {
                            // Log.i("SpeckService", "no correction!");
                            mPhoneTimestampCurrentPacketReceived = extrapolatedPhoneTimestamp;
                        }

                        currentSequenceNumberInBatch = 0;

                        // process any queued stored data. TODO:test this if the stored mode is activated again in the
                        // RESpeck.
                        while (!storedQueue.isEmpty()) {
                            // TODO: this doesn't seem to be reached? Is the queue only for the case when
                            // the RESpeck was disconnected from the phone?
                            RESpeckStoredSample s = storedQueue.remove();

                            // TODO: why do we need the offset of the livedata for the stored sample?
                            long currentTimeOffset = mPhoneTimestampCurrentPacketReceived / 1000 -
                                    mRESpeckTimestampCurrentPacketReceived;

                            Log.i("SpeckService", "Stored packet received:");
                            Log.i("SpeckService", "EXTRA_RESPECK_TIMESTAMP_OFFSET_SECS: " + currentTimeOffset);
                            Log.i("SpeckService", "EXTRA_RESPECK_RS_TIMESTAMP: " + s.getRESpeckTimestamp());
                            Log.i("SpeckService", "EXTRA_RESPECK_SEQ: " + s.getSequenceNumber());
                            Log.i("SpeckService", "EXTRA_RESPECK_LIVE_AVE_BR: " + s.getAverageBreathingRate());
                            Log.i("SpeckService", "EXTRA_RESPECK_LIVE_N_BR: " + s.getNumberOfBreaths());
                            Log.i("SpeckService", "EXTRA_RESPECK_LIVE_SD_BR: " + s.getStdBreathingRate());

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
                        // Only do something with data if the timestamps are synchronised
                        // Log.v("DF", "Acceleration packet received from RESpeck");
                        // If the currentSequenceNumberInBatch is -1, this means we have received acceleration
                        // packets before the first timestamp
                        if (currentSequenceNumberInBatch == -1) {
                            Log.e("SpeckService", "RESpeck acceleration packet received before timestamp packet");
                            return;
                        }

                        try {
                            final float x = combineAccelerationBytes(values[i + 1], values[i + 2]);
                            final float y = combineAccelerationBytes(values[i + 3], values[i + 4]);
                            final float z = combineAccelerationBytes(values[i + 5], values[i + 6]);

                            updateBreathing(x, y, z);

                            final float breathingRate = getBreathingRate();
                            final float breathingSignal = getBreathingSignal();
                            final float breathingAngle = getBreathingAngle();
                            final float activityLevel = getActivityLevel();
                            final int activityType = getCurrentActivityClassification();

                            // Calculate interpolated timestamp of current sample based on sequence number
                            // There are 32 samples in each acceleration batch the RESpeck sends.
                            long interpolatedPhoneTimestampOfCurrentSample = (long)
                                    ((mPhoneTimestampCurrentPacketReceived - mPhoneTimestampLastPacketReceived) *
                                            (currentSequenceNumberInBatch * 1. /
                                                    Constants.NUMBER_OF_SAMPLES_PER_BATCH)) +
                                    mPhoneTimestampLastPacketReceived;

                            /*
                            Log.i("SpeckService", "BS_TIMESTAMP " + String.valueOf(mPhoneTimestampCurrentPacketReceived));
                            Log.i("SpeckService", "RS_TIMESTAMP " + String.valueOf(mRESpeckTimestampCurrentPacketReceived));
                            Log.i("SpeckService", "Interpolated timestamp " + String.valueOf(
                                    interpolatedPhoneTimestampOfCurrentSample));
                            Log.i("SpeckService", "EXTRA_RESPECK_SEQ " + String.valueOf(currentSequenceNumberInBatch));
                            Log.i("SpeckService", "Breathing Rate " + String.valueOf(breathingRate));
                            Log.i("SpeckService", "Breathing Signal " + String.valueOf(breathingSignal));
                            Log.i("SpeckService", "Breathing Angle " + String.valueOf(breathingAngle));
                            Log.i("SpeckService", "BRA " + String.valueOf(mAverageBreathingRate));
                            Log.i("SpeckService", "STDBR " + String.valueOf(mStdDevBreathingRate));
                            Log.i("SpeckService", "NBreaths " + String.valueOf(mNumberOfBreaths));
                            Log.i("SpeckService", "Activity Level " + String.valueOf(activityLevel));
                            */

                            // Store the important data in the external storage if set in config
                            if (mIsStoreDataLocally) {
                                String storedLine = RESPECK_UUID + "," + interpolatedPhoneTimestampOfCurrentSample +
                                        "," + mRESpeckTimestampCurrentPacketReceived + "." +
                                        currentSequenceNumberInBatch + "," + x + "," + y + "," + z + "," +
                                        breathingSignal + "," + breathingRate + "," + activityLevel + "," +
                                        activityType;
                                writeToRESpeckAndMergedFile(storedLine);
                            }

                            // Send live broadcast intent
                            Intent liveDataIntent = new Intent(Constants.ACTION_RESPECK_LIVE_BROADCAST);
                            liveDataIntent.putExtra(Constants.INTERPOLATED_PHONE_TIMESTAMP,
                                    interpolatedPhoneTimestampOfCurrentSample);
                            liveDataIntent.putExtra(Constants.RESPECK_SENSOR_TIMESTAMP,
                                    mRESpeckTimestampCurrentPacketReceived);
                            liveDataIntent.putExtra(Constants.RESPECK_X, x);
                            liveDataIntent.putExtra(Constants.RESPECK_Y, y);
                            liveDataIntent.putExtra(Constants.RESPECK_Z, z);
                            liveDataIntent.putExtra(Constants.RESPECK_BREATHING_SIGNAL, breathingSignal);
                            liveDataIntent.putExtra(Constants.RESPECK_BREATHING_RATE, breathingRate);
                            liveDataIntent.putExtra(Constants.RESPECK_MINUTE_AVG_BREATHING_RATE, mAverageBreathingRate);
                            liveDataIntent.putExtra(Constants.RESPECK_BREATHING_ANGLE, breathingAngle);
                            liveDataIntent.putExtra(Constants.RESPECK_SEQUENCE_NUMBER, currentSequenceNumberInBatch);
                            liveDataIntent.putExtra(Constants.RESPECK_ACTIVITY_LEVEL, activityLevel);
                            liveDataIntent.putExtra(Constants.RESPECK_ACTIVITY_TYPE, activityType);
                            liveDataIntent.putExtra(Constants.RESPECK_BATTERY_PERCENT, mLatestBatteryPercent);
                            liveDataIntent.putExtra(Constants.RESPECK_REQUEST_CHARGE, mLatestRequestCharge);
                            sendBroadcast(liveDataIntent);

                            // Every full minute, calculate the average breathing rate in that minute. This value will
                            // only change after a call to "calculateAverageBreathing".
                            long currentProcessedMinute = DateUtils.truncate(new Date(
                                            mPhoneTimestampCurrentPacketReceived),
                                    Calendar.MINUTE).getTime();
                            if (currentProcessedMinute != latestProcessedMinute) {
                                calculateAverageBreathing();

                                mAverageBreathingRate = getAverageBreathingRate();
                                mStdDevBreathingRate = getStdDevBreathingRate();
                                mNumberOfBreaths = getNumberOfBreaths();

                                // Empty the minute average window
                                resetMedianAverageBreathing();

                                // Send average broadcast intent
                                Intent avgDataIntent = new Intent(Constants.ACTION_RESPECK_AVG_BROADCAST);
                                // The averaged data is not attached to a particular sensor record, so we only
                                // store the interpolated phone timestamp
                                avgDataIntent.putExtra(Constants.RESPECK_TIMESTAMP_MINUTE_AVG,
                                        currentProcessedMinute);
                                avgDataIntent.putExtra(Constants.RESPECK_MINUTE_AVG_BREATHING_RATE,
                                        mAverageBreathingRate);
                                avgDataIntent.putExtra(Constants.RESPECK_MINUTE_STD_BREATHING_RATE,
                                        mStdDevBreathingRate);
                                avgDataIntent.putExtra(Constants.RESPECK_MINUTE_NUMBER_OF_BREATHS, mNumberOfBreaths);
                                sendBroadcast(avgDataIntent);

                                /*
                                Log.i("SpeckService", "RESPECK_BS_TIMESTAMP " + ts_minute);
                                Log.i("SpeckService", "EXTRA_RESPECK_LIVE_AVE_BR: " + updatedAverageBreathingRate);
                                Log.i("SpeckService", "EXTRA_RESPECK_LIVE_N_BR: " + updatedNumberOfBreaths);
                                Log.i("SpeckService", "EXTRA_RESPECK_LIVE_SD_BR: " + updatedStdDevBreathingRate);
                                Log.i("SpeckService", "EXTRA_RESPECK_LIVE_ACTIVITY " + activityLevel);
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
                break;
            case RESPECK_BATTERY_LEVEL_CHARACTERISTIC:
                // Battery packet received which contains the charging level of the battery
                int batteryLevel = combineBatteryBytes(values[0], values[1]);

                // Log.i("RAT", "BATTERY LEVEL notification received: " + Integer.toString(battLevel));

                int chargePercentage = (100 * (batteryLevel - BATTERY_EMPTY_LEVEL) /
                        (BATTERY_FULL_LEVEL - BATTERY_EMPTY_LEVEL));

                if (chargePercentage < 1) {
                    chargePercentage = 1;
                } else if (chargePercentage > 100) {
                    chargePercentage = 100;
                }

                mLatestBatteryPercent = (float) chargePercentage;
                mLatestRequestCharge = batteryLevel <= PROMPT_TO_CHARGE_LEVEL;

                // Log.i("RAT", "Battery level: " + Float.toString(
                //         mLatestBatteryPercent) + ", request charge: " + Float.toString(mLatestRequestCharge));

                break;
            case RESPECK_BREATHING_RATES_CHARACTERISTIC:
                // Breathing rates packet received. This only happens when the RESpeck was disconnected and
                // therefore only stored the minute averages
                final int sequenceNumber = values[0] & 0xFF;

                // Duplicate sent?
                if (sequenceNumber == latestStoredRespeckSeq) {
                    return;
                } else {
                    latestStoredRespeckSeq = sequenceNumber;
                }

                long breathAveragePhoneTimestamp = -1;
                long breathAverageRESpeckTimestamp = -1;
                int breathAverageSequenceNumber = -1;

                for (int i = 1; i < values.length; i += 6) {
                    Byte startByte = values[i];

                    if (startByte == -1) {
                        // timestamp
                        Byte ts_1 = values[i + 1];
                        Byte ts_2 = values[i + 2];
                        Byte ts_3 = values[i + 3];
                        Byte ts_4 = values[i + 4];
                        breathAveragePhoneTimestamp = System.currentTimeMillis();
                        breathAverageRESpeckTimestamp = combineTimestampBytes(ts_1, ts_2, ts_3, ts_4) * 197 / 32768;
                        breathAverageSequenceNumber = 0;

                    } else if (startByte == -2) {
                        // breath average
                        int numberOfBreaths = values[i + 1] & 0xFF;
                        if (numberOfBreaths > 5) {
                            float meanBreathingRate = (float) (values[i + 2] & 0xFF) / 5.0f;
                            float sdBreathingRate = (float) Math.sqrt((float) (values[i + 3] & 0xFF) /
                                    10.0f);

                            Byte upperActivityLevel = values[i + 4];
                            Byte lowerActivityLevel = values[i + 5];
                            float combinedActivityLevel = combineActivityLevelBytes(upperActivityLevel,
                                    lowerActivityLevel);

                            RESpeckStoredSample ras = new RESpeckStoredSample(breathAveragePhoneTimestamp,
                                    breathAverageRESpeckTimestamp, breathAverageSequenceNumber++, numberOfBreaths,
                                    meanBreathingRate, sdBreathingRate, combinedActivityLevel);

                            // Add this stored sample to a queue. This way, we will empty the RESpeck buffer as
                            // quickly as possible until it can send live data again.
                            storedQueue.add(ras);
                        }
                    }
                }

                break;
        }
    }

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

    private float combineActivityLevelBytes(Byte upper, Byte lower) {
        short unsignedLower = (short) (lower & 0xFF);
        short unsignedUpper = (short) (upper & 0xFF);
        short value = (short) ((unsignedUpper << 8) | unsignedLower);
        return (value) / 1000.0f;
    }

    private int combineBatteryBytes(Byte upper, Byte lower) {
        short unsignedLower = (short) (lower & 0xFF);
        short unsignedUpper = (short) (upper & 0xFF);
        if (unsignedUpper > 0xF0) {
            unsignedUpper = (short) ((0xFF - unsignedUpper) + 1);
        }
        return (int) (short) ((unsignedUpper << 8) | unsignedLower);
    }

    // TODO: remove, as we store the activity classification anyway
    private void startActivityClassificationTask() {
        // We want to summarise predictions every 10 minutes. (we count the 2 second updates)
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
                    String lineToWrite = Utils.getCurrentTimeStamp() + "," +
                            Math.round(temporaryStoragePredictions[0] * 100. / SUMMARY_COUNT_MAX) + "," +
                            Math.round(temporaryStoragePredictions[1] * 100. / SUMMARY_COUNT_MAX) + "," +
                            Math.round(temporaryStoragePredictions[2] * 100. / SUMMARY_COUNT_MAX) + "\n";
                    try {
                        mActivitySummaryWriter.append(lineToWrite);
                        mActivitySummaryWriter.flush();
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
                    mAirspeckWriter.flush();
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

    public native void calculateAverageBreathing();

    public native float getStdDevBreathingRate();

    public native int getCurrentActivityClassification();

    public native void updateActivityClassification();

    public native float getUpperThreshold();

    public native float getLowerThreshold();
}

