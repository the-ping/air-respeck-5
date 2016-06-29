package com.specknet.airrespeck.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.specknet.airrespeck.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ListReadingFragment extends Fragment {

    private ListView mReadingsList;
    private List<HashMap<String, String>> mListData;
    private SimpleAdapter mSimpleAdapter;

    public ListReadingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mListData = new ArrayList<>();
        mSimpleAdapter = new SimpleAdapter(getActivity(), mListData, R.layout.reading_item,
                new String[]{"description", "value", "units"},
                new int[]{R.id.reading_item_description,
                        R.id.reading_item_value,
                        R.id.reading_item_units});
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.list_fragment_reading, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mReadingsList = (ListView) view.findViewById(R.id.readings_list);
        mReadingsList.setAdapter(mSimpleAdapter);
    }

    /**
     * Add a reading to {@link #mListData} to populate the mData view.
     * @param description String Reading name.
     * @param value int Reading value.
     * @param units String Reading units.
     */
    public void addReading(final String description, final int value, final String units) {
        HashMap<String, String> map = new HashMap<>();
        map.put("description", description);
        map.put("value", (value == 0) ? "" : String.valueOf(value));
        map.put("units", units);

        mListData.add(map);
        mSimpleAdapter.notifyDataSetChanged();
    }

    /**
     * Updates the value field in {@link #mListData} and notifies the adaptor to show the
     * changes in the mData view.
     * @param values List<Integer>() List with the new values for all readings. NOTE: This mData
     *               must be of the same size as the number of readings added to {@link #mListData}.
     */
    public void setListValues(final List<Integer> values) {
        int listDataCount = mListData.size();

        if (listDataCount != values.size()) {
            return;
        }

        for (int i = 0; i < listDataCount; ++i) {
            mListData.get(i).put("value", String.valueOf(values.get(i)));
        }

        mSimpleAdapter.notifyDataSetChanged();
    }
}
