package com.specknet.airrespeck.dialogs;

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;

/**
 * Dialog which is started when the orientation of the RESpeck is incorrect. The dialog is only "dismissable"
 * when the RESpeck shows an acceptable orientation (!= ACTIVITY_WRONG_ORIENTATION) for 10 seconds.
 */

public class WrongOrientationDialog extends DialogFragment {

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        ((MainActivity) getActivity()).setWrongOrientationDialogDisplayed(false);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog

        View dialogView = inflater.inflate(R.layout.wrong_orientation_dialog, null);
        TextView tv = (TextView) dialogView.findViewById(R.id.wrong_orientation_text);
        
        tv.setText(((MainActivity) getActivity()).getWrongOrientationText());

        // Pass null as the parent view because its going in the dialog layout
        builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton(R.string.wrong_orientation_done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                });


        return builder.create();
    }

}
