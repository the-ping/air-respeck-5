package com.specknet.airrespeck.models

import com.specknet.airrespeck.utils.Utils
import java.io.Serializable

/**
 * Data class to hold Pulseox data
 */

data class InhalerData(val phoneTimestamp: Long) : Serializable {

    // Returns a one-line representation of data separated by comma used for storage
    fun toStringForFile(): String {
        val builder = StringBuilder()
        builder.append(phoneTimestamp.toString())
        return builder.toString()
    }
}