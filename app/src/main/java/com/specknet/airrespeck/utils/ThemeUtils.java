package com.specknet.airrespeck.utils;


import android.app.Activity;
import android.content.Intent;

import com.specknet.airrespeck.R;


public class ThemeUtils {

    // Singleton instance
    private static ThemeUtils mThemeUtils;

    // Theme ids, must match available options in Settings page.
    public final static int SMALL_FONT_SIZE = 0;
    public final static int NORMAL_FONT_SIZE = 1;
    public final static int LARGE_FONT_SIZE = 2;
    public final static int HUGE_FONT_SIZE = 3;

    // NORMAL_FONT_SIZE as default theme
    private static int mTheme = NORMAL_FONT_SIZE;

    /**
     * Private constructor for singleton class.
     */
    private ThemeUtils() {

    }

    /**
     * Get singleton class instance.
     * @return Utils Singleton class instance.
     */
    public static ThemeUtils getInstance() {
        if (mThemeUtils == null) {
            mThemeUtils = new ThemeUtils();
        }
        return mThemeUtils;
    }

    /**
     * Set theme id.
     * @param theme int Theme id.
     */
    public static void setTheme(int theme) {
        mTheme = theme;
    }

    /**
     * Set theme id and re-start activity.
     * @param activity Activity Activity instance.
     * @param theme int Theme id.
     */
    public static void changeToTheme(Activity activity, int theme) {
        mTheme = theme;

        activity.finish();
        activity.startActivity(new Intent(activity, activity.getClass()));
    }

    /**
     * Set activity theme.
     * @param activity Activity Activity instance.
     */
    public static void onActivityCreateSetTheme(Activity activity) {
        switch (mTheme) {
            case SMALL_FONT_SIZE:
                activity.setTheme(R.style.FontSizeSmall);
                break;
            default:
            case NORMAL_FONT_SIZE:
                activity.setTheme(R.style.FontSizeNormal);
                break;
            case LARGE_FONT_SIZE:
                activity.setTheme(R.style.FontSizeLarge);
                break;
            case HUGE_FONT_SIZE:
                activity.setTheme(R.style.FontSizeHuge);
                break;
        }
    }
}
