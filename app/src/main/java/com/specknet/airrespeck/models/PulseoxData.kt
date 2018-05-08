package com.specknet.airrespeck.models

import com.specknet.airrespeck.utils.Utils
import java.io.Serializable

/**
 * Data class to hold Pulseox data
 */

data class PulseoxData(val phoneTimestamp: Long, val pulse: Int, val spo2: Int) : Serializable {

    // Returns a one-line representation of data separated by comma used for storage
    fun toStringForFile(): String {
        val builder = StringBuilder()
        builder.append(phoneTimestamp.toString() + "," + pulse + "," + spo2)
        return builder.toString()
    }
}