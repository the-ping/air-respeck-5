package com.specknet.airrespeck.utils

class SlidingWindow(val windowSize: Int, val step: Int, val numberOfFeatures: Int,
                    val calculateGradient: Boolean) {

    private val slidingWindow = ArrayList<ArrayList<Float>>()
    var isFull = false

    fun addToWindow(values: ArrayList<Float>) {
        if (slidingWindow.size < windowSize) {
            // accumulate values
            slidingWindow.add(values)
        }

        else {
            // conclude window and move one step to the right
            for(i in 1..step) {
                slidingWindow.removeAt(0)
            }

            slidingWindow.add(values)
        }
        isFull = slidingWindow.size == windowSize
    }

    fun getRawWindow(): ArrayList<ArrayList<Float>> {
        return slidingWindow
    }

    fun getGradientWindow(): ArrayList<ArrayList<Float>> {
        var returnWindow = ArrayList<ArrayList<Float>>()

        if (calculateGradient && isFull) {
            // this assumes that a sliding window will be of the form
            // [x1, y1, z1], [x2, y2, z2]....
            // so we sum on each vertical axis

            val prepGradX = arrayListOf<Float>()
            val prepGradY = arrayListOf<Float>()
            val prepGradZ = arrayListOf<Float>()

            slidingWindow.forEach {
                prepGradX.add(it[0])
                prepGradY.add(it[1])
                prepGradZ.add(it[2])
            }

            // calculate gradients
            val gradX = gradient(prepGradX)
            val gradY = gradient(prepGradY)
            val gradZ = gradient(prepGradZ)

            // now add them to a new transformed window
            returnWindow = arrayListOf(gradX, gradY, gradZ)
        }

        return returnWindow
    }

    fun extractStatsFromGrad(): ArrayList<Float> {
        return extractStatFeatures(
                this.getGradientWindow(),
                true
        )
    }

}