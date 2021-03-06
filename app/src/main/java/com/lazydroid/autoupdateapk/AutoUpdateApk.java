//
//	Copyright (c) 2012 lenik terenin
//
//	Licensed under the Apache License, Version 2.0 (the "License");
//	you may not use this file except in compliance with the License.
//	You may obtain a copy of the License at
//
//		http://www.apache.org/licenses/LICENSE-2.0
//
//	Unless required by applicable law or agreed to in writing, software
//	distributed under the License is distributed on an "AS IS" BASIS,
//	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//	See the License for the specific language governing permissions and
//	limitations under the License.

package com.lazydroid.autoupdateapk;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings.Secure;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.specknet.airrespeck.BuildConfig;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Observable;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class AutoUpdateApk extends Observable {

    // This class is supposed to be instantiated in any of your activities or,
    // better yet, in Application subclass. Something along the lines of:
    //
    //	private AutoUpdateApk aua;	<-- you need to add this line of code
    //
    //	public void onCreate(Bundle savedInstanceState) {
    //		super.onCreate(savedInstanceState);
    //		setContentView(R.layout.main);
    //
    //		aua = new AutoUpdateApk(getApplicationContext());	<-- and add this line too
    //
    public AutoUpdateApk(Context ctx) {
        setupVariables(ctx, false);
    }

    // By default, the available updates are shown as notification in the notification bar. If
    // you want them to appear as popup instead, set showAsPopup to true. A popup could irritate the user,
    // but is less likely ignored than a notification.
    public AutoUpdateApk(Context ctx, boolean showAsPopup) {
        setupVariables(ctx, showAsPopup);
    }


    // Set icon for notification popup (default = application icon)
    //
    public static void setIcon(int icon) {
        appIcon = icon;
    }

    // Set name to display in notification popup (default = application label)
    //
    public static void setName(String name) {
        appName = name;
    }

    // Set update interval (in milliseconds)
    //
    // There are nice constants in this file: MINUTES, HOURS, DAYS
    // you may use them to specify update interval like: 5 * DAYS
    //
    // Please, don't specify update interval below 1 hour, this might
    // be considered annoying behaviour and result in service suspension
    //
    public void setUpdateInterval(long interval) {
        if (interval > 60 * MINUTES) {
            UPDATE_INTERVAL = interval;
        } else {
            Log_e(TAG, "update interval is too short (less than 1 hour)");
        }
    }

    // software updates will use WiFi/Ethernet only (default mode)
    //
    public static void disableMobileUpdates() {
        mobile_updates = false;
    }

    // software updates will use any internet connection, including mobile
    // might be a good idea to have 'unlimited' plan on your 3.75G connection
    //
    public static void enableMobileUpdates() {
        mobile_updates = true;
    }

    // call this if you want to perform update on demand
    // (checking for updates more often than once an hour is not recommended
    // and polling server every few minutes might be a reason for suspension)
    //
    public void checkUpdatesManually() {
        checkUpdates(true);        // force update check
    }

    public static final String AUTOUPDATE_CHECKING = "autoupdate_checking";
    public static final String AUTOUPDATE_NO_UPDATE = "autoupdate_no_update";
    public static final String AUTOUPDATE_GOT_UPDATE = "autoupdate_got_update";
    public static final String AUTOUPDATE_HAVE_UPDATE = "autoupdate_have_update";

    public void clearSchedule() {
        schedule.clear();
    }

    public void addSchedule(int start, int end) {
        schedule.add(new ScheduleEntry(start, end));
    }

    //
    // ---------- everything below this line is private and does not belong to the public API ----------
    //
    protected final static String TAG = "AutoUpdateApk";

    private final static String ANDROID_PACKAGE = "application/vnd.android.package-archive";
    //	private final static String API_URL = "http://auto-update-apk.appspot.com/check";
    private final static String API_URL = "https://specknet-pyramid-test.appspot.com/check_for_updates";

    protected static Context context = null;
    protected static SharedPreferences preferences;
    private final static String LAST_UPDATE_KEY = "last_update";
    private static long last_update = 0;

    private static int appIcon = android.R.drawable.ic_popup_reminder;
    private static int versionCode = 0;
    private static String packageName;
    private static String appName;
    private static int device_id;

    public static final long MINUTES = 60 * 1000;
    public static final long HOURS = 60 * MINUTES;
    public static final long DAYS = 24 * HOURS;

    // 3-4 hours in dev.mode, 1-2 days for stable releases
    private static long UPDATE_INTERVAL = 3 * HOURS;    // how often to check

    private static boolean mobile_updates = false;        // download updates over wifi only

    private final static Handler updateHandler = new Handler();
    protected final static String UPDATE_FILE = "update_file";
    protected final static String SILENT_FAILED = "silent_failed";
    private final static String MD5_TIME = "md5_time";
    private final static String MD5_KEY = "md5";

    private static int NOTIFICATION_ID = 0xBEEF;
    private static long WAKEUP_INTERVAL = 15 * MINUTES;

    public static boolean showUpdateAsPopup;

    private class ScheduleEntry {
        public int start;
        public int end;

        public ScheduleEntry(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static ArrayList<ScheduleEntry> schedule = new ArrayList<ScheduleEntry>();

    private Runnable periodicUpdate = new Runnable() {
        @Override
        public void run() {
            checkUpdates(false);
            updateHandler.removeCallbacks(periodicUpdate);    // remove whatever others may have posted
            updateHandler.postDelayed(this, WAKEUP_INTERVAL);
        }
    };

    private BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo currentNetworkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

            // do application-specific task(s) based on the current network state, such
            // as enabling queuing of HTTP requests when currentNetworkInfo is connected etc.
            boolean notMobile = !currentNetworkInfo.getTypeName().equalsIgnoreCase("MOBILE");
            if (currentNetworkInfo.isConnected() && (mobile_updates || notMobile)) {
                checkUpdates(false);
                updateHandler.postDelayed(periodicUpdate, UPDATE_INTERVAL);
            } else {
                updateHandler.removeCallbacks(periodicUpdate);    // no network anyway
            }
        }
    };

    @SuppressLint({"HardwareIds"})
    private void setupVariables(Context ctx, boolean showAsPopup) {
        context = ctx;

        // Set whether available update should be shown as popup or notification
        showUpdateAsPopup = showAsPopup;

        packageName = context.getPackageName();
        preferences = context.getSharedPreferences(packageName + "_" + TAG, Context.MODE_PRIVATE);
        device_id = crc32(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
        last_update = preferences.getLong("last_update", 0);
        NOTIFICATION_ID += crc32(packageName);
        // schedule.add(new ScheduleEntry(0,24));

        ApplicationInfo appinfo = context.getApplicationInfo();
        if (appinfo.icon != 0) {
            appIcon = appinfo.icon;
        } else {
            Log_w(TAG, "unable to find application icon");
        }
        if (appinfo.labelRes != 0) {
            appName = context.getString(appinfo.labelRes);
        } else {
            Log_w(TAG, "unable to find application label");
        }
        if (new File(appinfo.sourceDir).lastModified() > preferences.getLong(MD5_TIME, 0)) {
            preferences.edit().putString(MD5_KEY, MD5Hex(appinfo.sourceDir)).apply();
            preferences.edit().putLong(MD5_TIME, System.currentTimeMillis()).apply();

            String update_file = preferences.getString(UPDATE_FILE, "");
            if (update_file.length() > 0) {
                if (new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS) + "/" + update_file).delete()) {
                    preferences.edit().remove(UPDATE_FILE).remove(SILENT_FAILED).apply();
                }
            }
        }
        raiseNotification();

        if (haveInternetPermissions()) {
            context.registerReceiver(connectivityReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    private boolean checkSchedule() {
        if (schedule.size() == 0) return true;    // empty schedule always fits

        int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        for (ScheduleEntry e : schedule) {
            if (now >= e.start && now < e.end) return true;
        }
        return false;
    }

    // required in order to prevent issues in earlier Android version.
    private static void disableConnectionReuseIfNecessary() {
        // see HttpURLConnection API doc
        if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    private static String getResponseText(InputStream inStream) {
        // http://nosymbolfound.blogspot.de/2013/01/stupid-scanner-tricks.html
        return new Scanner(inStream).useDelimiter("\\A").next();
    }

    private class checkUpdateTask extends AsyncTask<Void, Void, String[]> {

        protected String[] doInBackground(Void... v) {
            Log_v(TAG, "Start update task");
            long start = System.currentTimeMillis();

            disableConnectionReuseIfNecessary();

            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("pkgname", packageName)
                        .appendQueryParameter("version", "" + versionCode)
                        .appendQueryParameter("md5", preferences.getString(MD5_KEY, "0"))
                        .appendQueryParameter("id", String.format("%08x", device_id));
                final String postParameters = builder.build().getEncodedQuery();

                // set the timeout in milliseconds until a connection is established
                // the default value is zero, that means the timeout is not used
                conn.setConnectTimeout(20000);
                // set the default socket timeout (SO_TIMEOUT) in milliseconds
                // which is the timeout for waiting for data
                conn.setReadTimeout(20000);
                conn.setRequestMethod("POST");
                conn.setFixedLengthStreamingMode(postParameters.getBytes().length);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                // Send the POST out
                PrintWriter pw = new PrintWriter(conn.getOutputStream());
                pw.print(postParameters);
                pw.close();

                conn.connect();

                Log_v(TAG, "Before reading input stream");
                InputStream in = new BufferedInputStream(conn.getInputStream());
                final String result[] = getResponseText(in).split("\n");
                in.close();

                Log_v(TAG, "Input stream: " + Arrays.toString(result));

                conn.disconnect();

                if (result.length == 3 && result[0].equalsIgnoreCase("have update")) {
                    url = new URL(result[1]);
                    conn = (HttpURLConnection) url.openConnection();
                    // set the timeout in milliseconds until a connection is established
                    // the default value is zero, that means the timeout is not used
                    conn.setConnectTimeout(20000);
                    // set the default socket timeout (SO_TIMEOUT) in milliseconds
                    // which is the timeout for waiting for data
                    conn.setReadTimeout(30000);
                    conn.setDoInput(true);
                    conn.connect();
                    Log_v(TAG, "Content type: " + conn.getContentType());

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK &&
                            conn.getContentType().equalsIgnoreCase("application/download")) {

                        in = new BufferedInputStream(conn.getInputStream());
                        String fname = result[2];
                        FileOutputStream out = new FileOutputStream(
                                Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DOWNLOADS) + "/" + fname);

                        Log_v(TAG, "Write to file in bits of 4096 bytes");

                        byte[] buffer = new byte[4096];
                        int n;
                        while ((n = in.read(buffer)) > 0) {
                            out.write(buffer, 0, n);
                        }
                        in.close();
                        out.close();

                    } else {
                        Log_e(TAG, "Bad HTTP response");
                        return null;    // bad HTTP response or invalid content type
                    }
                    conn.disconnect();

                    setChanged();
                    notifyObservers(AUTOUPDATE_GOT_UPDATE);
                } else {
                    setChanged();
                    notifyObservers(AUTOUPDATE_NO_UPDATE);
                    Log_v(TAG, "no update available");
                }
                return result;

            } catch (MalformedURLException e) {
                // handle invalid URL
                Log_e(TAG, "Malformed URL", e);
            } catch (SocketTimeoutException e) {
                // handle timeout
                Log_e(TAG, "Request timeout", e);
            } catch (IOException e) {
                // handle I/0 errors
                Log_e(TAG, "IO exception", e);
            } finally {
                long elapsed = System.currentTimeMillis() - start;
                Log_v(TAG, "update check finished in " + elapsed + "ms");
            }
            return null;
        }

        protected void onPreExecute() {
            // show progress bar or something
            Log_v(TAG, "checking if there's update on the server");
        }

        protected void onPostExecute(String[] result) {
            // kill progress bar here
            if (result != null) {
                if (result[0].equalsIgnoreCase("have update")) {
                    preferences.edit().putString(UPDATE_FILE, result[2]).apply();
                    String updateFilePath = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS) + "/" + result[2];
                    preferences.edit().putString(MD5_KEY, MD5Hex(updateFilePath)).apply();
                    preferences.edit().putLong(MD5_TIME, System.currentTimeMillis()).apply();
                }
                raiseNotification();
            } else {
                Log_v(TAG, "no reply from update server");
            }
        }
    }

    private void checkUpdates(boolean forced) {
        long now = System.currentTimeMillis();
        if (forced || (last_update + UPDATE_INTERVAL) < now && checkSchedule()) {
            new checkUpdateTask().execute();
            last_update = System.currentTimeMillis();
            preferences.edit().putLong(LAST_UPDATE_KEY, last_update).apply();

            this.setChanged();
            this.notifyObservers(AUTOUPDATE_CHECKING);
        }
    }

    protected void raiseNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) context.getSystemService(ns);

        String update_file = preferences.getString(UPDATE_FILE, "");
        if (update_file.length() > 0) {
            setChanged();
            notifyObservers(AUTOUPDATE_HAVE_UPDATE);

            Log_i(TAG, "Raised notification. Update available");

            // raise the notification
            CharSequence contentTitle = appName + " update available";
            CharSequence contentText = "Select to install";
            File updateApk = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), update_file);


            Log_i(TAG, "File exists? " + updateApk.getAbsolutePath() + " " + updateApk.exists());
            Log_i(TAG, "File size: " + updateApk.length());

            // Bugfix for Android 7 (Nougat)
            // Only Android 7's PackageManager can install from FileProvider content://
            // http://stackoverflow.com/a/39333203/2378095
            Intent notificationIntent;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                notificationIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                notificationIntent.setDataAndType(
                        FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", updateApk),
                        ANDROID_PACKAGE);
            } else {
                notificationIntent = new Intent(Intent.ACTION_VIEW);
                notificationIntent.setDataAndType(
                        Uri.parse("file://" + updateApk.getAbsolutePath()),
                        ANDROID_PACKAGE);
            }

            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
            Log_i(TAG, "Created notification for update");

            if (showUpdateAsPopup) {
                try {
                    contentIntent.send();
                    Log_i(TAG, "Popup sent");
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            } else {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                builder.setSmallIcon(appIcon);
                builder.setTicker(appName + " update");
                builder.setContentTitle(contentTitle);
                builder.setContentText(contentText);
                builder.setContentIntent(contentIntent);
                builder.setWhen(System.currentTimeMillis());
                builder.setAutoCancel(true);
                builder.setOngoing(true);

                nm.notify(NOTIFICATION_ID, builder.build());
            }
        } else {
            //nm.cancel( NOTIFICATION_ID );	// tried this, but it just doesn't do the trick =(
            nm.cancelAll();
        }
    }

    private String MD5Hex(String filename) {
        final int BUFFER_SIZE = 8192;
        byte[] buf = new byte[BUFFER_SIZE];
        int length;
        try {
            FileInputStream fis = new FileInputStream(filename);
            BufferedInputStream bis = new BufferedInputStream(fis);
            MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            while ((length = bis.read(buf)) != -1) {
                md.update(buf, 0, length);
            }
            bis.close();

            byte[] array = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte anArray : array) {
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
            }
            Log_v(TAG, "md5sum: " + sb.toString());
            return sb.toString();
        } catch (Exception e) {
            Log_e(TAG, e.getMessage());
        }
        return "md5bad";
    }

    private boolean haveInternetPermissions() {
        Set<String> required_perms = new HashSet<String>();
        required_perms.add("android.permission.INTERNET");
        required_perms.add("android.permission.ACCESS_WIFI_STATE");
        required_perms.add("android.permission.ACCESS_NETWORK_STATE");

        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        int flags = PackageManager.GET_PERMISSIONS;
        PackageInfo packageInfo = null;

        try {
            packageInfo = pm.getPackageInfo(packageName, flags);
            versionCode = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            Log_e(TAG, e.getMessage());
        }
        if (packageInfo.requestedPermissions != null) {
            for (String p : packageInfo.requestedPermissions) {
                //Log_v(TAG, "permission: " + p.toString());
                required_perms.remove(p);
            }
            if (required_perms.size() == 0) {
                return true;    // permissions are in order
            }
            // something is missing
            for (String p : required_perms) {
                Log_e(TAG, "required permission missing: " + p);
            }
        }
        Log_e(TAG, "INTERNET/WIFI access required, but no permissions are found in Manifest.xml");
        return false;
    }

    private static int crc32(String str) {
        byte bytes[] = str.getBytes();
        Checksum checksum = new CRC32();
        checksum.update(bytes, 0, bytes.length);
        return (int) checksum.getValue();
    }

    // logging facilities to enable easy overriding. thanks, Dan!
    //
    protected void Log_v(String tag, String message) {
        Log_v(tag, message, null);
    }

    protected void Log_v(String tag, String message, Throwable e) {
        log("v", tag, message, e);
    }

    protected void Log_d(String tag, String message) {
        Log_d(tag, message, null);
    }

    protected void Log_d(String tag, String message, Throwable e) {
        log("d", tag, message, e);
    }

    protected void Log_i(String tag, String message) {
        Log_d(tag, message, null);
    }

    protected void Log_i(String tag, String message, Throwable e) {
        log("i", tag, message, e);
    }

    protected void Log_w(String tag, String message) {
        Log_w(tag, message, null);
    }

    protected void Log_w(String tag, String message, Throwable e) {
        log("w", tag, message, e);
    }

    protected void Log_e(String tag, String message) {
        Log_e(tag, message, null);
    }

    protected void Log_e(String tag, String message, Throwable e) {
        log("e", tag, message, e);
    }

    protected void log(String level, String tag, String message, Throwable e) {
        if (level.equalsIgnoreCase("v")) {
            if (e == null) android.util.Log.v(tag, message);
            else android.util.Log.v(tag, message, e);
        } else if (level.equalsIgnoreCase("d")) {
            if (e == null) android.util.Log.d(tag, message);
            else android.util.Log.d(tag, message, e);
        } else if (level.equalsIgnoreCase("i")) {
            if (e == null) android.util.Log.i(tag, message);
            else android.util.Log.i(tag, message, e);
        } else if (level.equalsIgnoreCase("w")) {
            if (e == null) android.util.Log.w(tag, message);
            else android.util.Log.w(tag, message, e);
        } else {
            if (e == null) android.util.Log.e(tag, message);
            else android.util.Log.e(tag, message + ": " + e.getMessage() + Arrays.toString(e.getStackTrace()));
        }
    }

}
