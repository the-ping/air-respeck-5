package com.specknet.airrespeck.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.android.gms.maps.model.LatLng;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.ReadingItemArrayAdapter;
import com.specknet.airrespeck.models.AirspeckMapData;
import com.specknet.airrespeck.models.ReadingItem;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment to show a summary of the Activity in the last hour, day, week
 */

public class SupervisedActivitySummaryFragment extends BaseFragment {

    private ArrayList<ReadingItem> mReadingItems;
    private ReadingItemArrayAdapter mListViewAdapter;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SupervisedActivitySummaryFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mReadingItems = new ArrayList<>();
        mReadingItems.add(new ReadingItem(Constants.ACTIVITY_SUMMARY_HOUR, "", "-"));
        mReadingItems.add(new ReadingItem(Constants.ACTIVITY_SUMMARY_DAY, "", "-"));
        mReadingItems.add(new ReadingItem(Constants.ACTIVITY_SUMMARY_WEEK, "", "-"));
        mListViewAdapter = new ReadingItemArrayAdapter(getActivity(), mReadingItems);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity_summary, container, false);

        // Attach the adapter to a ListView
        ListView mListView = (ListView) view.findViewById(R.id.readings_list);
        mListView.setAdapter(mListViewAdapter);

        List<String> defaultReadings = new ArrayList<>();
        defaultReadings.add("Loading data");
        defaultReadings.add("Loading data");
        defaultReadings.add("Loading data");
        setReadings(defaultReadings);

        updateActivitySummary();

        return view;
    }

    public void updateActivitySummary() {
        new LoadActivityDataTask().execute();
    }

    private class LoadActivityDataTask extends AsyncTask<Void, Integer, List<String>> {

        protected List<String> doInBackground(Void... params) {
            Log.i("AirspeckMap", "Started loading stored data task");

            long now = new Date().getTime();

            long oneHourBefore = now - 60 * 60 * 1000;
            long oneDayBefore = now - 24 * 60 * 60 * 1000;
            long oneWeekBefore = now - 7 * 24 * 60 * 60 * 1000;

            int[] hourStats = new int[]{0, 0, 0};
            int[] dayStats = new int[]{0, 0, 0};
            int[] weekStats = new int[]{0, 0, 0};

            // Go through filenames in Airspeck directory
            File dir = new File(Constants.RESPECK_DATA_DIRECTORY_PATH);
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (File file : directoryListing) {
                    String fileDate = file.getName().split(" ")[0];
                    try {
                        // If file lies in specified time period, open it and read content
                        long tsFile = Utils.timestampFromString(fileDate, "yyyy-MM-dd");

                        if (tsFile >= Utils.roundToDay(oneHourBefore) && tsFile <= now) {
                            BufferedReader reader = new BufferedReader(new FileReader(file));
                            // Skip first line as that's the header
                            reader.readLine();
                            String currentLine;
                            while ((currentLine = reader.readLine()) != null) {
                                String[] row = currentLine.split(",");
                                long tsRow = Long.parseLong(row[1]);
                                // Only if the timestamp of the currently read line is in specified time period,
                                // do we draw a circle on the map corresponding to the measurements
                                if (tsRow >= oneHourBefore && tsRow <= now) {
                                    int activityType = Integer.parseInt(row[10]);
                                    if (activityType != -1) {
                                        hourStats[activityType]++;
                                        dayStats[activityType]++;
                                        weekStats[activityType]++;
                                    }
                                }
                            }
                            reader.close();
                        } else if (tsFile >= Utils.roundToDay(oneDayBefore) && tsFile <= now) {
                            BufferedReader reader = new BufferedReader(new FileReader(file));
                            // Skip first line as that's the header
                            reader.readLine();
                            String currentLine;
                            while ((currentLine = reader.readLine()) != null) {
                                String[] row = currentLine.split(",");
                                long tsRow = Long.parseLong(row[1]);
                                // Only if the timestamp of the currently read line is in specified time period,
                                // do we draw a circle on the map corresponding to the measurements
                                if (tsRow >= oneDayBefore && tsRow <= now) {
                                    int activityType = Integer.parseInt(row[10]);
                                    if (activityType != -1) {
                                        dayStats[activityType]++;
                                        weekStats[activityType]++;
                                    }
                                }
                            }
                            reader.close();
                        } else if (tsFile >= Utils.roundToDay(oneWeekBefore) && tsFile <= now) {
                            BufferedReader reader = new BufferedReader(new FileReader(file));
                            // Skip first line as that's the header
                            reader.readLine();
                            String currentLine;
                            while ((currentLine = reader.readLine()) != null) {
                                String[] row = currentLine.split(",");
                                long tsRow = Long.parseLong(row[1]);
                                // Only if the timestamp of the currently read line is in specified time period,
                                // do we draw a circle on the map corresponding to the measurements
                                if (tsRow >= oneWeekBefore && tsRow <= now) {
                                    int activityType = Integer.parseInt(row[10]);
                                    if (activityType != -1) {
                                        weekStats[activityType]++;
                                    }
                                }
                            }
                            reader.close();
                        }
                    } catch (IOException | ParseException e) {
                        Log.i("ActivitySummary", "Error parsing RESpeck files: " + e.getMessage());
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.i("ActivitySummary", "Incomplete RESpeck data row: " + e.getMessage());
                    }
                }
            }

            // Calculate percentages
            List<String> readings = new ArrayList<>(3);
            int hourSum = Utils.sum(hourStats);
            int daySum = Utils.sum(dayStats);
            int weekSum = Utils.sum(weekStats);

            readings.add(0, String.format(Locale.UK, "Sit/stand:\u00A0%.1f%%, Walking:\u00A0%.1f%%, Lying:\u00A0%.1f%%",
                    (hourStats[0] * 1. / hourSum * 100), (hourStats[1] * 1. / hourSum * 100),
                    (hourStats[2] * 1. / hourSum * 100)));
            readings.add(1, String.format(Locale.UK, "Sit/stand:\u00A0%.1f%%, Walking:\u00A0%.1f%%, Lying:\u00A0%.1f%%",
                    (dayStats[0] * 1. / daySum * 100), (dayStats[1] * 1. / daySum * 100),
                    (dayStats[2] * 1. / daySum * 100)));
            readings.add(2, String.format(Locale.UK, "Sit/stand:\u00A0%.1f%%, Walking:\u00A0%.1f%%, Lying:\u00A0%.1f%%",
                    (weekStats[0] * 1. / weekSum * 100), (weekStats[1] * 1. / weekSum * 100),
                    (weekStats[2] * 1. / weekSum * 100)));
            return readings;
        }

        @Override
        protected void onPostExecute(List<String> readings) {
            setReadings(readings);
        }
    }

    private void setReadings(List<String> readings) {
        if (mReadingItems != null) {
            for (int i = 0; i < mReadingItems.size() && i < readings.size(); ++i) {
                mReadingItems.get(i).stringValue = readings.get(i);
            }
            mListViewAdapter.notifyDataSetChanged();
        }
    }
}