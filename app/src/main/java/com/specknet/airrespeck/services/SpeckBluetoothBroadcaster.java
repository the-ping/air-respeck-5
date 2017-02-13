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
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.models.RESpeckStoredSample;
import com.specknet.airrespeck.services.qoeuploadservice.QOERemoteUploadService;
import com.specknet.airrespeck.services.respeckuploadservice.RespeckRemoteUploadService;
import com.specknet.airrespeck.utils.Constants;

import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by Darius on 13.02.2017.
 */

public class SpeckBluetoothBroadcaster {

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
        }

        if (mGattQOE == null && device.getName().contains("QOE")) {
            Log.i("[Bluetooth]", "Connecting to " + device.getName());
            mDeviceQOE = device;
            mGattQOE = device.connectGatt(getApplicationContext(), true, mGattCallbackQOE);
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
                            float breathActivity = getActivity();

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
                                float act = getActivity();

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

    static {
        System.loadLibrary("respeck-jni");
    }

    //public native String getMsgFromJni();
    public native void initBreathing();

    public native void updateBreathing(float x, float y, float z);

    public native float getBreathingSignal();

    public native float getBreathingRate();

    public native float getAverageBreathingRate();

    public native float getActivity();

    public native int getNBreaths();

    public native float getBreathingAngle();

    //public native String stringfromJNI();
    public native void resetMA();

    public native void calculateMA();

    public native float getStdDevBreathingRate();
}

