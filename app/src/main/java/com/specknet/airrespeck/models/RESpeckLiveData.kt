package com.specknet.airrespeck.models

import java.io.Serializable

/**
 * Class for storing all RESpeck data
 */

data class RESpeckLiveData(val phoneTimestamp: Long, val respeckTimestamp: Long,
                           val sequenceNumberInBatch: Int, val accelX: Float, val accelY: Float, val accelZ: Float,
                           val breathingSignal: Float, val breathingRate: Float, val activityLevel: Float,
                           val activityType: Int, val avgBreathingRate: Float,
                           val minuteStepCount: Int, val frequency: Float = 0.0f, val battLevelval : Int = -1, val chargingStatus: Boolean = false) : Serializable {

    constructor(phoneTimestamp: Long, respeckTimestamp: Long,
                sequenceNumberInBatch: Int, accelX: Float, accelY: Float, accelZ: Float,
                breathingSignal: Float, breathingRate: Float, activityLevel: Float,
                activityType: Int, avgBreathingRate: Float,
                minuteStepCount: Int):
            this(phoneTimestamp, respeckTimestamp, sequenceNumberInBatch, accelX, accelY, accelZ,
            breathingSignal, breathingRate, activityLevel, activityType, avgBreathingRate, minuteStepCount, 0.0f, -1, false)

    constructor(phoneTimestamp: Long, respeckTimestamp: Long,
                sequenceNumberInBatch: Int, accelX: Float, accelY: Float, accelZ: Float,
                breathingSignal: Float, breathingRate: Float, activityLevel: Float,
                activityType: Int, avgBreathingRate: Float,
                minuteStepCount: Int, frequency: Float):
            this(phoneTimestamp, respeckTimestamp, sequenceNumberInBatch, accelX, accelY, accelZ,
                    breathingSignal, breathingRate, activityLevel, activityType, avgBreathingRate, minuteStepCount, frequency, -1, false)

//    constructor(phoneTimestamp: Long, respeckTimestamp: Long,
//                sequenceNumberInBatch: Int, accelX: Float, accelY: Float, accelZ: Float,
//                breathingSignal: Float, breathingRate: Float, activityLevel: Float,
//                activityType: Int, avgBreathingRate: Float,
//                minuteStepCount: Int, frequency: Float):
//            this(phoneTimestamp, respeckTimestamp, sequenceNumberInBatch, accelX, accelY, accelZ,
//                    breathingSignal, breathingRate, activityLevel, activityType, avgBreathingRate, minuteStepCount, frequency)

                    // Returns a one-line representation of data separated by comma used for storage
    fun toStringForFile(): String {
        return phoneTimestamp.toString() + "," + respeckTimestamp + "," + sequenceNumberInBatch + "," + accelX +
                "," + accelY + "," + accelZ + "," + breathingSignal + "," + breathingRate + "," + activityLevel +
                "," + activityType
    }
}