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
import com.github.mikephil.charting.utils.ColorTemplate;
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
import java.util.List;
import java.util.Locale;

public class sup_actsum_pastweekFragment extends Fragment {

    private ArrayList<ReadingItem> mReadingItems;
    private ReadingItemArrayAdapter mListViewAdapter;

    private final String KEY_HOUR = "hour";
    private final String KEY_DAY = "day";
    private final String KEY_WEEK = "week";

    private String mHourStatsString = "Loading data";
    private String mDayStatsString = "Loading data";
    private String mWeekStatsString = "Loading data";


    //ping add:

    private TextView sittime_text;
    private TextView walktime_text;
    private TextView lietime_text;
    private int sit_timeval;
    private int walk_timeval;
    private int lie_timeval;

    private int week_sit;
    private int week_walk;
    private int week_lie;

    private PieChart pieChart;
    private PieDataSet pieDataSet;
    private PieData pieData;


    public sup_actsum_pastweekFragment() {
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
        View view = inflater.inflate(R.layout.fragment_sup_actsum_pastweek, container, false);

        // Attach the adapter to a ListView
        ListView mListView = (ListView) view.findViewById(R.id.readings_list);
        mListView.setAdapter(mListViewAdapter);

        // Update readings with default "Loading data" values
        updateReadings();

        //ping add: set up pie chart
        pieChart = view.findViewById(R.id.week_piechart);
        pieChart.setNoDataText("Loading chart..");
        setupPieChart();

        sittime_text = view.findViewById(R.id.sit_time_wk);
        walktime_text = view.findViewById(R.id.walk_time_wk);
        lietime_text = view.findViewById(R.id.lie_time_wk);

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

    private void updatePieChart(int sit, int walk, int lie) {
        if (pieDataSet.getEntryCount()==0) {

            pieDataSet.addEntry(new PieEntry(week_sit, "Sit/Stand: " + week_sit + "%"));
            pieDataSet.addEntry(new PieEntry(week_walk, "Walk: " + week_walk + "%"));
            pieDataSet.addEntry(new PieEntry(week_lie, "Lie: " + week_lie + "%"));

            pieData.notifyDataChanged();
            pieChart.notifyDataSetChanged();
            pieChart.invalidate();



        } else {
            pieDataSet.removeEntryByXValue(0f);
            pieDataSet.removeEntryByXValue(1f);
            pieDataSet.removeEntryByXValue(2f);

            pieData.notifyDataChanged();
            pieChart.notifyDataSetChanged();
            pieChart.invalidate();

            pieDataSet.addEntry(new PieEntry(week_sit, "Sit/Stand"));
            pieDataSet.addEntry(new PieEntry(week_walk, "Walk"));
            pieDataSet.addEntry(new PieEntry(week_lie, "Lie"));

            pieData.notifyDataChanged();
            pieChart.notifyDataSetChanged();
            pieChart.invalidate();


        }
        pieChart.setCenterText("Activity past week");
    }

    private void setupPieChart() {

        int[] colors_list = {Color.rgb(255,183,77), Color.rgb(255,233,125), Color.rgb(200,135,25)};

        ArrayList<PieEntry> entries = new ArrayList<>();
//        entries.add(new PieEntry(day_sit, "Sit/Stand"));
//        entries.add(new PieEntry(day_walk, "Walk"));
//        entries.add(new PieEntry(day_lie, "Lie"));

        pieDataSet = new PieDataSet(entries, "");
        pieDataSet.setColors(colors_list);
//        pieDataSet.setValueTextColor(Color.BLACK);
//        pieDataSet.setValueTextSize(16f);

        pieData = new PieData(pieDataSet);

        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Loading chart..");
        pieData.setDrawValues(false); //remove y values
        pieChart.setDrawEntryLabels(false); //remove x values
        pieChart.setCenterTextSize(15);
        pieChart.getLegend().setTextSize(12);
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
        new sup_actsum_pastweekFragment.LoadActivityDataTask().execute();
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

            int[] weekSec = new int[] {0, 0, 0};

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
                                            weekSec[activityType]+=80; //every entry is 80ms long
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

            week_sit = (int) (weekStats[0] * 1. / weekSum * 100);
            week_walk = (int) (weekStats[1] * 1. / weekSum * 100);
            week_lie = (int) (weekStats[2] * 1. / weekSum * 100);

            sit_timeval = weekSec[0];
            walk_timeval = weekSec[1];
            lie_timeval = weekSec[2];

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            updateReadings();
            updatePieChart(week_sit, week_walk, week_lie);

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