package com.specknet.airrespeck.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.util.Log;

import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.InhalerData;
import com.specknet.airrespeck.models.LocationData;
import com.specknet.airrespeck.models.PulseoxData;
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * This class processes new Airspeck packets which are passed from the SpeckBluetoothService.
 * It contains all logic to transform the incoming bytes into the desired variables and then stores and broadcasts
 * this information
 */

public class InhalerPacketHandler {

    // Inhaler data
    private ByteBuffer packetData;

    // Initial values for last write timestamps
    private Date mDateOfLastInhalerWrite = new Date(0);

    private OutputStreamWriter mInhalerWriter;

    private static String inhalerUUID;

    private SpeckBluetoothService mSpeckService;

    private BroadcastReceiver mLocationReceiver;

    private boolean mIsStoreDataLocally;
    private boolean mIsEncryptData;
    private String subjectID;
    private String androidID;

    public InhalerPacketHandler(SpeckBluetoothService speckService) {
        mSpeckService = speckService;

        packetData = ByteBuffer.allocate(128); // A little larger than necessary in case we add fields to the packet

        Utils utils = Utils.getInstance();
        Map<String, String> loadedConfig = utils.getConfig(mSpeckService);
        inhalerUUID = loadedConfig.get(Constants.Config.INHALER_UUID);

        mIsStoreDataLocally = Boolean.parseBoolean(loadedConfig.get(Constants.Config.STORE_DATA_LOCALLY));

        mIsEncryptData = Boolean.parseBoolean(loadedConfig.get(Constants.Config.ENCRYPT_LOCAL_DATA));

        subjectID = loadedConfig.get(Constants.Config.SUBJECT_ID);
        androidID = Settings.Secure.getString(speckService.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    synchronized void processInhalerPacket(byte[] bytes) {
        Log.i("InhalerPacketHandler", "Processing Inhaler packet");
        long currentPhoneTimestamp = Utils.getUnixTimestamp();

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        Log.i("InhalerPacketHandler", "Payload: " + sb.toString());

        if (bytes[0] == 0x00) {
            // Only respond to button press. Ignore release.

            InhalerData newInhalerData = new InhalerData(currentPhoneTimestamp);
            Log.i("InhalerHandler", "New Inhaler packet processed: " + newInhalerData);

            // Send data to upload
            Intent intentData = new Intent(Constants.ACTION_INHALER_BROADCAST);
            intentData.putExtra(Constants.INHALER_DATA, newInhalerData);
            mSpeckService.sendBroadcast(intentData);

            // Store the important data in the external storage if set in config
            if (mIsStoreDataLocally) {
                writeToInhalerFile(newInhalerData);
            }
        }
    }


    private void writeToInhalerFile(InhalerData inhalerData) {
        // Check whether we are in a new day
        Date now = new Date();
        long currentWriteDay = DateUtils.truncate(now, Calendar.DAY_OF_MONTH).getTime();
        long previousWriteDay = DateUtils.truncate(mDateOfLastInhalerWrite, Calendar.DAY_OF_MONTH).getTime();
        long numberOfMillisInDay = 1000 * 60 * 60 * 24;

        String filenameInhaler = Utils.getInstance().getDataDirectory(
                mSpeckService) + Constants.INHALER_DATA_DIRECTORY_NAME + "Inhaler " +
                subjectID + " " + androidID + " " + inhalerUUID.replace(":", "")  +
                " " + new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(now) +
                ".csv";

        // If we are in a new day, create a new file if necessary
        if (!new File(filenameInhaler).exists() || currentWriteDay != previousWriteDay ||
                now.getTime() - mDateOfLastInhalerWrite.getTime() > numberOfMillisInDay) {
            try {
                // Close old connection if there was one
                if (mInhalerWriter != null) {
                    mInhalerWriter.close();
                }

                // The file could already exist if we just started the app. If not, add the header
                if (!new File(filenameInhaler).exists()) {
                    Log.i("InhalerPacketHandler", "Inhaler data file created with header");
                    // Open new connection to new file
                    mInhalerWriter = new OutputStreamWriter(
                            new FileOutputStream(filenameInhaler, true));
                    if (mIsEncryptData) {
                        mInhalerWriter.append("Encrypted").append("\n");
                    }
                    mInhalerWriter.append(Constants.INHALER_DATA_HEADER).append("\n");
                    mInhalerWriter.flush();
                } else {
                    // Open new connection to new file
                    mInhalerWriter = new OutputStreamWriter(
                            new FileOutputStream(filenameInhaler, true));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mDateOfLastInhalerWrite = now;

        // Write new line to file
        try {
            // Write Inhaler data together with subjectID. If encryption is enabled, encrypt line
            if (mIsEncryptData) {
                mInhalerWriter.append(Utils.encrypt(inhalerData.toStringForFile(), mSpeckService)).append("\n");
            } else {
                mInhalerWriter.append(inhalerData.toStringForFile()).append("\n");
            }
            mInhalerWriter.flush();
        } catch (IOException e) {
            Log.e("InhalerPacketHandler", "Error while writing to inhaler file: " + e.getMessage());
        }
    }

    void closeHandler() throws Exception {
        if (mInhalerWriter != null) {
            Log.i("InhalerPacketHandler", "Inhaler writer was closed");
            mInhalerWriter.close();
        }
    }
}
