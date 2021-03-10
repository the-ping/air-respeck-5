package com.specknet.airrespeck.fragments;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.ReadingItemArrayAdapter;
import com.specknet.airrespeck.adapters.ReadingItemSegmentedBarAdapter;
import com.specknet.airrespeck.models.ReadingItem;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class sup_actsum_todayFragment extends Fragment {

    private ArrayList<ReadingItem> mReadingItems;
    private ReadingItemArrayAdapter mListViewAdapter;

    private final String KEY_HOUR = "hour";
    private final String KEY_DAY = "day";
    private final String KEY_WEEK = "week";

    private String mHourStatsString = "Loading data";
    private String mDayStatsString = "Loading data";
    private String mWeekStatsString = "Loading data";

    private TextView p_text;

    public sup_actsum_todayFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReadingItems = new ArrayList<>();
        mReadingItems.add(new ReadingItem(Constants.ACTIVITY_SUMMARY_HOUR, "", "-")); //hour, , -
        mReadingItems.add(new ReadingItem(Constants.ACTIVITY_SUMMARY_DAY, "", "-"));
        mReadingItems.add(new ReadingItem(Constants.ACTIVITY_SUMMARY_WEEK, "", "-"));
        mListViewAdapter = new ReadingItemArrayAdapter(getActivity(), mReadingItems);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sup_actsum_today, container, false);


        // Attach the adapter to a ListView
        ListView mListView = (ListView) view.findViewById(R.id.readings_list);
        mListView.setAdapter(mListViewAdapter);


        // Update readings with default "Loading data" values
        updateReadings();

        //ping add:
        p_text = view.findViewById(R.id.print_test);
        p_text.setText(mReadingItems.get(2).toString());


        if (savedInstanceState != null) {
            // Load previously calculated values
            mHourStatsString = savedInstanceState.getString(KEY_HOUR);
            mDayStatsString = savedInstanceState.getString(KEY_DAY);
            mWeekStatsString = savedInstanceState.getString(KEY_WEEK);
            updateReadings();
        } else {
            updateActivitySummary();
        }

        startActivitySummaryUpdaterTask();

        return view;
    }

    private void startActivitySummaryUpdaterTask() {
        final int delay = 5 * 60 * 1000;
        final Handler h = new Handler();

        h.postDelayed(new Runnable() {
            public void run() {
                updateActivitySummary();
                h.postDelayed(this, delay);
            }
        }, delay);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_HOUR, mHourStatsString);
        outState.putString(KEY_DAY, mDayStatsString);
        outState.putString(KEY_WEEK, mWeekStatsString);
        super.onSaveInstanceState(outState);
    }

    public void updateActivitySummary() {
        new sup_actsum_todayFragment.LoadActivityDataTask().execute();
    }

    private class LoadActivityDataTask extends AsyncTask<Void, Integer, Void> {

        protected Void doInBackground(Void... params) {
            Log.i("ActivitySummary", "Started loading stored data task");

            long now = new Date().getTime();
            long oneHourBefore = now - 60 * 60 * 1000;
            long oneDayBefore = now - 24 * 60 * 60 * 1000;
            long oneWeekBefore = now - 7 * 24 * 60 * 60 * 1000;

            int[] hourStats = new int[]{0, 0, 0};
            int[] dayStats = new int[]{0, 0, 0};
            int[] weekStats = new int[]{0, 0, 0};

            // Go through filenames in Airspeck directory
            if (getActivity() == null) {
                return null;
            }
            File dir = new File(Utils.getInstance().getDataDirectory(getActivity()) +
                    Constants.RESPECK_DATA_DIRECTORY_NAME);
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (File file : directoryListing) {
                    String fileDate = file.getName().split(" ")[4];
                    try {
                        // If file lies in specified time period, open it and read content
                        long tsFile = Utils.timestampFromString(fileDate, "yyyy-MM-dd");

                        BufferedReader reader = new BufferedReader(new FileReader(file));
                        // Skip first line as that's the header
                        reader.readLine();
                        String currentLine;
                        while ((currentLine = reader.readLine()) != null) {
                            String[] row = currentLine.split(",");
                            long tsRow = Long.parseLong(row[0]);
                            // Only if the timestamp of the currently read line is in specified time period,
                            // do we draw a circle on the map corresponding to the measurements
                            try {
                                if (tsRow >= oneHourBefore && tsRow <= now) {
                                    int activityType = Integer.parseInt(row[9]);
                                    if (activityType != -1) {
                                        if (tsFile >= Utils.roundToDay(oneWeekBefore) && tsFile <= now) {
                                            weekStats[activityType]++;
                                            if (tsFile >= Utils.roundToDay(oneDayBefore) && tsFile <= now) {
                                                dayStats[activityType]++;
                                                if (tsFile >= Utils.roundToDay(oneHourBefore) && tsFile <= now) {
                                                    hourStats[activityType]++;
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (ArrayIndexOutOfBoundsException e) {
                                Log.i("ActivitySummary", "Incomplete RESpeck data row: " + e.getMessage());
                            }
                        }
                    } catch (IOException | ParseException e) {
                        Log.i("ActivitySummary", "Error parsing RESpeck files: " + e.getMessage());
                    }
                }
            }

            // Calculate percentages
            int hourSum = Utils.sum(hourStats);
            int daySum = Utils.sum(dayStats);
            int weekSum = Utils.sum(weekStats);

            mHourStatsString = String.format(Locale.UK,
                    "Sit/stand:\u00A0%.1f%%, Walking:\u00A0%.1f%%, Lying:\u00A0%.1f%%",
                    (hourStats[0] * 1. / hourSum * 100), (hourStats[1] * 1. / hourSum * 100),
                    (hourStats[2] * 1. / hourSum * 100));
            mDayStatsString = String.format(Locale.UK,
                    "Sit/stand:\u00A0%.1f%%, Walking:\u00A0%.1f%%, Lying:\u00A0%.1f%%",
                    (dayStats[0] * 1. / daySum * 100), (dayStats[1] * 1. / daySum * 100),
                    (dayStats[2] * 1. / daySum * 100));

            mWeekStatsString = String.format(Locale.UK,
                    "Sit/stand:\u00A0%.1f%%, Walking:\u00A0%.1f%%, Lying:\u00A0%.1f%%",
                    (weekStats[0] * 1. / weekSum * 100), (weekStats[1] * 1. / weekSum * 100),
                    (weekStats[2] * 1. / weekSum * 100));




            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            updateReadings();
        }

    }

    private void updateReadings() {
        if (mReadingItems != null) {
            mReadingItems.get(0).stringValue = mHourStatsString;
            mReadingItems.get(1).stringValue = mDayStatsString;
            mReadingItems.get(2).stringValue = mWeekStatsString;
            mListViewAdapter.notifyDataSetChanged();
        }
    }

}