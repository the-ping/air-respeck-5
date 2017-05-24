package com.specknet.airrespeck.models

import java.io.Serializable

/**
 * Class for storing all RESpeck data
 */

data class RESpeckLiveData(val uuid: String, val phoneTimestamp: Long, val respeckTimestamp: Long,
                           val sequenceNumberInBatch: Int, val accelX: Float, val accelY: Float, val accelZ: Float,
                           val breathingSignal: Float, val breathingRate: Float, val activityLevel: Float,
                           val activityType: Int, val avgBreathingRate: Float) : Serializable {

    // Returns a one-line representation of data separated by comma used for storage
    fun toStringForFile(): String {
        return uuid + "," + phoneTimestamp + "," + respeckTimestamp + "," + sequenceNumberInBatch + "," + accelX +
                "," + accelY + "," + accelZ + "," + breathingSignal + "," + breathingRate + "," + activityLevel +
                "," + activityType;
    }
}