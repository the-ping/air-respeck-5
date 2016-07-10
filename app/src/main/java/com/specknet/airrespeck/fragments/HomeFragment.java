package com.specknet.airrespeck.fragments;


import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.fragments.items.ReadingItem;
import com.specknet.airrespeck.fragments.items.ReadingItemArrayAdapter;
import com.specknet.airrespeck.utils.HorizontalGauge;

import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends BaseFragment implements View.OnClickListener {

    // List display mode
    private ListView mListView;
    private ArrayList<ReadingItem> mListReadingsData;
    private ReadingItemArrayAdapter mListViewAdapter;

    // Graphical display mode
    private FrameLayout mFrameLayout;
    private ArrayList<HorizontalGauge> mGraphicalReadingsData;
    private HorizontalGauge mCurrentReading;

    private ImageButton mPrevReading;
    private ImageButton mNextReading;

    // Feedback
    private TextView mFeedback;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HomeFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = null;

        if (mReadingsDisplayMode == 0) {
            initListView();
            loadListViewData();

            view = inflater.inflate(R.layout.fragment_home_list_view, container, false);

            // Attach the adapter to a ListView
            mListView = (ListView) view.findViewById(R.id.readings_list);
            mListView.setAdapter(mListViewAdapter);
        }
        else if (mReadingsDisplayMode == 1) {
            initGraphicalView();
            loadGraphicalViewData();

            view = inflater.inflate(R.layout.fragment_home_graphic_view, container, false);

            mFrameLayout = (FrameLayout) view.findViewById(R.id.reading_container);

            mPrevReading = (ImageButton) view.findViewById(R.id.prev_reading);
            mNextReading = (ImageButton) view.findViewById(R.id.next_reading);

            mPrevReading.setOnClickListener(this);
            mNextReading.setOnClickListener(this);

            mFrameLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mFrameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            // Once the view layout has finished its setup, we can add views
                            // to the frame layout
                            setupGraphicalView();
                        }
                    });
        }

        if (view != null) {
            mFeedback = (TextView) view.findViewById(R.id.feedback);
            updateFeedback();
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mFrameLayout != null) {
            mFrameLayout.removeAllViews();
        }
    }

    @Override
    public void onClick(View v) {
        if (mGraphicalReadingsData.isEmpty()) { return; }

        int index = mGraphicalReadingsData.indexOf(mCurrentReading);

        if (v.getId() == R.id.prev_reading) {
            index--;
            if (index < 0) { index = mGraphicalReadingsData.size()-1; }
        }
        else if (v.getId() == R.id.next_reading) {
            index++;
            if (index > mGraphicalReadingsData.size()-1) { index = 0; }
        }
        switchGraphicalReading(index);
    }

    /**
     * Helper setter
     */
    public void setReadings(final List<Integer> values, final int value) {
        if (mReadingsDisplayMode == 0) {
            setListReadingValues(values);
        }
        else {
            setGraphicalReadingValue(value);
        }
    }



    /***********************************************************************************************
     * LIST VIEW DISPLAY MODE
     ***********************************************************************************************/

    /**
     * Setup data structures for the List view display mode.
     */
    private void initListView() {
        if (mListReadingsData == null) {
            // Construct the data source
            mListReadingsData = new ArrayList<ReadingItem>();
        }

        if (mListViewAdapter == null) {
            // Create the adapter to convert the array to views
            mListViewAdapter = new ReadingItemArrayAdapter(getActivity(), mListReadingsData);
        }
    }

    /**
     * Setup the data for the List view display mode in {@link #mListReadingsData}.
     */
    private void loadListViewData() {
        if (mListReadingsData != null && mListReadingsData.size() == 0) {
            addListReading(getString(R.string.reading_respiratory_rate), getString(R.string.reading_unit_bpm), 0);
            addListReading(getString(R.string.reading_pm10), getString(R.string.reading_unit_ug_m3), 0);
            addListReading(getString(R.string.reading_pm2_5), getString(R.string.reading_unit_ug_m3), 0);
        }
    }

    /**
     * Add a reading to {@link #mListReadingsData} to populate the mData view.
     * @param name String Reading name.
     * @param units String Reading units.
     * @param value int Reading value.
     */
    public void addListReading(final String name, final String units, final int value) {
        ReadingItem item;
        item = new ReadingItem(name, units, value);
        mListReadingsData.add(item);
        mListViewAdapter.notifyDataSetChanged();
    }

    /**
     * Set the value field in {@link #mListReadingsData} and notifies the adaptor to show the
     * changes in the mData view.
     * @param values List<Integer>() List with the new values for all readings. NOTE: This mData
     *               must be of the same size as the number of readings added to {@link #mListReadingsData}.
     */
    public void setListReadingValues(final List<Integer> values) {
        int listDataCount = mListReadingsData.size();
        int count = values.size();

        for (int i = 0; i < listDataCount && i < count; ++i) {
            mListReadingsData.get(i).value = values.get(i);
        }

        mListViewAdapter.notifyDataSetChanged();
    }



    /***********************************************************************************************
     * GRAPHIC VIEW DISPLAY MODE
     ***********************************************************************************************/

    /**
     * Setup data structures for the Graphical view display mode.
     */
    private void initGraphicalView() {
        if (mGraphicalReadingsData == null) {
            mGraphicalReadingsData = new ArrayList<HorizontalGauge>();
        }
        mCurrentReading = null;
    }

    /**
     * Setup the data for the Graphical view display mode in {@link #mGraphicalReadingsData}.
     */
    private void loadGraphicalViewData() {
        if (mGraphicalReadingsData != null && mGraphicalReadingsData.size() == 0) {
            List<Integer> scaleCol = new ArrayList<Integer>();
            List<Float> scaleVal = new ArrayList<Float>();

            scaleCol.clear();
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.md_red_400));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.md_orange_400));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.md_green_400));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.md_orange_400));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.md_red_400));

            scaleVal.clear();
            scaleVal.add(0f);
            scaleVal.add(8f);
            scaleVal.add(12f);
            scaleVal.add(20f);
            scaleVal.add(35f);
            scaleVal.add(60f);

            addGraphicalReading(getString(R.string.reading_respiratory_rate), getString(R.string.reading_unit_bpm), scaleVal, scaleCol);

            scaleCol.clear();
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.md_green_400));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.md_orange_400));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.md_red_400));

            scaleVal.clear();
            scaleVal.add(0f);
            scaleVal.add(35f);
            scaleVal.add(50f);
            scaleVal.add(60f);

            addGraphicalReading(getString(R.string.reading_pm10), getString(R.string.reading_unit_ug_m3), scaleVal, scaleCol);

            scaleVal.clear();
            scaleVal.add(0f);
            scaleVal.add(15f);
            scaleVal.add(35f);
            scaleVal.add(60f);

            addGraphicalReading(getString(R.string.reading_pm2_5), getString(R.string.reading_unit_ug_m3), scaleVal, scaleCol);
        }
    }

    /**
     * Setup the Graphical view with the data stored in {@link #mGraphicalReadingsData}
     */
    private void setupGraphicalView() {
        if (!mGraphicalReadingsData.isEmpty()) {
            mFrameLayout.removeAllViews();

            TypedValue fontSizeAttr = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.textSize, fontSizeAttr, true);

            HorizontalGauge rv;
            for (int i = 0; i < mGraphicalReadingsData.size(); ++i) {
                rv = mGraphicalReadingsData.get(i);
                rv.setTitleFontSize(getResources().getDimensionPixelSize(fontSizeAttr.resourceId));
                rv.setValueFontSize(getResources().getDimensionPixelSize(fontSizeAttr.resourceId));
                rv.setVisibility(View.INVISIBLE);

                mFrameLayout.addView(rv, i);
            }

            mCurrentReading = mGraphicalReadingsData.get(0);
            mCurrentReading.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Switch the the reading view in the Graphical view display mode.
     * @param index int Reading index in {@link #mGraphicalReadingsData}.
     */
    private void switchGraphicalReading(int index) {
        if (mGraphicalReadingsData.isEmpty()) { return; }

        mCurrentReading.setVisibility(View.INVISIBLE);
        mCurrentReading = mGraphicalReadingsData.get(index);
        mCurrentReading.setVisibility(View.VISIBLE);
    }

    /**
     * Add a reading to {@link #mGraphicalReadingsData} to be shown in {@link #mFrameLayout}.
     * @param title String Reading mName.
     * @param units String Reading units.
     * @param scaleVal List<Float> List with the reading scale. Must contain at least 2
     *                 values: min and max.
     * @param scaleCol List<Integer> List with the int colour values for the bar.
     */
    public void addGraphicalReading(final String title, final String units,
                                 final List<Float> scaleVal, final List<Integer> scaleCol) {
        HorizontalGauge reading = new HorizontalGauge(getContext());
        //mCurrentReading.setLayoutParams(new ViewGroup.LayoutParams(600, 200));

        reading.setTitle(title);
        reading.setValueUnits(units);
        reading.setScale(scaleVal);
        reading.setColours(scaleCol);
        reading.setGradientColours(true);

        mGraphicalReadingsData.add(reading);
    }

    /**
     * Set the value in the current reading {@link #mCurrentReading}.
     * @param value int Reading value.
     */
    public void setGraphicalReadingValue(final int value) {
        mCurrentReading.setValue(value);
    }



    /***********************************************************************************************
     * FEEDBACK
     ***********************************************************************************************/

    /**
     * Updates the feedback mName
     */
    public void updateFeedback() {
        // TODO add dynamic feedback based on readings and user profile
        mFeedback.setText("Feedback message");
        mFeedback.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_running_man, 0, 0);
        //mFeedback.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_stop, 0, 0);
    }
}
