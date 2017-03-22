package com.specknet.airrespeck.services.respeckuploadservice


import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log

import com.google.gson.*
import com.specknet.airrespeck.utils.Constants
import com.specknet.airrespeck.utils.Utils
import com.squareup.tape.FileObjectQueue
import com.squareup.tape.SerializedConverter
import org.json.JSONException
import org.json.JSONObject
import rx.subjects.PublishSubject
import rx.Observable

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


class RespeckRemoteUploadService : Service() {
    companion object {
        const val FILENAME = "respeck_upload_queue6"

        internal lateinit var filequeue: FileObjectQueue<String>
        private lateinit var jsonHeaders: JsonObject
        private var configUrl = ""
        private var configPath = ""

        internal var mySubject = PublishSubject.create<JsonObject>()
        internal lateinit var respeckServer: RespeckServer
    }

    internal fun jsonArrayFrom(list: List<JsonObject>): JsonArray {
        val jsonArray = JsonArray().asJsonArray
        for (item in list) {
            jsonArray.add(item)
        }
        return jsonArray
    }

    internal fun jsonPacketFrom(it: String): JsonObject {
        val jsonData = jsonHeaders
        jsonData.add("data", Gson().fromJson(it, JsonArray::class.java))
        return jsonData
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        object : Thread() {
            override fun run() {
                Log.i("SpeckService", "Starting SpeckService...")
                initRespeckUploadService()
            }
        }.start()
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun initRespeckUploadService() {
        val utils = Utils.getInstance(applicationContext)

        // Create header json object
        val json = JSONObject()
        try {
            // TODO: eliminate this redundancy in new database schema
            json.put("bs_name", utils.properties.getProperty(Constants.Config.TABLET_SERIAL))
            json.put("tablet_serial", utils.properties.getProperty(Constants.Config.TABLET_SERIAL))
            json.put("respeck_uuid", utils.properties.getProperty(Constants.Config.RESPECK_UUID))
            json.put("rs_name", utils.properties.getProperty(Constants.Config.RESPECK_UUID))
            json.put("capture_name", utils.properties.getProperty(Constants.Config.RESPECK_KEY))
            json.put("patient_id", utils.properties.getProperty(Constants.Config.PATIENT_ID))
            //json.put("app_version", utils.appVersionCode)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Set server parameters
        jsonHeaders = Gson().fromJson(json.toString(), JsonElement::class.java).asJsonObject
        configUrl = Constants.UPLOAD_SERVER_URL
        configPath = Constants.UPLOAD_SERVER_PATH
        respeckServer = RespeckServer.create(configUrl)

        val respeckReceiver = RespeckReceiver()
        registerReceiver(respeckReceiver, IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST))
        registerReceiver(respeckReceiver, IntentFilter(Constants.ACTION_RESPECK_AVG_BROADCAST))
        registerReceiver(respeckReceiver, IntentFilter(Constants.ACTION_RESPECK_AVG_STORED_BROADCAST))

        // Setup upload queue which stores data until it can be uploaded
        val queueFile = File(filesDir, FILENAME)

        try {
            filequeue = FileObjectQueue(queueFile, SerializedConverter<String>())
        } catch (ex: IOException) {
            Log.d("Upload", "Respeck IOException" + ex.toString())
        }

        mySubject.buffer(10, TimeUnit.SECONDS, 500)
                .filter { !it.isEmpty() }
                .map { jsonArrayFrom(it) }
                .subscribe { filequeue.add(it.toString()) }

        Observable.interval(10, TimeUnit.SECONDS)
                .concatMap { Observable.range(0, filequeue.size()) }
                .map { jsonPacketFrom(filequeue.peek()) }
                .concatMap { respeckServer.submitData(it, configPath) }
                .doOnError { Log.e("Upload", "Respeck: " + it.toString()) }
                .retry()
                .doOnCompleted { }
                .subscribe { Log.d("Upload", "Respeck done: " + it.toString()); filequeue.remove() }
        Log.i("Upload", "Respeck upload service started.")
    }

    class RespeckReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_RESPECK_LIVE_BROADCAST -> {
                    val jsonLiveData = JSONObject()
                    try {
                        jsonLiveData.put("messagetype", "respeck_data")
                        jsonLiveData.put(Constants.RESPECK_SENSOR_TIMESTAMP,
                                intent.getLongExtra(Constants.RESPECK_SENSOR_TIMESTAMP, 0))
                        jsonLiveData.put(Constants.RESPECK_INTERPOLATED_PHONE_TIMESTAMP,
                                intent.getLongExtra(Constants.RESPECK_INTERPOLATED_PHONE_TIMESTAMP, 0))
                        jsonLiveData.put(Constants.RESPECK_X,
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_X, Float.NaN)))
                        jsonLiveData.put(Constants.RESPECK_Y,
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_Y, Float.NaN)))
                        jsonLiveData.put(Constants.RESPECK_Z,
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_Z, Float.NaN)))
                        jsonLiveData.put(Constants.RESPECK_BREATHING_RATE,
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_BREATHING_RATE, Float.NaN)))
                        jsonLiveData.put(Constants.RESPECK_BREATHING_SIGNAL,
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_BREATHING_SIGNAL, Float.NaN)))
                        jsonLiveData.put(Constants.RESPECK_ACTIVITY_LEVEL,
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_ACTIVITY_LEVEL, Float.NaN)))
                        jsonLiveData.put(Constants.RESPECK_ACTIVITY_TYPE,
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_ACTIVITY_TYPE, Float.NaN)))
                        jsonLiveData.put(Constants.RESPECK_SEQUENCE_NUMBER,
                                intent.getIntExtra(Constants.RESPECK_SEQUENCE_NUMBER, 0))
                        jsonLiveData.put(Constants.RESPECK_IS_DISCONNECTED_MODE, 0)

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    // Log.d("RESPECK", "Sent LIVE JSON to upload service: " + json.toString());
                    //Log.i("Upload", "Respeck upload live broadcast data")
                    mySubject.onNext(Gson().fromJson(jsonLiveData.toString(), JsonElement::class.java).asJsonObject)
                }
                Constants.ACTION_RESPECK_AVG_BROADCAST -> {
                    val jsonAverageData = JSONObject()
                    try {
                        jsonAverageData.put("messagetype", "respeck_processed")
                        jsonAverageData.put(Constants.RESPECK_INTERPOLATED_PHONE_TIMESTAMP,
                                intent.getLongExtra(Constants.RESPECK_INTERPOLATED_PHONE_TIMESTAMP, 0))
                        jsonAverageData.put(Constants.RESPECK_MINUTE_AVG_BREATHING_RATE,
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_MINUTE_AVG_BREATHING_RATE, Float.NaN)))
                        jsonAverageData.put(Constants.RESPECK_MINUTE_NUMBER_OF_BREATHS,
                                intent.getIntExtra(Constants.RESPECK_MINUTE_NUMBER_OF_BREATHS, 0))
                        jsonAverageData.put(Constants.RESPECK_MINUTE_STD_BREATHING_RATE,
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_MINUTE_STD_BREATHING_RATE, Float.NaN)))
                        // We don't calculate a minute average activity level at the moment
                        jsonAverageData.put(Constants.RESPECK_ACTIVITY_LEVEL, null)
                        jsonAverageData.put(Constants.RESPECK_IS_DISCONNECTED_MODE, 0)
                        jsonAverageData.put(Constants.RESPECK_IS_VALID_BREATHING_RATE, 1)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    Log.i("Upload", "Respeck upload averaged broadcast data")
                    mySubject.onNext(Gson().fromJson(jsonAverageData.toString(), JsonElement::class.java).asJsonObject)
                }
                Constants.ACTION_RESPECK_AVG_STORED_BROADCAST -> {
                    val jsonAverageStoredData = JSONObject()
                    try {
                        jsonAverageStoredData.put("messagetype", "respeck_processed")
                        jsonAverageStoredData.put(Constants.RESPECK_STORED_SENSOR_TIMESTAMP,
                                intent.getLongExtra(Constants.RESPECK_STORED_SENSOR_TIMESTAMP, 0))
                        jsonAverageStoredData.put(Constants.RESPECK_MINUTE_AVG_BREATHING_RATE,
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_MINUTE_AVG_BREATHING_RATE, Float.NaN)))
                        jsonAverageStoredData.put(Constants.RESPECK_MINUTE_STD_BREATHING_RATE,
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_MINUTE_STD_BREATHING_RATE, Float.NaN)))
                        jsonAverageStoredData.put(Constants.RESPECK_MINUTE_NUMBER_OF_BREATHS,
                                intent.getIntExtra(Constants.RESPECK_MINUTE_NUMBER_OF_BREATHS, 0))
                        // We don't calculate a minute average activity level at the moment
                        jsonAverageStoredData.put(Constants.RESPECK_ACTIVITY_LEVEL, null)
                        jsonAverageStoredData.put(Constants.RESPECK_IS_DISCONNECTED_MODE, 1)
                        jsonAverageStoredData.put(Constants.RESPECK_IS_VALID_BREATHING_RATE, 0)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    Log.i("Upload", "Respeck upload averaged stored broadcast data")
                    mySubject.onNext(Gson().fromJson(jsonAverageStoredData.toString(), JsonElement::class.java).asJsonObject)
                }

                else -> {
                    Log.i("Upload", "Respeck invalid message received")
                }
            }
        }

        private fun nanToNull(value: Float): Double? {
            if (value.isNaN()) {
                return null
            } else {
                return value.toDouble()
            }
        }
    }
}
