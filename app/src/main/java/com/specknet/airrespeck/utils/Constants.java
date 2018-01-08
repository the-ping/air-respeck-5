package com.specknet.airrespeck.utils;


import android.net.Uri;
import android.os.Environment;

import com.specknet.airrespeck.R;

import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Class to keep all constants and string keys in one place. Note that for most of the constants below we cannot use
 * enums, as we use the constants as keys for methods requiring a String, such as SharedPreferences.
 */
public class Constants {
    /**
     * Preferences
     */
    public static class Preferences {
        public static final String IS_APP_INITIAL_STARTUP = "is_app_initial_startup";

        public static final String FONT_SIZE = "font_size";

        public static final String MENU_MODE = "menu_mode";
        public static final String MENU_BUTTONS_PADDING = "menu_buttons_padding";
        public static final String MENU_TAB_ICONS = "menu_tab_icons";
        public static final String MENU_GRAPHS_SCREEN = "menu_graphs_screen";

        public static final String READINGS_MODE_HOME_SCREEN = "readings_mode_home_screen";
    }

    /**
     * UPLOAD SERVICES
     */
    public static final String UPLOAD_SERVER_URL = "https://specknet-pyramid-test.appspot.com/";
    public static final String UPLOAD_SERVER_PATH = "uploadAirRespeck";


    /**
     * HTTP WEB SERVICES (NEW USER, GET USER)
     */
    public static final String BASE_URL = "http://www.mocky.io";


    // Bluetooth connection timeout: how long to wait after loosing connection before trying reconnect
    public static final int RECONNECTION_TIMEOUT_MILLIS = 10000;


    /**
     * GPS Phone update. The active update interval specifies the time in between active updates of the GPS location,
     * i.e. how often the app explicitly asks for the location. The passive time specifies how often the app checks for
     * updates which are calculated for other apps. This might be Google Maps running in the foreground. As the
     * location is updated less than 5 seconds anyway, we can also use that location!
     */
    public static final Long GPS_UPDATE_INTERVAL_ACTIVE = 5000L;
    public static final Long GPS_UPDATE_INTERVAL_PASSIVE = 1000L;


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

    // Broadcast

    public static final String ACTION_RESPECK_LIVE_BROADCAST =
            "com.specknet.respeck.RESPECK_LIVE_BROADCAST";
    public static final String ACTION_RESPECK_AVG_BROADCAST =
            "com.specknet.respeck.RESPECK_AVG_BROADCAST";
    public static final String ACTION_RESPECK_AVG_STORED_BROADCAST =
            "com.specknet.respeck.RESPECK_AVG_STORED_BROADCAST";

    public static final String ACTION_DIARY_BROADCAST = "com.specknet.diarydaphne.DIARY_BROADCAST";
    public static final String DIARY_JSON = "diary_json";
    public static final String DIARY_FILE_STRING = "diary_string";

    public static final String ACTION_RESPECK_CONNECTED =
            "com.specknet.respeck.RESPECK_CONNECTED";
    public static final String ACTION_RESPECK_DISCONNECTED =
            "com.specknet.respeck.RESPECK_DISCONNECTED";
    public static final String ACTION_AIRSPECK_LIVE_BROADCAST =
            "com.specknet.airspeck.AIRSPECK_LIVE_BROADCAST";
    public static final String ACTION_AIRSPECK_CONNECTED =
            "com.specknet.airspeck.AIRSPECK_CONNECTED";
    public static final String ACTION_AIRSPECK_DISCONNECTED =
            "com.specknet.airspeck.AIRSPECK_DISCONNECTED";
    public static final String ACTION_PHONE_LOCATION_BROADCAST = "com.specknet.airrespeck.PHONE_LOCATION";

    /**
     * Readings
     */
    public static final String RESPECK_LIVE_DATA = "respeck_live_data";
    public static final String RESPECK_AVG_DATA = "respeck_avg_data";
    public static final String RESPECK_X = "x";
    public static final String RESPECK_Y = "y";
    public static final String RESPECK_Z = "z";
    public static final String RESPECK_BREATHING_RATE = "breathing_rate";
    public static final String RESPECK_BREATHING_SIGNAL = "breathing_signal";
    public static final String RESPECK_BREATHING_ANGLE = "respeck_breathing_angle";
    public static final String RESPECK_MINUTE_AVG_BREATHING_RATE = "avg_breathing_rate";
    public static final String RESPECK_MINUTE_STD_BREATHING_RATE = "sd_br";
    public static final String RESPECK_MINUTE_NUMBER_OF_BREATHS = "n_breaths";
    public static final String RESPECK_ACTIVITY_LEVEL = "activity";
    public static final String RESPECK_ACTIVITY_TYPE = "activity_category";
    public static final String RESPECK_BATTERY_PERCENT = "respeck_battery_percent";
    public static final String RESPECK_REQUEST_CHARGE = "respeck_request_charge";

