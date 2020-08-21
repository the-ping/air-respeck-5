package com.specknet.airrespeck.utils

import android.content.res.AssetManager
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Classifier(assetManager: AssetManager, modelPath: String, labelPath: String,
                 val windowSize: Int, val numberOfFeatures: Int) {

    private var interpreter: Interpreter
    var labelList: List<String>

    private var FloatSize = 4

    data class Recognition(
            var id: String = "",
            var result: String = "",
            var confidence: Double = 0.0
    ) {
        override fun toString(): String {
            return "Result = $result, Confidence = $confidence"
        }
    }

    init {

        // optional
        val options = Interpreter.Options()
        options.setNumThreads(5)
        options.setUseNNAPI(true)

        // must
        // initialise the interpreter with the loaded tflite model
        // after this we are ready to run inference on new input
        interpreter = Interpreter(loadModelFile(assetManager, modelPath), options)
        labelList = loadLabelList(assetManager, labelPath)
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        // get the file descriptor of the model file
        // use this because we know that the model in uncompressed
        val fileDescriptor = assetManager.openFd(modelPath)

        // open the input stream
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)

        // read the file channels along with its offset and length
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        // load the tflite model
        // this returns a MappedByteBuffer which can be consumed by the interpreter
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabelList(assetManager: AssetManager, labelPath: String): List<String> {
        return assetManager.open(labelPath).bufferedReader().useLines { it.toList() }

    }

    fun classifyActivity(slidingWindow: ArrayList<ArrayList<Float>>): Recognition {
        // something like
        Log.i("Classification", "Received window = " + slidingWindow.toString())
        val byteBuffer = convertWindowToByteBuffer(slidingWindow)
        val rawWindow = convertWindowToTypedArray(slidingWindow, windowSize, numberOfFeatures)
        // run the inference
        val result = Array(1){ FloatArray(labelList.size) }
        interpreter.run(rawWindow, result)

        return getSortedResult(result)
    }

    fun classifyActivityWithFeatures(slidingWindow: ArrayList<ArrayList<Float>>, features: ArrayList<Float>): Recognition {
        Log.i("Classification", "Received window = " + slidingWindow.toString())
        Log.i("Classification", "Received features = " + features.toString())

        val byteBufferWindow = convertWindowToByteBuffer(slidingWindow)
        val byteBufferFeatures = convertFeaturesToByteBuffer(features)
        val inputArray = arrayOf<Any>(byteBufferWindow, byteBufferFeatures)

        val rawWindow = arrayOf(
                convertWindowToRawArray(
                        slidingWindow,
                        windowSize,
                        numberOfFeatures
                )
        )
        val rawFeatures = arrayOf(features.toFloatArray())

        Log.i("SLDW", "New features window = " + rawWindow[0].contentDeepToString())
        Log.i("SLDW", "features = " + Arrays.toString(rawFeatures[0]))

        val rawInputArray = arrayOf<Any>(rawWindow, rawFeatures)
        val result = Array(1){ FloatArray(labelList.size) }
        val resultMap = HashMap<Int, Array<FloatArray>>()
        resultMap.put(0, result)

        if(inputArray.size == 0) {
            Log.i("Classification", "Input array is empty")
        }

        interpreter.runForMultipleInputsOutputs(rawInputArray, resultMap as Map<Int, Any>)

        Log.i("Classification", resultMap.toString())
        return getSortedResult(resultMap[0]!!)

    }

    companion object {
        fun convertWindowToRawArray(slidingWindow: ArrayList<ArrayList<Float>>, windowSize: Int, numberOfFeatures: Int): Array<FloatArray> {
            var resultArray = Array(windowSize) {FloatArray(numberOfFeatures)}

            var i = 0
            slidingWindow.forEach {
                var j = 0
                it.forEach {
                    resultArray[j][i] = it
                    j += 1
                }
                i +=1
            }

            return resultArray
        }

        fun convertWindowToTypedArray(slidingWindow: ArrayList<ArrayList<Float>>, windowSize: Int, numberOfFeatures: Int): Array<FloatArray> {
            val rawWindow = Array(windowSize) {FloatArray(numberOfFeatures)}

            var i = 0
            slidingWindow.forEach {
                val part = it.toFloatArray()
                rawWindow[i] = part
                i += 1
            }

            return rawWindow
        }

        fun convertWindowToFlattenedArray(slidingWindow: ArrayList<ArrayList<Float>>, windowSize: Int, numberOfFeatures: Int):Array<FloatArray> {
            var resultArray = Array(windowSize){FloatArray(numberOfFeatures)}
            val xs = slidingWindow[0]
            val ys = slidingWindow[1]
            val zs = slidingWindow[2]

            var i = 0
            var j = 0
            xs.forEach {
                resultArray[i][j%numberOfFeatures] = it
                j += 1

                if(j!= 0 && j%numberOfFeatures==0) {
                    i+= 1
                }
            }

            ys.forEach {
                resultArray[i][j%numberOfFeatures] = it
                j += 1

                if(j!= 0 && j%numberOfFeatures==0) {
                    i+= 1
                }
            }

            zs.forEach {
                resultArray[i][j%numberOfFeatures] = it
                j += 1

                if(j!= 0 && j%numberOfFeatures==0) {
                    i+= 1
                }
            }

            return resultArray

        }
    }



    private fun convertWindowToByteBuffer(slidingWindow: ArrayList<ArrayList<Float>>): ByteBuffer {
        Log.i("Classification", "Received window in byte buffer = " + slidingWindow)
        val byteBuffer = ByteBuffer.allocateDirect(FloatSize * windowSize * numberOfFeatures)
        byteBuffer.order(ByteOrder.nativeOrder())

        if(numberOfFeatures == 3){
            // Celina's model
            slidingWindow.forEach{
                byteBuffer.putFloat(it[1])
            }

            slidingWindow.forEach{
                byteBuffer.putFloat(it[2])
            }

            slidingWindow.forEach{
                byteBuffer.putFloat(it[3])
            }
        }

        else if(numberOfFeatures == 4) {
            // OG model
            // put the values in the sliding window here
            slidingWindow.forEach{
                byteBuffer.putFloat(it[0])
                byteBuffer.putFloat(it[1])
                byteBuffer.putFloat(it[2])
                byteBuffer.putFloat(it[3])
            }
//            slidingWindow.forEach{
//                byteBuffer.putFloat(it[0])
//            }
//            slidingWindow.forEach{
//                byteBuffer.putFloat(it[1])
//            }
//            slidingWindow.forEach{
//                byteBuffer.putFloat(it[2])
//            }
//            slidingWindow.forEach{
//                byteBuffer.putFloat(it[3])
//            }

        }

        return byteBuffer
    }

    private fun convertFeaturesToByteBuffer(features: ArrayList<Float>): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(FloatSize * features.size)
        byteBuffer.order(ByteOrder.nativeOrder())

        // put the features into the buffer
        features.forEach {
            byteBuffer.putFloat(it)
        }

        return byteBuffer
    }

    private fun getSortedResult(labelProbArray: Array<FloatArray>): Recognition {
        Log.d("Classifier", "List Size:(%d, %d, %d)".format(labelProbArray.size,labelProbArray[0].size,labelList.size))

        val results = labelList
        var max = -1.0
        var max_idx = 0

        Log.i("Classification", "Predictions = (%.4f, %.4f, %.4f)".format(labelProbArray[0][0], labelProbArray[0][1], labelProbArray[0][2]))

        for(i in 0..labelProbArray[0].size-1) {
            if(labelProbArray[0][i] > max) {
                max = labelProbArray[0][i].toDouble()
                max_idx = i
            }
        }

        val finalResult =
                Recognition(
                        "",
                        results[max_idx],
                        max
                )

        return finalResult
    }

}
