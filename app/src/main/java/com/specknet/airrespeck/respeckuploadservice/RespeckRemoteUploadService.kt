package com.specknet.airrespeck.respeckuploadservice


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

import com.google.gson.*
import com.squareup.tape.FileObjectQueue
import com.squareup.tape.SerializedConverter
import rx.subjects.PublishSubject
import rx.Observable

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


class RespeckRemoteUploadService {
    companion object {
        const val MSG_UPLOAD = "com.specknet.airrespeck.respeckuploadservice.MSG_UPLOAD"
        const val MSG_CONFIG = "com.specknet.airrespeck.respeckuploadservice.MSG_CONFIG"
        const val MSG_UPLOAD_DATA = "com.specknet.airrespeck.respeckuploadservice.live_data"
        const val MSG_CONFIG_JSON_HEADERS = "com.specknet.airrespeck.respeckuploadservice.config_headers"
        const val MSG_CONFIG_URL = "com.specknet.airrespeck.respeckuploadservice.config_url"
        const val MSG_CONFIG_PATH = "com.specknet.airrespeck.respeckuploadservice.config_path"
        const val FILENAME = "respeck_upload_queue6"

        internal lateinit var filequeue: FileObjectQueue<String>
        private lateinit var jsonHeaders: JsonObject
        private var configUrl = ""
        private var configPath = ""

        internal var mySubject = PublishSubject.create<JsonObject>()
        internal lateinit var respeckServer: RespeckServer
    }

    lateinit var ctx: Context

    internal fun jsonArrayFrom(list: List<JsonObject>):JsonArray {
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

    class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent:Intent) {
            val action = intent.action
            when (intent.action) {
                MSG_UPLOAD -> {
                    Log.i("Respeck", "MSG_UPLOAD message received")
                    mySubject.onNext(Gson().fromJson(intent.getStringExtra(MSG_UPLOAD_DATA), JsonElement::class.java).asJsonObject)
                }

                MSG_CONFIG -> {
                    Log.i("Respeck", "MSG_CONFIG message received")
                    jsonHeaders = Gson().fromJson(intent.getStringExtra(MSG_CONFIG_JSON_HEADERS), JsonElement::class.java).asJsonObject
                    configUrl = intent.getStringExtra(MSG_CONFIG_URL)
                    configPath = intent.getStringExtra(MSG_CONFIG_PATH)
                    respeckServer = RespeckServer.create(configUrl)
                }

                else -> { Log.i("Respeck", "Invalid message received") }
            }
        }
    }

    fun onCreate(ctx:Context) {
        Log.i("Respeck", "Service Started.")
        this.ctx = ctx
        val myReceiver = MyReceiver()
        ctx.registerReceiver(myReceiver, IntentFilter(RespeckRemoteUploadService.MSG_UPLOAD))
        ctx.registerReceiver(myReceiver, IntentFilter(RespeckRemoteUploadService.MSG_CONFIG))

        val queueFile = File(ctx.filesDir, FILENAME)

        try {
            filequeue = FileObjectQueue(queueFile, SerializedConverter<String>())
        } catch (ex: IOException) {
            Log.d("Respeck", "IOException" + ex.toString())
        }

        mySubject.buffer(10, TimeUnit.SECONDS, 500)
                .filter { !it.isEmpty() }
                .map { jsonArrayFrom(it) }
                .subscribe { filequeue.add(it.toString()) }

        Observable.interval(10, TimeUnit.SECONDS)
                  .concatMap { Observable.range(0,filequeue.size()) }
                  .map { jsonPacketFrom(filequeue.peek()) }
                  .concatMap { respeckServer.submitData(it, configPath) }
                  .doOnError { Log.e("Respeck", it.toString()) }
                  .retry()
                  .doOnCompleted {  }
                  .subscribe { Log.d("Respeck", "done: " + it.toString()); filequeue.remove() }
    }
}
