package com.specknet.airrespeck.services.respeckuploadservice


import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.provider.Settings
import android.util.Log

import com.google.gson.*
import com.specknet.airrespeck.R
import com.specknet.airrespeck.activities.MainActivity
import com.specknet.airrespeck.models.RESpeckAveragedData
import com.specknet.airrespeck.models.RESpeckLiveData
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


class RespeckAndDiaryRemoteUploadService : Service() {
    companion object {
        const val FILENAME = "respeck_upload_queue6"

        internal lateinit var filequeue: FileObjectQueue<String>
        private lateinit var jsonHeaders: JsonObject
        private var configUrl = Constants.UPLOAD_SERVER_URL
        private var configPath = Constants.UPLOAD_SERVER_PATH

        internal var mySubject = PublishSubject.create<JsonObject>()
        internal lateinit var respeckServer: RespeckServer
        internal val respeckReceiver = RespeckReceiver()
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
                Log.i("Upload", "Starting RESpeck upload...")
                startInForeground()
                initRespeckUploadService()
            }
        }.start()
        return Service.START_STICKY
    }

    private fun startInForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = Notification.Builder(this)
                .setContentTitle(getText(R.string.notification_respeck_upload_title))
                .setContentText(getText(R.string.notification_respeck_upload_text))
                .setSmallIcon(R.drawable.vec_wireless)
                .setContentIntent(pendingIntent)
                .build()

        // Just use a "random" service ID
        val SERVICE_NOTIFICATION_ID = 89347238
        startForeground(SERVICE_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        unregisterReceiver(respeckReceiver)
        super.onDestroy()
        Log.i("Upload", "RESpeck upload has been stopped")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun initRespeckUploadService() {
        val utils = Utils.getInstance(applicationContext)

        // Create header json object
        val jsonHeader = JSONObject()
        try {
            jsonHeader.put("android_id", Settings.Secure.getString(contentResolver,
                    Settings.Secure.ANDROID_ID))
            jsonHeader.put("respeck_uuid", utils.properties.getProperty(Constants.Config.RESPECK_UUID))
            var qoeuuid = utils.properties.getProperty(Constants.Config.AIRSPECK_UUID)
            if (qoeuuid == null) {
                qoeuuid = ""
            }
            jsonHeader.put("qoe_uuid", qoeuuid)
            jsonHeader.put("security_key", utils.properties.getProperty(Constants.Config.RESPECK_KEY))
            jsonHeader.put("patient_id", utils.properties.getProperty(Constants.Config.PATIENT_ID))
            jsonHeader.put("app_version", utils.appVersionCode)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        respeckServer = RespeckServer.create(configUrl)

        // Set server parameters
        jsonHeaders = Gson().fromJson(jsonHeader.toString(), JsonElement::class.java).asJsonObject

        registerReceiver(respeckReceiver, IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST))
        registerReceiver(respeckReceiver, IntentFilter(Constants.ACTION_RESPECK_AVG_BROADCAST))
        registerReceiver(respeckReceiver, IntentFilter(Constants.ACTION_RESPECK_AVG_STORED_BROADCAST))
        registerReceiver(respeckReceiver, IntentFilter(Constants.ACTION_DIARY_BROADCAST))

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

        Observable.interval(9, TimeUnit.SECONDS)
                .concatMap { Observable.range(0, filequeue.size()) }
                .map { jsonPacketFrom(filequeue.peek()) }
                .concatMap { respeckServer.submitData(it, configPath) }
                .doOnError { Log.e("Upload", "Respeck: " + it.toString()) }
                .retry()
                .doOnCompleted { }
                .subscribe {
                    Log.d("Upload", "Respeck upload subscribe return: " + it.toString())
                    Log.d("Upload", "Queue size: " + filequeue.size())
                    filequeue.remove()
                }

        Log.i("Upload", "Respeck upload service started.")
    }

    class RespeckReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_RESPECK_LIVE_BROADCAST -> {
                    /*
                    val jsonLiveData = JSONObject()
                    try {
                        val data = intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                        jsonLiveData.put("messagetype", "respeck_data")
                        jsonLiveData.put(Constants.RESPECK_SENSOR_TIMESTAMP, data.respeckTimestamp)
                        jsonLiveData.put(Constants.RESPECK_SEQUENCE_NUMBER, data.sequenceNumberInBatch)
                        // The upload server expects time in seconds without milliseconds
                        jsonLiveData.put(Constants.INTERPOLATED_PHONE_TIMESTAMP,
                                Math.round((data.phoneTimestamp / 1000).toDouble()))
                        jsonLiveData.put(Constants.RESPECK_X, nanToNull(data.accelX))
                        jsonLiveData.put(Constants.RESPECK_Y, nanToNull(data.accelY))
                        jsonLiveData.put(Constants.RESPECK_Z, nanToNull(data.accelZ))
                        jsonLiveData.put(Constants.RESPECK_BREATHING_RATE, nanToNull(data.breathingRate))
                        jsonLiveData.put(Constants.RESPECK_BREATHING_SIGNAL, nanToNull(data.breathingSignal))
                        jsonLiveData.put(Constants.RESPECK_ACTIVITY_LEVEL, nanToNull(data.activityLevel))
                        jsonLiveData.put(Constants.RESPECK_ACTIVITY_TYPE, data.activityType)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    // Log.d("RESPECK", "Sent LIVE JSON to upload service: " + json.toString());
                    Log.i("Upload", "Respeck upload live broadcast data")
                    mySubject.onNext(Gson().fromJson(jsonLiveData.toString(), JsonElement::class.java).asJsonObject)
                    */
                }
                Constants.ACTION_RESPECK_AVG_BROADCAST -> {
                    val jsonAverageData = JSONObject()
                    try {
                        val data = intent.getSerializableExtra(Constants.RESPECK_AVG_DATA) as RESpeckAveragedData
                        jsonAverageData.put("messagetype", "respeck_processed")
                        jsonAverageData.put("timestamp", data.timestamp)
                        jsonAverageData.put("breathing_rate", nanToNull(data.avgBreathingRate))
                        jsonAverageData.put("sd_br", nanToNull(data.stdBreathingRate))
                        jsonAverageData.put("n_breaths", data.numberOfBreaths)
                        jsonAverageData.put("act_level", nanToNull(data.activityLevel))
                        jsonAverageData.put("act_type", data.activityType)
                        jsonAverageData.put("step_count", data.minuteStepCount)
                        jsonAverageData.put("stored", 0)
                        jsonAverageData.put("valid", 1)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    Log.i("Upload", "Respeck upload averaged broadcast data")
                    mySubject.onNext(Gson().fromJson(jsonAverageData.toString(), JsonElement::class.java).asJsonObject)
                }
                Constants.ACTION_RESPECK_AVG_STORED_BROADCAST -> {
                    val jsonAverageStoredData = JSONObject()
                    try {
                        /*
                        TODO: Modify when stored mode works
                        jsonAverageStoredData.put("messagetype", "respeck_processed")
                        jsonAverageStoredData.put("timestamp",
                                intent.getLongExtra(Constants.RESPECK_STORED_SENSOR_TIMESTAMP, 0))
                        jsonAverageStoredData.put(Constants.RESPECK_MINUTE_AVG_BREATHING_RATE,
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_MINUTE_AVG_BREATHING_RATE, Float.NaN)))
                        jsonAverageStoredData.put("sd_br",
                                nanToNull(intent.getFloatExtra(Constants.RESPECK_MINUTE_STD_BREATHING_RATE, Float.NaN)))
                        jsonAverageStoredData.put(Constants.RESPECK_MINUTE_NUMBER_OF_BREATHS,
                                intent.getIntExtra(Constants.RESPECK_MINUTE_NUMBER_OF_BREATHS, 0))
                        // We don't calculate a minute average activity level at the moment
                        jsonAverageStoredData.put(Constants.RESPECK_ACTIVITY_LEVEL, null)
                        jsonAverageStoredData.put(Constants.RESPECK_IS_DISCONNECTED_MODE, 1)
                        jsonAverageStoredData.put(Constants.RESPECK_IS_VALID_BREATHING_RATE, 0)
                        */
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    Log.i("Upload", "Respeck upload averaged stored broadcast data")
                    //mySubject.onNext(Gson().fromJson(jsonAverageStoredData.toString(), JsonElement::class.java).asJsonObject)
                }
                Constants.ACTION_DIARY_BROADCAST -> {
                    val diaryString = intent.getSerializableExtra(Constants.DIARY_DATA) as String
                    val diaryGson = Gson().fromJson(diaryString, JsonElement::class.java).asJsonObject
                    diaryGson.addProperty("messagetype", "diary")
                    Log.i("Upload", "Diary upload")
                    mySubject.onNext(diaryGson)
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
