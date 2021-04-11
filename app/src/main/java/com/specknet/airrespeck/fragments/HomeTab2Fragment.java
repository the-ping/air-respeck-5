
package com.specknet.airrespeck.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.AirspeckDataObserver;
import com.specknet.airrespeck.activities.ConnectionStateObserver;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.activities.RESpeckDataObserver;
import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.RESpeckLiveData;
import com.specknet.airrespeck.models.ReadingItem;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.FileLogger;
import com.specknet.airrespeck.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

/**
 * Home screen for subjects using the app
 */

public class HomeTab2Fragment extends Fragment implements RESpeckDataObserver, AirspeckDataObserver,
        ConnectionStateObserver {

    static final int REQUEST_IMAGE_CAPTURE = 0;
    static final int REQUEST_VIDEO_CAPTURE = 1;


    File mediaFile = null;

    // Icons/Images
    private ImageView connectedStatusRESpeck;
    private ImageView connectedStatusAirspeck;
    private ProgressBar progressBarRESpeck;
    private ProgressBar progressBarAirspeck;
    private ImageView airspeckOffButton;
    private ImageView respeckPausePlayButton;
    private TextView respeckBatteryLevel;
    private ImageView batteryImage;
    private LinearLayout batteryContainer;

    private boolean isAirspeckEnabled;
    private boolean isRespeckEnabled;
    private boolean isRespeckPaused;

    TextView sbj_averageBreathingRateText;
    ImageView activityIcon;

    // set up pie chart
    private PieChart pieChart;
    private PieDataSet pieDataSet;
    private PieData pieData;

    private ArrayList<ReadingItem> mReadingItems;

    private String sittime_text;
    private String walktime_text;
    private String lietime_text;
    private int sit_timeval;
    private int walk_timeval;
    private int lie_timeval;
    private int day_sit;
    private int day_walk;
    private int day_lie;

    private final String KEY_HOUR = "hour";
    private final String KEY_DAY = "day";
    private final String KEY_WEEK = "week";

    private String mHourStatsString = "Loading data";
    private String mDayStatsString = "Loading data";
    private String mWeekStatsString = "Loading data";

    public HomeTab2Fragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReadingItems = new ArrayList<>();
        mReadingItems.add(new ReadingItem(Constants.ACTIVITY_SUMMARY_HOUR, "", "-")); //hour, , -
        mReadingItems.add(new ReadingItem(Constants.ACTIVITY_SUMMARY_DAY, "", "-"));
        mReadingItems.add(new ReadingItem(Constants.ACTIVITY_SUMMARY_WEEK, "", "-"));

        //set action bar title
        getActivity().setTitle("AirRespeck");
        getActivity().setTitleColor(0x000000);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_subject_home2, container, false);

        // Set up pie chart
        pieChart = view.findViewById(R.id.today_piechart);
        pieChart.setNoDataText("Loading..");
        setupPieChart();

