package com.specknet.airrespeck.utils;


import com.specknet.airrespeck.R;


public class Constants {
    // TODO: switch to enums instead of String constants

    /**
     * APP PROPERTIES FILE
     */
    public static final String PROPERTIES_FILE_NAME = "RESpeck.config";
    public static final String PFIELD_RESPECK_KEY = "RESpeckKey";
    public static final String PFIELD_RESPECK_UUID = "RESpeckUUID";
    public static final String PFIELD_QOEUUID = "QoEUUID";
    public static final String PFIELD_TABLET_SERIAL = "TabletSerial";
    public static final String PFIELD_PATIENT_ID = "PatientID";
    public static final String PFIELD_PATIENT_AGE = "PatientAge";
    public static final String PFIELD_USER_TYPE = "UserType";

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
    public static final String RESPECK_AVERAGE_BREATHING_RATE = "average_breathing_rate";
    public static final String RESPECK_STD_DEV_BREATHING_RATE = "std_dev_breathing_rate";
    public static final String RESPECK_N_BREATHS = "n_breaths";
    public static final String RESPECK_ACTIVITY = "activity";
    public static final String RESPECK_LIVE_SEQ = "live_seq";
    public static final String RESPECK_LIVE_RS_TIMESTAMP = "live_rs_timestamp";

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
    public static String[] READINGS_ORDER = {
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
    public static final int NUMBER_BREATHING_SIGNAL_SAMPLES_ON_CHART = 30;


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

    public static final String FONT_SIZE_NORMAL = "1";
    public static final String FONT_SIZE_LARGE = "2";

    public static final int USER_TYPE_SUBJECT = 0;
    public static final int USER_TYPE_RESEARCHER = 1;

    public static final String MENU_BUTTONS_PADDING_NORMAL = "5";

}
