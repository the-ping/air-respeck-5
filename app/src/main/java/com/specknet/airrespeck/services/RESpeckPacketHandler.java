package com.specknet.airrespeck.services;

import android.content.Intent;
import android.util.Log;

import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.RESpeckAveragedData;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.models.RESpeckStoredSample;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class processes new RESpeck packets which are passed from the SpeckBluetoothService.
 * It contains all logic to transform the incoming bytes into the desired variables and then stores and broadcasts
 * this information
 */

public class RESpeckPacketHandler {

    // Information about packets
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
    private float mLatestBatteryPercent = 0f;
    private boolean mLatestRequestCharge = false;

    // Minute average breathing stats
    private float mAverageBreathingRate;
    private float mStdDevBreathingRate;
    private int mNumberOfBreaths;

    // Writers
    private OutputStreamWriter mRespeckWriter;
    private OutputStreamWriter mMergedWriter;
    private Date mDateOfLastRESpeckWrite = new Date(0);

    private SpeckBluetoothService mSpeckService;

    private static String RESPECK_UUID;

    private boolean mIsStoreDataLocally;
    private boolean mIsStoreMergedFile;

    private Timer mActivityClassificationTimer;

    public RESpeckPacketHandler() {
        // This is only meant for running tests on the c code!
    }

    public RESpeckPacketHandler(SpeckBluetoothService speckService) {
        mSpeckService = speckService;

        // Initialise stored queue
        storedQueue = new LinkedList<>();

        Utils utils = Utils.getInstance(speckService);

        // Do we want to enable the post-filtering of the breathing signal?
        boolean isPostFilterBreathingSignalEnabled = !Boolean.parseBoolean(
                utils.getProperties().getProperty(
                        Constants.Config.IS_POST_FILTER_BREATHING_SIGNAL_DISABLED));

        RESPECK_UUID = utils.getProperties().getProperty(Constants.Config.RESPECK_UUID);

        // Do we store data locally?
        mIsStoreDataLocally = Boolean.parseBoolean(
                utils.getProperties().getProperty(Constants.Config.IS_STORE_DATA_LOCALLY));

        // Look whether Airspeck is enabled in config
        boolean isAirspeckEnabled = Boolean.parseBoolean(
                utils.getProperties().getProperty(Constants.Config.IS_AIRSPECK_ENABLED));

        // Do we store a merged Airspeck-RESpeck file in addition to the individual files? Only
        // works if Airspeck is enabled
        mIsStoreMergedFile = (Boolean.parseBoolean(
                utils.getProperties().getProperty(Constants.Config.IS_STORE_MERGED_FILE)) && isAirspeckEnabled);

        // Initialize Breathing Functions
        initBreathing(isPostFilterBreathingSignalEnabled, Constants.ACTIVITY_CUTOFF,
                Constants.THRESHOLD_FILTER_SIZE, Constants.MINIMUM_THRESHOLD, Constants.MAXIMUM_THRESHOLD,
                Constants.THRESHOLD_FACTOR);

        // Start task which makes activity predictions every 2 seconds
        startActivityClassificationTask();
    }

    public void processRESpeckLivePacket(final byte[] values) {

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
                    Log.e("RESpeckPacketHandler", "RESpeck: duplicate live timestamp received");
                    return;
                }
                long lastRESpeckTimestamp = mRESpeckTimestampCurrentPacketReceived;
                mRESpeckTimestampCurrentPacketReceived = newRESpeckTimestamp;

                // Log.i("RESpeckPacketHandler", "respeck ts: " + newRESpeckTimestamp);
                // Log.i("RESpeckPacketHandler", "respeck ts diff: " + (newRESpeckTimestamp - lastRESpeckTimestamp));

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

                //Log.i("RESpeckPacketHandler",
                //        "Diff phone respeck: " + (extrapolatedPhoneTimestamp - newRESpeckTimestamp));

