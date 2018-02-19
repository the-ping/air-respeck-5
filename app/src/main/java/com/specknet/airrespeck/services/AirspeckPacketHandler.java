package com.specknet.airrespeck.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.LocationData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.FileLogger;
import com.specknet.airrespeck.utils.Utils;

import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class processes new Airspeck packets which are passed from the SpeckBluetoothService.
 * It contains all logic to transform the incoming bytes into the desired variables and then stores and broadcasts
 * this information
 */

public class AirspeckPacketHandler {

    // Airspeck data
    private ByteBuffer packetData;
    private ByteBuffer opcData;
    private int[] mBins;
    private float mPm1;
    private float mPm2_5;
    private float mPm10;

    // Initial values for last write timestamps
    private Date mDateOfLastAirspeckWrite = new Date(0);

    private OutputStreamWriter mAirspeckWriter;

    private static String airspeckUUID;

    private SpeckBluetoothService mSpeckService;

    private BroadcastReceiver mLocationReceiver;
    private LocationData mLastPhoneLocation;

    private boolean mIsStoreDataLocally;
    private boolean mIsEncryptData;
    private String subjectID;
    private String androidID;

    public AirspeckPacketHandler(SpeckBluetoothService speckService) {
        mSpeckService = speckService;

        // Initialise opc data arrays
        opcData = ByteBuffer.allocate(62);
        opcData.order(ByteOrder.LITTLE_ENDIAN);
        packetData = ByteBuffer.allocate(128); // A little larger than necessary in case we add fields to the packet

        Utils utils = Utils.getInstance();
        Map<String, String> loadedConfig = utils.getConfig(mSpeckService);
        airspeckUUID = loadedConfig.get(Constants.Config.AIRSPECKP_UUID);

        mIsStoreDataLocally = Boolean.parseBoolean(loadedConfig.get(Constants.Config.STORE_DATA_LOCALLY));

        mIsEncryptData = Boolean.parseBoolean(loadedConfig.get(Constants.Config.ENCRYPT_LOCAL_DATA));

        subjectID = loadedConfig.get(Constants.Config.SUBJECT_ID);
        androidID = Settings.Secure.getString(speckService.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // Start broadcast receiver for phone location
        mLastPhoneLocation = new LocationData(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
        mLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mLastPhoneLocation = (LocationData) intent.getSerializableExtra(Constants.PHONE_LOCATION);
            }
        };
        speckService.registerReceiver(mLocationReceiver, new IntentFilter(Constants.ACTION_PHONE_LOCATION_BROADCAST));
    }

    synchronized void processAirspeckPacket(byte[] bytes) {
        int packetLength = bytes.length;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        Log.i("AirspeckPacketHandler", "Payload: " + sb.toString());

        if (packetData.position() > 0) {
            packetData.put(bytes);
            if (packetData.position() == 102) {
                Log.i("AirspeckPacketHandler",
                        "Full length packet received from old firmware without CRC byte");
                processDataFromPacket(packetData);
                packetData.clear();
            } else if (packetData.position() == 106) {
                // Set to beginning of CRC value
                packetData.position(104);
                int transmittedCRC = packetData.getInt();
                int calculatedCRC = calculateCRC16(packetData.array());

                if (calculatedCRC == transmittedCRC) {
                    Log.i("AirSpeckPacketHandler",
                            "Full length packet received from new firmware with CRC byte: " + packetData.position());
                    processDataFromPacket(packetData);
                    packetData.clear();
                } else {
                    /*
                     We are probably not at the end of the full packet, but somewhere in between:
                     Normally, the full packet is made up of 5 20 byte packets, and one 6 byte packet:
                     X[20 bytes]Y[20 bytes]Y[20 bytes]Y[20 bytes]Y[20 bytes]Y[4 bytes + 2 bytes CRC]X
                     Instead of the standard position X, we are at one of the Y's.
                     This can occur when the Airspeck temporarily disconnects and packets get dropped. In addition to
                     that, there has to be a "03" byte at Y in order for the packet to be accepted.
                     The correct beginning must be after the 6 byte packet. We can therefore find the real start by
                     first looking at position 6 and then incrementing by 20.
                     */
                    int curPos = 6;
                    while (((ByteBuffer) packetData.position(curPos)).get() != 0x03 && curPos < 106) {
                        curPos += 20;
                    }

                    if (curPos < 106) {
                        // We found another possible start location!
                        removeBytesFromStart(packetData, curPos);

                        String logMessage = String.format(Locale.UK, "Falsely detected beginning of packet. Dropped %d",
                                curPos);
                        Log.i("AirSpeckPacketHandler", logMessage);
                        FileLogger.logToFile(mSpeckService, logMessage);
                    } else {
                        // A bad packet was received. Clear buffer and start receiving new packet.
                        packetData.clear();
                        Log.i("AirSpeckPacketHandler", "CRC check failed. Starting new packet.");
                    }
                }
            } else if (packetData.position() > 102) {
                Log.i("AirSpeckPacketHandler", "Received packet with unexpected length: " + packetData.position());
            }
        } else if (bytes[0] == 0x03) {
            packetData.put(bytes);
        } else {
            Log.i("AirspeckPacketHandler", "Unknown packet received");
        }
    }

