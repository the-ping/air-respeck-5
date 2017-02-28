package com.specknet.airrespeck.models

/**
 * Class for storing RESpeck readings for displaying in the breathing graphs in the SupervisedRESpeckReadingsFragment
 */

data class BreathingGraphData(val timestamp: Float, val accelX: Float,
                              val accelY: Float, val accelZ: Float, val breathingSignal: Float)