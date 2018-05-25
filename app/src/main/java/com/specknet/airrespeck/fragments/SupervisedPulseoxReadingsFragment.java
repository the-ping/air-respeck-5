package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.PulseoxDataObserver;
import com.specknet.airrespeck.adapters.ReadingItemArrayAdapter;
import com.specknet.airrespeck.models.PulseoxData;
import com.specknet.airrespeck.models.ReadingItem;

import java.util.ArrayList;

/**
 * Fragment to display the respiratory signal
 */

public class SupervisedPulseoxReadingsFragment extends ConnectionOverlayFragment implements PulseoxDataObserver {

    // Breathing text values
    private ArrayList<ReadingItem> mReadingItems;
    private ReadingItemArrayAdapter mListViewAdapter;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SupervisedPulseoxReadingsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((MainActivity) getActivity()).registerPulseoxDataObserver(this);

        mReadingItems = new ArrayList<>();
        mReadingItems.add(new ReadingItem(getString(R.string.reading_pulse),
                getString(R.string.reading_unit_beats_pm), Float.NaN));
        mReadingItems.add(new ReadingItem(getString(R.string.reading_spo2),
                getString(R.string.reading_unit_percent), Float.NaN));
        mListViewAdapter = new ReadingItemArrayAdapter(getActivity(), mReadingItems);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pulseox_readings, container, false);

        // Attach the adapter to a ListView for displaying the Pulseox readings
        ListView mListView = (ListView) view.findViewById(R.id.readings_list);
        mListView.setAdapter(mListViewAdapter);

        return view;
    }

    @Override
    public void updatePulseoxData(PulseoxData data) {
        // Only update readings if they are not NaN
        if (!Float.isNaN(data.getPulse())) {
            Log.i("PulseoxReadings", "Updated pulse rate: " + data.getPulse());
            mReadingItems.get(0).value = data.getPulse();
        }
        mReadingItems.get(1).value = data.getSpo2();
        mListViewAdapter.notifyDataSetChanged();
    }
}
