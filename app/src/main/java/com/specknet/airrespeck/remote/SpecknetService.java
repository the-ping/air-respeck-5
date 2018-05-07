package com.specknet.airrespeck.remote;

import com.specknet.airrespeck.models.KeyHolder;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface SpecknetService {

    @POST("/make_upload_key")
    @FormUrlEncoded
    Call<KeyHolder> makeUploadKey(@Field("username") String username,
                                  @Field("password") String password,
                                  @Field("project_id") String project_id,
                                  @Field("android_id") String android_id);
}