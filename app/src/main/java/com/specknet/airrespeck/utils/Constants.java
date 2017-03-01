package com.specknet.airrespeck.utils;


import android.os.Environment;

import com.specknet.airrespeck.R;

import java.io.File;

/**
 * Class to keep all constants and string keys in one place. Note that for most of the constants below we cannot use
 * enums, as we use the constants as keys for methods requiring a String, such as SharedPreferences.
 */
public class Constants {

    /**
     * App config file
     */
    public static class Config {
        public static final String PROPERTIES_FILE_NAME = "RESpeck.config";
        public static final String RESPECK_KEY = "RESpeckKey";
        public static final String RESPECK_UUID = "RESpeckUUID";
        public static final String QOEUUID = "QoEUUID";
        public static final String TABLET_SERIAL = "TabletSerial";
        public static final String PATIENT_ID = "PatientID";
        public static final String PATIENT_AGE = "PatientAge";
        public static final String USER_TYPE = "UserType";
        public static final String IS_SUPERVISED_STARTING_MODE = "IsSupervisedStartingMode";
        public static final String IS_SUPERVISED_MODE_ENABLED = "EnableSupervisedMode";
        public static final String IS_SUBJECT_MODE_ENABLED = "EnableSubjectMode";
        public static final String IS_AIRSPECK_ENABLED = "EnableAirspeck";
        public static final String SHOW_SUPERVISED_AQ_GRAPHS = "EnableSupervisedAQGraphs";
        public static final String SHOW_SUPERVISED_ACTIVITY_SUMMARY = "EnableSupervisedActivitySummary";
        public static final String SHOW_SUPERVISED_RESPECK_READINGS = "EnableSupervisedRESpeckReadings";
        public static final String SHOW_SUPERVISED_AIRSPECK_READINGS = "EnableSupervisedAirspeckReadings";
        public static final String SHOW_SUPERVISED_OVERVIEW = "EnableSupervisedOverview";
        public static final String SHOW_PCA_GRAPH = "ShowPCAGraph";
        public static final String IS_UPLOAD_DATA_TO_SERVER = "UploadToServer";
        public static final String IS_STORE_DATA_LOCALLY = "StoreDataLocally";
        public static final String IS_STORE_MERGED_FILE = "StoreMergedFile";
    }

    /**
     * Preferences
     */
    public static class Preferences {
        public static final String IS_APP_INITIAL_STARTUP = "is_app_initial_startup";

        public static final String USER_ID = "user_id";

        public static final String FONT_SIZE = "font_size";

        public static final String MENU_MODE = "menu_mode";
        public static final String MENU_BUTTONS_PADDING = "menu_buttons_padding";
        public static final String MENU_TAB_ICONS = "menu_tab_icons";
        public static final String MENU_GRAPHS_SCREEN = "menu_graphs_screen";

        public static final String AIRSPECK_APP_ACCESS = "airspeck_app_access";
        public static final String RESPECK_APP_ACCESS = "respeck_app_access";

        public static final String READINGS_MODE_HOME_SCREEN = "readings_mode_home_screen";
        public static final String READINGS_MODE_AQREADINGS_SCREEN = "readings_mode_aqreadings_screen";
    }

    /**
     * UPLOAD SERVICES
     */
    public static final String UPLOAD_SERVER_URL = "https://beast.inf.ed.ac.uk/";
    public static final String UPLOAD_SERVER_PATH = "uploadAirRespeck";


    /**
     * HTTP WEB SERVICES (NEW USER, GET USER)
     */
    public static final String BASE_URL = "http://www.mocky.io";


    /**
     * Google play: Location Manager
     */
    public static final Long UPDATE_INTERVAL = 5000L;
    public static final Long FASTEST_INTERVAL = 1000L;


