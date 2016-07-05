package com.specknet.airrespeck.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.utils.ReadingView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class HomeFragment extends Fragment implements View.OnClickListener {

    // Preferences
    private SharedPreferences mSettings;
    private int mReadingsDisplayMode = -1;

    // List display mode
    private ListView mListView;
    private List<HashMap<String, String>> mListReadingsData;
    private SimpleAdapter mSimpleAdapter;

    // Cyclic display mode
    private FrameLayout mFrameLayout;
    private ArrayList<ReadingView> mCyclicReadingsData;
    private ReadingView mCurrentReading;

    private ImageButton mPrevReading;
    private ImageButton mNextReading;

    // Feedback
    private TextView mFeedback;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = PreferenceManager.getDefaultSharedPreferences(getContext());
        mReadingsDisplayMode = Integer.valueOf(mSettings.getString("home_screen_readings_display_mode", "0"));
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

            mListView = (ListView) view.findViewById(R.id.readings_list);
            mListView.setAdapter(mSimpleAdapter);
        }
        else if (mReadingsDisplayMode == 1) {
            initCyclicView();
            loadCyclicViewData();

            view = inflater.inflate(R.layout.fragment_home_cyclic_view, container, false);

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
                            setupCyclicView();
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
    public void onResume() {
        super.onResume();

        int newVal = Integer.valueOf(mSettings.getString("home_screen_readings_display_mode", "0"));

        if (mReadingsDisplayMode != newVal) {
            mReadingsDisplayMode = newVal;

            // Destroy and Re-create this fragment's view.
            final FragmentManager fm = this.getActivity().getSupportFragmentManager();
            fm.beginTransaction().
                    detach(this).
                    attach(this).
                    commit();
        }
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
        if (mCyclicReadingsData.isEmpty()) { return; }

        int index = mCyclicReadingsData.indexOf(mCurrentReading);

        if (v.getId() == R.id.prev_reading) {
            index--;
            if (index < 0) { index = mCyclicReadingsData.size()-1; }
        }
        else if (v.getId() == R.id.next_reading) {
            index++;
            if (index > mCyclicReadingsData.size()-1) { index = 0; }
        }
        switchCyclicReading(index);
    }

    /**
     * Helper setter
     */
    public void setReadings(final List<Integer> values, final int value) {
        if (mReadingsDisplayMode == 0) {
            setListReadingValues(values);
        }
        else {
            setCyclicReadingValue(value);
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
            mListReadingsData = new ArrayList<>();
        }

        if (mSimpleAdapter == null) {
            mSimpleAdapter = new SimpleAdapter(getActivity(), mListReadingsData, R.layout.reading_list_item,
                    new String[]{"description", "value", "units"},
                    new int[]{R.id.reading_item_description,
                            R.id.reading_item_value,
                            R.id.reading_item_units});
        }
    }

    /**
     * Setup the data for the List view display mode in {@link #mListReadingsData}.
     */
    private void loadListViewData() {
        if (mListReadingsData != null && mListReadingsData.size() == 0) {
            addListReading(getString(R.string.reading_respiratory_rate), 0, getString(R.string.reading_unit_bpm));
            addListReading(getString(R.string.reading_pm10), 0, getString(R.string.reading_unit_ug_m3));
            addListReading(getString(R.string.reading_pm2_5), 0, getString(R.string.reading_unit_ug_m3));
        }
    }

    /**
     * Add a reading to {@link #mListReadingsData} to populate the mData view.
     * @param description String Reading name.
     * @param value int Reading value.
     * @param units String Reading units.
     */
    public void addListReading(final String description, final int value, final String units) {
        HashMap<String, String> map = new HashMap<>();
        map.put("description", description);
        map.put("value", (value == 0) ? "" : String.valueOf(value));
        map.put("units", units);

        mListReadingsData.add(map);
        mSimpleAdapter.notifyDataSetChanged();
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
            mListReadingsData.get(i).put("value", String.valueOf(values.get(i)));
        }

        mSimpleAdapter.notifyDataSetChanged();
    }



    /***********************************************************************************************
     * CYCLIC VIEW DISPLAY MODE
     ***********************************************************************************************/

    /**
     * Setup data structures for the Cyclic view display mode.
     */
    private void initCyclicView() {
        if (mCyclicReadingsData == null) {
            mCyclicReadingsData = new ArrayList<>();
        }
        mCurrentReading = null;
    }

    /**
     * Setup the data for the Cyclic view display mode in {@link #mCyclicReadingsData}.
     */
    private void loadCyclicViewData() {
        if (mCyclicReadingsData != null && mCyclicReadingsData.size() == 0) {
            List<Integer> scaleCol = new ArrayList<>();
            List<Float> scaleVal = new ArrayList<>();

            scaleCol.clear();
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorRed));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorOrange));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorGreen));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorOrange));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorRed));

            scaleVal.clear();
            scaleVal.add(0f);
            scaleVal.add(8f);
            scaleVal.add(12f);
            scaleVal.add(20f);
            scaleVal.add(35f);
            scaleVal.add(60f);

            addCyclicReading(getString(R.string.reading_respiratory_rate), getString(R.string.reading_unit_bpm), scaleVal, scaleCol);

            scaleCol.clear();
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorGreen));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorOrange));
            scaleCol.add(ContextCompat.getColor(getContext(), R.color.colorRed));

            scaleVal.clear();
            scaleVal.add(0f);
            scaleVal.add(35f);
            scaleVal.add(50f);
            scaleVal.add(60f);

            addCyclicReading(getString(R.string.reading_pm10), getString(R.string.reading_unit_ug_m3), scaleVal, scaleCol);

            scaleVal.clear();
            scaleVal.add(0f);
            scaleVal.add(15f);
            scaleVal.add(35f);
            scaleVal.add(60f);

            addCyclicReading(getString(R.string.reading_pm2_5), getString(R.string.reading_unit_ug_m3), scaleVal, scaleCol);
        }
    }

    /**
     * Setup the Cyclic view with the data stored in {@link #mCyclicReadingsData}
     */
    private void setupCyclicView() {
        if (!mCyclicReadingsData.isEmpty()) {
            mFrameLayout.removeAllViews();
            ReadingView rv;

            for (int i = 0; i < mCyclicReadingsData.size(); ++i) {
                rv = mCyclicReadingsData.get(i);
                rv.setVisibility(View.INVISIBLE);
                mFrameLayout.addView(rv, i);
            }

            mCurrentReading = mCyclicReadingsData.get(0);
            mCurrentReading.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Switch the the reading view in the Cyclic view display mode.
     * @param index int Reading index in {@link #mCyclicReadingsData}.
     */
    private void switchCyclicReading(int index) {
        if (mCyclicReadingsData.isEmpty()) { return; }

        mCurrentReading.setVisibility(View.INVISIBLE);
        mCurrentReading = mCyclicReadingsData.get(index);
        mCurrentReading.setVisibility(View.VISIBLE);
    }

    /**
     * Add a reading to {@link #mCyclicReadingsData} to be shown in {@link #mFrameLayout}.
     * @param title String Reading title.
     * @param units String Reading units.
     * @param scaleVal List<Float> List with the reading scale. Must contain at least 2
     *                 values: min and max.
     * @param scaleCol List<Integer> List with the int colour values for the bar.
     */
    public void addCyclicReading(final String title, final String units,
                                 final List<Float> scaleVal, final List<Integer> scaleCol) {
        ReadingView reading = new ReadingView(getContext());
        //mCurrentReading.setLayoutParams(new ViewGroup.LayoutParams(600, 200));

        reading.setTitle(title);
        reading.setValueUnits(units);
        reading.setScale(scaleVal);
        reading.setColours(scaleCol);
        reading.setGradientColours(true);

        mCyclicReadingsData.add(reading);
    }

    /**
     * Set the value in the current reading {@link #mCurrentReading}.
     * @param value int Reading value.
     */
    public void setCyclicReadingValue(final int value) {
        mCurrentReading.setValue(value);
    }



    /***********************************************************************************************
     * FEEDBACK
     ***********************************************************************************************/

    /**
     * Updates the feedback content
     */
    public void updateFeedback() {
        // TODO add dynamic feedback based on readings and user profile
        mFeedback.setText("Feedback message");
        mFeedback.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_running_man, 0, 0);
        //mFeedback.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_stop, 0, 0);
    }
}
