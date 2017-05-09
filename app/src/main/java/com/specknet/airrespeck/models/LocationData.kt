package com.specknet.airrespeck.models

import java.io.Serializable

/**
 * Data class for storing longitude, latitude and altitude
 */

data class LocationData(val latitude: Double, val longitude: Double, val altitude: Double) : Serializable