    public static final String RESPECK_SENSOR_TIMESTAMP = "live_rs_timestamp";
    public static final String RESPECK_SEQUENCE_NUMBER = "live_seq";
    public static final String RESPECK_STORED_SENSOR_TIMESTAMP = "live_rs_timestamp";

    public static final String RESPECK_STORED_TIMESTAMP_OFFSET = "timestamp_offset";


    public static final String AIRSPECK_DATA = "airspeck_data";
    public static final String AIRSPECK_PM1 = "pm1";
    public static final String AIRSPECK_PM2_5 = "pm2_5";
    public static final String AIRSPECK_PM10 = "pm10";
    public static final String AIRSPECK_TEMPERATURE = "temperature";
    public static final String AIRSPECK_HUMIDITY = "humidity";
    public static final String AIRSPECK_BINS_0 = "bins0";
    public static final String AIRSPECK_BINS_1 = "bins1";
    public static final String AIRSPECK_BINS_2 = "bins2";
    public static final String AIRSPECK_BINS_3 = "bins3";
    public static final String AIRSPECK_BINS_4 = "bins4";
    public static final String AIRSPECK_BINS_5 = "bins5";
    public static final String AIRSPECK_BINS_6 = "bins6";
    public static final String AIRSPECK_BINS_7 = "bins7";
    public static final String AIRSPECK_BINS_8 = "bins8";
    public static final String AIRSPECK_BINS_9 = "bins9";
    public static final String AIRSPECK_BINS_10 = "bins10";
    public static final String AIRSPECK_BINS_11 = "bins11";
    public static final String AIRSPECK_BINS_12 = "bins12";
    public static final String AIRSPECK_BINS_13 = "bins13";
    public static final String AIRSPECK_BINS_14 = "bins14";
    public static final String AIRSPECK_BINS_15 = "bins15";
    public static final String AIRSPECK_BINS_TOTAL = "bins_total";

    public static final String ACTIVITY_SUMMARY_HOUR = "hour";
    public static final String ACTIVITY_SUMMARY_DAY = "day";
    public static final String ACTIVITY_SUMMARY_WEEK = "week";

    public static final String PHONE_LOCATION = "phone_location";

    /**
     * Additional data for uploading to the server
     */
    public static final String LOC_LATITUDE = "latitude";
    public static final String LOC_LONGITUDE = "longitude";
    public static final String LOC_ALTITUDE = "altitude";
    public static final String LOC_ACCURACY = "gps_accuracy";


    /**
     * GRAPHS FRAGMENT UI
     */
    public static final int BREATHING_SIGNAL_CHART_NUMBER_OF_SAMPLES = (int) Math.round(15 * 12.5);
    public static final int PM_CHART_NUMBER_OF_SAMPLES = 30;


    public static final String READINGS_MODE_HOME_SCREEN_LIST = "0";

    public static final String READINGS_MODE_AQREADINGS_SCREEN_LIST = "0";
    public static final String READINGS_MODE_AQREADINGS_SCREEN_SEGMENTED_BARS = "1";

    public static final String FONT_SIZE_NORMAL = "1";

    public static final String MENU_BUTTONS_PADDING_NORMAL = "5";

    /**
     * Activities
     */
    public static final int ACTIVITY_STAND_SIT = 0;
    public static final int ACTIVITY_WALKING = 1;
    public static final int ACTIVITY_LYING = 2;
    public static final int WRONG_ORIENTATION = 3;
    public static final String[] ACT_CLASS_NAMES = {"Sitting/Standing", "Walking", "Lying down", "Wrong orientation"};

    /*
    Others
     */
    // This was set as a compromise between optimising communication frequency (as low as possible)
    // and update frequency (as high as possible)
    public static final int NUMBER_OF_SAMPLES_PER_BATCH = 32;
    public static final double SAMPLING_FREQUENCY = 12.5;

    // The typical difference between two RESpeck packets dependent on the number of samples per batch. If the
    // sampling frequency were exactly 12.5, this would be 32/12.5*1000 = 2560. However, on a recording of 14 hours,
    // the average time difference was closer to 2563.
    public static final int AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS = 2563;
            // (int) Math.round(NUMBER_OF_SAMPLES_PER_BATCH / SAMPLING_FREQUENCY * 1000.);

    public static final int MAXIMUM_MILLISECONDS_DEVIATION_ACTUAL_AND_CORRECTED_TIMESTAMP = 400;

    public static final long TIME_TO_NEXT_TIMESTAMP_SYNCHRONISATION = 1000 * 60 * 60 * 24;

