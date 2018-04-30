package com.specknet.airrespeck.services.airspeckuploadservice

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.specknet.airrespeck.models.AirspeckData
import com.specknet.airrespeck.utils.Constants
import com.specknet.airrespeck.utils.Utils
import com.squareup.tape.FileObjectQueue
import com.squareup.tape.SerializedConverter
import org.json.JSONException
import org.json.JSONObject
import rx.Observable
import rx.subjects.PublishSubject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

const val FILENAME = "qoe_upload_queue6"

@SuppressLint("HardwareIds")
class AirspeckRemoteUploadService(bluetoothSpeckService: Service) {
    companion object {
        internal lateinit var filequeue: FileObjectQueue<String>
        private lateinit var jsonHeaders: JsonObject
        private var configUrl = ""
        private var configPath = ""

        protected var mySubject = PublishSubject.create<JsonObject>()
        internal lateinit var airspeckServer: AirspeckServer

    }

    private val airspeckReceiver = AirspeckReceiver()
    var speckService: Service

    private fun jsonArrayFrom(list: List<JsonObject>): JsonArray {
        val jsonArray = JsonArray().asJsonArray
        for (item in list) {
            jsonArray.add(item)
        }
        return jsonArray
    }

    private fun jsonPacketFrom(it: String): JsonObject {
        val jsonData = jsonHeaders
        jsonData.add("data", Gson().fromJson(it, JsonArray::class.java))
        return jsonData
    }

    fun stopUploading() {
        speckService.unregisterReceiver(airspeckReceiver)
        Log.i("AirspeckUpload", "Airspeck upload has been stopped")
    }


    init {
        val utils = Utils.getInstance()
        val loadedConfig = utils.getConfig(bluetoothSpeckService)
        speckService = bluetoothSpeckService

        // Create header json object
        val json = JSONObject()
        try {
            json.put("android_id", Settings.Secure.getString(speckService.contentResolver,
                    Settings.Secure.ANDROID_ID))
            json.put("respeck_uuid", loadedConfig.get(Constants.Config.RESPECK_UUID))
            var qoeuuid = loadedConfig.get(Constants.Config.AIRSPECKP_UUID)
            if (qoeuuid == null) {
                qoeuuid = ""
            }
            json.put("qoe_uuid", qoeuuid)
            json.put("security_key", Utils.getSecurityKey(speckService))
            json.put("patient_id", loadedConfig.get(Constants.Config.SUBJECT_ID))
            json.put("app_version", utils.appVersionCode)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        jsonHeaders = Gson().fromJson(json.toString(), JsonElement::class.java).asJsonObject
        configUrl = Constants.UPLOAD_SERVER_URL
        configPath = Constants.UPLOAD_SERVER_PATH
        airspeckServer = AirspeckServer.create(configUrl)

        speckService.registerReceiver(airspeckReceiver, IntentFilter(Constants.ACTION_AIRSPECK_LIVE_BROADCAST))

        // Setup upload queue which stores data until it can be uploaded
        val queueFile = File(speckService.filesDir, FILENAME)

        try {
            filequeue = FileObjectQueue(queueFile, SerializedConverter<String>())
        } catch (ex: IOException) {
            Log.d("AirspeckUpload", "Airspeck IOException" + ex.toString())
        }

        mySubject.buffer(10, TimeUnit.SECONDS, 500)
                .filter { !it.isEmpty() }
                .map { jsonArrayFrom(it) }
                .subscribe { filequeue.add(it.toString()) }

        Observable.interval(10, TimeUnit.SECONDS)
                .concatMap { Observable.range(0, filequeue.size()) }
                .map { jsonPacketFrom(filequeue.peek()) }
                .concatMap { airspeckServer.submitData(it, configPath) }
                .doOnError { Log.e("AirspeckUpload", "Error on upload Airspeck") }
                .retry()
                .doOnCompleted { }
                .subscribe { Log.d("AirspeckUpload", "Airspeck done: " + it.toString()); filequeue.remove() }
        Log.i("AirspeckUpload", "Airspeck upload service started.")
    }

    class AirspeckReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_AIRSPECK_LIVE_BROADCAST -> {
                    val json = JSONObject()
                    try {
                        json.put("messagetype", "qoe_data")

                        val data = intent.getSerializableExtra(Constants.AIRSPECK_DATA) as AirspeckData
                        json.put("timestamp", data.phoneTimestamp)
                        json.put(Constants.AIRSPECK_PM1, nanToNull(data.pm1))
                        json.put(Constants.AIRSPECK_PM2_5, nanToNull(data.pm2_5))
                        json.put(Constants.AIRSPECK_PM10, nanToNull(data.pm10))
                        json.put("temperature", nanToNull(data.temperature))
                        json.put(Constants.AIRSPECK_HUMIDITY, nanToNull(data.humidity))
                        json.put(Constants.AIRSPECK_BINS_0, data.bins[0])
                        json.put(Constants.AIRSPECK_BINS_1, data.bins[1])
                        json.put(Constants.AIRSPECK_BINS_2, data.bins[2])
                        json.put(Constants.AIRSPECK_BINS_3, data.bins[3])
                        json.put(Constants.AIRSPECK_BINS_4, data.bins[4])
                        json.put(Constants.AIRSPECK_BINS_5, data.bins[5])
                        json.put(Constants.AIRSPECK_BINS_6, data.bins[6])
                        json.put(Constants.AIRSPECK_BINS_7, data.bins[7])
                        json.put(Constants.AIRSPECK_BINS_8, data.bins[8])
                        json.put(Constants.AIRSPECK_BINS_9, data.bins[9])
                        json.put(Constants.AIRSPECK_BINS_10, data.bins[10])
                        json.put(Constants.AIRSPECK_BINS_11, data.bins[11])
                        json.put(Constants.AIRSPECK_BINS_12, data.bins[12])
                        json.put(Constants.AIRSPECK_BINS_13, data.bins[13])
                        json.put(Constants.AIRSPECK_BINS_14, data.bins[14])
                        json.put(Constants.AIRSPECK_BINS_15, data.bins[15])
                        json.put(Constants.AIRSPECK_BINS_TOTAL, data.binsTotalCount)
                        json.put(Constants.LOC_LATITUDE, data.location.latitude)
                        json.put(Constants.LOC_LONGITUDE, data.location.longitude)
                        json.put(Constants.LOC_ALTITUDE, data.location.altitude)
                        json.put(Constants.LOC_ACCURACY, nanToNull(data.location.accuracy))
                        json.put(Constants.AIRSPECK_BATTERY, data.battery)
                        json.put("fw", data.fwVersion)

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    Log.d("AirspeckUpload", "Airspeck upload live data: " + json.toString())
                    mySubject.onNext(Gson().fromJson(json.toString(), JsonElement::class.java).asJsonObject)
                }

                else -> {
                    Log.i("AirspeckUpload", "Airspeck invalid message received")
                }
            }
        }

        private fun nanToNull(value: Float?): Double? {
            if (value == null) {
                return null
            } else if (value.isNaN()) {
                return null
            } else {
                return value.toDouble()
            }
        }

    }
}
