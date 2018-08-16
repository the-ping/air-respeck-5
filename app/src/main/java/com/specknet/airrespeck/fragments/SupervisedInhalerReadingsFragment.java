package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.InhalerDataObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.PulseoxDataObserver;
import com.specknet.airrespeck.adapters.ReadingItemArrayAdapter;
import com.specknet.airrespeck.models.InhalerData;
import com.specknet.airrespeck.models.PulseoxData;
import com.specknet.airrespeck.models.ReadingItem;

import java.util.ArrayList;

/**
 * Fragment to display the respiratory signal
 */

public class SupervisedInhalerReadingsFragment extends ConnectionOverlayFragment implements InhalerDataObserver {

    // Breathing text values
    private ArrayList<ReadingItem> mReadingItems;
    private ReadingItemArrayAdapter mListViewAdapter;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SupervisedInhalerReadingsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((MainActivity) getActivity()).registerInhalerDataObserver(this);

        mReadingItems = new ArrayList<>();
        mReadingItems.add(new ReadingItem(getString(R.string.reading_press_time),
                getString(R.string.reading_unit_none), Float.NaN));
        mListViewAdapter = new ReadingItemArrayAdapter(getActivity(), mReadingItems);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_inhaler_readings, container, false);

        // Attach the adapter to a ListView for displaying the Pulseox readings
        ListView mListView = (ListView) view.findViewById(R.id.readings_list);
        mListView.setAdapter(mListViewAdapter);

        return view;
    }

    @Override
    public void updateInhalerData(InhalerData data) {
        // Only update readings if they are not NaN
        if (!Float.isNaN(data.getPhoneTimestamp())) {
            Log.i("InhalerReadings", "Last inhaler press: " + data.getPhoneTimestamp());
            mReadingItems.get(0).value = data.getPhoneTimestamp();
            mListViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        // Unregister this class as observer. If we haven't observed, nothing happens
        ((MainActivity) getActivity()).unregisterInhalerDataObserver(this);
        super.onDestroy();
    }
}