    /**
     * Storage
     */
    public static final String EXTERNAL_DIRECTORY_STORAGE_PATH = new File(Environment.getExternalStorageDirectory() +
            "/AirRespeck").getPath() + "/";
    public static final String RESPECK_DATA_DIRECTORY_NAME = "/RESpeck/";
    public static final String VOLUME_DATA_DIRECTORY_NAME = "/Volume calibration/";
    public static final String AIRSPECK_DATA_DIRECTORY_NAME = "/Airspeck/";
    public static final String PHONE_LOCATION_DIRECTORY_NAME = "/Phone GPS/";
    public static final String DIARY_DATA_DIRECTORY_NAME = "/Diary/";

    public static final String RESPECK_DATA_HEADER = "interpolatedPhoneTimestamp,respeckTimestamp,sequenceNumber,x,y,z," +
            "breathingSignal,breathingRate,activityLevel,activityType";

    public static final String VOLUME_DATA_HEADER = "subjectName,interpolatedPhoneTimestamp,x,y,z," +
            "breathingSignal,activityTypeSelection,bagSizeSelection";

    public static final String AIRSPECK_DATA_HEADER = "phoneTimestamp,pm1,pm2_5,pm10,temperature,humidity," +
            "bin0,bin1,bin2,bin3,bin4,bin5,bin6,bin7,bin8,bin9,bin10,bin11,bin12,bin13,bin14,bin15,total," +
            "gpsLongitude,gpsLatitude,gpsAltitude,gpsAccuracy";

    public static final String GPS_PHONE_HEADER = "timestamp,longitude,latitude,altitude,accuracy";

    public static final String DIARY_HEADER = "timestamp,diary_id,answer1,answer2,answer3,answer4,answer5,answer6," +
            "answer7,answer8,pef,fev1,fev6,fvc,fef2575";

    // Air quality display on map
    public static final int PM2_5_EUROPEAN_YEARLY_AVERAGE_MAX = 25;
    public static final int PM10_EUROPEAN_YEARLY_AVERAGE_MAX = 40;

    // Request permissions
    public static final int REQUEST_CODE_LOCATION_PERMISSION = 0;
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 1;

    // Characteristics Airspeck
    public static final String QOE_CLIENT_CHARACTERISTIC = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String AIRSPECK_LIVE_CHARACTERISTIC = "00001524-1212-efde-1523-784feabcd123";
    public final static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    // Characteristics RESpeck
    public final static String RESPECK_LIVE_CHARACTERISTIC = "00002010-0000-1000-8000-00805f9b34fb";
    public final static String RESPECK_BREATHING_RATES_CHARACTERISTIC = "00002016-0000-1000-8000-00805f9b34fb";
    public final static String RESPECK_STORED_DATA_CHARACTERISTIC = "00002015-0000-1000-8000-00805f9b34fb";
    public final static String RESPECK_BATTERY_LEVEL_CHARACTERISTIC = "00002017-0000-1000-8000-00805f9b34fb";

    // Breathing signal calculation constants
    public static final int THRESHOLD_FILTER_SIZE = 60;
    public static final float MINIMUM_THRESHOLD = 0.003f;
    public static final float MAXIMUM_THRESHOLD = 0.34f;
    public static final float THRESHOLD_FACTOR = 3.2f;
    public static final float ACTIVITY_CUTOFF = 0.3f;

    // Information for config content provider
    public static class Config {
        public static final String PROVIDER_NAME = "com.specknet.pairing.provider.config";
        public static final Uri CONFIG_CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/config");
        public static final String SUBJECT_ID = "SubjectID";
        public static final String RESPECK_UUID = "RESpeckUUID";
        public static final String AIRSPECKP_UUID = "AirspeckPUUID";
        public static final String SPIROMETER_UUID = "SpirometerUUID";
        public static final String UPLOAD_TO_SERVER = "UploadToServer";
        public static final String STORE_DATA_LOCALLY = "StoreDataLocally";
        public static final String ENCRYPT_LOCAL_DATA = "EncryptLocalData";
        public static final String IS_SUPERVISED_STARTING_MODE = "IsSupervisedStartingMode";
        public static final String ENABLE_PHONE_LOCATION_STORAGE = "EnablePhoneLocationStorage";
        public static final String ENABLE_VOLUME_BAG_CALIBRATION_VIEW = "EnableVolumeBagCalibrationView";
        public static final String DISABLE_POST_FILTERING_BREATHING = "DisablePostFilteringBreathing";
    }

    public static final String SECURITY_KEY_FILE = "SecurityKeyFile";
    public static final String SECURITY_KEY = "SecurityKey";
    public static final String PROJECT_ID = "ProjectID";
}