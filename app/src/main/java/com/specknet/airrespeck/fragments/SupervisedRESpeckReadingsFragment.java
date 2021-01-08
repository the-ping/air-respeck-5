package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.adapters.ReadingItemArrayAdapter;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.models.ReadingItem;
import com.specknet.airrespeck.views.BreathingGraphView;

import java.util.ArrayList;

/**
 * Fragment to display the respiratory signal
 */

public class SupervisedRESpeckReadingsFragment extends ConnectionOverlayFragment implements RESpeckDataObserver {

    // Breathing text values
    private ArrayList<ReadingItem> mReadingItems;
    private ReadingItemArrayAdapter mListViewAdapter;


    private BreathingGraphView mBreathingGraphView;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SupervisedRESpeckReadingsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize the utilities
        com.github.mikephil.charting.utils.Utils.init(getContext());

        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);

        mReadingItems = new ArrayList<>();
        mReadingItems.add(new ReadingItem(getString(R.string.reading_breathing_rate),
                getString(R.string.reading_unit_bpm), Float.NaN));
        mReadingItems.add(new ReadingItem(getString(R.string.reading_avg_breathing_rate),
                getString(R.string.reading_unit_bpm), Float.NaN));
        mReadingItems.add(new ReadingItem(getString(R.string.readings_step_count), "", 0f));
        mListViewAdapter = new ReadingItemArrayAdapter(getActivity(), mReadingItems);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_respeck_readings, container, false);

        // Attach the adapter to a ListView for displaying the RESpeck readings
        ListView mListView = (ListView) view.findViewById(R.id.readings_list);
        mListView.setAdapter(mListViewAdapter);

        // Create new graph
        mBreathingGraphView = new BreathingGraphView(getActivity());

        // Inflate breathing graph into container
        FrameLayout graphFrame = (FrameLayout) view.findViewById(R.id.breathing_graph_container);
        graphFrame.addView(mBreathingGraphView);

        mBreathingGraphView.startBreathingGraphUpdates();



        return view;
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
            // Only update readings if they are not NaN
            if (!Float.isNaN(data.getBreathingRate())) {
                Log.i("RespeckReadings", "Updated breathing rate: " + data.getBreathingRate());
                mReadingItems.get(0).value = data.getBreathingRate();
            }
            mReadingItems.get(1).value = data.getAvgBreathingRate();
            mReadingItems.get(2).value = data.getMinuteStepCount();
            mListViewAdapter.notifyDataSetChanged();

            // Update the graph
            mBreathingGraphView.addToBreathingGraphQueue(data);
    }

    @Override
    public void onDetach() {
        mBreathingGraphView.stopBreathingGraphUpdates();
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        // Unregister this class as observer. If we haven't observed, nothing happens
        ((MainActivity) getActivity()).unregisterRESpeckDataObserver(this);
        super.onDestroy();
    }
}
