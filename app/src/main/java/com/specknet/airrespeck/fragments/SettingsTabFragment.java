package com.specknet.airrespeck.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lazydroid.autoupdateapk.AutoUpdateApk;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.AirspeckDataObserver;
import com.specknet.airrespeck.activities.ConnectionStateObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import org.w3c.dom.Text;

import java.util.Map;

public class SettingsTabFragment extends Fragment implements RESpeckDataObserver,
        ConnectionStateObserver {
    // Utils
    private Utils mUtils;

    TextView sub_id_textview;
    TextView version_textview;
    TextView respeck_id_textview;
    TextView charging_textview;
    TextView show_connection;
    TextView show_battery;

    private boolean isRespeckEnabled;

    private BroadcastReceiver respeckBroadcasterReceiver;

    public SettingsTabFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set action bar title
        getActivity().setTitle("Settings");
        getActivity().setTitleColor(0x000000);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings_tab, container, false);

        // Get options from project config
        Map<String, String> config = Utils.getInstance().getConfig(getActivity());

        // Load build version
        mUtils = Utils.getInstance();
        version_textview = view.findViewById(R.id.build_version);
        version_textview.setText(mUtils.getAppVersionName());

        // Display repspeck details and version build
        String sub_id = config.get(Constants.Config.SUBJECT_ID);
        sub_id_textview = (TextView) view.findViewById(R.id.subjectid_text);
        sub_id_textview.setText("Subject ID: " + sub_id);

        String respeck_id = config.get(Constants.Config.RESPECK_UUID);
        respeck_id_textview = (TextView) view.findViewById(R.id.respeckid_text);
        respeck_id_textview.setText("Respeck ID: " + respeck_id);

        show_connection = view.findViewById(R.id.rdevice_connection_status);
        show_battery = view.findViewById(R.id.rdevice_battery_view);
        charging_textview = (TextView) view.findViewById(R.id.rdevice_charging_view);

        isRespeckEnabled = !config.get(Constants.Config.RESPECK_UUID).isEmpty();

        if (isRespeckEnabled) {
            // Update connection symbol based on state stored in MainActivity
            updateRESpeckConnectionSymbol(((MainActivity) getActivity()).getIsRESpeckConnected());
            ((MainActivity) getActivity()).registerRESpeckDataObserver(this);
        }


//        getBatteryInfo();

        return view;
    }

    public void updateRESpeckConnectionSymbol(boolean isConnected) {
        if (isConnected) {
            show_connection.setText("Connection: Connected");

        } else {
            show_connection.setText("Connection: Disconnected");
        }
    }


    @Override
    public void updateRESpeckData(RESpeckLiveData data) {

        updateRESpeckConnectionSymbol(true);

        if (data.getBattLevel()!= -1) {
            show_battery.setText("Battery: " + data.getBattLevel() + "%");
//            show_connection.setText("Connection: Connected");
        }

        if (data.getChargingStatus()) {
            charging_textview.setText("Charging: True");
        }
        else {
            charging_textview.setText("Charging: False");
        }


    }

    @Override
    public void updateConnectionState(boolean showRESpeckConnected, boolean airspeckConnected, boolean pulseoxConnected, boolean inhalerConnected) {
        if (isRespeckEnabled) {
            updateRESpeckConnectionSymbol(showRESpeckConnected);
        }
    }
}