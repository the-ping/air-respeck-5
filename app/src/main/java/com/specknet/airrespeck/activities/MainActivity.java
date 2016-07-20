package com.specknet.airrespeck.activities;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.fragments.GraphsFragment;
import com.specknet.airrespeck.fragments.HomeFragment;
import com.specknet.airrespeck.fragments.AQReadingsFragment;
import com.specknet.airrespeck.fragments.MenuFragment;
import com.specknet.airrespeck.utils.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class MainActivity extends BaseActivity implements
        MenuFragment.OnMenuSelectedListener {

    private Thread mUpdateThread;

    private static final String TAG_HOME_FRAGMENT = "HOME_FRAGMENT";
    private static final String TAG_AQREADINGS_FRAGMENT = "AQREADINGS_FRAGMENT";
    private static final String TAG_GRAPHS_FRAGMENT = "GRAPHS_FRAGMENT";
    private static final String TAG_CURRENT_FRAGMENT = "CURRENT_FRAGMENT";

    private HomeFragment mHomeFragment;
    private AQReadingsFragment mAQReadingsFragment;
    private GraphsFragment mGraphsFragment;
    private Fragment mCurrentFragment;

    //BLUETOOTH
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private static String RESPECK_UUID = "F5:85:7D:EA:61:F9";
    private static String QOE_UUID = "FC:A6:33:A2:A4:5A";
    private int DEVICES_CONNECTED = 1;
    private BluetoothGatt mGatt, mGatt2;
    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    private final static String QOE_CLIENT_CHARACTERISTIC = "00002902-0000-1000-8000-00805f9b34fb";
    private final static String QOE_LIVE_CHARACTERISTIC = "00002002-e117-4bff-b00d-b20878bc3f44";//Service

    //CODIGO MANTAS
    private static byte[][] lastPackets_1  = new byte[4][];
    private static byte[][] lastPackets_2  = new byte[5][];
    int lastSample = 0;
    private static int[] sampleIDs_1 = {0, 0, 0, 0};
    private static int[] sampleIDs_2 = {0, 1, 2, 3, 4};



    List<Float> mHomeScreenReadingValues = new ArrayList<Float>();
    List<Float> mAQReadingValues = new ArrayList<Float>();


    private final Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            doUpdate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize fragments
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState != null) {
            mHomeFragment =
                    (HomeFragment) fm.getFragment(savedInstanceState, TAG_HOME_FRAGMENT);
            mAQReadingsFragment =
                    (AQReadingsFragment) fm.getFragment(savedInstanceState, TAG_AQREADINGS_FRAGMENT);
            mGraphsFragment =
                    (GraphsFragment) fm.getFragment(savedInstanceState, TAG_GRAPHS_FRAGMENT);
            mCurrentFragment = fm.getFragment(savedInstanceState, TAG_CURRENT_FRAGMENT);

            if (mHomeFragment == null) {
                mHomeFragment = new HomeFragment();
            }
            if (mAQReadingsFragment == null) {
                mAQReadingsFragment = new AQReadingsFragment();
            }
            if (mGraphsFragment == null) {
                mGraphsFragment = new GraphsFragment();
            }
        }
        else {
            mHomeFragment = new HomeFragment();
            mAQReadingsFragment = new AQReadingsFragment();
            mGraphsFragment = new GraphsFragment();
            mCurrentFragment = mHomeFragment;
        }

        // Choose layout
        if (mMenuModePref == 0) {
            setContentView(R.layout.activity_main_buttons);

            FragmentTransaction trans = fm.beginTransaction();

            if (mCurrentFragment instanceof HomeFragment) {
                trans.replace(R.id.content, mCurrentFragment, TAG_HOME_FRAGMENT);
            }
            else if (mCurrentFragment instanceof AQReadingsFragment) {
                trans.replace(R.id.content, mCurrentFragment, TAG_AQREADINGS_FRAGMENT);
            }
            else if (mCurrentFragment instanceof GraphsFragment) {
                trans.replace(R.id.content, mCurrentFragment, TAG_GRAPHS_FRAGMENT);
            }
            else {
                trans.replace(R.id.content, mHomeFragment, TAG_HOME_FRAGMENT);
                mCurrentFragment = mHomeFragment;
            }

            trans.commit();
        }
        else if (mMenuModePref == 1) {
            setContentView(R.layout.activity_main_tabs);

            // Create the adapter that will return a fragment for each of the three
            // primary sections of the activity.
            SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
            sectionsPagerAdapter.setContext(getApplicationContext());

            // Set up the ViewPager with the sections adapter.
            ViewPager viewPager = (ViewPager) findViewById(R.id.container);
            if (viewPager != null) {
                viewPager.setAdapter(sectionsPagerAdapter);
            }

            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            if (tabLayout != null) {
                tabLayout.setupWithViewPager(viewPager);
            }

            if (mMenuTabIconsPref) {
                tabLayout.getTabAt(0).setIcon(Constants.menuIconsResId[0]);
                tabLayout.getTabAt(1).setIcon(Constants.menuIconsResId[2]);
                tabLayout.getTabAt(2).setIcon(Constants.menuIconsResId[3]);
            }
        }

        // Add the toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
    }

    protected void onResume() {
        super.onResume();
        if (mGatt2 != null)
            System.out.println("BT: " + mGatt2.toString());
    }

    protected void onPause() {
        super.onPause();
        if (mGatt2 != null)
            System.out.println("BT: " + mGatt2.toString());
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        FragmentManager fm = getSupportFragmentManager();

        if (mHomeFragment != null && mHomeFragment.isAdded()) {
            fm.putFragment(outState, TAG_HOME_FRAGMENT, mHomeFragment);
        }

        if (mAQReadingsFragment != null && mAQReadingsFragment.isAdded()) {
            fm.putFragment(outState, TAG_AQREADINGS_FRAGMENT, mAQReadingsFragment);
        }

        if (mGraphsFragment != null && mGraphsFragment.isAdded()) {
            fm.putFragment(outState, TAG_GRAPHS_FRAGMENT, mGraphsFragment);
        }

        if (mCurrentFragment != null && mCurrentFragment.isAdded()) {
            fm.putFragment(outState, TAG_CURRENT_FRAGMENT, mCurrentFragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu, this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setVisible(mRespeckAppAccessPref);
        menu.getItem(1).setVisible(mAirspeckAppAccessPref);

        if (Objects.equals(mCurrentUser.getGender(), "M")) {
            menu.getItem(2).setIcon(R.drawable.ic_user_male);
        }
        else if (Objects.equals(mCurrentUser.getGender(), "F")) {
            menu.getItem(2).setIcon(R.drawable.ic_user_female);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStattement
        if (id == R.id.action_connect) {
            connect();
        }
        else if (id == R.id.action_user_profile) {
            startActivity(new Intent(this, UserProfileActivity.class));
        }
        else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.SettingsFragment.class.getName());
            intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
            startActivity(intent);
        }
        else if (id == R.id.action_airspeck) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.airquality.sepa",
                        "com.airquality.sepa.DataCollectionActivity"));
                startActivity(intent);
            }
            catch (Exception e) {
                Toast.makeText(this, R.string.airspeck_not_found, Toast.LENGTH_LONG).show();
            }
        }
        else if (id == R.id.action_respeck) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.pulrehab",
                        "com.pulrehab.fragments.MainActivity"));
                startActivity(intent);
            }
            catch (Exception e) {
                Toast.makeText(this, R.string.respeck_not_found, Toast.LENGTH_LONG).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onButtonSelected(int buttonId) {
        switch (buttonId) {
            // Home
            case 0:
                replaceFragment(mHomeFragment, TAG_HOME_FRAGMENT);
                break;
            // Air Quality
            case 1:
                replaceFragment(mAQReadingsFragment, TAG_AQREADINGS_FRAGMENT);
                break;
            // Dashboard
            case 2:
                replaceFragment(mGraphsFragment, TAG_GRAPHS_FRAGMENT);
                break;
        }
    }

    /**
     * Replace the current fragment with the given one.
     * @param fragment Fragment New fragment.
     * @param tag String Tag for the new fragment.
     */
    public void replaceFragment(Fragment fragment, String tag) {
        replaceFragment(fragment, tag, false);
    }

    /**
     * Replace the current fragment with the given one.
     * @param fragment Fragment New fragment.
     * @param tag String Tag for the new fragment.
     * @param addToBackStack boolean Whether to add the previous fragment to the Back Stack, or not.
     */
    public void replaceFragment(Fragment fragment, String tag, boolean addToBackStack) {
        if (fragment.isVisible()) {
            return;
        }

        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        //trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        trans.replace(R.id.content, fragment, tag);
        if (addToBackStack) {
            trans.addToBackStack(null);
        }
        trans.commit();

        mCurrentFragment = fragment;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private Context mContext;
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setContext(Context context) {
            mContext = context;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return the relevant fragment per each page.
            switch (position) {
                case 0:
                    return mHomeFragment;
                case 1:
                    return mAQReadingsFragment;
                case 2:
                    return mGraphsFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.menu_home);
                case 1:
                    return getString(R.string.menu_air_quality);
                case 2:
                    return getString(R.string.menu_graphs);
            }
            return null;
        }
    }

    //-------------------------------------------------------------------------------------------------------------
    //BLUETOOTH METHODS--------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------

    private void connect() {
        //Initializing Bluetooth adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        Toast.makeText(getApplicationContext(), "Turning on Bluetooth!", Toast.LENGTH_SHORT).show();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
                ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(RESPECK_UUID).build();
                ScanFilter filtertwo = new ScanFilter.Builder().setDeviceAddress(QOE_UUID).build();
                filters.add(filter);
                filters.add(filtertwo);
            }
            Log.i("Bluetooth Adapter", "Starting Scan of Devices");
            mLEScanner.startScan(filters, settings, mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            BluetoothDevice btDevice = result.getDevice();
            Toast.makeText(getApplicationContext(),"Reaching to "+btDevice.getName(),Toast.LENGTH_SHORT).show();
            connectToDevice(btDevice);
            DEVICES_CONNECTED = DEVICES_CONNECTED+1;

            if(DEVICES_CONNECTED == 2){
                Toast.makeText(getApplicationContext(),"Scanner Off",Toast.LENGTH_SHORT).show();
                mLEScanner.stopScan(mScanCallback);
            }


        }



        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(getApplicationContext(),"Bluetooth Search Fail",Toast.LENGTH_SHORT).show();
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        //if (mGatt == null) {

        if(device.getName().equals("Respeck_LNT18")){
            //Toast.makeText(getApplicationContext(),"Connecting to "+device.getName(),Toast.LENGTH_SHORT).show();
            //mGatt = device.connectGatt(getApplicationContext(), true, mGattCallbackRespeck);
        }else if(device.getName().equals("QOE")){
            Toast.makeText(getApplicationContext(),"Connecting to "+device.getName(),Toast.LENGTH_SHORT).show();
            mGatt2 = device.connectGatt(getApplicationContext(), true, mGattCallbackQOE);
        }

        //scanLeDevice(false);will stop after first device detection
        // }
    }


    private final BluetoothGattCallback mGattCallbackQOE = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,int newState) {
            Log.i("QOE", "Discovering Services");
            gatt.discoverServices();
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i("QOE", "Services Discovered");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                Log.i("onServicesDiscoveredQOE", services.toString());
                //gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));

                for (BluetoothGattService s : services) {
                    BluetoothGattCharacteristic characteristic = s.getCharacteristic(UUID.fromString(QOE_LIVE_CHARACTERISTIC));

                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor3 = characteristic.getDescriptor(UUID.fromString(QOE_CLIENT_CHARACTERISTIC));
                        descriptor3.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor3);
                    }
                }


            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (descriptor.getCharacteristic().getUuid().equals(UUID.fromString(QOE_LIVE_CHARACTERISTIC))) {
                Log.i("QOE", "Descriptor Write Success");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            BluetoothGattCharacteristic characteristic_alpha_2;
            characteristic_alpha_2 = characteristic;
            int sampleId2 = characteristic_alpha_2.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

            int packetNumber2 = characteristic_alpha_2.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);


            lastPackets_2[packetNumber2] = characteristic_alpha_2.getValue();
            sampleIDs_2[packetNumber2] = sampleId2;
            if (characteristic.getUuid().equals(UUID.fromString(QOE_LIVE_CHARACTERISTIC))) {
                if ((sampleIDs_2[0] == sampleIDs_2[1]) && lastSample != sampleIDs_2[0] && (sampleIDs_2[1] == sampleIDs_2[2] && (sampleIDs_2[2] == sampleIDs_2[3]) && (sampleIDs_2[3] == sampleIDs_2[4]))) {


                    //CODIGO MANTAS
                    SimpleDateFormat formater1 = new SimpleDateFormat("yyyyMMddkkmmss");
                    Date date = new Date();
                    String time1 = formater1.format(date);

                    byte[] finalPacket = {};
                    int size = 5;

                    finalPacket = new byte[lastPackets_2[0].length + lastPackets_2[1].length + lastPackets_2[2].length + lastPackets_2[3].length + lastPackets_2[4].length - 8];
                    int finalPacketIndex = 0;
                    for (int i = 0; i < size; i++) {
                        for (int j = 2; j < lastPackets_2[i].length; j++) {
                            finalPacket[finalPacketIndex] = lastPackets_2[i][j];
                            finalPacketIndex++;
                        }
                    }

                    ByteBuffer packetBufferLittleEnd = ByteBuffer.wrap(finalPacket).order(ByteOrder.LITTLE_ENDIAN);
                    ByteBuffer packetBufferBigEnd = ByteBuffer.wrap(finalPacket).order(ByteOrder.BIG_ENDIAN);

                    int bin0 = packetBufferLittleEnd.getShort();
                    int bin1 = packetBufferLittleEnd.getShort();
                    int bin2 = packetBufferLittleEnd.getShort();
                    int bin3 = packetBufferLittleEnd.getShort();
                    int bin4 = packetBufferLittleEnd.getShort();
                    int bin5 = packetBufferLittleEnd.getShort();
                    int bin6 = packetBufferLittleEnd.getShort();
                    int bin7 = packetBufferLittleEnd.getShort();
                    int bin8 = packetBufferLittleEnd.getShort();
                    int bin9 = packetBufferLittleEnd.getShort();
                    int bin10 = packetBufferLittleEnd.getShort();
                    int bin11 = packetBufferLittleEnd.getShort();
                    int bin12 = packetBufferLittleEnd.getShort();
                    int bin13 = packetBufferLittleEnd.getShort();
                    int bin14 = packetBufferLittleEnd.getShort();
                    int bin15 = packetBufferLittleEnd.getShort();

                    int total = bin0 + bin1 + bin2 + bin3
                            + bin4 + bin5 + bin6 + bin7
                            + bin8 + bin9 + bin10 + bin11
                            + bin12 + bin13 + bin14 + bin15;

                    String bins_data_string = bin0 + "," + bin1 + "," + bin2 + "," + bin3 + ","
                            + bin4 + "," + bin5 + "," + bin6 + "," + bin7 + ","
                            + bin8 + "," + bin9 + "," + bin10 + "," + bin11 + ","
                            + bin12 + "," + bin13 + "," + bin14 + "," + bin15;

                    double temperature = -1;
                    double hum = -1;


                    float pm1 = -1;
                    float pm2_5 = -1;
                    float pm10 = -1;

                    float opctemp = -1;

                    int o3_ae = -1;
                    int o3_we = -1;

                    int no2_ae = -1;
                    int no2_we = -1;

                    // MtoF
                    packetBufferLittleEnd.getInt();
                    // opc_temp
                    opctemp = packetBufferLittleEnd.getInt();
                    // opc_pressure
                    packetBufferLittleEnd.getInt();
                    // period count
                    packetBufferLittleEnd.getInt();
                    // uint16_t checksum ????
                    packetBufferLittleEnd.getShort();
                    pm1 = packetBufferLittleEnd.getFloat();
                    pm2_5 = packetBufferLittleEnd.getFloat();
                    pm10 = packetBufferLittleEnd.getFloat();

                    o3_ae = packetBufferBigEnd.getShort(62);
                    o3_we = packetBufferBigEnd.getShort(64);

                    no2_ae = packetBufferBigEnd.getShort(66);
                    no2_we = packetBufferBigEnd.getShort(68);

                    /* uint16_t temp */
                    int temp = packetBufferBigEnd.getShort(70) & 0xffff;

                    /* uint16_t humidity */
                    int humidity = packetBufferBigEnd.getShort(72);

                    temperature = ((temp - 3960) / 100.0);
                    hum = (-2.0468 + (0.0367 * humidity) + (-0.0000015955 * humidity * humidity));
                    Log.i("QOE", "PM1: " + pm1);

                    Log.i("QOE", "PM2.5: " + pm2_5);
                    Log.i("QOE", "PM10: " + pm10);

                    mHomeScreenReadingValues.clear();
                    mHomeScreenReadingValues.add(0f);
                    mHomeScreenReadingValues.add(pm2_5);
                    mHomeScreenReadingValues.add(pm10);


                    mAQReadingValues.clear();
                    mAQReadingValues.add((float)temperature);
                    mAQReadingValues.add((float)hum);
                    mAQReadingValues.add((float)o3_ae);
                    mAQReadingValues.add((float)no2_ae);
                    mAQReadingValues.add(pm1);
                    mAQReadingValues.add(pm2_5);
                    mAQReadingValues.add(pm10);
                    mAQReadingValues.add((float)total);


                    /*
                    bin0Value = Float.valueOf(bin0);
                    bin1Value = Float.valueOf(bin1);
                    bin2Value = Float.valueOf(bin2);
                    bin3Value = Float.valueOf(bin3);
                    bin4Value = Float.valueOf(bin4);
                    bin5Value = Float.valueOf(bin5);
                    bin6Value = Float.valueOf(bin6);
                    bin7Value = Float.valueOf(bin7);
                    bin8Value = Float.valueOf(bin8);
                    bin9Value = Float.valueOf(bin9);
                    bin10Value = Float.valueOf(bin10);
                    bin11Value = Float.valueOf(bin11);
                    bin12Value = Float.valueOf(bin12);
                    bin13Value = Float.valueOf(bin13);
                    bin14Value = Float.valueOf(bin14);
                    bin15Value = Float.valueOf(bin15);
                    */
                    myHandler.sendEmptyMessage(0);

                    lastSample = sampleIDs_2[0];
                }
            }
        }
    };
    private float doUpdate(){
        mHomeFragment.setReadings(mHomeScreenReadingValues);
        mAQReadingsFragment.setReadings(mAQReadingValues);
        return 0;
    }
}
