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
        public static final String QOE_UUID = "QoEUUID";
        public static final String TABLET_SERIAL = "TabletSerial";
        public static final String PATIENT_ID = "PatientID";
        public static final String PATIENT_AGE = "PatientAge";
        public static final String USER_TYPE = "UserType";
        public static final String IS_SUPERVISED_STARTING_MODE = "IsSupervisedStartingMode";
        public static final String IS_SUPERVISED_MODE_ENABLED = "EnableSupervisedMode";
        public static final String IS_SUBJECT_MODE_ENABLED = "EnableSubjectMode";
        public static final String IS_AIRSPECK_ENABLED = "EnableAirspeck";
        public static final String IS_RESPECK_DISABLED = "DisableRESpeck";
        public static final String IS_STORE_ALL_AIRSPECK_FIELDS = "StoreAllAirspeckFields";
        public static final String SHOW_SUPERVISED_AQ_GRAPHS = "EnableSupervisedAQGraphs";
        public static final String SHOW_SUPERVISED_ACTIVITY_SUMMARY = "EnableSupervisedActivitySummary";
        public static final String SHOW_SUPERVISED_RESPECK_READINGS = "EnableSupervisedRESpeckReadings";
        public static final String SHOW_SUPERVISED_AIRSPECK_READINGS = "EnableSupervisedAirspeckReadings";
        public static final String SHOW_SUPERVISED_AQ_MAP = "EnableSupervisedAQMap";
        public static final String SHOW_SUPERVISED_OVERVIEW = "EnableSupervisedOverview";
        public static final String SHOW_SUBJECT_HOME = "EnableSubjectHome";
        public static final String SHOW_SUBJECT_VALUES = "EnableSubjectValues";
        public static final String SHOW_SUBJECT_WINDMILL = "EnableSubjectWindmill";
        public static final String SHOW_PCA_GRAPH = "ShowPCAGraph";
        public static final String IS_UPLOAD_DATA_TO_SERVER = "UploadToServer";
        public static final String IS_STORE_DATA_LOCALLY = "StoreDataLocally";
        public static final String IS_STORE_MERGED_FILE = "StoreMergedFile";
        public static final String IS_SHOW_DUMMY_AIRSPECK_DATA = "ShowDummyAirspeckData";
        public static final String IS_STORE_PHONE_GPS = "EnablePhoneLocationStorage";
        public static final String IS_SHOW_VOLUME_CALIBRATION_SCREEN = "EnableVolumeCalibration";
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
    public static final String RESPECK_UUID = "respeck uuid";
    public static final String QOE_UUID = "respeck uuid";
    public static final String ACTION_PHONE_LOCATION_BROADCAST = "com.specknet.airrespeck.PHONE_LOCATION";

    /**
     * Readings
     */
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
    public static final String RESPECK_TIMESTAMP_MINUTE_AVG = "timestamp";

    public static final String INTERPOLATED_PHONE_TIMESTAMP = "timestamp";
    public static final String PHONE_TIMESTAMP_HOUR = "timestamp hour";

    public static final String RESPECK_STORED_TIMESTAMP_OFFSET = "timestamp_offset";
    public static final String RESPECK_IS_DISCONNECTED_MODE = "stored";
    public static final String RESPECK_IS_VALID_BREATHING_RATE = "valid";


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
    public static final String AIRSPECK_ALL_MEASURES = "all_airspeck_measures";

    public static final String ACTIVITY_SUMMARY_HOUR = "hour";
    public static final String ACTIVITY_SUMMARY_DAY = "day";
    public static final String ACTIVITY_SUMMARY_WEEK = "week";

    /*public static final String PHONE_LOCATION_LATITUDE = "phone_location_latitude";
    public static final String PHONE_LOCATION_LONGITUDE = "phone_location_longitude";
    public static final String PHONE_LOCATION_ALTITUDE = "phone_location_altitude";*/
    public static final String PHONE_LOCATION = "phone_location";



    /**
     * Additional data for uploading to the server
     */
    public static final String LOC_LATITUDE = "latitude";
    public static final String LOC_LONGITUDE = "longitude";
    public static final String LOC_ALTITUDE = "altitude";


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
    public static final int BREATHING_SIGNAL_CHART_NUMBER_OF_SAMPLES = (int) Math.round(15 * 12.5);
    public static final int PM_CHART_NUMBER_OF_SAMPLES = 30;


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
    public static final int WRONG_ORIENTATION = 3;
    public static final String[] ACT_CLASS_NAMES = {"Sitting/Standing", "Walking", "Lying down", "Wrong orientation"};
    public static final int NUMBER_OF_ACTIVITY_TYPES = 4;

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
    public static final String ACTIVITY_SUMMARY_FILE_PATH = EXTERNAL_DIRECTORY_STORAGE_PATH + "Activity Summary.csv";
    public static final String RESPECK_DATA_DIRECTORY_PATH = EXTERNAL_DIRECTORY_STORAGE_PATH + "RESpeck/";
    public static final String VOLUME_DATA_DIRECTORY_PATH = EXTERNAL_DIRECTORY_STORAGE_PATH + "Volume calibration/";
    public static final String AIRSPECK_DATA_DIRECTORY_PATH = EXTERNAL_DIRECTORY_STORAGE_PATH + "Airspeck/";
    public static final String MERGED_DATA_DIRECTORY_PATH = EXTERNAL_DIRECTORY_STORAGE_PATH + "RESpeck and Airspeck/";
    public static final String PHONE_LOCATION_DIRECTORY_PATH = EXTERNAL_DIRECTORY_STORAGE_PATH + "Phone GPS/";

    public static final String RESPECK_DATA_HEADER = "respeckUUID,interpolatedPhoneTimestamp,respeckTimestamp.sequenceNumber,x,y,z," +
            "breathingSignal,breathingRate,activityLevel,activityType";

    public static final String VOLUME_DATA_HEADER = "interpolatedPhoneTimestamp,x,y,z," +
            "breathingSignal,activityTypeSelection,bagSizeSelection";

    public static final String AIRSPECK_DATA_HEADER_SUBSET = "airspeckUUID,phoneTimestamp,temperature,humidity,no2,o3,bin0," +
            "gpsLongitude,gpsLatitude,gpsAltitude";

    public static final String AIRSPECK_DATA_HEADER_ALL = "airspeckUUID,phoneTimestamp,pm1,pm2_5,pm10,temperature,humidity,no2,o3," +
            "bin0,bin1,bin2,bin3,bin4,bin5,bin6,bin7,bin8,bin9,bin10,bin11,bin12,bin13,bin14,bin15,total," +
            "gpsLongitude,gpsLatitude,gpsAltitude";

    public static final String MERGED_DATA_HEADER_SUBSET = "respeckUUID,interpolatedPhoneTimestamp," +
            "respeckTimestamp.sequenceNumber,x,y,z," +
            "breathingSignal,breathingRate,activityLevel,activityType,airspeckUUID,airspeckTimestamp,temperature,humidity,no2," +
            "o3,bin0,gpsLongitude,gpsLatitude,gpsAltitude";

    public static final String MERGED_DATA_HEADER_ALL = "respeckUUID,interpolatedPhoneTimestamp,respeckTimestamp.sequenceNumber," +
            "x,y,z,breathingSignal,breathingRate,activityLevel,activityType,airspeckUUID,airspeckTimestamp,pm1,pm2_5,pm10," +
            "temperature,humidity,no2,o3,bin0,bin1,bin2,bin3,bin4,bin5,bin6,bin7,bin8,bin9,bin10,bin11,bin12,bin13," +
            "bin14,bin15,total,gpsLongitude,gpsLatitude,gpsAltitude";

    public static final String ACTIVITY_SUMMARY_HEADER = "timestamp_end_of_10_minutes,percentage_standing_sitting," +
            "percentage_walking,percentage_lying";

    public static final String GPS_PHONE_HEADER = "timestamp,longitude,latitude,altitude";

    /*
     * PCA zero mean signal calculation
     */
    public static final int NUMBER_OF_SAMPLES_REQUIRED_FOR_PCA = (int) Math.round(20 * 12.5);
    public static final int NUMBER_OF_SAMPLES_FOR_MEAN_SUBTRACTION = 69;
    public static final int NUMBER_OF_SAMPLES_FOR_MEAN_POST_FILTER = 7;
    public static final int NUMBER_OF_SAMPLES_FOR_MEAN_PRE_FILTER = 5;

    public static final int NUMBER_OF_TIMSTAMPS_FOR_SYNCHRONISATION = 100;

    // Air quality display on map
    public static final int PM2_5_EUROPEAN_YEARLY_AVERAGE_MAX = 25;
    public static final int PM10_EUROPEAN_YEARLY_AVERAGE_MAX = 40;
}
