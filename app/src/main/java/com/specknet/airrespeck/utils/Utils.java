package com.specknet.airrespeck.utils;


import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.specknet.airrespeck.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;


public final class Utils {

    private static Utils mUtils;
    private final Context mContext;

    /**
     * Private constructor for singleton class.
     * @param context Context Application context.
     */
    private Utils(Context context) {
        mContext = context;
    }

    /**
     * Get singleton class instance.
     * @param context Context Application context.
     * @return Utils Singleton class instance.
     */
    public static Utils getInstance(Context context) {
        if (mUtils == null) {
            mUtils = new Utils(context);
        }
        return mUtils;
    }

    /**
     * Get screen size.
     * @return Point Screen size i.e. Point(x, y).
     */
    public Point getScreenSize() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return size;
    }

    /**
     * Round a float number to 2 digits.
     * @param value float The value to be rounded.
     * @return float The rounded value.
     */
    public float roundToTwoDigits(final float value) {
        return Float.valueOf(String.format("%.2f%n", value));
    }

    /**
     * Get all properties.
     * @return Properties All properties.
     */
    public Properties getProperties() {
        Properties properties = new Properties();
        AssetManager assetManager = mContext.getAssets();

        try {
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
        } catch (IOException e) { }

        return properties;
    }

    /**
     * Get a property value by its key.
     * @param key String Property key.
     * @return String Proverty value.
     */
    public String getProperty(String key) {
        Properties properties = new Properties();
        AssetManager assetManager = mContext.getAssets();

        try {
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
        } catch (IOException e) { }

        return properties.getProperty(key);
    }
}
