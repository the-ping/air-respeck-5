package com.specknet.airrespeck.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;

/**
 * Dialog which is started when GPS is turned off.
 */

public class TurnGPSOnDialog extends DialogFragment {

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        View dialogView = inflater.inflate(R.layout.gps_turnon_dialog, null);
        TextView turnOnMessageText = (TextView) dialogView.findViewById(R.id.text_gps_turn_on);

        if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.M) {
            // Give additional instructions on Redmi, as you need to enable GPS from notification bar
            turnOnMessageText.setText("Please switch GPS Mode to 'High accuracy' on the next screen. This can " +
                    "only be done by a researcher if the phone is locked down.");
        }

        // Pass null as the parent view because its going in the dialog layout
        builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                        dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        ((MainActivity) getActivity()).setIsGPSDialogDisplayed(false);
        super.onDismiss(dialog);
    }
}
