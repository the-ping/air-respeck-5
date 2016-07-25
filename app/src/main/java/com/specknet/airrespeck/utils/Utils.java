package com.specknet.airrespeck.utils;


import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.specknet.airrespeck.BuildConfig;
import com.specknet.airrespeck.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;


public final class Utils {

    private static Utils mUtils;
    private final Context mContext;
    private static Properties mProperties = null;

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
     * Get all the configuration properties.
     * @return Properties All configuration properties.
     */
    public Properties getConfigProperties() {
        Properties properties = new Properties();
        AssetManager assetManager = mContext.getAssets();

        try {
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
        } catch (IOException e) { }

        return properties;
    }

    /**
     * Get a configuration property value by its key.
     * @param key String Configuration property key.
     * @return String Configuration property value.
     */
    public String getConfigProperty(String key) {
        Properties properties = new Properties();
        AssetManager assetManager = mContext.getAssets();

        try {
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
        } catch (IOException e) { }

        return properties.getProperty(key);
    }

    /**
     * Load properties file from the external storage directory.
     * @param fileName String Properties file name.
     */
    private void loadPropertiesFile(final String fileName) {
        try {
            // Load file
            File file = new File(Environment.getExternalStorageDirectory(), fileName);
            InputStream inputStream = new FileInputStream(file);

            // Load file stream
            mProperties = new Properties();
            mProperties.load(inputStream);
        }
        catch (FileNotFoundException e) {
            Log.e("AirRespeck Properties", "Properties file not found.");
            e.printStackTrace();
        }
        catch (IOException e) {
            Log.e("AirRespeck Properties", "Cannot load properties file.");
            e.printStackTrace();
        }
    }

    /**
     * Get a properties object from a file from the external storage directory.
     * @return Properties The properties object.
     */
    public Properties getProperties () {
        if (mProperties == null) {
            loadPropertiesFile(Constants.PROPERTIES_FILE_NAME);
        }
        return mProperties;
    }

    /**
     * Get app version code.
     * @return int The version code.
     */
    public int getAppVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    /**
     * Get app version name.
     * @return String The version name.
     */
    public String getAppVersionName() {
        return BuildConfig.VERSION_NAME;
    }
}
