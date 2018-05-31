package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class SupervisedRESpeckRawAccerelationData extends ConnectionOverlayFragment implements RESpeckDataObserver {

    // Breathing text values
    private ArrayList<ReadingItem> mReadingItems;
    private ReadingItemArrayAdapter mListViewAdapter;


    private BreathingGraphView mBreathingGraphView;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SupervisedRESpeckRawAccerelationData() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize the utilities
        com.github.mikephil.charting.utils.Utils.init(getContext());

        ((MainActivity) getActivity()).registerRESpeckDataObserver(this);

        mReadingItems = new ArrayList<>();
        mReadingItems.add(new ReadingItem("x", "", 0f));
        mReadingItems.add(new ReadingItem("y", "", 0f));
        mReadingItems.add(new ReadingItem("z", "", 0f));
        mListViewAdapter = new ReadingItemArrayAdapter(getActivity(), mReadingItems);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_respeck_raw_acceleration, container, false);

        // Attach the adapter to a ListView for displaying the RESpeck readings
        ListView mListView = (ListView) view.findViewById(R.id.readings_list);
        mListView.setAdapter(mListViewAdapter);

        return view;
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {
        // Only update readings if they are not NaN
        mReadingItems.get(0).value = data.getAccelX();
        mReadingItems.get(1).value = data.getAccelY();
        mReadingItems.get(2).value = data.getAccelZ();
        mListViewAdapter.notifyDataSetChanged();
    }


    @Override
    public void onDestroy() {
        // Unregister this class as observer. If we haven't observed, nothing happens
        ((MainActivity) getActivity()).unregisterRESpeckDataObserver(this);
        super.onDestroy();
    }
}
