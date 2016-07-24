package com.specknet.airrespeck.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.ReadingItemArcProgressAdapter;
import com.specknet.airrespeck.adapters.ReadingItemArrayAdapter;
import com.specknet.airrespeck.adapters.ReadingItemSegmentedBarAdapter;
import com.specknet.airrespeck.lib.Segment;
import com.specknet.airrespeck.models.ReadingItem;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class AQReadingsFragment extends BaseFragment {

    private static final String ARG_COLUMN_COUNT = "COLUMN_COUNT";
    private int mColumnCount = 2;

    private ArrayList<ReadingItem> mReadingItems;

    private ReadingItemArrayAdapter mListViewAdapter;
    private ReadingItemSegmentedBarAdapter mSegmentedBarAdapter;
    private ReadingItemArcProgressAdapter mArcProgressAdapter;

    private OnAQFragmentInteractionListener mListener;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AQReadingsFragment() {

    }

    @SuppressWarnings("unused")
    public static AQReadingsFragment newInstance(int columnCount) {
        AQReadingsFragment fragment = new AQReadingsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        calculateColumnSize(mReadingsModeAQReadingsScreen);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }

        mReadingItems = new ArrayList<ReadingItem>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(mReadingsModeAQReadingsScreen), container, false);

        Context context = view.getContext();

        // Set the adapter
        if (mReadingsModeAQReadingsScreen == 0) {
            if (view instanceof ListView) {
                ListView listView = (ListView) view;
                mListViewAdapter = new ReadingItemArrayAdapter(context, getReadingItems());
                listView.setAdapter(mListViewAdapter);
            }
        }
        else {
            if (view instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) view;
                if (mColumnCount <= 1) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                } else {
                    recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
                }

                if (mReadingsModeAQReadingsScreen == 1) {
                    mSegmentedBarAdapter = new ReadingItemSegmentedBarAdapter(context, getReadingItems(), mListener);
                    recyclerView.setAdapter(mSegmentedBarAdapter);
                } else if (mReadingsModeAQReadingsScreen == 2) {
                    mArcProgressAdapter = new ReadingItemArcProgressAdapter(context, getReadingItems(), mListener);
                    recyclerView.setAdapter(mArcProgressAdapter);
                }
            }
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // No listener needed for now
        /*if (context instanceof OnAQFragmentInteractionListener) {
            mListener = (OnAQFragmentInteractionListener) context;
        }
        else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAQFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();

        calculateColumnSize(mReadingsModeAQReadingsScreen);
    }

    /**
     * This interface must be implemented by activities that contain this fragment
     * to allow an interaction in this fragment to be communicated to the activity
     * and potentially other fragments contained in that activity.
     */
    public interface OnAQFragmentInteractionListener {
        void onAQReadingsFragmentInteraction(ReadingItem item);
    }

    /**
     * Set the number of columns {@link #mColumnCount}
     * @param readingsDisplayMode int Display mode preference
     */
    private void calculateColumnSize(final int readingsDisplayMode) {
        switch (readingsDisplayMode) {
            case 0:
            case 1:
                mColumnCount = 1;
                break;
            case 2:
                Utils mUtils = Utils.getInstance(getContext());
                mColumnCount = (int) Math.floor(mUtils.getScreenSize().x /
                        getResources().getDimension(R.dimen.arc_progress_item_width));
                break;
        }
    }

    /**
     * Returns the layout according to the current display mode preference
     * @param readingsDisplayMode int Display mode preference
     * @return int Layout resource id
     */
    private int getLayout(final int readingsDisplayMode) {
        switch (readingsDisplayMode) {
            case 0:
                return R.layout.fragment_aqreadings_listview;
            case 1:
                return R.layout.fragment_aqreadings_list_segmentedbar;
            case 2:
                return R.layout.fragment_aqreadings_list_arcprogress;
        }
        return 0;
    }

    private void notifyDataSetChange(final int readingsDisplayMode) {
        switch (readingsDisplayMode) {
            case 0:
                mListViewAdapter.notifyDataSetChanged();
                break;
            case 1:
                mSegmentedBarAdapter.notifyDataSetChanged();
                break;
            case 2:
                mArcProgressAdapter.notifyDataSetChanged();
                break;
        }
    }

    /**
     * Construct and return a list with all the air quality readings.
     * @return List<ReadingItem> List with all reading mReadingItems.
     */
    private ArrayList<ReadingItem> getReadingItems() {
        if (mReadingItems != null && mReadingItems.size() == 0) {
            ReadingItem item;
            ArrayList<Segment> segments;

            for (String key : Constants.READINGS_ORDER) {
                switch (key) {
                    case Constants.QOE_PM1:
                        segments = new ArrayList<Segment>();
                        segments.add(new Segment(0, 10f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        segments.add(new Segment(10f, 25f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(new Segment(25f, 60f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
                        item = new ReadingItem(getString(R.string.reading_pm1_0), getString(R.string.reading_unit_ug_m3), 0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_PM2_5:
                        segments = new ArrayList<Segment>();
                        segments.add(new Segment(0, 35f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        segments.add(new Segment(35f, 53f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(new Segment(53f, 70f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
                        item = new ReadingItem(getString(R.string.reading_pm2_5), getString(R.string.reading_unit_ug_m3), 0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_PM10:
                        segments = new ArrayList<Segment>();
                        segments.add(new Segment(0, 50f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        segments.add(new Segment(50f, 75f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(new Segment(75f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
                        item = new ReadingItem(getString(R.string.reading_pm10), getString(R.string.reading_unit_ug_m3), 0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_TEMPERATURE:
                        segments = new ArrayList<Segment>();
                        segments.add(new Segment(-10f, 0f, "", ContextCompat.getColor(getContext(), R.color.md_blue_800)));
                        segments.add(new Segment(0f, 10f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        segments.add(new Segment(10f, 20f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                        segments.add(new Segment(0f, 30f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(new Segment(30f, 40f, "", ContextCompat.getColor(getContext(), R.color.md_red_300)));
                        segments.add(new Segment(40f, 60f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                        item = new ReadingItem(getString(R.string.reading_temp), getString(R.string.reading_unit_c), 0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_REL_HUMIDITY:
                        segments = new ArrayList<Segment>();
                        segments.add(new Segment(0, 29f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
                        segments.add(new Segment(29f, 39f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                        segments.add(new Segment(39f, 45f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                        segments.add(new Segment(45f, 54f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(new Segment(54f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                        item = new ReadingItem(getString(R.string.reading_rel_humidity), getString(R.string.reading_unit_percent), 0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_O3:
                        segments = new ArrayList<Segment>();
                        segments.add(new Segment(0, 100f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        segments.add(new Segment(100f, 160f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(new Segment(160f, 240f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
                        item = new ReadingItem(getString(R.string.reading_o3), getString(R.string.reading_unit_ug_m3), 0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_NO2:
                        segments = new ArrayList<Segment>();
                        segments.add(new Segment(0, 200f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        segments.add(new Segment(200f, 400f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(new Segment(400f, 600f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
                        item = new ReadingItem(getString(R.string.reading_no2), getString(R.string.reading_unit_ug_m3), 0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_BINS_TOTAL:
                        segments = new ArrayList<Segment>();
                        segments.add(new Segment(0, 40f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        item = new ReadingItem(getString(R.string.reading_bins), "", 0, segments);
                        mReadingItems.add(item);
                        break;
                }
            }
        }
        return mReadingItems;
    }

    /**
     * Build the segments list for the relative humidity based on the current temperature value.
     * @param temperature int The current temperature.
     * @return ArrayList<Segment> The segments list.
     */
    private ArrayList<Segment> buildRelativeHumidityScale(final int temperature) {
        ArrayList<Segment> segments = new ArrayList<Segment>();

        if (temperature <= 21f) {
            segments.add(new Segment(0f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
        }
        else if (temperature == 22) {
            segments.add(new Segment(0f, 85f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
            segments.add(new Segment(85f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
        }
        else if (temperature == 23) {
            segments.add(new Segment(0f, 75f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
            segments.add(new Segment(75f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
        }
        else if (temperature == 24) {
            segments.add(new Segment(0f, 65f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
            segments.add(new Segment(65f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
        }
        else if (temperature == 25) {
            segments.add(new Segment(0f, 55f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
            segments.add(new Segment(55f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
        }
        else if (temperature == 26) {
            segments.add(new Segment(0f, 45f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
            segments.add(new Segment(45f, 95f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
            segments.add(new Segment(95f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
        }
        else if (temperature == 27) {
            segments.add(new Segment(0f, 40f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
            segments.add(new Segment(40f, 85f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
            segments.add(new Segment(85f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
        }
        else if (temperature == 28) {
            segments.add(new Segment(0f, 30f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
            segments.add(new Segment(30f, 75f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
            segments.add(new Segment(75f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
        }
        else if (temperature == 29) {
            segments.add(new Segment(0f, 65f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
            segments.add(new Segment(65f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
        }
        else if (temperature == 30) {
            segments.add(new Segment(0f, 60f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
            segments.add(new Segment(60f, 90f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(90f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
        }
        else if (temperature == 31) {
            segments.add(new Segment(0f, 50f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
            segments.add(new Segment(50f, 80f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(80f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
        }
        else if (temperature == 32) {
            segments.add(new Segment(0f, 45f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
            segments.add(new Segment(45f, 70f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(70f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
        }
        else if (temperature == 33) {
            segments.add(new Segment(0f, 35f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
            segments.add(new Segment(35f, 65f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(65f, 95f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(95f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
        }
        else if (temperature == 34) {
            segments.add(new Segment(0f, 30f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
            segments.add(new Segment(30f, 55f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(55f, 85f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(85f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
        }
        else if (temperature == 35) {
            segments.add(new Segment(0f, 25f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
            segments.add(new Segment(25f, 50f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(50f, 80f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(80f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
        }
        else if (temperature == 36) {
            segments.add(new Segment(0f, 45f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(45f, 70f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(70f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
        }
        else if (temperature == 37) {
            segments.add(new Segment(0f, 40f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(40f, 65f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(65f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
        }
        else if (temperature == 38) {
            segments.add(new Segment(0f, 35f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(35f, 60f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(60f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
        }
        else if (temperature == 39) {
            segments.add(new Segment(0f, 30f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(30f, 50f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(50f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
        }
        else if (temperature == 40) {
            segments.add(new Segment(0f, 25f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(25f, 45f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(45f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
        }
        else if (temperature == 41) {
            segments.add(new Segment(0f, 20f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(20f, 40f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(40f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
        }
        else if (temperature == 42) {
            segments.add(new Segment(0f, 20f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(20f, 40f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(40f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
        }
        else if (temperature >= 43) {
            segments.add(new Segment(0f, 35f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(35f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
        }
        return segments;
    }

    /**
     * Helper setter
     */
    public void setReadings(final HashMap<String, Float> values) {
        if (mReadingItems != null) {
            int i = 0;
            for (String key : Constants.READINGS_ORDER) {
                mReadingItems.get(i).value = values.get(key);
                i++;
            }

            int index = Arrays.asList(Constants.READINGS_ORDER).indexOf(Constants.QOE_REL_HUMIDITY);
            mReadingItems.get(index).segments = buildRelativeHumidityScale(Math.round(values.get(Constants.QOE_TEMPERATURE)));

            notifyDataSetChange(mReadingsModeAQReadingsScreen);
        }
    }
}
