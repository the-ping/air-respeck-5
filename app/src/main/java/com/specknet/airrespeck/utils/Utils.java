package com.specknet.airrespeck.utils;


import android.content.Context;
import android.content.res.AssetManager;

import com.specknet.airrespeck.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public final class Utils {

    public static int[] menuIconsResId = {
            R.drawable.ic_home,
            R.drawable.ic_health,
            R.drawable.ic_air,
            R.drawable.ic_dashboard,
            R.drawable.ic_settings
    };

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
