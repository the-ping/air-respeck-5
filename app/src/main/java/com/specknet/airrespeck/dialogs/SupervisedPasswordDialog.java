package com.specknet.airrespeck.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.utils.Constants;

/**
 * Created by Darius on 20.03.2017.
 */

public class SupervisedPasswordDialog extends DialogFragment {

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        View dialogView = inflater.inflate(R.layout.supervised_password_dialog, null);

        final EditText passwordField = (EditText) dialogView.findViewById(R.id.dialog_password);

        // Pass null as the parent view because its going in the dialog layout
        builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton(R.string.supervised_mode_dialog_enter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        for (String p: Constants.passwordsSupervisedMode) {
                            if (passwordField.getText().toString().equals(p)) {
                                ((MainActivity) getActivity()).displaySupervisedMode();
                                break;
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.supervised_mode_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                });

        return builder.create();
    }
}
