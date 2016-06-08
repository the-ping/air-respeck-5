package com.specknet.airrespeck;


import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public final class Utils {
    private Utils() {

    }

    public static Properties getProperties(Context context) {
        Properties properties = new Properties();
        AssetManager assetManager = context.getAssets();
        try {
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
        } catch (IOException e) { }
        return properties;
    }

    public static String getProperty(String key, Context context) {
        Properties properties = new Properties();
        AssetManager assetManager = context.getAssets();
        try {
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
        } catch (IOException e) { }
        return properties.getProperty(key);
    }
}
