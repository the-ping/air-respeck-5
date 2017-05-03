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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

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


public class SupervisedAirspeckReadingsFragment extends BaseFragment {

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
    public SupervisedAirspeckReadingsFragment() {

    }

    @SuppressWarnings("unused")
    public static SupervisedAirspeckReadingsFragment newInstance(int columnCount) {
        SupervisedAirspeckReadingsFragment fragment = new SupervisedAirspeckReadingsFragment();
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
        switch (mReadingsModeAQReadingsScreen) {
            case Constants.READINGS_MODE_AQREADINGS_SCREEN_LIST:
                ListView listView = (ListView) view.findViewById(R.id.listView_item_list);
                mListViewAdapter = new ReadingItemArrayAdapter(context, getReadingItems());
                listView.setAdapter(mListViewAdapter);
                break;
            case Constants.READINGS_MODE_AQREADINGS_SCREEN_SEGMENTED_BARS: {
                RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.sb_item_list);

                if (mColumnCount <= 1) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                } else {
                    recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
                }

                mSegmentedBarAdapter = new ReadingItemSegmentedBarAdapter(context, getReadingItems(), mListener);
                recyclerView.setAdapter(mSegmentedBarAdapter);
                break;
            }
            case Constants.READINGS_MODE_AQREADINGS_SCREEN_ARCS: {
                RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.ap_item_list);