    private void removeBytesFromStart(ByteBuffer bf, int n) {
        int index = 0;
        for (int i = n; i < bf.position(); i++) {
            bf.put(index++, bf.get(i));
            bf.put(i, (byte) 0);
        }
        bf.position(index);
    }

    private int calculateCRC16(byte[] byteArray) {
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)

        for (byte b : byteArray) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;
        return crc;
    }

    private void processDataFromPacket(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);
        byte header = buffer.get();
        long uuid = buffer.getLong();

        // Load subject ID from device. Not used at the moment
        byte[] subjectIDBytes = new byte[6];
        buffer.get(subjectIDBytes);
        String loadedSubjectID = new String(subjectIDBytes);
        Log.i("AirspeckPacketHandler", "Received subject ID: " + loadedSubjectID);

        // Timestamp also not used at the moment
        int timestamp = buffer.getInt();

        // Load temperature and humidity
        float temperature = buffer.getShort() * 0.1f;
        float humidity = buffer.getShort() * 0.1f;
        Log.i("AirspeckPacketHandler", "Temperature: " + temperature + " Humidity: " + humidity);

        // Load OPC data
        opcData.put(buffer.array(), buffer.position(), 62);
        buffer.position(buffer.position() + 62);
        processOPCPacket(opcData);
        opcData.clear();

        short batteryLevel = buffer.getShort();
        Log.i("AirspeckPacketHandler", "Battery: " + batteryLevel);

        // Process location
        LocationData location;
        float latitude = buffer.getFloat();
        float longitude = buffer.getFloat();
        short altitude = buffer.getShort();

        // Fallback to phone location if Airspeck GPS is sending zero data.
        if ((latitude == 0f || Float.isNaN(latitude)) && (longitude == 0f || Float.isNaN(longitude))) {
            Log.i("AirspeckPacketHandler", "Airspeck didn't receive GPS, fallback to phone GPS");
            location = mLastPhoneLocation;
        } else {
            Log.i("AirspeckPacketHandler", "Airspeck GPS received: " + latitude + ", " + longitude);
            location = new LocationData(latitude, longitude, altitude, Float.NaN);
        }

        byte lastErrorId = buffer.get();

        short lux = buffer.getShort();
        short motion = buffer.getShort();
        Log.i("AirspeckPacketHandler", "Lux: " + lux + " Motion: " + motion);

        char lastError = buffer.getChar();

        long currentPhoneTimestamp = Utils.getUnixTimestamp();

        AirspeckData newAirspeckData = new AirspeckData(currentPhoneTimestamp, mPm1, mPm2_5, mPm10,
                temperature, humidity, mBins, location, lux, motion, batteryLevel);

        Log.i("AirspeckHandler", "New Airspeck packet processed: " + newAirspeckData);

        // Send data to upload
        Intent intentData = new Intent(Constants.ACTION_AIRSPECK_LIVE_BROADCAST);
        intentData.putExtra(Constants.AIRSPECK_DATA, newAirspeckData);
        mSpeckService.sendBroadcast(intentData);

        // Store the important data in the external storage if set in config
        if (mIsStoreDataLocally) {
            writeToAirspeckFile(newAirspeckData);
        }
    }

    private void processOPCPacket(ByteBuffer buffer) {
        // Log.i("AirspeckPacketHandler", "Processing OPC data: " + buffer.toString());
        buffer.position(0);

        mBins = new int[16];
        for (int i = 0; i < mBins.length; i++) {
            mBins[i] = buffer.getShort() & 0xffff;
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

        mPm1 = buffer.getFloat();
        mPm2_5 = buffer.getFloat();
        mPm10 = buffer.getFloat();
    }

    private void writeToAirspeckFile(AirspeckData airspeckData) {
        // Check whether we are in a new day
        Date now = new Date();
        long currentWriteDay = DateUtils.truncate(now, Calendar.DAY_OF_MONTH).getTime();
        long previousWriteDay = DateUtils.truncate(mDateOfLastAirspeckWrite, Calendar.DAY_OF_MONTH).getTime();
        long numberOfMillisInDay = 1000 * 60 * 60 * 24;

        String filenameAirspeck = Utils.getInstance().getDataDirectory(
                mSpeckService) + Constants.AIRSPECK_DATA_DIRECTORY_NAME + "Airspeck " +
                subjectID + " " + androidID + " " + airspeckUUID.replace(":", "") + " " +
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
                    if (mIsEncryptData) {
                        mAirspeckWriter.append("Encrypted").append("\n");
                    }
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
            // Write Airspeck data together with subjectID. If encryption is enabled, encrypt line
            if (mIsEncryptData) {
                mAirspeckWriter.append(Utils.encrypt(airspeckData.toStringForFile(), mSpeckService)).append("\n");
            } else {
                mAirspeckWriter.append(airspeckData.toStringForFile()).append("\n");
            }
            mAirspeckWriter.flush();
        } catch (IOException e) {
            Log.e("AirspeckPacketHandler", "Error while writing to airspeck file: " + e.getMessage());
        }
    }

    void closeHandler() throws Exception {
        if (mAirspeckWriter != null) {
            Log.i("AirspeckPacketHandler", "Respeck writer was closed");
            mAirspeckWriter.close();
        }
    }
}