                // If the last timestamp plus the average time difference is more than
                // x seconds apart, we use the actual phone timestamp. Otherwise, we use the
                // last plus the average time difference.
                if (Math.abs(extrapolatedPhoneTimestamp - actualPhoneTimestamp) >
                        Constants.MAXIMUM_MILLISECONDS_DEVIATION_ACTUAL_AND_CORRECTED_TIMESTAMP) {
                    // Log.i("RESpeckPacketHandler", "correction!");
                    mPhoneTimestampCurrentPacketReceived = actualPhoneTimestamp;
                } else {
                    // Log.i("RESpeckPacketHandler", "no correction!");
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

                    Log.i("RESpeckPacketHandler", "Stored packet received:");
                    Log.i("RESpeckPacketHandler", "EXTRA_RESPECK_TIMESTAMP_OFFSET_SECS: " + currentTimeOffset);
                    Log.i("RESpeckPacketHandler", "EXTRA_RESPECK_RS_TIMESTAMP: " + s.getRESpeckTimestamp());
                    Log.i("RESpeckPacketHandler", "EXTRA_RESPECK_SEQ: " + s.getSequenceNumber());
                    Log.i("RESpeckPacketHandler", "EXTRA_RESPECK_LIVE_AVE_BR: " + s.getAverageBreathingRate());
                    Log.i("RESpeckPacketHandler", "EXTRA_RESPECK_LIVE_N_BR: " + s.getNumberOfBreaths());
                    Log.i("RESpeckPacketHandler", "EXTRA_RESPECK_LIVE_SD_BR: " + s.getStdBreathingRate());

                    // Send stored data broadcast
                    Intent liveDataIntent = new Intent(Constants.ACTION_RESPECK_AVG_STORED_BROADCAST);
                    liveDataIntent.putExtra(Constants.RESPECK_STORED_TIMESTAMP_OFFSET, currentTimeOffset);
                    liveDataIntent.putExtra(Constants.RESPECK_STORED_SENSOR_TIMESTAMP, s.getRESpeckTimestamp());
                    liveDataIntent.putExtra(Constants.RESPECK_MINUTE_AVG_BREATHING_RATE,
                            s.getAverageBreathingRate());
                    liveDataIntent.putExtra(Constants.RESPECK_MINUTE_STD_BREATHING_RATE,
                            s.getStdBreathingRate());
                    liveDataIntent.putExtra(Constants.RESPECK_MINUTE_NUMBER_OF_BREATHS, s.getNumberOfBreaths());
                    mSpeckService.sendBroadcast(liveDataIntent);
                }
            } else if (startByte == -2 && currentSequenceNumberInBatch >= 0) { //OxFE - accel packet
                // Only do something with data if the timestamps are synchronised
                // Log.v("DF", "Acceleration packet received from RESpeck");
                // If the currentSequenceNumberInBatch is -1, this means we have received acceleration
                // packets before the first timestamp
                if (currentSequenceNumberInBatch == -1) {
                    Log.e("RESpeckPacketHandler", "RESpeck acceleration packet received before timestamp packet");
                    return;
                }

                try {
                    final float x = combineAccelerationBytes(values[i + 1], values[i + 2]);
                    final float y = combineAccelerationBytes(values[i + 3], values[i + 4]);
                    final float z = combineAccelerationBytes(values[i + 5], values[i + 6]);

                    updateBreathing(x, y, z);

                    final float breathingRate = getBreathingRate();
                    final float breathingSignal = getBreathingSignal();
                    final float activityLevel = getActivityLevel();
                    final int activityType = getCurrentActivityClassification();

                    // Calculate interpolated timestamp of current sample based on sequence number
                    // There are 32 samples in each acceleration batch the RESpeck sends.
                    long interpolatedPhoneTimestampOfCurrentSample = (long)
                            ((mPhoneTimestampCurrentPacketReceived - mPhoneTimestampLastPacketReceived) *
                                    (currentSequenceNumberInBatch * 1. /
                                            Constants.NUMBER_OF_SAMPLES_PER_BATCH)) +
                            mPhoneTimestampLastPacketReceived;

                    RESpeckLiveData newRESpeckLiveData = new RESpeckLiveData(RESPECK_UUID,
                            interpolatedPhoneTimestampOfCurrentSample, mRESpeckTimestampCurrentPacketReceived,
                            currentSequenceNumberInBatch, x, y, z, breathingSignal, breathingRate, activityLevel,
                            activityType, mAverageBreathingRate);

                    // Log.i("RESpeckHandler", "New RESpeck data: " + newRESpeckLiveData);

                    // Store the important data in the external storage if set in config
                    if (mIsStoreDataLocally) {
                        writeToRESpeckAndMergedFile(newRESpeckLiveData);
                    }

                    // Send live broadcast intent
                    Intent liveDataIntent = new Intent(Constants.ACTION_RESPECK_LIVE_BROADCAST);
                    liveDataIntent.putExtra(Constants.RESPECK_LIVE_DATA, newRESpeckLiveData);
                    mSpeckService.sendBroadcast(liveDataIntent);

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

                        RESpeckAveragedData avgData = new RESpeckAveragedData(currentProcessedMinute,
                                mAverageBreathingRate, mStdDevBreathingRate, mNumberOfBreaths);


                        // Send average broadcast intent
                        Intent avgDataIntent = new Intent(Constants.ACTION_RESPECK_AVG_BROADCAST);
                        avgDataIntent.putExtra(Constants.RESPECK_AVG_DATA, avgData);
                        mSpeckService.sendBroadcast(avgDataIntent);

                        // Log.i("RESpeckHandler", "Avg data: " + avgData);

                        latestProcessedMinute = currentProcessedMinute;
                    }

                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }

                // Increase sequence number for next incoming sample
                currentSequenceNumberInBatch += 1;
            }
        }
    }

    public void processBatteryLevelCharacteristic(final byte[] values) {
        // Battery packet received which contains the charging level of the battery
        // Currently, this is not working, which is why this method is not called
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
    }

    public void processStoredDataCharacteristic(final byte[] values) {
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

    private void writeToRESpeckAndMergedFile(RESpeckLiveData data) {
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
                    Log.i("RESpeckPacketHandler", "RESpeck data file created with header");
                    // Open new connection to file (which creates file)
                    mRespeckWriter = new OutputStreamWriter(
                            new FileOutputStream(filenameRESpeck, true));

                    mRespeckWriter.append(Constants.RESPECK_DATA_HEADER).append("\n");
                } else {
                    Log.i("RESpeckPacketHandler", "Open existing RESpeck file");
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
                        Log.i("RESpeckPacketHandler", "Merged data file created with header");
                        // Open new connection to new file
                        mMergedWriter = new OutputStreamWriter(
                                new FileOutputStream(filenameMerged, true));
                        mMergedWriter.append(Constants.MERGED_DATA_HEADER).append("\n");
                    } else {
                        Log.i("RESpeckPacketHandler", "Open existing merged file");
                        // Open new connection to new file
                        mMergedWriter = new OutputStreamWriter(
                                new FileOutputStream(filenameMerged, true));
                    }
                }
            } catch (IOException e) {
                Log.e("RESpeckPacketHandler", "Error while creating respeck or merged file: " + e.getMessage());
            }
        }
        mDateOfLastRESpeckWrite = now;

        try {
            // Write new line to file
            mRespeckWriter.append(data.toStringForFile()).append("\n");
            mRespeckWriter.flush();

        } catch (IOException e) {
            Log.e("RESpeckPacketHandler", "Error while writing to respeck file: " + e.getMessage());
        }
        try {
            // If we want to store a merged file of Airspeck and RESpeck data, append the most
            // recent Airspeck data to the current RESpeck data
            if (mIsStoreMergedFile) {
                AirspeckData recentData = mSpeckService.getMostRecentAirspeckReading();
                String airspeckString;
                if (recentData == null) {
                    airspeckString = ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,";
                } else {
                    airspeckString = recentData.toStringForFile();
                }
                mMergedWriter.append(data.toStringForFile()).append(",").append(airspeckString).append("\n");
                mMergedWriter.flush();
            }
        } catch (IOException e) {
            Log.e("RESpeckPacketHandler", "Error while writing to merged file: " + e.getMessage());
        }
    }

    private void startActivityClassificationTask() {
        // How often do we update the activity classification?
        // half the window size for the activity predictions, in milliseconds
        final int delay = 2000;

        mActivityClassificationTimer = new Timer();
        mActivityClassificationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateActivityClassification();
            }
        }, 0, delay);
    }

    public void closeHandler() throws IOException {
        Log.i("RESpeckPacketHandler", "Close handler (i.e. OutputstreamWriters)");
        if (mRespeckWriter != null) {
            mRespeckWriter.flush();
            mRespeckWriter.close();
        }

        if (mMergedWriter != null) {
            mMergedWriter.flush();
            mMergedWriter.close();
        }

        if (mActivityClassificationTimer != null) {
            mActivityClassificationTimer.cancel();
        }
    }

    static {
        System.loadLibrary("respeck-jni");
    }

    // JNI methods
    public native void initBreathing(boolean isPostFilteringEnabled, float activityCutoff, int thresholdFilterSize,
                                     float lowerThresholdLimit, float upperThresholdLimit, float threshold_factor);

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
