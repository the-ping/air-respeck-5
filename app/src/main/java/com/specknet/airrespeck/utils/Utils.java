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
import com.specknet.airrespeck.datamodels.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
     * Get the screen density.
     * @return float Screen density.
     */
    public float getScreenDensity() {
        return mContext.getResources().getDisplayMetrics().density;
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


    /**
     * Get the current age given the birth date.
     * @param dateOfBirth Date The birth date.
     * @return int The current age.
     */
    public int getAge(Date dateOfBirth) {

        Calendar today = Calendar.getInstance();
        Calendar birthDate = Calendar.getInstance();

        int age = 0;

        birthDate.setTime(dateOfBirth);
        if (birthDate.after(today)) {
            throw new IllegalArgumentException("Can't be born in the future");
        }

        age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);

        // If birth date is greater than today's date (after 2 days adjustment of leap year) then decrement age one year
        if ( (birthDate.get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR) > 3) ||
                (birthDate.get(Calendar.MONTH) > today.get(Calendar.MONTH ))){
            age--;
        }
        // If birth date and today's date are of same month and birth day of month is greater than today's day of month then decrement age
        else if ((birthDate.get(Calendar.MONTH) == today.get(Calendar.MONTH )) &&
                (birthDate.get(Calendar.DAY_OF_MONTH) > today.get(Calendar.DAY_OF_MONTH ))){
            age--;
        }

        return age;
    }

    /**
     * Get the user group age.
     * @param age int The user's age.
     * @return int The group age the user belongs to.
     */
    public int getUserGroupAge(final int age) {
        if ( age >= Integer.parseInt(getConfigProperty("adolescent_min")) &&
                age <= Integer.parseInt(getConfigProperty("adolescent_max")) ) {
            return Constants.UGA_ADOLESCENT;
        }
        else if ( age >= Integer.parseInt(getConfigProperty("young_adult_min")) &&
                age <= Integer.parseInt(getConfigProperty("young_adult_max")) ) {
            return Constants.UGA_YOUNG_ADULT;
        }
        else if ( age >= Integer.parseInt(getConfigProperty("middleaged_adult_min")) &&
                age <= Integer.parseInt(getConfigProperty("middleaged_adult_max")) ) {
            return Constants.UGA_MIDDLEAGED_ADULT;
        }
        else if ( age >= Integer.parseInt(getConfigProperty("elderly_adult_min")) &&
                age <= Integer.parseInt(getConfigProperty("elderly_adult_max")) ) {
            return Constants.UGA_EDERLY_ADULT;
        }
        return -1;
    }

    /**
     * Method to configure the UI preferences based on the user details
     * (i.e. user type (subject, researcher), age).
     * @param user User the user instance.
     */
    public void setupUI(User user) {
        PreferencesUtils.getInstance(mContext);
        PreferencesUtils.getInstance().put(PreferencesUtils.Key.USER_ID, user.getUniqueId());

        if (user.getUserType() == 2) {
            // Users of type "Researcher" will have the tabbed main menu as default
            PreferencesUtils.getInstance().put(PreferencesUtils.Key.MENU_MODE, "1");
            PreferencesUtils.getInstance().put(PreferencesUtils.Key.AIRSPECK_APP_ACCESS, true);
            PreferencesUtils.getInstance().put(PreferencesUtils.Key.RESPECK_APP_ACCESS, true);
        }
        else if (user.getUserType() == 1) {
            // Users of type "Subject" will have different configurations based on age
            switch (getUserGroupAge(getAge(user.getBirthDate()))) {
                case Constants.UGA_ADOLESCENT:
                    // Menu type: Buttons
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.MENU_MODE, "0");
                    // Font size: Normal
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.FONT_SIZE, "1");
                    // Home screen, readings display type: Segmented bar
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.READINGS_MODE_HOME_SCREEN, "1");
                    // Air Quality screen, readings display type: Segmented bar
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.READINGS_MODE_AQREADINGS_SCREEN, "1");
                    //Graphs screen: disabled
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.MENU_GRAPHS_SCREEN, false);
                    // External apps access: disabled
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.AIRSPECK_APP_ACCESS, false);
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.RESPECK_APP_ACCESS, false);
                    break;
                case Constants.UGA_YOUNG_ADULT:
                    // Menu type: Tabs
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.MENU_MODE, "1");
                    // Font size: Normal
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.FONT_SIZE, "1");
                    // Home screen, readings display type: Segmented bar
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.READINGS_MODE_HOME_SCREEN, "0");
                    // Air Quality screen, readings display type: Segmented bar
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.READINGS_MODE_AQREADINGS_SCREEN, "0");
                    //Graphs screen: disabled
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.MENU_GRAPHS_SCREEN, false);
                    // External apps access: disabled
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.AIRSPECK_APP_ACCESS, false);
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.RESPECK_APP_ACCESS, false);
                    break;
                case Constants.UGA_MIDDLEAGED_ADULT:
                    // Menu type: Tabs
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.MENU_MODE, "1");
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.MENU_TAB_ICONS, true);
                    // Font size: Normal
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.FONT_SIZE, "1");
                    // Home screen, readings display type: Segmented bar
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.READINGS_MODE_HOME_SCREEN, "0");
                    // Air Quality screen, readings display type: Segmented bar
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.READINGS_MODE_AQREADINGS_SCREEN, "0");
                    //Graphs screen: disabled
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.MENU_GRAPHS_SCREEN, false);
                    // External apps access: disabled
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.AIRSPECK_APP_ACCESS, false);
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.RESPECK_APP_ACCESS, false);
                    break;
                case Constants.UGA_EDERLY_ADULT:
                    // Menu type: Buttons
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.MENU_MODE, "0");
                    // Font size: Large
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.FONT_SIZE, "2");
                    // Home screen, readings display type: Segmented bar
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.READINGS_MODE_HOME_SCREEN, "1");
                    // Air Quality screen, readings display type: Segmented bar
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.READINGS_MODE_AQREADINGS_SCREEN, "1");
                    //Graphs screen: disabled
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.MENU_GRAPHS_SCREEN, false);
                    // External apps access: disabled
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.AIRSPECK_APP_ACCESS, false);
                    PreferencesUtils.getInstance().put(PreferencesUtils.Key.RESPECK_APP_ACCESS, false);
                    break;
                default:
                    throw new IllegalArgumentException("User must be at least 12 years old.");
            }
        }
    }
}
