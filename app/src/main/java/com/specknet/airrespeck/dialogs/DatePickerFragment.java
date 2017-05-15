package com.specknet.airrespeck.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;

import com.specknet.airrespeck.fragments.SupervisedAQMapLoaderFragment;

import java.util.Calendar;

/**
 * Created by Darius on 09.05.2017.
 */

public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Log.i("Date picker", String.format("year %d, month %d, day %d", year, month, day));
        SupervisedAQMapLoaderFragment parent = (SupervisedAQMapLoaderFragment) getArguments().get(
                SupervisedAQMapLoaderFragment.KEY_PARENT);
        String type = (String) getArguments().get(SupervisedAQMapLoaderFragment.KEY_TYPE);

        if (parent != null && type != null) {
            if (type.equals(SupervisedAQMapLoaderFragment.TYPE_FROM)) {
                // Months start at zero in picker
                parent.changeFromDate(year, month + 1, day);
            } else if (type.equals(SupervisedAQMapLoaderFragment.TYPE_TO)) {
                parent.changeToDate(year, month + 1, day);
            }

        }
    }
}
