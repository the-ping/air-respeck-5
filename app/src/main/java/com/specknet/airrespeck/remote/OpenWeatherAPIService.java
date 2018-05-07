package com.specknet.airrespeck.remote;

import com.specknet.airrespeck.models.KeyHolder;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface OpenWeatherAPIService {

    @GET("/data/2.5/weather?APPID=f446cb0a3c12c227cb4049e561fee5f5&units=metric")
    Call<OpenWeatherData> getWeather(@Query("lat") Double latitude,
                               @Query("lon") Double longitude);

}