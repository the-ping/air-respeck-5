package com.specknet.airrespeck.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MapsAQActivity;

/**
 * Fragment for loading Google Maps Activity with pollution information
 */

public class SupervisedAQMapLoaderFragment extends BaseFragment {


    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SupervisedAQMapLoaderFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aq_map_loader, container, false);

        Button liveMapButton = (Button) view.findViewById(R.id.live_map_button);
        liveMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(getActivity(), MapsAQActivity.class);
                startActivity(mapIntent);
            }
        });

        // Load and disable custom timeframe selection view
        final LinearLayout customSelectionLayout = (LinearLayout) view.findViewById(R.id.custom_selection_layout);
        customSelectionLayout.setVisibility(View.GONE);

        // Load and fill timeframe selection spinner
        Spinner timeframeSpinner = (Spinner) view.findViewById(R.id.spinner_timeframe);

        String[] activitySpinnerElements = new String[]{"Last day", "Last week", "Custom"};
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, activitySpinnerElements);
        timeframeSpinner.setAdapter(activityAdapter);

        timeframeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 2) {
                    // Enable custom timerange selector
                    customSelectionLayout.setVisibility(View.VISIBLE);
                } else {
                    customSelectionLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Open date and time pickers when custom view is selected


        // Open historical map when buttton is pressed

        return view;
    }
}