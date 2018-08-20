package com.specknet.airrespeck.services.inhaleruploadservice

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
import com.specknet.airrespeck.models.InhalerData
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

const val FILENAME = "inhaler_upload_queue"

//@SuppressLint("HardwareIds")
class InhalerRemoteUploadService(bluetoothSpeckService: Service) {
    companion object {
        internal lateinit var filequeue: FileObjectQueue<String>
        private lateinit var jsonHeaders: JsonObject
        private var configUrl = ""
        private var configPath = ""

        protected var mySubject = PublishSubject.create<JsonObject>()
        internal lateinit var inhalerServer: InhalerServer

    }

    private val inhalerReceiver = InhalerReceiver()
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
        speckService.unregisterReceiver(inhalerReceiver)
        Log.i("InhalerUpload", "Inhaler upload has been stopped")
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
            var inhuuid = loadedConfig.get(Constants.Config.INHALER_UUID)
            if (inhuuid == null) {
                inhuuid = ""
            }
            json.put("inh_uuid", inhuuid)
            json.put("security_key", Utils.getSecurityKey(speckService))
            json.put("patient_id", loadedConfig.get(Constants.Config.SUBJECT_ID))
            json.put("app_version", utils.appVersionCode)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        jsonHeaders = Gson().fromJson(json.toString(), JsonElement::class.java).asJsonObject
        configUrl = Constants.UPLOAD_SERVER_URL
        configPath = Constants.UPLOAD_SERVER_PATH
        inhalerServer = InhalerServer.create(configUrl)

        speckService.registerReceiver(inhalerReceiver, IntentFilter(Constants.ACTION_INHALER_BROADCAST))

        // Setup upload queue which stores data until it can be uploaded
        val queueFile = File(speckService.filesDir, FILENAME)

        try {
            filequeue = FileObjectQueue(queueFile, SerializedConverter<String>())
        } catch (ex: IOException) {
            Log.d("InhalerUpload", "Inhaler IOException" + ex.toString())
        }

        mySubject.buffer(10, TimeUnit.SECONDS, 500)
                .filter { !it.isEmpty() }
                .map { jsonArrayFrom(it) }
                .subscribe { filequeue.add(it.toString()) }

        Observable.interval(10, TimeUnit.SECONDS)
                .concatMap { Observable.range(0, filequeue.size()) }
                .map { jsonPacketFrom(filequeue.peek()) }
                .concatMap { inhalerServer.submitData(it, configPath) }
                .doOnError { Log.e("InhalerUpload", "Error on upload Inhaler") }
                .retry()
                .doOnCompleted { }
                .subscribe { Log.d("InhalerUpload", "Inhaler done: " + it.toString()); filequeue.remove() }
        Log.i("InhalerUpload", "Inhaler upload service started.")
    }

    class InhalerReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_INHALER_BROADCAST -> {
                    val json = JSONObject()
                    try {
                        json.put("messagetype", "pox_data")
                        val data = intent.getSerializableExtra(Constants.INHALER_DATA) as InhalerData
                        json.put("timestamp", data.phoneTimestamp)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    Log.d("InhalerUpload", "Inhaler upload live data: " + json.toString())
                    mySubject.onNext(Gson().fromJson(json.toString(), JsonElement::class.java).asJsonObject)
                }

                else -> {
                    Log.i("InhalerUpload", "Inhaler invalid message received")
                }
            }
        }
    }
}
