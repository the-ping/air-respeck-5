package com.specknet.airrespeck.services;

import android.content.Intent;
import android.provider.Settings.Secure;
import android.util.Log;

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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Collections;

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
    private long lastProcessedMinute = 0L;
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
    private ArrayList<Float> lastMinuteActivityLevel = new ArrayList<>();
    private ArrayList<Integer> lastMinuteActivityType = new ArrayList<>();

    // frequency estimation
    private ArrayList<Long> frequencyTimestamps = new ArrayList<>();
    private ArrayList<Float> minuteFrequency = new ArrayList<>();
    private ArrayList<Float> rollingMedianFrequency = new ArrayList<>();
    private float mSamplingFrequency = Constants.SAMPLING_FREQUENCY;
    

    // Writers
    private OutputStreamWriter mRespeckWriter;
    private Date mDateOfLastRESpeckWrite = new Date(0);

    private SpeckBluetoothService mSpeckService;

    private static String RESPECK_UUID;

    private boolean mIsStoreDataLocally;
    private String patientID;
    private String androidID;

    private boolean mIsEncryptData;

    public RESpeckPacketHandler() {
        // This is only meant for running tests on the c code!
    }

    public RESpeckPacketHandler(SpeckBluetoothService speckService) {
        mSpeckService = speckService;

        // Initialise stored queue
        storedQueue = new LinkedList<>();

        Utils utils = Utils.getInstance();
        Map<String, String> loadedConfig = utils.getConfig(mSpeckService);

        // Do we want to enable the post-filtering of the breathing signal?
        boolean isPostFilterBreathingSignalEnabled = !Boolean.parseBoolean(
                loadedConfig.get(Constants.Config.DISABLE_POST_FILTERING_BREATHING));

        RESPECK_UUID = loadedConfig.get(Constants.Config.RESPECK_UUID);

        mIsStoreDataLocally = Boolean.parseBoolean(loadedConfig.get(Constants.Config.STORE_DATA_LOCALLY));

        mIsEncryptData = Boolean.parseBoolean(loadedConfig.get(Constants.Config.ENCRYPT_LOCAL_DATA));

        patientID = loadedConfig.get(Constants.Config.SUBJECT_ID);
        androidID = Secure.getString(speckService.getContentResolver(), Secure.ANDROID_ID);

        // TODO maybe calculate here what the true sampling frequency has been over the last minute
        // TODO darius said that the manual processing takes each minute separately and calculates the
        // true sampling rate from there

        // Initialize Breathing Functions
        initBreathing(isPostFilterBreathingSignalEnabled, Constants.ACTIVITY_CUTOFF, Constants.THRESHOLD_FILTER_SIZE,
                Constants.MINIMUM_THRESHOLD, Constants.MAXIMUM_THRESHOLD, Constants.THRESHOLD_FACTOR, mSamplingFrequency
                );
    }

    void processRESpeckLivePacket(final byte[] values) {
        // First look at the length to determine which packet format is in use
        if (values.length < 192) {
            // RESpeck v2
            processRESpeckV2Packet(values);
        } else {
            processRESpeckV4Packet(values);
        }
    }

    void processRESpeckV2Packet(final byte[] values) {
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
                mRESpeckTimestampCurrentPacketReceived = newRESpeckTimestamp;

                // Log.i("RESpeckPacketHandler", "respeck ts: " + newRESpeckTimestamp);

                // Independent of the RESpeck timestamp, we use the phone timestamp
                long actualPhoneTimestamp = Utils.getUnixTimestamp();

                // If this is our first sequence, or the last sequence was more than 2.5 times the average time
                // difference in the past, we use the typical time difference between the RESpeck packets for
                // determining the previous timestamp. This only affects the minute calculations. The breathing rate
                // is calculated based on only the sampling rate.
                if (mPhoneTimestampCurrentPacketReceived == -1 || mPhoneTimestampCurrentPacketReceived + 2.5 * Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS < actualPhoneTimestamp) {
                    mPhoneTimestampLastPacketReceived = actualPhoneTimestamp - Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS;
                } else {
                    // Store the previously used phone timestamp as previous timestamp
                    mPhoneTimestampLastPacketReceived = mPhoneTimestampCurrentPacketReceived;
                }

                long extrapolatedPhoneTimestamp = mPhoneTimestampLastPacketReceived + Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS;

                //Log.i("RESpeckPacketHandler",
                //        "Diff phone respeck: " + (extrapolatedPhoneTimestamp - newRESpeckTimestamp));

                // If the last timestamp plus the average time difference is more than
                // x seconds apart, we use the actual phone timestamp. Otherwise, we use the
                // last plus the average time difference.
                if (Math.abs(
                        extrapolatedPhoneTimestamp - actualPhoneTimestamp) > Constants.MAXIMUM_MILLISECONDS_DEVIATION_ACTUAL_AND_CORRECTED_TIMESTAMP) {
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
                    /*
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
                    */
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

                    final float breathingSignal = getBreathingSignal();
                    final float activityLevel = getActivityLevel();
                    final int activityType = getActivityClassification();
                    final float breathingRate = getBreathingRate();
                    resetBreathingRate();

                    // Store activity level and type for minute average
                    lastMinuteActivityLevel.add(activityLevel);
                    lastMinuteActivityType.add(activityType);

                    // Calculate interpolated timestamp of current sample based on sequence number
                    // There are 32 samples in each acceleration batch the RESpeck sends.
                    long interpolatedPhoneTimestampOfCurrentSample = (long) ((mPhoneTimestampCurrentPacketReceived - mPhoneTimestampLastPacketReceived) * (currentSequenceNumberInBatch * 1. / Constants.NUMBER_OF_SAMPLES_PER_BATCH)) + mPhoneTimestampLastPacketReceived;

                    RESpeckLiveData newRESpeckLiveData = new RESpeckLiveData(interpolatedPhoneTimestampOfCurrentSample,
                            mRESpeckTimestampCurrentPacketReceived, currentSequenceNumberInBatch, x, y, z,
                            breathingSignal, breathingRate, activityLevel, activityType, mAverageBreathingRate,
                            getMinuteStepcount());

                    // Log.i("RESpeckPacketHandler", "New RESpeck data: " + newRESpeckLiveData);

                    // Store the important data in the external storage if set in config
                    if (mIsStoreDataLocally) {
                        writeToRESpeck(newRESpeckLiveData);
                    }

                    // Send live broadcast intent
                    Intent liveDataIntent = new Intent(Constants.ACTION_RESPECK_LIVE_BROADCAST);
                    liveDataIntent.putExtra(Constants.RESPECK_LIVE_DATA, newRESpeckLiveData);
                    mSpeckService.sendBroadcast(liveDataIntent);

                    // Every full minute, calculate the average breathing rate in that minute. This value will
                    // only change after a call to "calculateAverageBreathing".
                    long currentProcessedMinute = DateUtils.truncate(new Date(mPhoneTimestampCurrentPacketReceived),
                            Calendar.MINUTE).getTime();

                    // Set last processed minute to current one if this is the first packet
                    if (lastProcessedMinute == 0L) {
                        lastProcessedMinute = currentProcessedMinute;
                    }

                    if (currentProcessedMinute != lastProcessedMinute) {
                        calculateAverageBreathing();

                        mAverageBreathingRate = getAverageBreathingRate();
                        float stdDevBreathingRate = getStdDevBreathingRate();
                        int numberOfBreaths = getNumberOfBreaths();
                        int stepCountC = getMinuteStepcount();

                        // Reset the minute step count
                        resetMinuteStepcount();

                        // Empty the minute average window
                        resetMedianAverageBreathing();

                        // Get activity level and type
                        float meanActivityLevel = Utils.mean(lastMinuteActivityLevel);
                        int modeActivityType = Utils.mode(lastMinuteActivityType);

                        // Reset last minute values
                        lastMinuteActivityLevel = new ArrayList<>();
                        lastMinuteActivityType = new ArrayList<>();

                        RESpeckAveragedData avgData = new RESpeckAveragedData(lastProcessedMinute,
                                mAverageBreathingRate, stdDevBreathingRate, numberOfBreaths, meanActivityLevel,
                                modeActivityType, stepCountC, mSpeckService.getRESpeckFwVersion());

                        // Send average broadcast intent
                        Intent avgDataIntent = new Intent(Constants.ACTION_RESPECK_AVG_BROADCAST);
                        avgDataIntent.putExtra(Constants.RESPECK_AVG_DATA, avgData);
                        mSpeckService.sendBroadcast(avgDataIntent);

                        Log.i("RESpeckPacketHandler", "Avg data: " + avgData);

                        lastProcessedMinute = currentProcessedMinute;
                    }

                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }

                // Increase sequence number for next incoming sample
                currentSequenceNumberInBatch += 1;
            }
        }
    }

    void processRESpeckV4Packet(final byte[] values) {
        //get the respeck timestamp
        /*
        // Read timestamp from packet
        Byte ts_1 = values[0];
        Byte ts_2 = values[1];
        Byte ts_3 = values[2];
        Byte ts_4 = values[3];

        long uncorrectedRESpeckTimestamp = combineTimestampBytes(ts_1, ts_2, ts_3, ts_4);
        // Convert the timestamp of the RESpeck to correspond to milliseconds
        long newRESpeckTimestamp = (long) (uncorrectedRESpeckTimestamp * 197 / 32768. * 1000);
*/
        byte[] time_array = {values[4], values[5], values[6], values[7], values[0], values[1], values[2], values[3]};
        // and try ByteBuffer:
        ByteBuffer buffer = ByteBuffer.wrap(time_array);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);
        long uncorrectedRESpeckTimestamp = buffer.getLong();
        long newRESpeckTimestamp = uncorrectedRESpeckTimestamp / 1000000;

        Log.i("RESpeckPacketHandler", "rsts: " + Long.toString(newRESpeckTimestamp / 1000));

        // Independent of the RESpeck timestamp, we use the phone timestamp
        final long actualPhoneTimestamp = Utils.getUnixTimestamp();

        // If this is our first sequence, or the last sequence was more than 2.5 times the average time
        // difference in the past, we use the typical time difference between the RESpeck packets for
        // determining the previous timestamp. This only affects the minute calculations. The breathing rate
        // is calculated based on only the sampling rate.
        if (mPhoneTimestampCurrentPacketReceived == -1 || mPhoneTimestampCurrentPacketReceived + 2.5 * Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS < actualPhoneTimestamp) {
            mPhoneTimestampLastPacketReceived = actualPhoneTimestamp - Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS;
        } else {
            // Store the previously used phone timestamp as previous timestamp
            mPhoneTimestampLastPacketReceived = mPhoneTimestampCurrentPacketReceived;
        }

        long extrapolatedPhoneTimestamp = mPhoneTimestampLastPacketReceived + Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS;

        //Log.i("RESpeckPacketHandler",
        //        "Diff phone respeck: " + (extrapolatedPhoneTimestamp - newRESpeckTimestamp));

        // If the last timestamp plus the average time difference is more than
        // x seconds apart, we use the actual phone timestamp. Otherwise, we use the
        // last plus the average time difference.
        if (Math.abs(
                extrapolatedPhoneTimestamp - actualPhoneTimestamp) > Constants.MAXIMUM_MILLISECONDS_DEVIATION_ACTUAL_AND_CORRECTED_TIMESTAMP) {
            // Log.i("RESpeckPacketHandler", "correction!");
            mPhoneTimestampCurrentPacketReceived = actualPhoneTimestamp;
        } else {
            // Log.i("RESpeckPacketHandler", "no correction!");
            mPhoneTimestampCurrentPacketReceived = extrapolatedPhoneTimestamp;
        }

        currentSequenceNumberInBatch = 0;


        for (int i = 8; i < values.length; i += 6) {
            final float x = combineAccelerationBytes(values[i + 0], values[i + 1]);
            final float y = combineAccelerationBytes(values[i + 2], values[i + 3]);
            final float z = combineAccelerationBytes(values[i + 4], values[i + 5]);

            updateBreathing(x, y, z);

            final float breathingSignal = getBreathingSignal();
            final float activityLevel = getActivityLevel();
            final int activityType = getActivityClassification();
            final float breathingRate = getBreathingRate();
            resetBreathingRate(); // TODO question - why is this here?
            // this sets the current breathing rate to NaN so the next time we call
            // getBreathingRate we will get NaN?


            // Store activity level and type for minute average
            lastMinuteActivityLevel.add(activityLevel);
            lastMinuteActivityType.add(activityType);

            // Calculate interpolated timestamp of current sample based on sequence number
            // There are 32 samples in each acceleration batch the RESpeck sends.
            long interpolatedPhoneTimestampOfCurrentSample = (long) ((mPhoneTimestampCurrentPacketReceived - mPhoneTimestampLastPacketReceived) * (currentSequenceNumberInBatch * 1. / Constants.NUMBER_OF_SAMPLES_PER_BATCH)) + mPhoneTimestampLastPacketReceived;

            // Store the timestamps for frequency estimation
            frequencyTimestamps.add(interpolatedPhoneTimestampOfCurrentSample);

            // check for the full minute before creating the live data package
            // Also calculate the approximation of the true sampling frequency
            long currentProcessedMinute = DateUtils.truncate(new Date(mPhoneTimestampCurrentPacketReceived),
                    Calendar.MINUTE).getTime();

            RESpeckLiveData newRESpeckLiveData = new RESpeckLiveData(interpolatedPhoneTimestampOfCurrentSample,
                    newRESpeckTimestamp, currentSequenceNumberInBatch, x, y, z, breathingSignal, breathingRate,
                    activityLevel, activityType, mAverageBreathingRate, getMinuteStepcount());

            if (currentProcessedMinute != lastProcessedMinute) {

                if (minuteFrequency.size() < Constants.MINUTES_FOR_MEDIAN_CALC) {
                    Log.i("Freq", "One minute passed, calculating frequency");
                    // calculate an approximation of the sampling frequency
                    // and add it to a list for running median
                    mSamplingFrequency = calculateSamplingFrequency();
                    minuteFrequency.add(mSamplingFrequency);

                    Collections.sort(minuteFrequency);
                    float medianFrequency;

                    if (minuteFrequency.size() % 2 == 0) {
                        //Average 2 middles values
                        medianFrequency = (minuteFrequency.get(minuteFrequency.size()/2) + minuteFrequency.get(minuteFrequency.size()/2 - 1)) / 2;
                    }
                    else {
                        //Take middle value
                        medianFrequency = (minuteFrequency.get(minuteFrequency.size()/2));
                    }

                    Log.i("Freq", "medianFrequency = " + medianFrequency);
                    if (medianFrequency > 10 && medianFrequency < 15) {
                        updateSamplingFrequency(medianFrequency);
                    }
                }
                //After this, just stick to final mSamplingFrequency calculated.

                // modify the respeck packet here
                newRESpeckLiveData = new RESpeckLiveData(interpolatedPhoneTimestampOfCurrentSample,
                        newRESpeckTimestamp, currentSequenceNumberInBatch, x, y, z, breathingSignal, breathingRate,
                        activityLevel, activityType, mAverageBreathingRate, getMinuteStepcount(), mSamplingFrequency);


                Log.i("Freq", "newRespeckLiveData = " + newRESpeckLiveData);
            }

            // Log.i("RESpeckPacketHandler", "New RESpeck data: " + newRESpeckLiveData);

            // Store the important data in the external storage if set in config
            if (mIsStoreDataLocally) {
                writeToRESpeck(newRESpeckLiveData);
            }

            // Test: send live broadcast intent with strings and floats (for rehab app)
            Intent liveDataIntentTest = new Intent(Constants.ACTION_RESPECK_REHAB_BROADCAST);

            liveDataIntentTest.putExtra(Constants.RESPECK_REHAB_DATA, "testString");
            liveDataIntentTest.putExtra(Constants.EXTRA_RESPECK_LIVE_BR, breathingRate);
            liveDataIntentTest.putExtra(Constants.EXTRA_RESPECK_LIVE_BS, breathingSignal);
            liveDataIntentTest.putExtra(Constants.EXTRA_RESPECK_LIVE_X, x);
            liveDataIntentTest.putExtra(Constants.EXTRA_RESPECK_LIVE_Y, y);
            liveDataIntentTest.putExtra(Constants.EXTRA_RESPECK_LIVE_Z, z);
            liveDataIntentTest.putExtra(Constants.EXTRA_RESPECK_BS_TIMESTAMP, interpolatedPhoneTimestampOfCurrentSample);
            liveDataIntentTest.putExtra(Constants.EXTRA_RESPECK_RS_TIMESTAMP, newRESpeckTimestamp);
            liveDataIntentTest.putExtra(Constants.EXTRA_RESPECK_LIVE_ACTIVITY, activityLevel);
            liveDataIntentTest.putExtra(Constants.EXTRA_RESPECK_LIVE_ACTIVITY_TYPE, activityType);
            mSpeckService.sendBroadcast(liveDataIntentTest);

            // Send live broadcast intent
            Intent liveDataIntent = new Intent(Constants.ACTION_RESPECK_LIVE_BROADCAST);
            liveDataIntent.putExtra(Constants.RESPECK_LIVE_DATA, newRESpeckLiveData);
            mSpeckService.sendBroadcast(liveDataIntent);

            // Every full minute, calculate the average breathing rate in that minute. This value will
            // only change after a call to "calculateAverageBreathing".
            currentProcessedMinute = DateUtils.truncate(new Date(mPhoneTimestampCurrentPacketReceived),
                    Calendar.MINUTE).getTime();
            if (currentProcessedMinute != lastProcessedMinute) {

                calculateAverageBreathing();

                mAverageBreathingRate = getAverageBreathingRate();
                float stdDevBreathingRate = getStdDevBreathingRate();
                int numberOfBreaths = getNumberOfBreaths();
                int stepCountC = getMinuteStepcount();

                // Reset the minute step count
                resetMinuteStepcount();

                // Empty the minute average window
                resetMedianAverageBreathing();

                // Get activity level and type
                float meanActivityLevel = Utils.mean(lastMinuteActivityLevel);
                int modeActivityType = Utils.mode(lastMinuteActivityType);

                // Reset last minute values
                lastMinuteActivityLevel = new ArrayList<>();
                lastMinuteActivityType = new ArrayList<>();

                RESpeckAveragedData avgData = new RESpeckAveragedData(currentProcessedMinute, mAverageBreathingRate,
                        stdDevBreathingRate, numberOfBreaths, meanActivityLevel, modeActivityType, stepCountC,
                        mSpeckService.getRESpeckFwVersion());

                // Send average broadcast intent
                Intent avgDataIntent = new Intent(Constants.ACTION_RESPECK_AVG_BROADCAST);
                avgDataIntent.putExtra(Constants.RESPECK_AVG_DATA, avgData);
                mSpeckService.sendBroadcast(avgDataIntent);

                Log.i("RESpeckPacketHandler", "Avg data: " + avgData);

                lastProcessedMinute = currentProcessedMinute;
            }

            currentSequenceNumberInBatch += 1;
        }
    }

    public void processBatteryLevelCharacteristic(final byte[] values) {
        // Battery packet received which contains the charging level of the battery
        // Currently, this is not working, which is why this method is not called
        int batteryLevel = combineBatteryBytes(values[0], values[1]);

        // Log.i("RAT", "BATTERY LEVEL notification received: " + Integer.toString(battLevel));

        int chargePercentage = (100 * (batteryLevel - BATTERY_EMPTY_LEVEL) / (BATTERY_FULL_LEVEL - BATTERY_EMPTY_LEVEL));

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
                    float sdBreathingRate = (float) Math.sqrt((float) (values[i + 3] & 0xFF) / 10.0f);

                    Byte upperActivityLevel = values[i + 4];
                    Byte lowerActivityLevel = values[i + 5];
                    float combinedActivityLevel = combineActivityLevelBytes(upperActivityLevel, lowerActivityLevel);

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

    private void writeToRESpeck(RESpeckLiveData data) {
        // Check whether we are in a new day
        Date now = new Date();
        long currentWriteDay = DateUtils.truncate(now, Calendar.DAY_OF_MONTH).getTime();
        long previousWriteDay = DateUtils.truncate(mDateOfLastRESpeckWrite, Calendar.DAY_OF_MONTH).getTime();
        long numberOfMillisInDay = 1000 * 60 * 60 * 24;

        String fw_version = mSpeckService.getRESpeckFwVersion();

        String filenameRESpeck = Utils.getInstance().getDataDirectory(
                mSpeckService) + Constants.RESPECK_DATA_DIRECTORY_NAME + "RESpeck " + patientID + " " + androidID + " " + RESPECK_UUID.replace(
                ":", "") + "(" + fw_version + ") " + new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(now) + ".csv";

        // If the file doesn't exist, or we are in a new day, create a new file
        if (!new File(
                filenameRESpeck).exists() || currentWriteDay != previousWriteDay || now.getTime() - mDateOfLastRESpeckWrite.getTime() > numberOfMillisInDay) {
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
                    mRespeckWriter = new OutputStreamWriter(new FileOutputStream(filenameRESpeck, true));
                    mRespeckWriter.append(Constants.RESPECK_DATA_HEADER + "\n");
                } else {
                    Log.i("RESpeckPacketHandler", "Open existing RESpeck file");
                    mRespeckWriter = new OutputStreamWriter(new FileOutputStream(filenameRESpeck, true));
                }
            } catch (IOException e) {
                Log.e("RESpeckPacketHandler", "Error while creating respeck or merged file: " + e.getMessage());
            }
        }
        mDateOfLastRESpeckWrite = now;

        try {
            // Write new line to file. If concatenation is split up with append, the second part might not be written,
            // meaning that there will be a line without a line break in the file.
            mRespeckWriter.append(data.toStringForFile() + "\n");
            mRespeckWriter.flush();

        } catch (IOException e) {
            Log.e("RESpeckPacketHandler", "Error while writing to respeck file: " + e.getMessage());
        }
    }

    void closeHandler() throws IOException {
        Log.i("RESpeckPacketHandler", "Close handler (i.e. OutputstreamWriters)");
        if (mRespeckWriter != null) {
            mRespeckWriter.flush();
            mRespeckWriter.close();
        }
    }

    private float calculateSamplingFrequency() {
        if(frequencyTimestamps.size() <= 1) {
            return 0;
        }
        long first_ts = frequencyTimestamps.get(0);
        long last_ts = frequencyTimestamps.get(frequencyTimestamps.size() - 1);

        float samplingFrequency = ((frequencyTimestamps.size() * 1.f) / (last_ts - first_ts)) * 1000.f;
        Log.i("Freq", "samplingFrequency = " + samplingFrequency);

        // clear the frequency array
        frequencyTimestamps.clear();

        return samplingFrequency;
    }

    static {
        System.loadLibrary("respeck-jni");
    }

    // JNI methods
    public native void initBreathing(boolean isPostFilteringEnabled, float activityCutoff, int thresholdFilterSize,
                                     float lowerThresholdLimit, float upperThresholdLimit, float thresholdFactor,
                                     float samplingFrequency);

    public native int getMinuteStepcount();

    public native void resetMinuteStepcount();

    public native void updateBreathing(float x, float y, float z);

    public native float getBreathingSignal();

    public native float getBreathingRate();

    public native void resetBreathingRate();

    public native boolean getIsBreathEnd();

    public native float getAverageBreathingRate();

    public native float getActivityLevel();

    public native int getNumberOfBreaths();

    public native float getBreathingAngle();

    public native void resetMedianAverageBreathing();

    public native void calculateAverageBreathing();

    public native float getStdDevBreathingRate();

    public native int getActivityClassification();

    public native float getUpperThreshold();

    public native float getLowerThreshold();

    public native void updateSamplingFrequency(float sampling_frequency);
}