//        sittime_text = view.findViewById(R.id.subj_sit_time);
//        walktime_text = view.findViewById(R.id.subj_walk_time);
//        lietime_text = view.findViewById(R.id.subj_lie_time);

        if (savedInstanceState != null) {
            // Load previously calculated values
            mHourStatsString = savedInstanceState.getString(KEY_HOUR);
            mDayStatsString = savedInstanceState.getString(KEY_DAY);
            mWeekStatsString = savedInstanceState.getString(KEY_WEEK);
            updatepieReadings();
        } else {
            updateActivitySummary();
        }

        startActivitySummaryUpdaterTask();

        // Load Icons
        sbj_averageBreathingRateText = (TextView) view.findViewById(R.id.home_breathrate);
        activityIcon = (ImageView) view.findViewById(R.id.activity_icon);

        // Get options from project config
        Map<String, String> config = Utils.getInstance().getConfig(getActivity());

        // Load connection symbols
        connectedStatusRESpeck = (ImageView) view.findViewById(R.id.connected_status_respeck);
        connectedStatusAirspeck = (ImageView) view.findViewById(R.id.connected_status_airspeck);

        progressBarRESpeck = (ProgressBar) view.findViewById(R.id.progress_bar_respeck);
        progressBarAirspeck = (ProgressBar) view.findViewById(R.id.progress_bar_airspeck);

        ImageView airspeckDisabledImage = (ImageView) view.findViewById(R.id.not_enabled_airspeck);
        ImageView respeckDisabledImage = (ImageView) view.findViewById(R.id.not_enabled_respeck);

        respeckBatteryLevel = (TextView) view.findViewById(R.id.respeck_battery_level);
        batteryImage = (ImageView) view.findViewById(R.id.battery_image);
        respeckPausePlayButton = (ImageView) view.findViewById(R.id.respeck_pause_button);

        batteryContainer = (LinearLayout) view.findViewById(R.id.battery_container_respeck);

        isRespeckPaused = false;

        respeckPausePlayButton.setEnabled(false);
        respeckPausePlayButton.setOnClickListener(v -> {
            if (isRespeckPaused) {
                // Send CONTINUE command
                Intent intentData = new Intent(Constants.ACTION_RESPECK_RECORDING_CONTINUE);
                getActivity().sendBroadcast(intentData);

                isRespeckPaused = false;

                respeckPausePlayButton.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_24dp));

                connectedStatusRESpeck.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.vec_respeck));

                FileLogger.logToFile(getActivity(), "RESpeck recording continued");
            } else {
                respeckPausePlayButton.setEnabled(false);
                showRESpeckPauseDialog();
            }
        });

        // Initialise turn off button for Airspeck
        airspeckOffButton = (ImageButton) view.findViewById(R.id.airspeck_off_button);
        airspeckOffButton.setEnabled(false);
        airspeckOffButton.setOnClickListener(view15 -> {
            // Send switch off message to BLE service
            airspeckOffButton.setEnabled(false);
            showAirspeckPowerOffDialog();
        });

        isAirspeckEnabled = !config.get(Constants.Config.AIRSPECKP_UUID).isEmpty();
        isRespeckEnabled = !config.get(Constants.Config.RESPECK_UUID).isEmpty();

        // Show disabled symbol if a device is not paired
        if (isAirspeckEnabled) {
            // Update connection symbol based on state stored in MainActivity
            updateAirspeckConnectionSymbol(((MainActivity) getActivity()).getIsAirspeckConnected());
            ((MainActivity) getActivity()).registerAirspeckDataObserver(this);
        } else {
            // Only show disabled symbol if Airspeck is disabled
            connectedStatusAirspeck.setVisibility(View.GONE);
            progressBarAirspeck.setVisibility(View.GONE);
            airspeckDisabledImage.setVisibility(View.VISIBLE);
        }
        if (isRespeckEnabled) {
            // Update connection symbol based on state stored in MainActivity
            updateRESpeckConnectionSymbol(((MainActivity) getActivity()).getIsRESpeckConnected());
            ((MainActivity) getActivity()).registerRESpeckDataObserver(this);
        } else {
            // Only show disabled symbol if RESpeck is disabled
            connectedStatusRESpeck.setVisibility(View.GONE);
            progressBarRESpeck.setVisibility(View.GONE);
            respeckDisabledImage.setVisibility(View.VISIBLE);
        }

        // Register this fragment as connection state observer
        ((MainActivity) getActivity()).registerConnectionStateObserver(this);

        return view;
    }


    private void setupPieChart() {
        //medium, light, dark secondary colors
        int[] colors_list = {Color.rgb(255,183,77), Color.rgb(255,233,125), Color.rgb(200,135,25)};

        ArrayList<PieEntry> entries = new ArrayList<>();

        pieDataSet = new PieDataSet(entries, "");
        pieDataSet.setColors(colors_list);

        pieData = new PieData(pieDataSet);

        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Loading chart..");
        pieData.setDrawValues(false); //remove y values
        pieChart.setDrawEntryLabels(false); //remove x values


        // style legend
        pieChart.getLegend().setTextSize(12);
        pieChart.getLegend().setOrientation(Legend.LegendOrientation.VERTICAL); //vertical legend
        pieChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        pieChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);

    }

    private void startActivitySummaryUpdaterTask() {
        final int delay = 5 * 60 * 1000;
        final Handler h = new Handler();

        h.postDelayed(new Runnable() {
            public void run() {
                updateActivitySummary();
                h.postDelayed(this, delay);
            }
        }, delay);


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_HOUR, mHourStatsString);
        outState.putString(KEY_DAY, mDayStatsString);
        outState.putString(KEY_WEEK, mWeekStatsString);
        super.onSaveInstanceState(outState);
    }

    public void updateActivitySummary() {
        new HomeTab2Fragment.LoadActivityDataTask().execute();
    }

    private class LoadActivityDataTask extends AsyncTask<Void, Integer, Void> {

        protected Void doInBackground(Void... params) {
            Log.i("ActivitySummary", "Started loading stored data task");

            long now = new Date().getTime();
            long oneHourBefore = now - 60 * 60 * 1000;
            long oneDayBefore = now - 24 * 60 * 60 * 1000;
            long oneWeekBefore = now - 7 * 24 * 60 * 60 * 1000;

            int[] hourStats = new int[]{0, 0, 0};
            int[] dayStats = new int[]{0, 0, 0};
            int[] weekStats = new int[]{0, 0, 0};

            int[] daySec = new int[] {0, 0, 0};

            // Go through filenames in Airspeck directory
            if (getActivity() == null) {
                return null;
            }
            File dir = new File(Utils.getInstance().getDataDirectory(getActivity()) +
                    Constants.RESPECK_DATA_DIRECTORY_NAME);
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (File file : directoryListing) {
                    String fileDate = file.getName().split(" ")[4];
                    try {
                        // If file lies in specified time period, open it and read content
                        long tsFile = Utils.timestampFromString(fileDate, "yyyy-MM-dd");

                        BufferedReader reader = new BufferedReader(new FileReader(file));
                        // Skip first line as that's the header
                        reader.readLine();
                        String currentLine;
                        while ((currentLine = reader.readLine()) != null) {
                            String[] row = currentLine.split(",");
                            long tsRow = Long.parseLong(row[0]);
                            // Only if the timestamp of the currently read line is in specified time period,
                            // do we draw a circle on the map corresponding to the measurements
                            try {
                                if (tsRow >= oneHourBefore && tsRow <= now) {
                                    int activityType = Integer.parseInt(row[9]);
                                    if (activityType != -1) {
                                        if (tsFile >= Utils.roundToDay(oneWeekBefore) && tsFile <= now) {
                                            weekStats[activityType]++;
                                            if (tsFile >= Utils.roundToDay(oneDayBefore) && tsFile <= now) {
                                                dayStats[activityType]++;
                                                daySec[activityType]+=80; //every entry is 80ms long
                                                if (tsFile >= Utils.roundToDay(oneHourBefore) && tsFile <= now) {
                                                    hourStats[activityType]++;

                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (ArrayIndexOutOfBoundsException e) {
                                Log.i("ActivitySummary", "Incomplete RESpeck data row: " + e.getMessage());
                            }
                        }
                    } catch (IOException | ParseException e) {
                        Log.i("ActivitySummary", "Error parsing RESpeck files: " + e.getMessage());
                    }
                }
            }

            // Calculate percentages
            int hourSum = Utils.sum(hourStats);
            int daySum = Utils.sum(dayStats);
            int weekSum = Utils.sum(weekStats);

            mHourStatsString = String.format(Locale.UK,
                    "Sit/stand:\u00A0%.1f%%, Walking:\u00A0%.1f%%, Lying:\u00A0%.1f%%",
                    (hourStats[0] * 1. / hourSum * 100), (hourStats[1] * 1. / hourSum * 100),
                    (hourStats[2] * 1. / hourSum * 100));
            mDayStatsString = String.format(Locale.UK,
                    "Sit/stand:\u00A0%.1f%%, Walking:\u00A0%.1f%%, Lying:\u00A0%.1f%%",
                    (dayStats[0] * 1. / daySum * 100), (dayStats[1] * 1. / daySum * 100),
                    (dayStats[2] * 1. / daySum * 100));

            mWeekStatsString = String.format(Locale.UK,
                    "Sit/stand:\u00A0%.1f%%, Walking:\u00A0%.1f%%, Lying:\u00A0%.1f%%",
                    (weekStats[0] * 1. / weekSum * 100), (weekStats[1] * 1. / weekSum * 100),
                    (weekStats[2] * 1. / weekSum * 100));

            day_sit = (int) (dayStats[0] * 1. / daySum * 100);
            day_walk = (int) (dayStats[1] * 1. / daySum * 100);
            day_lie = (int) (dayStats[2] * 1. / daySum * 100);

            sit_timeval = daySec[0];
            walk_timeval = daySec[1];
            lie_timeval = daySec[2];

            return null;
        }

        private void updatePieChart() {

            if (pieDataSet.getEntryCount()==0) {

                pieDataSet.addEntry(new PieEntry(day_sit, "Sit/Stand"));
                pieDataSet.addEntry(new PieEntry(day_walk, "Walk"));
                pieDataSet.addEntry(new PieEntry(day_lie, "Lie"));

                pieData.notifyDataChanged();
                pieChart.notifyDataSetChanged();
                pieChart.invalidate();
                pieChart.setCenterText("");


            } else {
                pieDataSet.removeEntryByXValue(0f);
                pieDataSet.removeEntryByXValue(1f);
                pieDataSet.removeEntryByXValue(2f);

                pieData.notifyDataChanged();
                pieChart.notifyDataSetChanged();
                pieChart.invalidate();

                pieDataSet.addEntry(new PieEntry(day_sit, "Sit/Stand"));
                pieDataSet.addEntry(new PieEntry(day_walk, "Walk"));
                pieDataSet.addEntry(new PieEntry(day_lie, "Lie"));

                pieData.notifyDataChanged();
                pieChart.notifyDataSetChanged();
                pieChart.invalidate();

                pieChart.setCenterText("");

            }
        }

        @Override
        protected void onPostExecute(Void nothing) {


            // display duration in h:m:s format
            int hr = sit_timeval/1000/60/60;
            int min = (sit_timeval - hr*60*60*1000)/1000/60;
            int sec = (sit_timeval - min*60*1000)/1000;

            int walk_hr = walk_timeval/1000/60/60;
            int walk_min = (walk_timeval - walk_hr*1000*60*60)/1000/60;
            int walk_sec = (walk_timeval - walk_min*1000*60)/1000;

            int lie_hr = lie_timeval/1000/60/60;
            int lie_min = (lie_timeval - lie_hr*1000*60*60)/1000/60;
            int lie_sec = (lie_timeval - lie_min*1000*60)/1000;
            updatepieReadings();
            updatePieChart();


        }

    }


    private void updatepieReadings() {
        if (mReadingItems != null) {
            mReadingItems.get(0).stringValue = mHourStatsString;
            mReadingItems.get(1).stringValue = mDayStatsString;
            mReadingItems.get(2).stringValue = mWeekStatsString;
        }
    }

    public void replaceFragment(Fragment someFragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_container, someFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void updateReadings(RESpeckLiveData data) {
        // Update readings and activity symbol
        // Set breathing rate text to currently calculated rates
        sbj_averageBreathingRateText.setText(String.format(Locale.UK, "%.2f ", data.getAvgBreathingRate()));

        // Set activity icon to reflect currently predicted activity
        //ping add: reflect label
        switch (data.getActivityType()) {
            case Constants.ACTIVITY_LYING:
                activityIcon.setImageResource(R.drawable.vec_lying);
                break;
            case Constants.ACTIVITY_LYING_DOWN_LEFT:
                activityIcon.setImageResource(R.drawable.lying_to_left);
                break;
            case Constants.ACTIVITY_LYING_DOWN_RIGHT:
                activityIcon.setImageResource(R.drawable.lying_to_right);
                break;
            case Constants.ACTIVITY_LYING_DOWN_STOMACH:
                activityIcon.setImageResource(R.drawable.lying_stomach);
                break;
            case Constants.ACTIVITY_SITTING_BENT_BACKWARD:
                activityIcon.setImageResource(R.drawable.sitting_backward);
                break;
            case Constants.ACTIVITY_SITTING_BENT_FORWARD:
                activityIcon.setImageResource(R.drawable.sitting_forward);
                break;
            case Constants.ACTIVITY_STAND_SIT:
                activityIcon.setImageResource(R.drawable.vec_standing_sitting);
                break;
            case Constants.ACTIVITY_MOVEMENT:
                activityIcon.setImageResource(R.drawable.movement);
                break;
            case Constants.ACTIVITY_WALKING:
                activityIcon.setImageResource(R.drawable.vec_walking);
                break;
            case Constants.SS_COUGHING:
                activityIcon.setImageResource(R.drawable.ic_cough);
                break;
            default:
                activityIcon.setImageResource(R.drawable.vec_xmark);
        }


    }


    @Override
    public void updateConnectionState(boolean showRESpeckConnected, boolean showAirspeckConnected,
                                      boolean showPulseoxConnecting, boolean showInhalerConnecting) {
        if (isRespeckEnabled) {
            updateRESpeckConnectionSymbol(showRESpeckConnected);
        }
        if (isAirspeckEnabled) {
            updateAirspeckConnectionSymbol(showAirspeckConnected);
        }
    }

    // Let connection symbol flicker if new data comes in
    @Override
    public void updateAirspeckData(AirspeckData data) {
        updateAirspeckConnectionSymbol(true);
    }

    @Override
    public void updateRESpeckData(RESpeckLiveData data) {

        updateRESpeckConnectionSymbol(true);

        // update battery level and charging status
        if (data.getBattLevel() != -1) {
            batteryContainer.setVisibility(View.VISIBLE);
            respeckBatteryLevel.setText(data.getBattLevel() + "%");
        }
        else {
            batteryContainer.setVisibility(View.INVISIBLE);
        }

        if (data.getChargingStatus()) {
            batteryImage.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.vec_battery));
        }
        else {
            batteryImage.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_battery_full));
        }

        // Update the other readings
        updateReadings(data);
    }

    private void showRESpeckPauseDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder
                .setMessage(getString(R.string.respeck_pause_message))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        isRespeckPaused = true;

                        // Change to dark respeck indicating disconnection
                        connectedStatusRESpeck.setImageDrawable(
                                ContextCompat.getDrawable(getActivity(), R.drawable.vec_respeck_off));

                        // Set button image to play
                        respeckPausePlayButton.setImageDrawable(
                                ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_button));

                        // Enable button again
                        respeckPausePlayButton.setEnabled(true);

                        // Broadcast PAUSE message
                        Intent intentData = new Intent(Constants.ACTION_RESPECK_RECORDING_PAUSE);
                        getActivity().sendBroadcast(intentData);

                        FileLogger.logToFile(getActivity(), "RESpeck recording paused");
                    }
                })
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        respeckPausePlayButton.setEnabled(true);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        respeckPausePlayButton.setEnabled(true);
                    }
                });
        alertDialogBuilder.create().show();
    }

    private void showAirspeckPowerOffDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder
                .setMessage(getString(R.string.airspeck_power_off_message))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        connectedStatusAirspeck.setVisibility(View.GONE);
                        progressBarAirspeck.setVisibility(View.VISIBLE);
                        Intent i = new Intent(Constants.AIRSPECK_OFF_ACTION);
                        getActivity().sendBroadcast(i);
                        FileLogger.logToFile(getActivity(), "Airspeck power off button pressed");
                    }
                })
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        airspeckOffButton.setEnabled(true);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        airspeckOffButton.setEnabled(true);
                    }
                });
        alertDialogBuilder.create().show();
    }


    public void startRehabApp(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String phoneID = Utils.getInstance().getConfig(context).get("PhoneID");
        String subjectID = Utils.getInstance().getConfig(context).get(Constants.Config.SUBJECT_ID);
        Log.i("Crashlytis", "Phone id = " + phoneID);
        Log.i("Crashlytis", "Subject id = " + subjectID);

        if (intent == null) {
            Toast.makeText(context, "Rehab app not installed. Contact researchers for further information.",
                    Toast.LENGTH_LONG).show();
        } else {
            intent.putExtra(Constants.PHONE_ID, phoneID);
            intent.putExtra(Constants.SUBJECT_ID, subjectID);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void updateRESpeckConnectionSymbol(boolean isConnected) {
        if (isConnected) {
            respeckPausePlayButton.setEnabled(true);

            // "Flash" with symbol when updating to indicate data coming in
            if (isRespeckPaused) {
                connectedStatusRESpeck.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.vec_respeck_off));

                progressBarRESpeck.setVisibility(View.GONE);
                connectedStatusRESpeck.setVisibility(View.VISIBLE);
            } else {
                connectedStatusRESpeck.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.vec_respeck));

                progressBarRESpeck.setVisibility(View.GONE);
                connectedStatusRESpeck.setVisibility(View.INVISIBLE);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connectedStatusRESpeck.setVisibility(View.VISIBLE);
                    }
                }, 100);
            }

        } else {
            connectedStatusRESpeck.setVisibility(View.GONE);
            progressBarRESpeck.setVisibility(View.VISIBLE);
            respeckPausePlayButton.setEnabled(false);
        }
    }

    public void updateAirspeckConnectionSymbol(boolean isConnected) {
        if (isConnected) {
            // "Flash" with symbol when updating to indicate data coming in
            progressBarAirspeck.setVisibility(View.GONE);
            connectedStatusAirspeck.setVisibility(View.INVISIBLE);
            airspeckOffButton.setEnabled(true);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    connectedStatusAirspeck.setVisibility(View.VISIBLE);
                }
            }, 100);

        } else {
            airspeckOffButton.setEnabled(false);
            connectedStatusAirspeck.setVisibility(View.GONE);
            progressBarAirspeck.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        // Unregister this class as observer. If we haven't observed, nothing happens
        ((MainActivity) getActivity()).unregisterRESpeckDataObserver(this);
        ((MainActivity) getActivity()).unregisterAirspeckDataObserver(this);
        ((MainActivity) getActivity()).unregisterConnectionStateObserver(this);
        super.onDestroy();
    }


}




