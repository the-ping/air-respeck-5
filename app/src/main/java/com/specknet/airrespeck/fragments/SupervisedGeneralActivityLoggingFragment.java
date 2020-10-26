package com.specknet.airrespeck.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.FileLogger;

public class SupervisedGeneralActivityLoggingFragment extends ConnectionOverlayFragment {

    private String transitText = "";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_activity_logging_general, container, false);

        final RadioButton[] transitButtons = new RadioButton[]{
                (RadioButton) view.findViewById(R.id.radioButtonStatic),
                (RadioButton) view.findViewById(R.id.radioButtonWalking),
                (RadioButton) view.findViewById(R.id.radioButtonBicycle),
                (RadioButton) view.findViewById(R.id.radioButtonMotorbike),
                (RadioButton) view.findViewById(R.id.radioButtonAuto),
                (RadioButton) view.findViewById(R.id.radioButtonCar),
                (RadioButton) view.findViewById(R.id.radioButtonMetro),
                (RadioButton) view.findViewById(R.id.radioButtonBus),
        };

        for (RadioButton button : transitButtons) {
            button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        // Uncheck all transit buttons
                        for (RadioButton button : transitButtons) {
                            button.setChecked(false);
                        }
                        // Only check the selected one again
                        buttonView.setChecked(true);
                        transitText = (String) buttonView.getText();
                    }
                }
            });
        }

        final TableLayout transitGroup = (TableLayout) view.findViewById(R.id.transitGroup);

        // When indoor is selected, hide transit
        final RadioGroup inoutRadioGroup = (RadioGroup) view.findViewById(R.id.inoutGroup);
        inoutRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioButtonIn) {
                    //
                    transitText = "";
                    transitGroup.setVisibility(View.GONE);
                } else {
                    transitGroup.setVisibility(View.VISIBLE);
                }
            }
        });

        Button storeActivityButton = (Button) view.findViewById(R.id.storeActivityButton);
        storeActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checkedIDInOut = inoutRadioGroup.getCheckedRadioButtonId();
                String textInOut = "";
                if (checkedIDInOut != -1) {
                    RadioButton radioButton = (RadioButton) inoutRadioGroup.findViewById(checkedIDInOut);
                    textInOut = (String) radioButton.getText();

                    Toast.makeText(getActivity(), "Activity stored", Toast.LENGTH_SHORT).show();
                    FileLogger.logToFile(getActivity(), textInOut + ", " + transitText, "Activity");

                    // Send broadcast
                    Intent broadcastIntent = new Intent(Constants.ACTION_IS_INDOOR_BROADCAST);
                    broadcastIntent.putExtra(Constants.IS_INDOOR, textInOut.equals("Indoor"));
                    getActivity().sendBroadcast(broadcastIntent);
                } else {
                    Toast.makeText(getActivity(), "Select either Indoor or Outdoor", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

}
