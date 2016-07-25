package com.specknet.airrespeck.utils;


import com.specknet.airrespeck.R;


public class Constants {
    /**
     * APP PROPERTIES FILE
     */
    public static final String PROPERTIES_FILE_NAME = "AirRespeck.config";


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
     * Menu icons
     */
    public static int[] menuIconsResId = {
            R.drawable.ic_home,
            R.drawable.ic_health,
            R.drawable.ic_air,
            R.drawable.ic_graphs,
            R.drawable.ic_settings
    };


    /**
     * Readings
     */
    public static final String RESPECK_BREATHING_RATE = "breathing_rate";

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
}
