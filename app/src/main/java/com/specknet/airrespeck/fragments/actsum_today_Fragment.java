package com.specknet.airrespeck.fragments;

import android.graphics.Color;
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

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.ReadingItemArrayAdapter;
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


public class actsum_today_Fragment extends Fragment {

    private ArrayList<ReadingItem> mReadingItems;
    private ReadingItemArrayAdapter mListViewAdapter;

    private final String KEY_HOUR = "hour";
    private final String KEY_DAY = "day";
    private final String KEY_WEEK = "week";

    private String mHourStatsString = "Loading data";
    private String mDayStatsString = "Loading data";
    private String mWeekStatsString = "Loading data";

    // activity label and time values
    private TextView sittime_text;
    private TextView walktime_text;
    private TextView lietime_text;
    private int sit_timeval;
    private int walk_timeval;
    private int lie_timeval;
    private int day_sit;
    private int day_walk;
    private int day_lie;

    private PieChart pieChart;
    private PieDataSet pieDataSet;
    private PieData pieData;

    public actsum_today_Fragment() {
        // Required empty public constructor
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_actsum_today_, container, false);

        // Update readings with default "Loading data" values
        updateReadings();

        // Set up pie chart
        pieChart = view.findViewById(R.id.subj_today_piechart);
        pieChart.setNoDataText("Loading chart..");
        setupPieChart();

        sittime_text = view.findViewById(R.id.subj_sit_time);
        walktime_text = view.findViewById(R.id.subj_walk_time);
        lietime_text = view.findViewById(R.id.subj_lie_time);

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

    private void updatePieChart() {

        if (pieDataSet.getEntryCount()==0) {

            pieDataSet.addEntry(new PieEntry(day_sit, "Sit/Stand: " + day_sit + "%"));
            pieDataSet.addEntry(new PieEntry(day_walk, "Walk: " + day_walk + "%"));
            pieDataSet.addEntry(new PieEntry(day_lie, "Lie: " + day_lie + "%"));

            pieData.notifyDataChanged();
            pieChart.notifyDataSetChanged();
            pieChart.invalidate();

            pieChart.setCenterText("Activity past 24h");

        } else {
            pieDataSet.removeEntryByXValue(0f);
            pieDataSet.removeEntryByXValue(1f);
            pieDataSet.removeEntryByXValue(2f);

            pieData.notifyDataChanged();
            pieChart.notifyDataSetChanged();
            pieChart.invalidate();

            pieDataSet.addEntry(new PieEntry(day_sit, "Sit/Stand " + day_sit + "%"));
            pieDataSet.addEntry(new PieEntry(day_walk, "Walk " + day_walk + "%"));
            pieDataSet.addEntry(new PieEntry(day_lie, "Lie " + day_lie + "%"));

            pieData.notifyDataChanged();
            pieChart.notifyDataSetChanged();
            pieChart.invalidate();

            pieChart.setCenterText("Activity past 24h");


        }
    }

    private void setupPieChart() {
        //medium, light, dark secondary colors
        int[] colors_list = {Color.rgb(255,183,77), Color.rgb(255,233,125), Color.rgb(200,135,25)};

        ArrayList<PieEntry> entries = new ArrayList<>();

        pieDataSet = new PieDataSet(entries, "");
        pieDataSet.setColors(colors_list);

        pieData = new PieData(pieDataSet);

        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Loading chart..");
        pieData.setDrawValues(false); //remove y values
        pieChart.setDrawEntryLabels(false); //remove x values
        pieChart.setCenterTextSize(15);

        // style legend
        pieChart.getLegend().setTextSize(16);
        pieChart.getLegend().setOrientation(Legend.LegendOrientation.VERTICAL); //vertical legend
        pieChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        pieChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);



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
        new actsum_today_Fragment.LoadActivityDataTask().execute();
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

            int[] daySec = new int[] {0, 0, 0};

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
                                                daySec[activityType]+=80; //every entry is 80ms long
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

            day_sit = (int) (dayStats[0] * 1. / daySum * 100);
            day_walk = (int) (dayStats[1] * 1. / daySum * 100);
            day_lie = (int) (dayStats[2] * 1. / daySum * 100);

            sit_timeval = daySec[0];
            walk_timeval = daySec[1];
            lie_timeval = daySec[2];

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            updateReadings();
            updatePieChart();

            // display duration in h:m:s format
            int hr = sit_timeval/1000/60/60;
            int min = (sit_timeval - hr*60*60*1000)/1000/60;
            int sec = (sit_timeval - min*60*1000)/1000;

            int walk_hr = walk_timeval/1000/60/60;
            int walk_min = (walk_timeval - walk_hr*1000*60*60)/1000/60;
            int walk_sec = (walk_timeval - walk_min*1000*60)/1000;

            int lie_hr = lie_timeval/1000/60/60;
            int lie_min = (lie_timeval - lie_hr*1000*60*60)/1000/60;
            int lie_sec = (lie_timeval - lie_min*1000*60)/1000;

            sittime_text.setText(String.format(Locale.UK, "%dh : %dm : %ds", hr, min, sec));
            walktime_text.setText(String.format(Locale.UK, "%dh : %dm : %ds", walk_hr, walk_min, walk_sec));
            lietime_text.setText(String.format(Locale.UK, "%dh : %dm : %ds", lie_hr, lie_min, lie_sec));

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