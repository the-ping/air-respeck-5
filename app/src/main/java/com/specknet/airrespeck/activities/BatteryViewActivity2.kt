package com.specknet.airrespeck.activities

//import android.support.v7.app.AppCompatActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.specknet.airrespeck.R
import com.specknet.airrespeck.models.RESpeckLiveData
import com.specknet.airrespeck.utils.Constants
import com.specknet.airrespeck.utils.Utils

class BatteryViewActivity2 : AppCompatActivity(), RESpeckDataObserver{

    lateinit var respeckBatteryView: TextView
    lateinit var respeckChargingView: TextView
    lateinit var respeckConnectionStatus: TextView
    lateinit var respeckBroadcastReceiver: BroadcastReceiver

    val filter = IntentFilter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battery_view)

        respeckBatteryView = findViewById(R.id.respeck_battery_view) as TextView
        respeckChargingView = findViewById(R.id.respeck_charging_view) as TextView
        respeckConnectionStatus = findViewById(R.id.respeck_connection_status) as TextView


        // register receiver
        respeckBroadcastReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {
                    val liveRespeckData = intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    updateRESpeckData(liveRespeckData)
                }
                else if (action == Constants.ACTION_RESPECK_CONNECTED) {
                    respeckConnectionStatus.text = "Connection: Connected"
                }
                else if (action == Constants.ACTION_RESPECK_DISCONNECTED) {
                    respeckConnectionStatus.text = "Connection: Disconnected"
                }

            }


        }

        filter.addAction(Constants.ACTION_RESPECK_LIVE_BROADCAST)
        filter.addAction(Constants.ACTION_RESPECK_CONNECTED)
        filter.addAction(Constants.ACTION_RESPECK_DISCONNECTED)

        this.registerReceiver(respeckBroadcastReceiver, filter)


    }

    override fun updateRESpeckData(data: RESpeckLiveData) {
        if (data.battLevel != -1) {
            respeckBatteryView.text = "Battery: " + data.battLevel + "%"
            respeckConnectionStatus.text = "Connection: Connected"
        }

        if (data.chargingStatus) {
            respeckChargingView.text = "Charging: True"
        }
        else {
            respeckChargingView.text = "Charging: False"
        }


    }

    override fun onDestroy() {
        unregisterReceiver(respeckBroadcastReceiver)
        super.onDestroy()
    }
}