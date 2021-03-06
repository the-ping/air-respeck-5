package com.specknet.airrespeck.services.airspeckuploadservice


import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import rx.Observable


interface AirspeckServer {
    @POST("{path}")
    public fun submitData(@Body bodyData: JsonObject, @Path("path") path: String): Observable<JsonObject>
    companion object {
        fun create(baseUrl: String) : AirspeckServer {
            val gsonBuilder = GsonBuilder()
            val restAdapter = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
                    .build()

            return restAdapter.create(AirspeckServer::class.java)
        }
    }
}