package com.specknet.airrespeck.models

import java.io.Serializable

/**
 * Class to hold averaged RESpeck data
 */

data class RESpeckAveragedData(val timestamp: Long, val avgBreathingRate: Float, val stdBreathingRate: Float,
                               val numberOfBreaths: Int, val activityLevel: Float, val activityType: Int,
                               val minuteStepCount: Int, val fwVersion: String) : Serializable