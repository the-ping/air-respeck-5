package com.specknet.airrespeck.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.specknet.airrespeck.BuildConfig;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;


public final class Utils {

    private static Utils mUtils;
    private final Context mContext;
    private Map<String,String> loadedConfig;

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
    public static synchronized Utils getInstance(Context context) {
        Log.i("DFUtils","mUtils" + mUtils);
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

    public static String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.UK).format(new Date());
    }

    public String getDataDirectory() {
        // First, we check whether there is a data path stored in preferences
        SharedPreferences prefs = mContext.getSharedPreferences(
                "com.specknet.airrespeck", Context.MODE_PRIVATE);
        final String dataDirectoryKey = "com.specknet.airrespeck.datadirectory";
        String dataDirectoryPath = prefs.getString(dataDirectoryKey, "");

        // Get previously used ID from file path. If this doesn't match with current ID, create new file!
        String previousId = new File(dataDirectoryPath).getName().split(" ")[0];
        String currentId = getConfig(Constants.Config.SUBJECT_ID);

        // If this is the first time the app is started, or the directory doesn't exist, or the subject ID has changed,
        // create a new directory
        if (dataDirectoryPath.equals("") || !new File(dataDirectoryPath).exists() || !previousId.equals(currentId)) {
            dataDirectoryPath = Constants.EXTERNAL_DIRECTORY_STORAGE_PATH +
                    currentId + " " +
                    Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID) + " " +
                    new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.UK).format(new Date());

            prefs.edit().putString(dataDirectoryKey, dataDirectoryPath).apply();

            File directory;
            // Create other directories
            if (!getConfig(Constants.Config.RESPECK_UUID).isEmpty()) {
                directory = new File(dataDirectoryPath + Constants.RESPECK_DATA_DIRECTORY_NAME);
                if (!directory.exists()) {
                    boolean created = directory.mkdirs();
                    if (created) {
                        Log.i("DF", "Directory created: " + directory);
                    } else {
                        throw new RuntimeException("Couldn't create RESpeck folder on external storage");
                    }
                }
            }
            if (!getConfig(Constants.Config.AIRSPECK_UUID).isEmpty()) {
                directory = new File(dataDirectoryPath + Constants.AIRSPECK_DATA_DIRECTORY_NAME);
                if (!directory.exists()) {
                    boolean created = directory.mkdirs();
                    if (created) {
                        Log.i("DF", "Directory created: " + directory);
                    } else {
                        throw new RuntimeException("Couldn't create Airspeck folder on external storage");
                    }
                }
            }

            if (!getConfig(Constants.Config.ENABLE_PHONE_LOCATION_STORAGE).isEmpty()) {
                directory = new File(dataDirectoryPath + Constants.PHONE_LOCATION_DIRECTORY_NAME);
                if (!directory.exists()) {
                    boolean created = directory.mkdirs();
                    if (created) {
                        Log.i("DF", "Directory created: " + directory);
                    } else {
                        throw new RuntimeException("Couldn't create phone directory on external storage");
                    }
                }
            }
        }

        return dataDirectoryPath;
    }

    public void loadConfig(MainActivity mainActivity) {
        CursorLoader cursorLoader = new CursorLoader(mainActivity, Constants.Config.CONFIG_CONTENT_URI,
                null, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();
        // Set cursor to first row
        cursor.moveToNext();
        loadedConfig = new HashMap<>();
        // Save each row as key-value pair in HashMap
        while (!cursor.isAfterLast()) {
            loadedConfig.put(cursor.getString(0), cursor.getString(1));
            cursor.moveToNext();
        }
        Log.i("DFUtils", "cursor loaded");
        mainActivity.afterConfigurationLoaded();
    }

    public String getConfig(String key) {
        Log.i("DFUtils", "config queried: " + loadedConfig);
        return loadedConfig.get(key);
    }

    public int getAppVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

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

    public static float sum(float[] intArray) {
        float sum = 0;
        for (float val : intArray) {
            sum += val;
        }
        return sum;
    }

    public static float mean(int[] a) {
        if (a.length == 0) {
            return Float.NaN;
        }
        int sum = 0;
        for (int elem : a) {
            sum += elem;
        }
        return sum / a.length;
    }

    public static float mean(ArrayList<Float> a) {
        if (a.size() == 0) {
            return Float.NaN;
        }
        Float sum = 0f;
        for (Float elem : a) {
            sum += elem;
        }
        return sum / a.size();
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
                        dialogInterface.dismiss();
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
                        dialogInterface.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static int mode(ArrayList<Integer> a) {
        int mode = a.get(0);
        int maxCount = 0;
        for (int i = 0; i < a.size(); i++) {
            int value = a.get(i);
            int count = 0;
            for (int j = 0; j < a.size(); j++) {
                if (a.get(j) == value) count++;
                if (count > maxCount) {
                    mode = value;
                    maxCount = count;
                }
            }
        }
        if (maxCount > 1) {
            return mode;
        }
        return 0;
    }

    public static float norm(float[] array) {
        float retval = 0;
        for (float e : array) {
            retval += e * e;
        }
        return (float) Math.sqrt(retval);
    }

    // Note: only works with 3D vectors.
    public static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    public static float[] normalize(float[] a) {
        float[] retval = new float[a.length];
        float norm = norm(a);
        for (int i = 0; i < a.length; i++) {
            retval[i] = a[i] / norm;
        }
        return retval;
    }

    public static float[] cross(float[] arrayA, float[] arrayB) {
        float[] retArray = new float[3];
        retArray[0] = arrayA[1] * arrayB[2] - arrayA[2] * arrayB[1];
        retArray[1] = arrayA[2] * arrayB[0] - arrayA[0] * arrayB[2];
        retArray[2] = arrayA[0] * arrayB[1] - arrayA[1] * arrayB[0];
        return retArray;
    }

    public String getSecurityKey() {
        return context.getSharedPreferences(Constants.SECURITY_KEY_FILE, Context.MODE_PRIVATE).getString(
                Constants.SECURITY_KEY, "");
    }
}