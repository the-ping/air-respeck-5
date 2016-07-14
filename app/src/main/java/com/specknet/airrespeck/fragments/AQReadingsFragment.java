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
import com.specknet.airrespeck.utils.Utils;

import java.util.ArrayList;
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

        calculateColumnSize(mReadingsDisplayMode);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }

        mReadingItems = new ArrayList<ReadingItem>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(mReadingsDisplayMode), container, false);

        Context context = view.getContext();

        // Set the adapter
        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            }
            else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            if (mReadingsDisplayMode == 1) {
                mSegmentedBarAdapter = new ReadingItemSegmentedBarAdapter(context, getReadingItems(), mListener);
                recyclerView.setAdapter(mSegmentedBarAdapter);
            }
            else if (mReadingsDisplayMode == 2) {
                mArcProgressAdapter = new ReadingItemArcProgressAdapter(context, getReadingItems(), mListener);
                recyclerView.setAdapter(mArcProgressAdapter);
            }
        }
        else if (view instanceof ListView) {
            if (mReadingsDisplayMode == 0) {
                ListView listView = (ListView) view;
                mListViewAdapter = new ReadingItemArrayAdapter(context, getReadingItems());
                listView.setAdapter(mListViewAdapter);
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

        calculateColumnSize(mReadingsDisplayMode);
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

            segments = new ArrayList<>();
            segments.add(new Segment(-10f, 0f, "", ContextCompat.getColor(getContext(), R.color.md_blue_800)));
            segments.add(new Segment(0f, 10f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
            segments.add(new Segment(10f, 20f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(20f, 30f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(30f, 40f, "", ContextCompat.getColor(getContext(), R.color.md_red_300)));
            segments.add(new Segment(40f, 60f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
            item = new ReadingItem(getString(R.string.reading_temp), getString(R.string.reading_unit_c), 0, segments);
            mReadingItems.add(item);

            segments = new ArrayList<>();
            segments.add(new Segment(0, 29f, "", ContextCompat.getColor(getContext(), R.color.md_light_blue_400)));
            segments.add(new Segment(30f, 39f, "", ContextCompat.getColor(getContext(), R.color.md_green_300)));
            segments.add(new Segment(40f, 45f, "", ContextCompat.getColor(getContext(), R.color.md_yellow_600)));
            segments.add(new Segment(46f, 54f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(55f, 60f, "", ContextCompat.getColor(getContext(), R.color.md_red_600)));
            item = new ReadingItem(getString(R.string.reading_rel_humidity), getString(R.string.reading_unit_percent), 0, segments);
            mReadingItems.add(item);

            segments = new ArrayList<>();
            segments.add(new Segment(0, 100f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
            segments.add(new Segment(101f, 160f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(161f, 240f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
            item = new ReadingItem(getString(R.string.reading_o3), getString(R.string.reading_unit_ug_m3), 0, segments);
            mReadingItems.add(item);

            segments = new ArrayList<>();
            segments.add(new Segment(0, 200f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
            segments.add(new Segment(201f, 400f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(401f, 600f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
            item = new ReadingItem(getString(R.string.reading_no2), getString(R.string.reading_unit_ug_m3), 0, segments);
            mReadingItems.add(item);

            segments = new ArrayList<>();
            segments.add(new Segment(0, 10f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
            segments.add(new Segment(10f, 25f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(25f, 60f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
            item = new ReadingItem(getString(R.string.reading_pm1_0), getString(R.string.reading_unit_ug_m3), 0, segments);
            mReadingItems.add(item);

            segments = new ArrayList<>();
            segments.add(new Segment(0, 35f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
            segments.add(new Segment(36f, 53f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(54f, 70f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
            item = new ReadingItem(getString(R.string.reading_pm2_5), getString(R.string.reading_unit_ug_m3), 0, segments);
            mReadingItems.add(item);

            segments = new ArrayList<>();
            segments.add(new Segment(0, 50f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
            segments.add(new Segment(51f, 75f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(76f, 100f, "", ContextCompat.getColor(getContext(), R.color.md_red_400)));
            item = new ReadingItem(getString(R.string.reading_pm10), getString(R.string.reading_unit_ug_m3), 0, segments);
            mReadingItems.add(item);

            segments = new ArrayList<>();
            segments.add(new Segment(0, 60f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
            item = new ReadingItem(getString(R.string.reading_bins), getString(R.string.reading_unit_ug_m3), 0, segments);
            mReadingItems.add(item);
        }
        return mReadingItems;
    }

    /**
     * Helper setter
     */
    public void setReadings(final List<Float> values) {
        if (mReadingItems != null) {
            for (int i = 0; i < mReadingItems.size() && i < values.size(); ++i) {
                mReadingItems.get(i).value = values.get(i);
            }
            notifyDataSetChange(mReadingsDisplayMode);
        }
    }
}
