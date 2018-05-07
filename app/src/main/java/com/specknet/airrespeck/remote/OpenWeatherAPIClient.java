package com.specknet.airrespeck.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OpenWeatherAPIClient {

    private static OpenWeatherAPIService openWeatherAPIService = null;
    private final static String BASE_URL = "http://api.openweathermap.org";

    public static OpenWeatherAPIService getOpenWeatherAPIService() {
        if (openWeatherAPIService == null) {
            openWeatherAPIService = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(OpenWeatherAPIService.class);
        }
        return openWeatherAPIService;
    }
}