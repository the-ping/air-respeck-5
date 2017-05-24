package com.specknet.airrespeck.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.specknet.airrespeck.BuildConfig;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.models.AirspeckData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;


public final class Utils {

    private static Utils mUtils;
    private final Context mContext;
    private static Properties mProperties = null;

    /**
     * Private constructor for singleton class.
     *
     * @param context Context Application context.
     */
    private Utils(Context context) {
        mContext = context;
    }

    /**
     * Get singleton class instance.
     *
     * @param context Context Application context.
     * @return Utils Singleton class instance.
     */
    public static Utils getInstance(Context context) {
        if (mUtils == null) {
            mUtils = new Utils(context);
        }
        return mUtils;
    }

    /***********************************************************************************************
     * GENERAL UTIL METHODS
     **********************************************************************************************/

    /**
     * Get screen size.
     *
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
     * Get the screen density.
     *
     * @return float Screen density.
     */
    public float getScreenDensity() {
        return mContext.getResources().getDisplayMetrics().density;
    }


    /**
     * Get unix timestamp.
     *
     * @return long The timestamp.
     */
    public static long getUnixTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * Round a float number to 2 digits.
     *
     * @param value float The value to be rounded.
     * @return float The rounded value.
     */
    public float roundToTwoDigits(final float value) {
        return Float.valueOf(String.format(Locale.UK, "%.2f%n", value));
    }

    /**
     * Get all the configuration properties.
     *
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

    public static String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.UK).format(new Date());
    }

    /**
     * Get a configuration property value by its key.
     *
     * @param key String Configuration property key.
     * @return String Configuration property value.
     */
    public String getConfigProperty(String key) {
        Properties properties = new Properties();
        AssetManager assetManager = mContext.getAssets();

        try {
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return properties.getProperty(key);
    }

    /**
     * Load properties file from the external storage directory.
     *
     * @param fileName String Properties file name.
     */
    private void loadPropertiesFile(final String fileName) {
        try {
            mProperties = new Properties();

            // Load file
            File file = new File(Environment.getExternalStorageDirectory(), fileName);
            InputStream inputStream = new FileInputStream(file);

            // Load file stream
            mProperties.load(inputStream);
            Log.i("DF", "Loaded properties file");
        } catch (FileNotFoundException e) {
            try {
                Toast.makeText(mContext, "Properties file not found", Toast.LENGTH_LONG).show();
            } catch (RuntimeException re) {
                // Do nothing. This means we tried to make a toast message within a non-activity thread
            }
            Log.e("DF", "Properties file not found.");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("DF", "Cannot load properties file.");
            e.printStackTrace();
        }
    }

    /**
     * Get a properties object from a file from the external storage directory.
     *
     * @return Properties The properties object.
     */
    public Properties getProperties() {
        if (mProperties == null) {
            loadPropertiesFile(Constants.Config.PROPERTIES_FILE_NAME);
        }
        return mProperties;
    }

    /**
     * Get app version code.
     *
     * @return int The version code.
     */
    public int getAppVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    /**
     * Get app version name.
     *
     * @return String The version name.
     */
    public String getAppVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    public static int sum(int[] intArray) {
        int sum = 0;
        for (int val : intArray) {
            sum += val;
        }
        return sum;
    }

    public static float mean(Float[] a) {
        Float sum = 0f;
        for (Float elem : a) {
            sum += elem;
        }
        return sum / a.length;
    }

    public static float mean(Long[] a) {
        long sum = 0;
        for (Long elem : a) {
            sum += elem;
        }
        return sum / a.length;
    }

    public static float mean(float[] a) {
        float sum = 0f;
        for (float elem : a) {
            sum += elem;
        }
        return sum / a.length;
    }

    public static float median(Float[] a) {
        Float[] aCopy = Arrays.copyOf(a, a.length);

        // sort aCopy
        Arrays.sort(aCopy);

        // return the middle value. if a has even length, return the mean of two middle values
        int middle = aCopy.length / 2;
        if (aCopy.length % 2 == 1) {
            return aCopy[middle];
        } else {
            return (aCopy[middle - 1] + aCopy[middle]) / 2.0f;
        }
    }

    public static long timestampFromString(String timestamp, String format) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.UK);
        return dateFormat.parse(timestamp).getTime();
    }

    public static long roundToDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static float onlyKeepTimeInHour(long timestamp) {
        long millisInHour = 36000000;
        return (float) (timestamp % millisInHour);
    }

    public static float[] toPrimitiveFloatArray(Float[] array) {
        float[] floatArray = new float[array.length];
        int i = 0;
        for (Float f : array) {
            floatArray[i++] = (f != null ? f : Float.NaN);
        }
        return floatArray;
    }

    public static void writeToFile(String filename, String line) {
        OutputStreamWriter writer;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(filename, true));
            writer.append(line);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkAndRequestLocationPermission(final Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    Constants.REQUEST_CODE_LOCATION_PERMISSION);
            return false;
        } else {
            return true;
        }
    }

    public static void showLocationRequestDialog(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.location_request_dialog_message)
                .setTitle(R.string.location_request_dialog_title);
        builder.setNeutralButton(R.string.dialog_button_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Utils.checkAndRequestLocationPermission(activity);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static boolean checkAndRequestStoragePermission(final Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.REQUEST_CODE_STORAGE_PERMISSION);
            return false;
        } else {
            return true;
        }
    }

    public static void showStorageRequestDialog(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.storage_request_dialog_message)
                .setTitle(R.string.storage_request_dialog_title);
        builder.setNeutralButton(R.string.dialog_button_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Utils.checkAndRequestStoragePermission(activity);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}