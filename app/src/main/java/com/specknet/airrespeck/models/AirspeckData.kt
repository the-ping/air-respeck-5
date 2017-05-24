package com.specknet.airrespeck.models

import com.specknet.airrespeck.utils.Utils
import java.io.Serializable

/**
 * Data class to hold Airspeck data
 */

data class AirspeckData(val uuid: String, val phoneTimestamp: Long, val pm1: Float, val pm2_5: Float, val pm10: Float,
                        val temperature: Float, val humidity: Float, val no2we: Float,val no2ae: Float, val o3we: Float,
                        val o3ae: Float,val bins: IntArray, val location: LocationData) : Serializable {
    val binsTotalCount: Int = Utils.sum(bins)

    // Returns a one-line representation of data separated by comma used for storage
    fun toStringForFile(): String {
        val builder = StringBuilder()
        builder.append(uuid + "," + phoneTimestamp + "," + pm1 + "," + pm2_5 + "," + pm10 + "," +
                temperature + "," + humidity + "," + no2we + "," + no2ae + "," + o3we + "," + o3ae + ",")
        bins.forEach { bin -> builder.append(bin.toString() + ",") }
        builder.append(binsTotalCount.toString() + "," + location.longitude + "," + location.latitude + "," +
                location.altitude)
        return builder.toString()
    }
}