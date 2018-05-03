package com.specknet.airrespeck.utils;

import android.content.Context;
import android.icu.util.Output;
import android.util.Log;

import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Class to allow logging to file for debugging purposes
 */

public class FileLogger {
    private static Date dateOfLastWrite = new Date(1L);
    private static OutputStreamWriter outputWriter;

    public static void logToFile(Context context, String log) {
        logToFile(context, log, "Logs");
    }

    public static void logToFile(Context context, String log, String filenameDescriptor) {
        Date now = new Date(Utils.getUnixTimestamp());

        String filename = Utils.getInstance().getDataDirectory(context) + Constants.LOGGING_DIRECTORY_NAME +
                filenameDescriptor + " " + new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(now) + ".csv";
        try {
            if (!new File(filename).exists()) {
                outputWriter = new OutputStreamWriter(
                        new FileOutputStream(filename, true));

                long offset = new GregorianCalendar().getTimeZone().getOffset(new Date().getTime());
                outputWriter.append(
                        "Timestamps in local time for easy debugging! Timezone offset to UTC (millis): " + offset + "\n");
                outputWriter.close();
            }

            outputWriter = new OutputStreamWriter(
                    new FileOutputStream(filename, true));
            outputWriter.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK).format(now)).
                    append(": ").append(log).append("\n");
            outputWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
