package com.specknet.airrespeck.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
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
    private ByteBuffer packetData;
    private float mLastTemperatureAirspeck = Float.NaN;
    private float mLastHumidityAirspeck = Float.NaN;

    // GPS
    private LocationData mLastPhoneLocation = new LocationData(Double.NaN, Double.NaN, Double.NaN, Float.NaN);
    private BroadcastReceiver mLocationReceiver;

    // Initial values for last write timestamps
    private Date mDateOfLastAirspeckWrite = new Date(0);

    private OutputStreamWriter mAirspeckWriter;

    private static String AIRSPECK_UUID;

    private SpeckBluetoothService mSpeckService;

    private boolean mIsStoreDataLocally;
    private String patientID;
    private String androidID;
    private short lux;
    private short motion;

    public AirspeckPacketHandler(SpeckBluetoothService speckService) {
        mSpeckService = speckService;

        // Initialise opc data arrays
        opcData = ByteBuffer.allocate(62);
        opcData.order(ByteOrder.LITTLE_ENDIAN);
        packetData = ByteBuffer.allocate(128); // A little larger than necessary in case we add fields to the packet

        Utils utils = Utils.getInstance(speckService);
        AIRSPECK_UUID = utils.getProperties().getProperty(Constants.Config.AIRSPECK_UUID);

        // Do we store data locally?
        mIsStoreDataLocally = Boolean.parseBoolean(
                utils.getProperties().getProperty(Constants.Config.IS_STORE_DATA_LOCALLY));

        patientID = utils.getProperties().getProperty(Constants.Config.SUBJECT_ID);
        androidID = Settings.Secure.getString(speckService.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // Start broadcastreceiver for phone location
        mLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //mLastPhoneLocation = (LocationData) intent.getSerializableExtra(Constants.PHONE_LOCATION);
            }
        };
        speckService.registerReceiver(mLocationReceiver, new IntentFilter(Constants.ACTION_PHONE_LOCATION_BROADCAST));
    }

    void processCompleteAirSpeckPacket(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);
        byte header = buffer.get();
        long uuid = buffer.getLong();
        byte[] patientIdBytes = new byte[6];
        //Log.i("AirspeckPacketHandler", "Buffer: " + buffer.toString());
        //Log.i("AirspeckPacketHandler", "position: " + buffer.position());
        buffer.get(patientIdBytes);
        patientID = new String( patientIdBytes);
        Log.i("AirspeckPacketHandler", "patientID: " + patientID);

        //short patientID = buffer.getShort();
        int timestamp = buffer.getInt();
        short temperature = buffer.getShort();
        short humidity = buffer.getShort();
        Log.i("AirspeckPacketHandler", "temperature: " + temperature + " humidity: " + humidity);
        opcData.put(buffer.array(), buffer.position(), 62);
        buffer.position(buffer.position() + 62);
        short battery_level = buffer.getShort();
        float latitude = buffer.getFloat();
        float longitude = buffer.getFloat();
        short height = buffer.getShort();
        byte last_error_id = buffer.get();
        lux = buffer.getShort();
        motion = buffer.getShort();

        Log.i("AirspeckPacketHandler", "battery: " + battery_level + ", lux: " + lux);
        mLastPhoneLocation = new LocationData(latitude, longitude, height, (float)1.0);
        char last_error = buffer.getChar();
        mLastTemperatureAirspeck = temperature * 0.1f;
//        Log.i("AirspeckPacketHandler", "Temp: " + mLastTemperatureAirspeck);
        mLastHumidityAirspeck = humidity * 0.1f;
        processOPC(opcData);
        opcData.clear();
    }

    void processAirspeckPacket(byte[] bytes) {

        int packetLength = bytes.length;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        Log.i("AirspeckPacketHandler", "Payload: " + sb.toString());

        if (packetData.position() > 0) {
            packetData.put(bytes);
            if (packetData.position() >= 102) {
                Log.i("AirSpeckPacketHandler", "completed packet");
                //packetData.clear();
                processCompleteAirSpeckPacket(packetData);
                packetData.clear();

            }
        } else if (bytes[0] == 0x03) {
            packetData.put(bytes);
        } else {
            Log.i("AirspeckPacketHandler", "out of order packet");
        }
    }

    private void processOPC(ByteBuffer buffer) {
        // Log.i("AirspeckPacketHandler", "Processing OPC data: " + buffer.toString());
        buffer.position(0);

        int[] bins = new int[16];
        for (int i = 0; i < bins.length; i++) {
            bins[i] = buffer.getShort() & 0xffff;
            // Log.i("AirspeckPacketHandler", "Bin " + i + ": " + bins[i]);
        }

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



        AirspeckData newAirspeckData = new AirspeckData(currentPhoneTimestamp, pm1, pm2_5, pm10,
                mLastTemperatureAirspeck, mLastHumidityAirspeck, bins, mLastPhoneLocation, lux, motion);

        Log.i("AirspeckHandler", "New airspeck packet: " + newAirspeckData);

        Intent intentData = new Intent(Constants.ACTION_AIRSPECK_LIVE_BROADCAST);
        intentData.putExtra(Constants.AIRSPECK_DATA, newAirspeckData);
        mSpeckService.sendBroadcast(intentData);

        // Store the important data in the external storage if set in config
        if (mIsStoreDataLocally) {
            writeToAirspeckFile(newAirspeckData);
        }
    }


    private void writeToAirspeckFile(AirspeckData airspeckData) {
        // Check whether we are in a new day
        Date now = new Date();
        long currentWriteDay = DateUtils.truncate(now, Calendar.DAY_OF_MONTH).getTime();
        long previousWriteDay = DateUtils.truncate(mDateOfLastAirspeckWrite, Calendar.DAY_OF_MONTH).getTime();
        long numberOfMillisInDay = 1000 * 60 * 60 * 24;

        String filenameAirspeck = Utils.getInstance(
                mSpeckService).getDataDirectory() + Constants.AIRSPECK_DATA_DIRECTORY_NAME + "Airspeck " +
                patientID + " " + androidID + " " + AIRSPECK_UUID.replace(":","") + " " +
                new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(now) +
                ".csv";

        // If we are in a new day, create a new file if necessary
        if (!new File(filenameAirspeck).exists() || currentWriteDay != previousWriteDay ||
                now.getTime() - mDateOfLastAirspeckWrite.getTime() > numberOfMillisInDay) {
            try {
                // Close old connection if there was one
                if (mAirspeckWriter != null) {
                    mAirspeckWriter.close();
                }

                // The file could already exist if we just started the app. If not, add the header
                if (!new File(filenameAirspeck).exists()) {
                    Log.i("AirspeckPacketHandler", "Airspeck data file created with header");
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
            // Write Airspeck data together with patientID
            mAirspeckWriter.append(airspeckData.toStringForFile()).append("\n");
            mAirspeckWriter.flush();
        } catch (IOException e) {
            Log.e("AirspeckPacketHandler", "Error while writing to airspeck file: " + e.getMessage());
        }
    }


    public void closeHandler() throws Exception {
        if (mAirspeckWriter != null) {
            Log.i("AirspeckPacketHandler", "Respeck writer was closed");
            mAirspeckWriter.close();
        }
        mSpeckService.unregisterReceiver(mLocationReceiver);
    }
}
