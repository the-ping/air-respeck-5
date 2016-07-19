package com.specknet.airrespeck.http;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.specknet.airrespeck.datamodels.User;

import retrofit2.Call;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.http.*;


public class HttpApi {
    public static final String ENDPOINT = "http://www.mocky.io";

    private static HttpApi mInstance = null;
    private HttpApi.service mService;

    /**
     * Service definition
     */
    public interface service {
        @GET("/v2/578c20370f00008f1237082c/{userId}")
        //@GET("/getUser/{userId}")
        Call<User> getUser(@Path("userId") String userId);

        @POST("/v2/578c08250f0000b80f370822")
        //@POST("/createUser")
        Call<LinkedTreeMap> createUser(@Body User userData);
    }

    /**
     * Private constructor
     */
    private HttpApi() {
        // Gson setup
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        // Retrofit setup
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        // Service setup
        mService = retrofit.create(HttpApi.service.class);
    }

    /**
     * Get the HttpApi singleton instance
     */
    public static HttpApi getInstance() {
        if (mInstance == null) {
            mInstance = new HttpApi();
        }
        return mInstance;
    }

    /**
     * Get the API service to execute calls with
     */
    public HttpApi.service getService() {
        return mService;
    }
}