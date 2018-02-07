package com.specknet.airrespeck.models

import com.specknet.airrespeck.utils.Utils
import java.io.Serializable

/**
 * Data class to hold Airspeck data
 */

data class AirspeckData(val phoneTimestamp: Long, val pm1: Float, val pm2_5: Float, val pm10: Float,
                        val temperature: Float, val humidity: Float, val bins: IntArray, val location: LocationData,
                        val lux: Long, val motion: Long, val battery: Short) : Serializable {
    val binsTotalCount: Int = Utils.sum(bins)

    // Returns a one-line representation of data separated by comma used for storage
    fun toStringForFile(): String {
        val builder = StringBuilder()
        builder.append(phoneTimestamp.toString() + "," + pm1 + "," + pm2_5 + "," + pm10 + "," +
                temperature + "," + humidity + ",")
        bins.forEach { bin -> builder.append(bin.toString() + ",") }
        builder.append(binsTotalCount.toString() + "," + location.longitude + "," + location.latitude + "," +
                location.altitude + "," + location.accuracy + "," + lux + "," + motion + "," + battery)
        return builder.toString()
    }
}