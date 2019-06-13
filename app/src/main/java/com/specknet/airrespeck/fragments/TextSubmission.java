package com.specknet.airrespeck.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TextSubmission extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.text_submission_fragment, null))
                // Add action buttons
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String directory = Utils.getInstance().getDataDirectory(getActivity()) +
                                Constants.MEDIA_DIRECTORY_NAME;
                        String filename = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        File mediaFile = new File(new File(directory), filename.concat(".txt"));
                        try {
                            mediaFile.createNewFile();
                            FileWriter fileWriter = new FileWriter(mediaFile.getPath(), true);
                            BufferedWriter bufferWriter = new BufferedWriter(fileWriter);
                            EditText editText = (EditText) getDialog().findViewById(R.id.editText);
                            bufferWriter.write(editText.getText().toString());
                            bufferWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(getActivity().getApplicationContext(), "Text submitted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        TextSubmission.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}