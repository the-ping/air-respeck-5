package com.specknet.airrespeck.models

/**
 * Data class for storing Airspeck measurements which will be displayed on a map.
 */

import com.google.android.gms.maps.model.LatLng

data class AirspeckMapData(val location: LatLng, val pm1: Float, val pm2_5: Float, val pm10: Float);