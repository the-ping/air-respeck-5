package com.specknet.airrespeck.models

import com.specknet.airrespeck.utils.Utils
import java.io.Serializable

/**
 * Data class to hold Pulseox data
 */

data class PulseoxAveragedData(val timestamp: Long, val pulse: Float, val spo2: Float) : Serializable {

    // Returns a one-line representation of data separated by comma used for storage
    fun toStringForFile(): String {
        val builder = StringBuilder()
        builder.append(phoneTimestamp.toString() + "," + pulse + "," + spo2)
        return builder.toString()
    }
}