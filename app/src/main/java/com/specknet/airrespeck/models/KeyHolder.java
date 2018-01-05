package com.specknet.airrespeck.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class KeyHolder {

    @SerializedName("key")
    @Expose
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

}
