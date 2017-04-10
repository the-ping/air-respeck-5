package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.ReadingItemArrayAdapter;
import com.specknet.airrespeck.models.ReadingItem;
import com.specknet.airrespeck.utils.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Darius on 21.02.2017.
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

        updateActivitySummary();

        return view;
    }

    public void updateActivitySummary() {
        List<String> values = loadSummaryValues();
        setReadings(values);
    }

    private void setReadings(final List<String> values) {
        if (mReadingItems != null) {
            for (int i = 0; i < mReadingItems.size() && i < values.size(); ++i) {
                mReadingItems.get(i).stringValue = values.get(i);
            }
            mListViewAdapter.notifyDataSetChanged();
        }
    }

    private List<String> loadSummaryValues() {
        List<String> readings = new ArrayList<>();

        final String filenameSummaryStorage = Constants.EXTERNAL_DIRECTORY_STORAGE_PATH +
                Constants.ACTIVITY_SUMMARY_FILE_PATH;

        if (new File(filenameSummaryStorage).exists()) {
            try {
                InputStream inputStream = new FileInputStream(filenameSummaryStorage);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                DateFormat simpleFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.UK);
                Date currentDate = new Date();
                String oneWeekBefore = simpleFormat.format(new Date(currentDate.getTime() - 7 * 24 * 60 * 60 * 1000));
                String oneDayBefore = simpleFormat.format(new Date(currentDate.getTime() - 24 * 60 * 60 * 1000));
                String oneHourBefore = simpleFormat.format(new Date(currentDate.getTime() - 60 * 60 * 1000));

                ArrayList<String> weekBeforeStats = new ArrayList<>();
                ArrayList<String> dayBeforeStats = new ArrayList<>();
                ArrayList<String> hourBeforeStats = new ArrayList<>();

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String parts[] = line.split("\t");
                    String timestamp = parts[0];

                    if (timestamp.compareTo(oneWeekBefore) >= 0) {
                        weekBeforeStats.add(line);
                        if (timestamp.compareTo(oneDayBefore) >= 0) {
                            dayBeforeStats.add(line);
                            if (timestamp.compareTo(oneHourBefore) >= 0) {
                                hourBeforeStats.add(line);
                            }
                        }
                    }
                }

                readings.add(getPercentageSummaryFromLines(hourBeforeStats));
                readings.add(getPercentageSummaryFromLines(dayBeforeStats));
                readings.add(getPercentageSummaryFromLines(weekBeforeStats));

                inputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            readings.add("Waiting for data");
            readings.add("Waiting for data");
            readings.add("Waiting for data");
        }

        return readings;
    }

    private String getPercentageSummaryFromLines(ArrayList<String> lines) {
        int sumSitStand = 0;
        int sumWalking = 0;
        int sumLying = 0;

        for (String line : lines) {
            String[] parts = line.split("\t");
            sumSitStand += Integer.parseInt(parts[1]);
            sumWalking += Integer.parseInt(parts[2]);
            sumLying += Integer.parseInt(parts[3]);
        }
        return String.format(Locale.UK, "Sit/stand:\u00A0%d%%, Walking:\u00A0%d%%, Lying:\u00A0%d%%",
                Math.round(sumSitStand * 1. / lines.size()),
                Math.round(sumWalking * 1. / lines.size()),
                Math.round(sumLying * 1. / lines.size()));
    }
}