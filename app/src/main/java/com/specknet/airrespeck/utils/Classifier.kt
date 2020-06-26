package com.specknet.airrespeck.utils

import android.content.res.AssetManager
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.collections.ArrayList

class Classifier(assetManager: AssetManager, modelPath: String, labelPath: String,
                 private val windowSize: Int, private val numberOfFeatures: Int) {

    private var interpreter: Interpreter
    private var labelList: List<String>

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
        val byteBuffer = convertWindowToByteBuffer(slidingWindow)

        // run the inference
        val result = Array(1){ FloatArray(labelList.size) }
        interpreter.run(byteBuffer, result)

        return getSortedResult(result)
    }

    private fun convertWindowToByteBuffer(slidingWindow: ArrayList<ArrayList<Float>>): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(FloatSize * windowSize * numberOfFeatures)
        byteBuffer.order(ByteOrder.nativeOrder())

        // put the values in the sliding window here
        slidingWindow.forEach{
            byteBuffer.putFloat(it[0])
            byteBuffer.putFloat(it[1])
            byteBuffer.putFloat(it[2])
            byteBuffer.putFloat(it[3])
        }

//        slidingWindow.forEach{
//            byteBuffer.putFloat(it[1])
//        }
//
//        slidingWindow.forEach{
//            byteBuffer.putFloat(it[2])
//        }
//
//        slidingWindow.forEach{
//            byteBuffer.putFloat(it[3])
//        }

        return byteBuffer
    }

    private fun getSortedResult(labelProbArray: Array<FloatArray>): Recognition {
        Log.d("Classifier", "List Size:(%d, %d, %d)".format(labelProbArray.size,labelProbArray[0].size,labelList.size))

        val results = listOf<String>("Coughing", "Non-coughing", "Movement")

        var max = -1.0
        var max_idx = 0

        Log.i("Classification", "Predictions = (%.4f, %.4f, %.4f)".format(labelProbArray[0][0], labelProbArray[0][1], labelProbArray[0][2]))

        for(i in 0..labelProbArray[0].size-1) {
            if(labelProbArray[0][i] > max) {
                max = labelProbArray[0][i].toDouble()
                max_idx = i
            }
        }

        val finalResult = Recognition("", results[max_idx], max)

        return finalResult
    }

}
