package com.specknet.airrespeck.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import com.specknet.airrespeck.fragments.SupervisedAirspeckMapLoaderFragment;

import java.util.Calendar;

/**
 * Created by Darius on 09.05.2017.
 */

public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerFragment and return it
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        SupervisedAirspeckMapLoaderFragment parent = (SupervisedAirspeckMapLoaderFragment) getArguments().get(
                SupervisedAirspeckMapLoaderFragment.KEY_PARENT);
        String type = (String) getArguments().get(SupervisedAirspeckMapLoaderFragment.KEY_TYPE);

        if (parent != null) {
            if (type.equals(SupervisedAirspeckMapLoaderFragment.TYPE_FROM)) {
                parent.changeFromTime(hourOfDay, minute);
            } else if (type.equals(SupervisedAirspeckMapLoaderFragment.TYPE_TO)) {
                parent.changeToTime(hourOfDay, minute);
            }

        }
    }
}

