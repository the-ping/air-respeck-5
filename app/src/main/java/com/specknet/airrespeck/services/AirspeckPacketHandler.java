package com.specknet.airrespeck.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.LocationData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * This class processes new Airspeck packets which are passed from the SpeckBluetoothService.
 * It contains all logic to transform the incoming bytes into the desired variables and then stores and broadcasts
 * this information
 */

public class AirspeckPacketHandler {

    // Airspeck
    private ByteBuffer opcData;
    private float mLastTemperatureAirspeck = Float.NaN;
    private float mLastHumidityAirspeck = Float.NaN;
    private float mLastNO2ae = Float.NaN;
    private float mLastNO2we = Float.NaN;
    private float mLastO3ae = Float.NaN;
    private float mLastO3we = Float.NaN;

    // GPS
    private LocationData mLastPhoneLocation;
    private BroadcastReceiver mLocationReceiver;

    // Initial values for last write timestamps
    private Date mDateOfLastAirspeckWrite = new Date(0);

    private OutputStreamWriter mAirspeckWriter;

    private static String AIRSPECK_UUID;

    private SpeckBluetoothService mSpeckService;

    private boolean mIsStoreDataLocally;

    private AirspeckData mMostRecentAirspeckData;

    // Singleton pattern to allow only one instance of this class
    private static AirspeckPacketHandler mSingletonInstance;

    public static AirspeckPacketHandler getInstance(SpeckBluetoothService speckService) {
        if (mSingletonInstance == null) {
            mSingletonInstance = new AirspeckPacketHandler(speckService);
        }
        return mSingletonInstance;
    }

    public AirspeckData getMostRecentAirspeckData() {
        return mMostRecentAirspeckData;
    }

    private AirspeckPacketHandler(SpeckBluetoothService speckService) {
        mSpeckService = speckService;

        // Initialise opc data arrays
        opcData = ByteBuffer.allocate(62);
        opcData.order(ByteOrder.LITTLE_ENDIAN);

        Utils utils = Utils.getInstance(speckService);
        AIRSPECK_UUID = utils.getProperties().getProperty(Constants.Config.AIRSPECK_UUID);

        // Do we store data locally?
        mIsStoreDataLocally = Boolean.parseBoolean(
                utils.getProperties().getProperty(Constants.Config.IS_STORE_DATA_LOCALLY));

        // Start broadcastreceiver for phone location
        mLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mLastPhoneLocation = (LocationData) intent.getSerializableExtra(Constants.PHONE_LOCATION);
            }
        };
        speckService.registerReceiver(mLocationReceiver, new IntentFilter(Constants.ACTION_PHONE_LOCATION_BROADCAST));
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
                opcData.clear();
                opcData.put(payload);
                break;
            case SENSOR_TYPE_OPC2:
                Log.i("SpeckService", "OPC2 packet received");
                opcData.put(payload);
                break;
            case SENSOR_TYPE_OPC3:
                Log.i("SpeckService", "OPC3 packet received");
                opcData.put(payload);
                break;
            case SENSOR_TYPE_OPC4:
                Log.i("SpeckService", "OPC4 packet received");
                opcData.put(payload);
                processOPC(opcData);
                opcData.clear();
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
    }

    private void processGasData(byte[] data) {
        // TODO: implement
        mLastNO2ae = -1;
        mLastNO2we = -1;
        mLastO3ae = -1;
        mLastO3we = -1;
    }

    private void processOPC(ByteBuffer buffer) {
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

        long currentPhoneTimestamp = Utils.getUnixTimestamp();

        // TODO: get real location from new Airspeck
        String locationString = ",,";

        AirspeckData newAirspeckData = new AirspeckData(AIRSPECK_UUID, currentPhoneTimestamp, pm1, pm2_5, pm10,
                mLastTemperatureAirspeck, mLastHumidityAirspeck, mLastNO2we, mLastNO2ae, mLastO3we, mLastO3ae, bins,
                mLastPhoneLocation);

        mMostRecentAirspeckData = newAirspeckData;

        Log.i("AirspeckHandler", "New airspeck packet: " + newAirspeckData);

        Intent intentData = new Intent(Constants.ACTION_AIRSPECK_LIVE_BROADCAST);
        intentData.putExtra(Constants.AIRSPECK_DATA, newAirspeckData);
        mSpeckService.sendBroadcast(intentData);

        // Store the important data in the external storage if set in config
        if (mIsStoreDataLocally) {
            writeToAirspeckFile(newAirspeckData);
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

    private void writeToAirspeckFile(AirspeckData airspeckData) {
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
                    mAirspeckWriter.append(Constants.AIRSPECK_DATA_HEADER).append("\n");
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
            mAirspeckWriter.append(airspeckData.toStringForFile()).append("\n");
            mAirspeckWriter.flush();
        } catch (IOException e) {
            Log.e("SpeckService", "Error while writing to airspeck file: " + e.getMessage());
            // This very likely happened because this service wasn't stopped properly when the app was
            // closed
            mSpeckService.stopSpeckService();
        }
    }


    public void closeHandler() throws Exception {
        if (mAirspeckWriter != null) {
            Log.i("SpeckService", "Respeck writer was closed");
            mAirspeckWriter.close();
        }
        mSpeckService.unregisterReceiver(mLocationReceiver);
    }
}
