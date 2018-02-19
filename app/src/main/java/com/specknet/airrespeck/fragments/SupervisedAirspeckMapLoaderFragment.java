package com.specknet.airrespeck.fragments;

import android.app.DialogFragment;
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
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MapsAQActivity;
import com.specknet.airrespeck.dialogs.DatePickerFragment;
import com.specknet.airrespeck.dialogs.TimePickerFragment;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment for loading Google Maps Activity with pollution information
 */

public class SupervisedAirspeckMapLoaderFragment extends BaseFragment implements Serializable {

    public static final String KEY_PARENT = "parent";
    public static final String KEY_TYPE = "type";
    public static final String TYPE_TO = "to";
    public static final String TYPE_FROM = "from";

    private final String TIMEPERIOD_LAST_HOUR = "Previous hour";
    private final String TIMEPERIOD_LAST_THREE_HOURS = "Previous three hours";
    private final String TIMEPERIOD_CUSTOM = "Custom time period";

    private Button mFromDateButton;
    private Button mFromTimeButton;
    private Button mToDateButton;
    private Button mToTimeButton;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SupervisedAirspeckMapLoaderFragment() {

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
                mapIntent.putExtra(MapsAQActivity.MAP_TYPE, MapsAQActivity.MAP_TYPE_LIVE);
                startActivity(mapIntent);
            }
        });

        // Load and disable custom timeframe selection view
        final LinearLayout customSelectionLayout = (LinearLayout) view.findViewById(R.id.custom_selection_layout);
        customSelectionLayout.setVisibility(View.GONE);

        // Load and fill timeframe selection spinner
        final Spinner timeframeSpinner = (Spinner) view.findViewById(R.id.spinner_timeframe);

        String[] activitySpinnerElements = new String[]{TIMEPERIOD_LAST_HOUR, TIMEPERIOD_LAST_THREE_HOURS, TIMEPERIOD_CUSTOM};
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


        Utils utils = Utils.getInstance();
        boolean isEncryptionOn = Boolean.parseBoolean(
                utils.getConfig(getActivity()).get(Constants.Config.ENCRYPT_LOCAL_DATA));
        if (isEncryptionOn) {
            LinearLayout historicalContainer = (LinearLayout) view.findViewById(R.id.historical_data_container);
            historicalContainer.setVisibility(View.GONE);
        } else {
            // Load date and time pickers for when custom view is selected
            mFromDateButton = (Button) view.findViewById(R.id.date_from);
            mFromTimeButton = (Button) view.findViewById(R.id.time_from);
            mToDateButton = (Button) view.findViewById(R.id.date_to);
            mToTimeButton = (Button) view.findViewById(R.id.time_to);

            mFromTimeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTimePicker(TYPE_FROM);
                }
            });

            mToTimeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTimePicker(TYPE_TO);
                }
            });

            mFromDateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatePicker(TYPE_FROM);
                }
            });

            mToDateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatePicker(TYPE_TO);
                }
            });

            // Open historical map when buttton is pressed
            Button historicalMapButton = (Button) view.findViewById(R.id.historical_loader_button);
            historicalMapButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    long tsFrom;
                    long tsTo;

                    if (timeframeSpinner.getSelectedItem().equals(TIMEPERIOD_LAST_HOUR)) {
                        tsTo = new Date().getTime();
                        // Subtract day
                        tsFrom = tsTo - 1000 * 60 * 60;
                    } else if (timeframeSpinner.getSelectedItem().equals(TIMEPERIOD_LAST_THREE_HOURS)) {
                        tsTo = new Date().getTime();
                        // Subtract week
                        tsFrom = tsTo - 1000 * 60 * 60 * 3;
                    } else {
                        // Custom selection
                        String tsFromString = mFromDateButton.getText() + " " + mFromTimeButton.getText();
                        String tsToString = mToDateButton.getText() + " " + mToTimeButton.getText();

                        try {
                            tsFrom = Utils.timestampFromString(tsFromString, "dd-MM-yyyy HH:mm");
                            tsTo = Utils.timestampFromString(tsToString, "dd-MM-yyyy HH:mm");
                        } catch (ParseException e) {
                            Toast.makeText(getContext(), getString(R.string.maps_loader_invalid_timestamps),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    if (tsTo < tsFrom) {
                        Toast.makeText(getContext(), getString(R.string.maps_loader_invalid_timestamps_to_before_from),
                                Toast.LENGTH_LONG).show();
                    } else if (tsTo > new Date().getTime()) {
                        Toast.makeText(getContext(), getString(R.string.maps_loader_invalid_timestamp_in_future),
                                Toast.LENGTH_LONG).show();
                    } else if (tsTo - tsFrom > 1000 * 60 * 60 * 3 + 1) { // +1 so that full new hour can be specified
                        Toast.makeText(getContext(), getString(R.string.maps_loader_period_too_long),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Intent mapIntent = new Intent(getActivity(), MapsAQActivity.class);
                        mapIntent.putExtra(MapsAQActivity.MAP_TYPE, MapsAQActivity.MAP_TYPE_HISTORICAL);
                        mapIntent.putExtra(MapsAQActivity.TIMESTAMP_FROM, tsFrom);
                        mapIntent.putExtra(MapsAQActivity.TIMESTAMP_TO, tsTo);
                        startActivity(mapIntent);
                    }
                }
            });
        }
        return view;
    }

    public void showTimePicker(String type) {
        DialogFragment newFragment = new TimePickerFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_PARENT, this);
        args.putSerializable(KEY_TYPE, type);
        newFragment.setArguments(args);
        newFragment.show(getActivity().getFragmentManager(), "timePicker");
    }

    public void showDatePicker(String type) {
        DialogFragment newFragment = new DatePickerFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_PARENT, this);
        args.putSerializable(KEY_TYPE, type);
        newFragment.setArguments(args);
        newFragment.show(getActivity().getFragmentManager(), "datePicker");
    }

    public void changeToTime(int hourOfDay, int minute) {
        mToTimeButton.setText(String.format(Locale.UK, "%d:%02d", hourOfDay, minute));
    }

    public void changeFromTime(int hourOfDay, int minute) {
        mFromTimeButton.setText(String.format(Locale.UK, "%d:%02d", hourOfDay, minute));
    }

    public void changeToDate(int year, int month, int day) {
        mToDateButton.setText(String.format(Locale.UK, "%d-%02d-%02d", day, month, year));
    }

    public void changeFromDate(int year, int month, int day) {
        mFromDateButton.setText(String.format(Locale.UK, "%d-%02d-%02d", day, month, year));
    }
}