    /**
     * Menu icons
     */
    public static int MENU_ICON_HOME = R.drawable.ic_home;
    public static int MENU_ICON_HEALTH = R.drawable.ic_health;
    public static int MENU_ICON_ACTIVITY = R.drawable.vec_running_man_orange;
    public static int MENU_ICON_AIR = R.drawable.ic_air;
    public static int MENU_ICON_GRAPHS = R.drawable.ic_graphs;
    public static int MENU_ICON_SETTINGS = R.drawable.ic_settings;
    public static int MENU_ICON_INFO = R.drawable.vec_info;


    /**
     * Readings
     */
    public static final String RESPECK_X = "x";
    public static final String RESPECK_Y = "y";
    public static final String RESPECK_Z = "z";
    public static final String RESPECK_BREATHING_RATE = "breathing_rate";
    public static final String RESPECK_BREATHING_SIGNAL = "breathing_signal";
    public static final String RESPECK_BREATHING_ANGLE = "breathing_angle";
    public static final String RESPECK_AVERAGE_BREATHING_RATE = "minute_average_breathing_rate";
    public static final String RESPECK_STD_DEV_BREATHING_RATE = "std_dev_breathing_rate";
    public static final String RESPECK_N_BREATHS = "n_breaths";
    public static final String RESPECK_ACTIVITY_LEVEL = "activity_level";
    public static final String RESPECK_ACTIVITY_TYPE = "activity_type";
    public static final String RESPECK_LIVE_SEQ = "live_seq";
    public static final String RESPECK_LIVE_RS_TIMESTAMP = "live_rs_timestamp";
    public static final String RESPECK_LIVE_INTERPOLATED_TIMESTAMP = "live_interpolated_timestamp";
    public static final String RESPECK_BATTERY_PERCENT = "battery_percentage";
    public static final String RESPECK_REQUEST_CHARGE = "request_charge";

    public static final String QOE_PM1 = "pm1";
    public static final String QOE_PM2_5 = "pm2_5";
    public static final String QOE_PM10 = "pm10";
    public static final String QOE_TEMPERATURE = "temperature";
    public static final String QOE_HUMIDITY = "humidity";
    public static final String QOE_NO2 = "no2";
    public static final String QOE_S1ae_NO2 = "s1_ae";
    public static final String QOE_S1we_NO2 = "s1_we";
    public static final String QOE_O3 = "o3";
    public static final String QOE_S2ae_O3 = "s2_ae";
    public static final String QOE_S2we_O3 = "s2_we";
    public static final String QOE_BINS_0 = "bins0";
    public static final String QOE_BINS_1 = "bins1";
    public static final String QOE_BINS_2 = "bins2";
    public static final String QOE_BINS_3 = "bins3";
    public static final String QOE_BINS_4 = "bins4";
    public static final String QOE_BINS_5 = "bins5";
    public static final String QOE_BINS_6 = "bins6";
    public static final String QOE_BINS_7 = "bins7";
    public static final String QOE_BINS_8 = "bins8";
    public static final String QOE_BINS_9 = "bins9";
    public static final String QOE_BINS_10 = "bins10";
    public static final String QOE_BINS_11 = "bins11";
    public static final String QOE_BINS_12 = "bins12";
    public static final String QOE_BINS_13 = "bins13";
    public static final String QOE_BINS_14 = "bins14";
    public static final String QOE_BINS_15 = "bins15";
    public static final String QOE_BINS_TOTAL = "bins_total";

    public static final String ACTIVITY_SUMMARY_HOUR = "hour";
    public static final String ACTIVITY_SUMMARY_DAY = "day";
    public static final String ACTIVITY_SUMMARY_WEEK = "week";


    /**
     * Additional data for uploading to the server
     */
    public static final String LOC_LATITUDE = "latitude";
    public static final String LOC_LONGITUDE = "longitude";
    public static final String LOC_ALTITUDE = "altitude";
    public static final String UNIX_TIMESTAMP = "timestamp";


    /**
     * Readings order in Air Quality Fragment
     */
    public static String[] READINGS_QOE = {
            QOE_TEMPERATURE,
            QOE_HUMIDITY,
            QOE_NO2,
            QOE_O3,
            QOE_PM1,
            QOE_PM2_5,
            QOE_PM10,
            QOE_BINS_TOTAL
    };


