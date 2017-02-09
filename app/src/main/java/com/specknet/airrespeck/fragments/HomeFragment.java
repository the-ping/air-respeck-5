package com.specknet.airrespeck.fragments;


import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.lib.Segment;
import com.specknet.airrespeck.lib.SegmentedBar;
import com.specknet.airrespeck.lib.SegmentedBarSideTextStyle;
import com.specknet.airrespeck.models.ReadingItem;
import com.specknet.airrespeck.adapters.ReadingItemArrayAdapter;
import com.specknet.airrespeck.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import static com.specknet.airrespeck.R.styleable.SegmentedBar;


public class HomeFragment extends BaseFragment implements View.OnClickListener {

    private ArrayList<ReadingItem> mReadingItems;

    // List View
    private ReadingItemArrayAdapter mListViewAdapter;

    // Segmented Bar
    private TextView mCurrentReadingName;
    private FrameLayout mFrameLayout;
    private ArrayList<SegmentedBar> mSegmentedBars;
    private SegmentedBar mCurrentSegmentedBar;

    // Feedback
    private TextView mFeedback;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HomeFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();
        loadData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = null;

        if (mReadingsModeHomeScreen == Integer.parseInt(Constants.READINGS_MODE_HOME_SCREEN_LIST)) {
            view = inflater.inflate(R.layout.fragment_home_listview, container, false);

            // Attach the adapter to a ListView
            ListView mListView = (ListView) view.findViewById(R.id.readings_list);
            mListView.setAdapter(mListViewAdapter);
        }
        else if (mReadingsModeHomeScreen == Integer.parseInt(Constants.READINGS_MODE_HOME_SCREEN_SEGMENTED_BARS)) {
            view = inflater.inflate(R.layout.fragment_home_segmentedbar, container, false);

            mCurrentReadingName = (TextView) view.findViewById(R.id.current_reading_name);

            mFrameLayout = (FrameLayout) view.findViewById(R.id.reading_container);

            ImageButton mPrevReading = (ImageButton) view.findViewById(R.id.prev_reading);
            ImageButton mNextReading = (ImageButton) view.findViewById(R.id.next_reading);

            mPrevReading.setOnClickListener(this);
            mNextReading.setOnClickListener(this);

            mFrameLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mFrameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            // Once the view layout has finished its setup, we can add views
                            // to the frame layout
                            setupSegmentedBars();
                        }
                    });
        }

        if (view != null) {
            mConnectingLayout = (LinearLayout) view.findViewById(R.id.connecting_layout);

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
        if (mSegmentedBars.isEmpty()) { return; }

        int index = mSegmentedBars.indexOf(mCurrentSegmentedBar);

        if (v.getId() == R.id.prev_reading) {
            index--;
            if (index < 0) { index = mSegmentedBars.size()-1; }
        }
        else if (v.getId() == R.id.next_reading) {
            index++;
            if (index > mSegmentedBars.size()-1) { index = 0; }
        }
        switchSegmentedBar(index);
    }



    /***********************************************************************************************
     * READINGS
     ***********************************************************************************************/

    /**
     * Initialize data structures and view adapters.
     */
    private void init() {
        if (mReadingItems == null) {
            // Construct the data source
            mReadingItems = new ArrayList<ReadingItem>();
        }

        if (mListViewAdapter == null) {
            // Create the adapter to convert the array to views
            mListViewAdapter = new ReadingItemArrayAdapter(getActivity(), mReadingItems);
        }

        if (mSegmentedBars == null) {
            mSegmentedBars = new ArrayList<SegmentedBar>();
        }
        mCurrentSegmentedBar = null;
    }

    /**
     * Load reading data in {@link #mReadingItems}.
     */
    private void loadData() {
        if (mReadingItems != null && mReadingItems.size() == 0) {
            ReadingItem item;
            ArrayList<Segment> segments;

            segments = new ArrayList<>();
            segments.add(new Segment(0f, 11f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            segments.add(new Segment(12f, 20f, "", ContextCompat.getColor(getContext(), R.color.md_green_400)));
            segments.add(new Segment(21f, 40f, "", ContextCompat.getColor(getContext(), R.color.md_orange_400)));
            item = new ReadingItem(getString(R.string.reading_respiratory_rate), getString(R.string.reading_unit_bpm), 0, segments);
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
        }
    }

    /**
     * Setup the segmented bars in {@link #mSegmentedBars}
     */
    private void setupSegmentedBars() {
        if (mSegmentedBars.isEmpty()) {
            SegmentedBar bar;
            for (int i = 0; i < mReadingItems.size(); ++i) {
                bar = new SegmentedBar(getContext());
                bar.setValueWithUnit(mReadingItems.get(i).value, mReadingItems.get(i).unit);
                bar.setSegments(mReadingItems.get(i).segments);
                bar.setSideTextStyle(SegmentedBarSideTextStyle.TWO_SIDED);
                bar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                bar.setPadding(10, 10, 10, 10);
                mSegmentedBars.add(bar);
            }
        }

        mFrameLayout.removeAllViews();

        for (int i = 0; i < mSegmentedBars.size(); ++i) {
            SegmentedBar sb = mSegmentedBars.get(i);

            sb.setVisibility(View.INVISIBLE);

            mFrameLayout.addView(sb, i);
        }

        mCurrentSegmentedBar = mSegmentedBars.get(0);
        mCurrentSegmentedBar.setVisibility(View.VISIBLE);

        mCurrentReadingName.setText(mReadingItems.get(0).name);
    }

    /**
     * Look for the current segmented bar and updates its value with the data from the data set
     */
    private void notifySegmentedBarDataSetChange() {
        for (int i = 0; i < mSegmentedBars.size(); ++i) {
            mSegmentedBars.get(i).setValue(mReadingItems.get(i).value);
        }
    }

    /**
     * Switch the the reading view in the Graphical view display mode.
     * @param index int Reading index in {@link #mSegmentedBars}.
     */
    private void switchSegmentedBar(int index) {
        if (mSegmentedBars.isEmpty()) { return; }

        mCurrentSegmentedBar.setVisibility(View.INVISIBLE);
        mCurrentSegmentedBar = mSegmentedBars.get(index);
        mCurrentSegmentedBar.setVisibility(View.VISIBLE);

        mCurrentReadingName.setText(mReadingItems.get(index).name);
    }


    /**
     * Helper setter
     */
    public void setReadings(final List<Float> values) {
        if (mReadingItems != null) {
            for (int i = 0; i < mReadingItems.size() && i < values.size(); ++i) {
                mReadingItems.get(i).value = values.get(i);
            }
            mListViewAdapter.notifyDataSetChanged();
            notifySegmentedBarDataSetChange();
        }
    }



    /***********************************************************************************************
     * FEEDBACK
     ***********************************************************************************************/

    /**
     * Updates the feedback mName
     */
    public void updateFeedback() {
        // TODO Add dynamic feedback based on user respiratory model, current sensors' readings, and user profile (i.e. age, gender)
        //mFeedback.setText("Feedback message");
        //mFeedback.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_running_man, 0, 0);
        //mFeedback.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_stop, 0, 0);
    }
}
