package com.specknet.airrespeck.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.HashMapAdapter;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.util.Map;



/**
 * Page to view the configuration set by the pairing app
 */

public class ConfigViewActivity extends AppCompatActivity {

    private ListView configList;
    private String subjectID;
    private String respeckID;
    private TextView show_subjectid;
    private TextView show_respeckid;
    private TextView show_connection;
    private TextView show_battery;
    private TextView show_charging;

    private BroadcastReceiver respeckBroadcasterReceiver;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_config);

//        configList = (ListView) findViewById(R.id.list_view_config_view);
//        HashMapAdapter adapter = new HashMapAdapter(Utils.getInstance().getConfig(this));
//        configList.setAdapter(adapter);

        // Load config variables
        Map<String, String> config = Utils.getInstance().getConfig(this);

        //Pairing info section
        subjectID = config.get(Constants.Config.SUBJECT_ID);
        respeckID = config.get(Constants.Config.RESPECK_UUID);
        show_subjectid = findViewById(R.id.subjectid_text);
        show_respeckid = findViewById(R.id.respeckid_text);
        show_subjectid.setText("Subject ID: " + subjectID);
        show_respeckid.setText("Respeck ID: \n" + respeckID);

        //Status section
        show_connection = findViewById(R.id.rdevice_connection_status);
        show_battery = findViewById(R.id.rdevice_battery_view);
        show_charging = findViewById(R.id.rdevice_charging_view);
        //retrieve battery info
        getBatteryInfo();

    }

    private void getBatteryInfo() {
        respeckBroadcasterReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction() == Constants.ACTION_RESPECK_LIVE_BROADCAST) {
                    updateRESpeckData((RESpeckLiveData) intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA));
                }
                else if (intent.getAction() == Constants.ACTION_RESPECK_CONNECTED) {
                    show_connection.setText("Connection: Connected");
                }
                else if (intent.getAction() == Constants.ACTION_RESPECK_DISCONNECTED) {
                    show_connection.setText("Connection: Disconnected");                }
            }
        };

        registerReceiver(respeckBroadcasterReceiver, new IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST));
        registerReceiver(respeckBroadcasterReceiver, new IntentFilter(Constants.ACTION_RESPECK_CONNECTED));
        registerReceiver(respeckBroadcasterReceiver, new IntentFilter(Constants.ACTION_RESPECK_DISCONNECTED));
    }


    private void updateRESpeckData(RESpeckLiveData data) {
        if (data.getBattLevel()!= -1) {
            show_battery.setText("Battery: " + data.getBattLevel() + "%");
            show_connection.setText("Connection: Connected");
        }

        if (data.getChargingStatus()) {
            show_charging.setText("Charging: True");
        }
        else {
            show_charging.setText("Charging: False");
        }


    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(respeckBroadcasterReceiver);
        super.onDestroy();
    }

    private void loadValuesFromContentProvider() {
        CursorLoader cursorLoader = new CursorLoader(getBaseContext(), Constants.Config.CONFIG_CONTENT_URI,
                null, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getBaseContext(),
                R.layout.list_item_config_view,
                cursor,
                new String[]{"Name", "Value"},
                new int[]{R.id.key, R.id.value}, 0);

        configList.setAdapter(adapter);
    }



}