                if (mColumnCount <= 1) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                } else {
                    recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
                }

                mArcProgressAdapter = new ReadingItemArcProgressAdapter(context, getReadingItems(), mListener);
                recyclerView.setAdapter(mArcProgressAdapter);
                break;
            }
        }

        mIsCreated = true;

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
     *
     * @param readingsDisplayMode int Display mode preference
     */
    private void calculateColumnSize(final String readingsDisplayMode) {
        switch (readingsDisplayMode) {
            case Constants.READINGS_MODE_AQREADINGS_SCREEN_LIST:
            case Constants.READINGS_MODE_AQREADINGS_SCREEN_SEGMENTED_BARS:
                mColumnCount = 1;
                break;
            case Constants.READINGS_MODE_AQREADINGS_SCREEN_ARCS:
                Utils mUtils = Utils.getInstance(getContext());
                mColumnCount = (int) Math.floor(mUtils.getScreenSize().x /
                        getResources().getDimension(R.dimen.arc_progress_item_width));
                break;
        }
    }

    /**
     * Returns the layout according to the current display mode preference
     *
     * @param readingsDisplayMode int Display mode preference
     * @return int Layout resource id
     */
    private int getLayout(final String readingsDisplayMode) {
        switch (readingsDisplayMode) {
            case Constants.READINGS_MODE_AQREADINGS_SCREEN_LIST:
                return R.layout.fragment_aqreadings_listview;
            case Constants.READINGS_MODE_AQREADINGS_SCREEN_SEGMENTED_BARS:
                return R.layout.fragment_aqreadings_list_segmentedbar;
            case Constants.READINGS_MODE_AQREADINGS_SCREEN_ARCS:
                return R.layout.fragment_aqreadings_list_arcprogress;
            default:
                return R.layout.fragment_aqreadings_listview;
        }
    }

    private void notifyDataSetChange(final String readingsDisplayMode) {
        switch (readingsDisplayMode) {
            case Constants.READINGS_MODE_AQREADINGS_SCREEN_LIST:
                mListViewAdapter.notifyDataSetChanged();
                break;
            case Constants.READINGS_MODE_AQREADINGS_SCREEN_SEGMENTED_BARS:
                mSegmentedBarAdapter.notifyDataSetChanged();
                break;
            case Constants.READINGS_MODE_AQREADINGS_SCREEN_ARCS:
                mArcProgressAdapter.notifyDataSetChanged();
                break;
        }
    }

    /**
     * Construct and return a list with all the air quality readings.
     *
     * @return List<ReadingItem> List with all reading mReadingItems.
     */
    private ArrayList<ReadingItem> getReadingItems() {
        if (mReadingItems != null && mReadingItems.size() == 0) {
            ReadingItem item;
            ArrayList<Segment> segments;

            for (String key : Constants.READINGS_QOE) {
                switch (key) {
                    case Constants.QOE_PM1:
                        segments = new ArrayList<Segment>();
                        segments.add(
                                new Segment(0, 10f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        segments.add(
                                new Segment(11f, 25f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(
                                new Segment(26f, 60f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
                        item = new ReadingItem(getString(R.string.reading_pm1_0),
                                getString(R.string.reading_unit_ug_m3), 0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_PM2_5:
                        segments = new ArrayList<Segment>();
                        segments.add(
                                new Segment(0, 35f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        segments.add(
                                new Segment(36f, 53f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(
                                new Segment(54f, 70f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
                        item = new ReadingItem(getString(R.string.reading_pm2_5),
                                getString(R.string.reading_unit_ug_m3), 0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_PM10:
                        segments = new ArrayList<Segment>();
                        segments.add(
                                new Segment(0, 50f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        segments.add(
                                new Segment(51f, 75f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(
                                new Segment(76f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
                        item = new ReadingItem(getString(R.string.reading_pm10), getString(R.string.reading_unit_ug_m3),
                                0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_TEMPERATURE:
                        segments = new ArrayList<Segment>();
                        segments.add(
                                new Segment(-10f, 0f, "", ContextCompat.getColor(getContext(), R.color.md_blue_800)));
                        segments.add(
                                new Segment(1f, 10f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        segments.add(
                                new Segment(11f, 20f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                        segments.add(
                                new Segment(21f, 30f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(
                                new Segment(31f, 40f, "", ContextCompat.getColor(getContext(), R.color.md_red_300)));
                        segments.add(
                                new Segment(41f, 60f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                        item = new ReadingItem(getString(R.string.reading_temp), getString(R.string.reading_unit_c), 0,
                                segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_HUMIDITY:
                        segments = new ArrayList<Segment>();
                        segments.add(new Segment(0, 29f, "",
                                ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
                        segments.add(
                                new Segment(30f, 39f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                        segments.add(
                                new Segment(40f, 45f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                        segments.add(
                                new Segment(46f, 54f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(
                                new Segment(55f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                        item = new ReadingItem(getString(R.string.reading_rel_humidity),
                                getString(R.string.reading_unit_percent), 0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_O3:
                        segments = new ArrayList<Segment>();
                        segments.add(
                                new Segment(0, 100f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        segments.add(new Segment(101f, 160f, "",
                                ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(
                                new Segment(161f, 240f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
                        item = new ReadingItem(getString(R.string.reading_o3), getString(R.string.reading_unit_ug_m3),
                                0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_NO2:
                        segments = new ArrayList<Segment>();
                        segments.add(
                                new Segment(0, 200f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
                        segments.add(new Segment(201f, 400f, "",
                                ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                        segments.add(
                                new Segment(401f, 600f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
                        item = new ReadingItem(getString(R.string.reading_no2), getString(R.string.reading_unit_ug_m3),
                                0, segments);
                        mReadingItems.add(item);
                        break;

                    case Constants.QOE_BINS_TOTAL:
                        segments = new ArrayList<Segment>();
                        segments.add(
                                new Segment(0, 40f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
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
     *
     * @param temperature int The current temperature.
     * @return ArrayList<Segment> The segments list.
     */
    private ArrayList<Segment> buildRelativeHumidityScale(final int temperature) {
        ArrayList<Segment> segments = new ArrayList<Segment>();

        if (temperature <= 21f) {
            segments.add(new Segment(0f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
        } else if (temperature >= 43) {
            segments.add(new Segment(0f, 35f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(36f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
        } else {
            switch (temperature) {
                case 22:
                    segments.add(
                            new Segment(0f, 85f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
                    segments.add(
                            new Segment(86f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    break;
                case 23:
                    segments.add(
                            new Segment(0f, 75f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
                    segments.add(
                            new Segment(76f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    break;
                case 24:
                    segments.add(
                            new Segment(0f, 65f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
                    segments.add(
                            new Segment(66f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    break;
                case 25:
                    segments.add(
                            new Segment(0f, 55f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
                    segments.add(
                            new Segment(56f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    break;
                case 26:
                    segments.add(
                            new Segment(0f, 45f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
                    segments.add(new Segment(46f, 95f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    segments.add(
                            new Segment(96f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    break;
                case 27:
                    segments.add(
                            new Segment(0f, 40f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
                    segments.add(new Segment(41f, 85f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    segments.add(
                            new Segment(86f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    break;
                case 28:
                    segments.add(
                            new Segment(0f, 30f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
                    segments.add(new Segment(31f, 75f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    segments.add(
                            new Segment(76f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    break;
                case 29:
                    segments.add(new Segment(0f, 65f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    segments.add(
                            new Segment(66f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    break;
                case 30:
                    segments.add(new Segment(0f, 60f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    segments.add(
                            new Segment(61f, 90f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(91f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    break;
                case 31:
                    segments.add(new Segment(0f, 50f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    segments.add(
                            new Segment(51f, 80f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(81f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    break;
                case 32:
                    segments.add(new Segment(0f, 45f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    segments.add(
                            new Segment(46f, 70f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(71f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    break;
                case 33:
                    segments.add(new Segment(0f, 35f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    segments.add(
                            new Segment(36f, 65f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(66f, 95f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    segments.add(new Segment(96f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                    break;
                case 34:
                    segments.add(new Segment(0f, 30f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    segments.add(
                            new Segment(31f, 55f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(56f, 85f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    segments.add(new Segment(86f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                    break;
                case 35:
                    segments.add(new Segment(0f, 25f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
                    segments.add(
                            new Segment(26f, 50f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(51f, 80f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    segments.add(new Segment(81f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                    break;
                case 36:
                    segments.add(new Segment(0f, 45f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(46f, 70f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    segments.add(new Segment(71f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                    break;
                case 37:
                    segments.add(new Segment(0f, 40f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(41f, 65f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    segments.add(new Segment(66f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                    break;
                case 38:
                    segments.add(new Segment(0f, 35f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(36f, 60f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    segments.add(new Segment(61f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                    break;
                case 39:
                    segments.add(new Segment(0f, 30f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(31f, 50f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    segments.add(new Segment(51f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                    break;
                case 40:
                    segments.add(new Segment(0f, 25f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(26f, 45f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    segments.add(new Segment(46f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                    break;
                case 41:
                    segments.add(new Segment(0f, 20f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(21f, 40f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    segments.add(new Segment(41f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                    break;
                case 42:
                    segments.add(new Segment(0f, 20f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
                    segments.add(
                            new Segment(21f, 40f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
                    segments.add(new Segment(41f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
                    break;
            }
        }
        return segments;
    }

    /**
     * Helper setter
     */
    public void setReadings(final HashMap<String, Float> values) {
        if (mIsCreated) {
            int i = 0;
            for (String key : Constants.READINGS_QOE) {
                mReadingItems.get(i).value = values.get(key);
                i++;
            }

            int index = Arrays.asList(Constants.READINGS_QOE).indexOf(Constants.QOE_HUMIDITY);
            mReadingItems.get(index).segments = buildRelativeHumidityScale(
                    Math.round(values.get(Constants.QOE_TEMPERATURE)));

            notifyDataSetChange(mReadingsModeAQReadingsScreen);
        }
    }
}
