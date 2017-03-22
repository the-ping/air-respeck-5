package com.specknet.airrespeck.services.qoeuploadservice

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
import java.util.HashMap
import java.util.concurrent.TimeUnit

class QOERemoteUploadService : Service() {
    companion object {
        const val FILENAME = "qoe_upload_queue6"

        internal lateinit var filequeue: FileObjectQueue<String>
        private lateinit var jsonHeaders: JsonObject
        private var configUrl = ""
        private var configPath = ""

        internal var mySubject = PublishSubject.create<JsonObject>()
        internal lateinit var qoeServer: QOEServer
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
        initQOEUploadService()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun initQOEUploadService() {
        val utils = Utils.getInstance(applicationContext)

        // Create header json object
        val json = JSONObject()
        try {
            json.put("patient_id", utils.properties.getProperty(Constants.Config.PATIENT_ID))
            var qoeuuid = utils.properties.getProperty(Constants.Config.QOEUUID)
            if (qoeuuid == null) {
                qoeuuid = ""
            }
            json.put("airspeck_uuid", qoeuuid)
            json.put("tablet_serial", utils.properties.getProperty(Constants.Config.TABLET_SERIAL))
            json.put("app_version", utils.appVersionCode)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        jsonHeaders = Gson().fromJson(json.toString(), JsonElement::class.java).asJsonObject
        configUrl = Constants.UPLOAD_SERVER_URL
        configPath = Constants.UPLOAD_SERVER_PATH
        qoeServer = QOEServer.create(configUrl)

        val qoeReceiver = QOEReceiver()
        registerReceiver(qoeReceiver, IntentFilter(Constants.ACTION_AIRSPECK_LIVE_BROADCAST))

        // Setup upload queue which stores data until it can be uploaded
        val queueFile = File(filesDir, FILENAME)

        try {
            filequeue = FileObjectQueue(queueFile, SerializedConverter<String>())
        } catch (ex: IOException) {
            Log.d("Upload", "Airspeck IOException" + ex.toString())
        }

        mySubject.buffer(10, TimeUnit.SECONDS, 500)
                .filter { !it.isEmpty() }
                .map { jsonArrayFrom(it) }
                .subscribe { filequeue.add(it.toString()) }

        Observable.interval(10, TimeUnit.SECONDS)
                .concatMap { Observable.range(0, filequeue.size()) }
                .map { jsonPacketFrom(filequeue.peek()) }
                .concatMap { qoeServer.submitData(it, configPath) }
                .doOnError { Log.e("Upload", "Airspeck: " + it.toString()) }
                .retry()
                .doOnCompleted { }
                .subscribe { Log.d("Upload", "Airspeck done: " + it.toString()); filequeue.remove() }
        Log.i("Upload", "Airspeck upload service started.")
    }

    class QOEReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_AIRSPECK_LIVE_BROADCAST -> {
                    val json = JSONObject()
                    try {
                        json.put("messagetype", "qoe_data")
                        json.put(Constants.QOE_TIMESTAMP,
                                intent.getLongExtra(Constants.QOE_TIMESTAMP, 0))

                        val readings = intent.getSerializableExtra(Constants.AIRSPECK_ALL_MEASURES)
                                as HashMap<String, Float>

                        json.put(Constants.QOE_PM1, nanToNull(readings[Constants.QOE_PM1]))
                        json.put(Constants.QOE_PM2_5, nanToNull(readings[Constants.QOE_PM2_5]))
                        json.put(Constants.QOE_PM10, nanToNull(readings[Constants.QOE_PM10]))
                        json.put(Constants.QOE_TEMPERATURE, nanToNull(readings[Constants.QOE_TEMPERATURE]))
                        json.put(Constants.QOE_HUMIDITY, nanToNull(readings[Constants.QOE_HUMIDITY]))
                        json.put(Constants.QOE_S1ae_NO2, nanToNull(readings[Constants.QOE_S1ae_NO2]))
                        json.put(Constants.QOE_S1we_NO2, nanToNull(readings[Constants.QOE_S1we_NO2]))
                        json.put(Constants.QOE_S2ae_O3, nanToNull(readings[Constants.QOE_S2ae_O3]))
                        json.put(Constants.QOE_S2we_O3, nanToNull(readings[Constants.QOE_S2we_O3]))
                        json.put(Constants.QOE_BINS_0, nanToNull(readings[Constants.QOE_BINS_0]))
                        json.put(Constants.QOE_BINS_1, nanToNull(readings[Constants.QOE_BINS_1]))
                        json.put(Constants.QOE_BINS_2, nanToNull(readings[Constants.QOE_BINS_2]))
                        json.put(Constants.QOE_BINS_3, nanToNull(readings[Constants.QOE_BINS_3]))
                        json.put(Constants.QOE_BINS_4, nanToNull(readings[Constants.QOE_BINS_4]))
                        json.put(Constants.QOE_BINS_5, nanToNull(readings[Constants.QOE_BINS_5]))
                        json.put(Constants.QOE_BINS_6, nanToNull(readings[Constants.QOE_BINS_6]))
                        json.put(Constants.QOE_BINS_7, nanToNull(readings[Constants.QOE_BINS_7]))
                        json.put(Constants.QOE_BINS_8, nanToNull(readings[Constants.QOE_BINS_8]))
                        json.put(Constants.QOE_BINS_9, nanToNull(readings[Constants.QOE_BINS_9]))
                        json.put(Constants.QOE_BINS_10, nanToNull(readings[Constants.QOE_BINS_10]))
                        json.put(Constants.QOE_BINS_11, nanToNull(readings[Constants.QOE_BINS_11]))
                        json.put(Constants.QOE_BINS_12, nanToNull(readings[Constants.QOE_BINS_12]))
                        json.put(Constants.QOE_BINS_13, nanToNull(readings[Constants.QOE_BINS_13]))
                        json.put(Constants.QOE_BINS_14, nanToNull(readings[Constants.QOE_BINS_14]))
                        json.put(Constants.QOE_BINS_15, nanToNull(readings[Constants.QOE_BINS_15]))
                        json.put(Constants.QOE_BINS_TOTAL, nanToNull(readings[Constants.QOE_BINS_TOTAL]))
                        json.put(Constants.LOC_LATITUDE, nanToNull(readings[Constants.LOC_LATITUDE]))
                        json.put(Constants.LOC_LONGITUDE, nanToNull(readings[Constants.LOC_LONGITUDE]))
                        json.put(Constants.LOC_ALTITUDE, nanToNull(readings[Constants.LOC_ALTITUDE]))

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    Log.d("Upload", "Airspeck upload live data: " + json.toString())
                    mySubject.onNext(Gson().fromJson(json.toString(), JsonElement::class.java).asJsonObject)
                }

                else -> {
                    Log.i("Upload", "Airspeck invalid message received")
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
