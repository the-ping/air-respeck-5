package com.specknet.airrespeck.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.fragments.items.ReadingItem;
import com.specknet.airrespeck.fragments.items.ReadingItemRecyclerViewAdapter;
import com.specknet.airrespeck.utils.Utils;

import java.util.ArrayList;
import java.util.List;


public class AirQualityFragment extends BaseFragment {

    private static final String ARG_COLUMN_COUNT = "COLUMN_COUNT";
    private int mColumnCount = 2;

    private List<ReadingItem> mReadingItems;

    private OnAirQualityFragmentInteractionListener mListener;
    private ReadingItemRecyclerViewAdapter mAdapter;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AirQualityFragment() {

    }

    @SuppressWarnings("unused")
    public static AirQualityFragment newInstance(int columnCount) {
        AirQualityFragment fragment = new AirQualityFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mReadingsDisplayMode == 0) {
            mColumnCount = 1;
        }
        else if (mReadingsDisplayMode == 1) {
            Utils mUtils = new Utils(getContext());
            mColumnCount = (int) Math.floor(mUtils.getScreenSize().x /
                    getResources().getDimension(R.dimen.circular_gauge_item_width));
        }

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }

        mReadingItems = new ArrayList<ReadingItem>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_air_quality_item_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            }
            else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            if (mReadingsDisplayMode == 0) {
                mAdapter = new ReadingItemRecyclerViewAdapter(context, R.layout.reading_list_view_item, getAirQualityReadingItems(), mListener);
            }
            else if (mReadingsDisplayMode == 1) {
                mAdapter = new ReadingItemRecyclerViewAdapter(context, R.layout.fragment_air_quality_item, getAirQualityReadingItems(), mListener);
            }

            recyclerView.setAdapter(mAdapter);
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // No listener needed for now
        /*if (context instanceof OnAirQualityFragmentInteractionListener) {
            mListener = (OnAirQualityFragmentInteractionListener) context;
        }
        else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAirQualityFragmentInteractionListener");
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

        if (mReadingsDisplayMode == 0) {
            mColumnCount = 1;
        }
        else if (mReadingsDisplayMode == 1) {
            Utils mUtils = new Utils(getContext());
            mColumnCount = (int) Math.floor(mUtils.getScreenSize().x /
                    getResources().getDimension(R.dimen.circular_gauge_item_width));
        }
    }

    /**
     * This interface must be implemented by activities that contain this fragment
     * to allow an interaction in this fragment to be communicated to the activity
     * and potentially other fragments contained in that activity.
     */
    public interface OnAirQualityFragmentInteractionListener {
        void onAirQualityFragmentInteraction(ReadingItem item);
    }

    /**
     * Construct and return a list with all the air quality readings.
     * @return List<ReadingItem> List with all reading mReadingItems.
     */
    private List<ReadingItem> getAirQualityReadingItems() {
        if (mReadingItems != null && mReadingItems.size() == 0) {
            ReadingItem item;

            item = new ReadingItem(getString(R.string.reading_temp), getString(R.string.reading_unit_c), 0);
            mReadingItems.add(item);
            item = new ReadingItem(getString(R.string.reading_rel_humidity), getString(R.string.reading_unit_percent), 0);
            mReadingItems.add(item);
            item = new ReadingItem(getString(R.string.reading_o3), getString(R.string.reading_unit_ug_m3), 0);
            mReadingItems.add(item);
            item = new ReadingItem(getString(R.string.reading_no2), getString(R.string.reading_unit_ug_m3), 0);
            mReadingItems.add(item);
            item = new ReadingItem(getString(R.string.reading_pm1_0), getString(R.string.reading_unit_ug_m3), 0);
            mReadingItems.add(item);
            item = new ReadingItem(getString(R.string.reading_pm2_5), getString(R.string.reading_unit_ug_m3), 0);
            mReadingItems.add(item);
            item = new ReadingItem(getString(R.string.reading_pm10), getString(R.string.reading_unit_ug_m3), 0);
            mReadingItems.add(item);
            item = new ReadingItem(getString(R.string.reading_bins), getString(R.string.reading_unit_ug_m3), 0);
            mReadingItems.add(item);
        }
        return mReadingItems;
    }

    /**
     * Helper setter
     */
    public void setReadings(final List<Integer> values) {
        if (mReadingItems != null) {
            for (int i = 0; i < mReadingItems.size() && i < values.size(); ++i) {
                mReadingItems.get(i).value = values.get(i);
            }
            mAdapter.notifyDataSetChanged();
        }
    }
}
