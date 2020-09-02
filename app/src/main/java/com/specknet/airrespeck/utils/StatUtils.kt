package com.specknet.airrespeck.utils

import android.util.Log
import kotlin.math.pow

fun gradient(arr: ArrayList<Float>): ArrayList<Float> {
    if(arr.size < 2) {
        Log.e("Gradient", "Received empty or unit array!")
        return arrayListOf<Float>()
    }

    var resGrad = ArrayList<Float>()

    for(i in 0..arr.size - 1) {
        if (i == 0) {
            resGrad.add((arr[i+1] - arr[i])/1)
        }
        else if (i == arr.size-1) {
            resGrad.add((arr[i] - arr[i-1])/1)
        }
        else {
            resGrad.add((arr[i+1] - arr[i-1])/2)
        }
    }

    return resGrad
}

fun windowMean(arr: ArrayList<Float>): Float {
    return arr.average().toFloat()
}


fun windowStd(arr: ArrayList<Float>): Float {
    val mean = windowMean(arr).toDouble()
    var deviation = arrayListOf<Float>()
    for (i in 0..arr.size-1) {
        deviation.add(Math.abs(arr[i] - mean).pow(2).toFloat())
    }
    val std = Math.sqrt(
            windowMean(
                    deviation
            ).toDouble()).toFloat()

    return std
}

fun absMean(arr: ArrayList<Float>): Float {
    val mean = windowMean(arr)
    val new_arr = ArrayList<Float>()

    for (i in 0..arr.size-1) {
        new_arr.add(Math.abs(arr[i] - mean))
    }

    return new_arr.average().toFloat()
}

fun histogram(arr: ArrayList<Float>, n_bins: Int, normalise: Boolean = true): ArrayList<Float> {
    var min = arr.min()
    var max = arr.max()

    // expand empty range to avoid divide by zero
    if (min != null && max != null) {
        if(min == max) {
            min -= 0.5f
            max += 0.5f
        }
    }

    var curr_bin = 0

    val sorted_arr = arr.sorted()

    val bin_edges = linspace(
            min!!,
            max!!,
            n_bins + 1,
            true
    )

    val bin_counts = arrayListOf<Float>()
    for(i in 0..n_bins-1) {
        bin_counts.add(0f)
    }

    for(i in 0..sorted_arr.size - 1) {

        while (curr_bin < n_bins) {
            val curr_elem = sorted_arr[i]

            if(curr_elem < bin_edges[curr_bin + 1]) {
                bin_counts[curr_bin] += 1f
                break
            }
            else {
                curr_bin += 1
            }

        }

        if (curr_bin == n_bins) {
            // we are at the last bin so we should check for equality
            val curr_elem = sorted_arr[i]

            if (curr_elem <= bin_edges[curr_bin]) {
                bin_counts[curr_bin - 1] += 1f
                break
            }
        }

    }

    if(normalise) {
        val windowSize = arr.size

        for (i in 0..bin_counts.size-1) {
            bin_counts[i] /= windowSize*1f
        }
    }

    return bin_counts
}

fun linspace(start: Float, stop: Float, num: Int, endpoint: Boolean = true): ArrayList<Float> {
    /*
    Returns evenly spaced numbers over a specified interval.
     */

    if(num < 0) {
        println("Number of samples must be non-negative")
    }
    var div = 0
    if (endpoint) div = num - 1
    else div = num

    val delta = stop - start
    val y = arrayListOf<Float>()
    for(i in 0..num-1) {
        y.add(i.toFloat())
    }

    val step = delta / div

    if(step == 0f) {
        for (i in 0..y.size-1) {
            y[i] = y[i] / div
            y[i] = y[i] * delta + start
        }
    }
    else {
        for (i in 0..y.size-1) {
            y[i] = y[i] * step + start
        }
    }

    y[y.size - 1] = stop

    return y
}

fun extractStatFeatures(windowData: ArrayList<ArrayList<Float>>, normalise: Boolean = true): ArrayList<Float> {
    /*
    Given a window of data where each column represents an axis, extracts:
    - mean
    - standard deviation
    - abs mean
    - histograms (10 bins)
    for each axis.
     */

    // first calculate how much space we need to extract the features
    // this will be numAxes * 13 (1 number for mean, std and abs mean and 10 numbers for the histogram)
    val numFeatures = windowData.size * 13
    val features = arrayListOf<Float>()

    // initialise the features array
    for (i in 0..numFeatures - 1) {
        features.add(0f)
    }

    for (axis in 0..windowData.size-1) {
        val axisMean =
                windowMean(windowData[axis])
        val axisStd =
                windowStd(windowData[axis])
        val axisAbsMean =
                absMean(windowData[axis])

        val axisHist = histogram(
                windowData[axis],
                10,
                normalise
        )

        // add there to the appropriate places
        features.set(axis, axisMean)
        features.set(axis + 1*3, axisStd)
        features.set(axis + 2*3, axisAbsMean)

        for (i in 0..axisHist.size-1) {
            features.set(axis*10+3*3+i, axisHist[i])
        }
    }

    return features
}