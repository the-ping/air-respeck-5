package com.specknet.airrespeck.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.util.Log;

import com.specknet.airrespeck.models.AirspeckData;
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

public class PulseoxPacketHandler {

    // Pulseox data
    private ByteBuffer packetData;

    // Initial values for last write timestamps
    private Date mDateOfLastPulseoxWrite = new Date(0);

    private OutputStreamWriter mPulseoxWriter;

    private static String pulseoxUUID;

    private SpeckBluetoothService mSpeckService;

    private BroadcastReceiver mLocationReceiver;

    private boolean mIsStoreDataLocally;
    private boolean mIsEncryptData;
    private String subjectID;
    private String androidID;

    public PulseoxPacketHandler(SpeckBluetoothService speckService) {
        mSpeckService = speckService;

        packetData = ByteBuffer.allocate(128); // A little larger than necessary in case we add fields to the packet

        Utils utils = Utils.getInstance();
        Map<String, String> loadedConfig = utils.getConfig(mSpeckService);
        pulseoxUUID = "00:11:22:33:44:55";

        mIsStoreDataLocally = Boolean.parseBoolean(loadedConfig.get(Constants.Config.STORE_DATA_LOCALLY));

        mIsEncryptData = Boolean.parseBoolean(loadedConfig.get(Constants.Config.ENCRYPT_LOCAL_DATA));

        subjectID = loadedConfig.get(Constants.Config.SUBJECT_ID);
        androidID = Settings.Secure.getString(speckService.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    synchronized void processPulseoxPacket(byte[] bytes) {
        Log.i("PulseoxPacketHandler", "Processing Pulseox packet");
        long currentPhoneTimestamp = Utils.getUnixTimestamp();

        int pulse = bytes[9];
        Log.i("RAT", "Pulse:" + Integer.toString(pulse));

        int sat = bytes[7];
        Log.i("RAT", "Sat:" + Integer.toString(sat));

        PulseoxData newPulseoxData = new PulseoxData(currentPhoneTimestamp, pulse, sat);

        Log.i("PulseoxHandler", "New Pulseox packet processed: " + newPulseoxData);

        // Send data to upload
        Intent intentData = new Intent(Constants.ACTION_PULSEOX_BROADCAST);
        intentData.putExtra(Constants.PULSEOX_DATA, newPulseoxData);
        mSpeckService.sendBroadcast(intentData);

        // Store the important data in the external storage if set in config
        if (mIsStoreDataLocally) {
            writeToPulseoxFile(newPulseoxData);
        }
    }


    private void writeToPulseoxFile(PulseoxData pulseoxData) {
        // Check whether we are in a new day
        Date now = new Date();
        long currentWriteDay = DateUtils.truncate(now, Calendar.DAY_OF_MONTH).getTime();
        long previousWriteDay = DateUtils.truncate(mDateOfLastPulseoxWrite, Calendar.DAY_OF_MONTH).getTime();
        long numberOfMillisInDay = 1000 * 60 * 60 * 24;

        String filenamePulseox = Utils.getInstance().getDataDirectory(
                mSpeckService) + Constants.PULSEOX_DATA_DIRECTORY_NAME + "Pulseox " +
                subjectID + " " + androidID + " " + pulseoxUUID.replace(":", "")  +
                new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(now) +
                ".csv";

        // If we are in a new day, create a new file if necessary
        if (!new File(filenamePulseox).exists() || currentWriteDay != previousWriteDay ||
                now.getTime() - mDateOfLastPulseoxWrite.getTime() > numberOfMillisInDay) {
            try {
                // Close old connection if there was one
                if (mPulseoxWriter != null) {
                    mPulseoxWriter.close();
                }

                // The file could already exist if we just started the app. If not, add the header
                if (!new File(filenamePulseox).exists()) {
                    Log.i("PulseoxkPacketHandler", "Pulseox data file created with header");
                    // Open new connection to new file
                    mPulseoxWriter = new OutputStreamWriter(
                            new FileOutputStream(filenamePulseox, true));
                    if (mIsEncryptData) {
                        mPulseoxWriter.append("Encrypted").append("\n");
                    }
                    mPulseoxWriter.append(Constants.PULSEOX_DATA_HEADER).append("\n");
                    mPulseoxWriter.flush();
                } else {
                    // Open new connection to new file
                    mPulseoxWriter = new OutputStreamWriter(
                            new FileOutputStream(filenamePulseox, true));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mDateOfLastPulseoxWrite = now;

        // Write new line to file
        try {
            // Write Pulseox data together with subjectID. If encryption is enabled, encrypt line
            if (mIsEncryptData) {
                mPulseoxWriter.append(Utils.encrypt(pulseoxData.toStringForFile(), mSpeckService)).append("\n");
            } else {
                mPulseoxWriter.append(pulseoxData.toStringForFile()).append("\n");
            }
            mPulseoxWriter.flush();
        } catch (IOException e) {
            Log.e("PulseoxPacketHandler", "Error while writing to pulseox file: " + e.getMessage());
        }
    }

    void closeHandler() throws Exception {
        if (mPulseoxWriter != null) {
            Log.i("PulseoxPacketHandler", "Pulseox writer was closed");
            mPulseoxWriter.close();
        }
    }
}
