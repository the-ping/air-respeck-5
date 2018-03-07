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
import java.util.Locale;

/**
 * Class to allow logging to file for debugging purposes
 */

public class FileLogger {
    private static Date dateOfLastWrite = new Date(1L);
    private static OutputStreamWriter outputWriter;

    public static void logToFile(Context context, String log) {
        Date now = new Date();
        long currentWriteDay = DateUtils.truncate(now, Calendar.DAY_OF_MONTH).getTime();
        long previousWriteDay = DateUtils.truncate(dateOfLastWrite, Calendar.DAY_OF_MONTH).getTime();
        long numberOfMillisInDay = 1000 * 60 * 60 * 24;

        String filename = Utils.getInstance().getDataDirectory(context) + Constants.LOGGING_DIRECTORY_NAME +
                "Logs " + new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(now) + ".csv";

        // If we are in a new day, create a new file if necessary
        if (!new File(filename).exists() || currentWriteDay != previousWriteDay ||
                now.getTime() - dateOfLastWrite.getTime() > numberOfMillisInDay) {
            try {
                // Close old connection if there was one
                if (outputWriter != null) {
                    outputWriter.close();
                }

                // Open new connection to new file
                outputWriter = new OutputStreamWriter(
                        new FileOutputStream(filename, true));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            outputWriter.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.UK).format(now)).
                    append(": ").append(log).append("\n");
            outputWriter.flush();
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
    }
}