    /**
     * GRAPHS FRAGMENT UI
     */
    public static final int NUMBER_BREATHING_SIGNAL_SAMPLES_ON_CHART = (int) Math.round(15 * 12.5);


    /**
     * USERS' GROUP AGES
     */
    public static final int UGA_ADOLESCENT = 1;
    public static final int UGA_YOUNG_ADULT = 2;
    public static final int UGA_MIDDLEAGED_ADULT = 3;
    public static final int UGA_ELDERLY_ADULT = 4;

    /**
     * Preferences option values
     */
    public static final String MENU_MODE_BUTTONS = "0";
    public static final String MENU_MODE_TABS = "1";

    public static final String READINGS_MODE_HOME_SCREEN_LIST = "0";
    public static final String READINGS_MODE_HOME_SCREEN_SEGMENTED_BARS = "1";

    public static final String READINGS_MODE_AQREADINGS_SCREEN_LIST = "0";
    public static final String READINGS_MODE_AQREADINGS_SCREEN_SEGMENTED_BARS = "1";
    public static final String READINGS_MODE_AQREADINGS_SCREEN_ARCS = "2";

    public static final String FONT_SIZE_NORMAL = "1";
    public static final String FONT_SIZE_LARGE = "2";

    public static final int USER_TYPE_SUBJECT = 0;
    public static final int USER_TYPE_RESEARCHER = 1;

    public static final String MENU_BUTTONS_PADDING_NORMAL = "5";

    /**
     * Activities
     */
    public static final int ACTIVITY_STAND_SIT = 0;
    public static final int ACTIVITY_WALKING = 1;
    public static final int ACTIVITY_LYING = 2;
    public static final int NUM_ACT_CLASSES = 3;
    public static final String[] ACT_CLASS_NAMES = {"Sitting/Standing", "Walking", "Lying down"};

    /*
    Others
     */
    public static final int AVERAGE_TIME_DIFFERENCE_BETWEEN_PACKETS = 2535;
    public static final int NUMBER_OF_SAMPLES_PER_BATCH = 32;

    /**
     * Storage
     */
    public static final String EXTERNAL_DIRECTORY_STORAGE_PATH = new File(Environment.getExternalStorageDirectory(),
            "/AirRespeck").getPath();
    public static final String ACTIVITY_SUMMARY_FILE_PATH = EXTERNAL_DIRECTORY_STORAGE_PATH + "/Activity_summary.csv";
    public static final String RESPECK_DATA_DIRECTORY_PATH = EXTERNAL_DIRECTORY_STORAGE_PATH + "/RESpeck/";
    public static final String AIRSPECK_DATA_DIRECTORY_PATH = EXTERNAL_DIRECTORY_STORAGE_PATH + "/Airspeck/";
    public static final String MERGED_DATA_DIRECTORY_PATH = EXTERNAL_DIRECTORY_STORAGE_PATH + "/RESpeck_and_Airspeck/";
    public static final String RESPECK_DATA_HEADER = "interpolatedPhoneTimestamp,respeckTimestamp.sequenceNumber,x,y,z," +
            "breathingSignal,breathingRate,activityLevel,activityType";
    public static final String AIRSPECK_DATA_HEADER = "phoneTimestamp,temperature,humidity,no2,o3,bin0";
    public static final String MERGED_DATA_HEADER = "interpolatedPhoneTimestamp,respeckTimestamp,x,y,z," +
            "breathingSignal,breathingRate,activityLevel,activityType,temperature,humidity,no2,o3,bin0";


    /*
    PCA zero mean signal calculation
     */
    public static final int NUMBER_OF_SAMPLES_REQUIRED_FOR_PCA = (int) Math.round(20 * 12.5);
    public static final int NUMBER_OF_SAMPLES_FOR_MEAN_SUBTRACTION = 69;
    public static final int NUMBER_OF_SAMPLES_FOR_MEAN_POST_FILTER = 7;
    public static final int NUMBER_OF_SAMPLES_FOR_MEAN_PRE_FILTER = 5;
}
