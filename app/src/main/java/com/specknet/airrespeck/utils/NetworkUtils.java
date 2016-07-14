package com.specknet.airrespeck.utils;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


public class NetworkUtils {

    private static NetworkUtils mNetworkUtils;
    private final Context mContext;

    /**
     * Private constructor.
     * @param context Context Application context.
     */
    private NetworkUtils(Context context) {
        mContext = context;
    }

    /**
     * Get singleton class instance.
     * @param context Context Application context.
     * @return NetworkUtils Singleton class instance.
     */
    public static NetworkUtils getInstance(Context context) {
        if (mNetworkUtils == null) {
            mNetworkUtils = new NetworkUtils(context);
        }
        return mNetworkUtils;
    }

    /**
     * Check if Internet connectivity is available
     * @return boolean True if Internet is available, else false.
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }
}
