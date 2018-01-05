package com.specknet.airrespeck.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SpecknetClient {

    private static SpecknetService specknetService = null;
    private final static String BASE_URL = "https://dashboard.specknet.uk/";

    public static SpecknetService getSpecknetService() {
        if (specknetService == null) {
            specknetService = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(SpecknetService.class);
        }
        return specknetService;
    }
}