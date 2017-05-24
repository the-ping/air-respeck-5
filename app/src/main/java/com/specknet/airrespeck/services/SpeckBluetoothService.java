package com.specknet.airrespeck.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleScanResult;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.util.UUID;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Service for connecting to RESpeck and Airspeck sensors, converting the data into a readable format and
 * sending the result to the interested Activities.
 */

public class SpeckBluetoothService extends Service {

    // Bluetooth connection
    private BluetoothAdapter mBluetoothAdapter;
    public static RxBleClient rxBleClient;
    private boolean mIsAirspeckFound;
    private boolean mIsRESpeckFound;
    private Subscription scanSubscription;
    private Subscription airspeckSubscription;
    private Subscription respeckLiveSubscription;

    // Config settings
    private boolean mIsAirspeckEnabled;
    private boolean mIsRESpeckEnabled;

    // The UUIDs will be loaded from Config
    private static String RESPECK_UUID;
    private static String AIRSPECK_UUID;

    // Classes to handle received packets
    private RESpeckPacketHandler respeckHandler;
    private AirspeckPacketHandler airspeckHandler;

    public SpeckBluetoothService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        new Thread() {
            @Override
            public void run() {
                Log.i("SpeckService", "Starting SpeckService...");
                startInForeground();
                initSpeckService();
                startServiceAndBluetoothScanning();
            }
        }.start();
        return START_STICKY;
    }

    private void startInForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getText(R.string.notification_speck_title))
                .setContentText(getText(R.string.notification_speck_text))
                .setSmallIcon(R.drawable.vec_wireless)
                .setContentIntent(pendingIntent)
                .build();

        // Just use a "random" service ID
        final int SERVICE_NOTIFICATION_ID = 8598001;
        startForeground(SERVICE_NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        stopSpeckService();
        Log.i("SpeckService", "SpeckService has been stopped");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // We don't allow threads to bind to this service. Once the service is started, it sends updates
        // via broadcasts and there is no need for calls from outside
        return null;
    }

    /**
     * Initiate Bluetooth adapter.
     */
    public void initSpeckService() {
        loadConfig();

        // Initializes a Bluetooth adapter. For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(
                Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Get singleton instances of packet handler classes
        respeckHandler = RESpeckPacketHandler.getInstance(this);
        airspeckHandler = AirspeckPacketHandler.getInstance(this);
    }

    private void loadConfig() {
        // Get references to Utils
        Utils mUtils = Utils.getInstance(getApplicationContext());

        // Look whether Airspeck is enabled in config
        mIsAirspeckEnabled = Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_AIRSPECK_ENABLED));

        // Is RESpeck enabled?
        mIsRESpeckEnabled = !Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_RESPECK_DISABLED));

        // Look whether Airspeck is enabled in config
        mIsAirspeckEnabled = Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_AIRSPECK_ENABLED));

        // Get Bluetooth address
        AIRSPECK_UUID = mUtils.getProperties().getProperty(Constants.Config.AIRSPECK_UUID);
        RESPECK_UUID = mUtils.getProperties().getProperty(Constants.Config.RESPECK_UUID);
    }

    /**
     * Check Bluetooth availability and initiate devices scanning.
     */
    public void startServiceAndBluetoothScanning() {
        // Check if Bluetooth is supported on the device
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
            return;
        }

        mIsAirspeckFound = false;
        mIsRESpeckFound = false;

        rxBleClient = RxBleClient.create(this);
        Log.i("SpeckService", "Scanning..");

        scanSubscription = rxBleClient.scanBleDevices()
                .subscribe(
                        new Action1<RxBleScanResult>() {
                            @Override
                            public void call(RxBleScanResult rxBleScanResult) {
                                Log.i("SpeckService",
                                        "FOUND :" + rxBleScanResult.getBleDevice().getName() + ", " +
                                                rxBleScanResult.getBleDevice().getMacAddress());
                                if (mIsAirspeckEnabled && !mIsAirspeckFound) {
                                    // Process scan result here.
                                    if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(
                                            AIRSPECK_UUID)) {
                                        mIsAirspeckFound = true;
                                        SpeckBluetoothService.this.connectAndNotifyAirspeck();
                                    }
                                }
                                if (mIsRESpeckEnabled && !mIsRESpeckFound) {
                                    if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(RESPECK_UUID)) {
                                        mIsRESpeckFound = true;
                                        SpeckBluetoothService.this.connectAndNotifyRespeck();
                                    }
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                // Handle an error here.
                                Log.e("SpeckService", throwable.toString());
                            }
                        }
                );
    }

    private void connectAndNotifyAirspeck() {
        if ((mIsAirspeckFound && mIsRESpeckFound) || !mIsRESpeckEnabled)
            scanSubscription.unsubscribe();

        RxBleDevice device = rxBleClient.getBleDevice(AIRSPECK_UUID);
        // Given characteristic has been changes, here is the value.
        airspeckSubscription = device.establishConnection(true)
                .flatMap(new Func1<RxBleConnection, Observable<?>>() {
                    @Override
                    public Observable<?> call(RxBleConnection rxBleConnection) {
                        return rxBleConnection.setupNotification(UUID.fromString(
                                Constants.AIRSPECK_LIVE_CHARACTERISTIC));
                    }
                })
                .doOnNext(new Action1<Object>() {
                    @Override
                    public void call(Object notificationObservable) {
                        // Notification has been set up
                        Log.i("SpeckService", "Subscribed to AIRSPECK");
                        Intent airspeckFoundIntent = new Intent(Constants.ACTION_AIRSPECK_CONNECTED);
                        airspeckFoundIntent.putExtra(Constants.AIRSPECK_UUID, AIRSPECK_UUID);
                        sendBroadcast(airspeckFoundIntent);
                    }
                })
                .flatMap(
                        new Func1<Object, Observable<?>>() {
                            @Override
                            public Observable<?> call(Object notificationObservable) {
                                return (Observable) notificationObservable;
                            }
                        }) // <-- Notification has been set up, now observe value changes.
                .subscribe(
                        new Action1<Object>() {
                            @Override
                            public void call(Object bytes) {
                                airspeckHandler.processAirspeckPacket((byte[]) bytes);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                // Handle an error here.
                                Log.i("SpeckService", "AIRSPECK DISCONNECTED: " + throwable.toString());
                                Intent airspeckDisconnectedIntent = new Intent(Constants.ACTION_AIRSPECK_DISCONNECTED);
                                sendBroadcast(airspeckDisconnectedIntent);
                                SpeckBluetoothService.this.reconnectAirspeck();
                            }
                        }
                );
    }

    private void reconnectAirspeck() {
        airspeckSubscription.unsubscribe();
        connectAndNotifyAirspeck();
    }

    private void connectAndNotifyRespeck() {
        if ((mIsAirspeckFound && mIsRESpeckFound) || !mIsAirspeckEnabled)
            scanSubscription.unsubscribe();

        RxBleDevice device = rxBleClient.getBleDevice(RESPECK_UUID);
        respeckLiveSubscription = device.establishConnection(true)
                .flatMap(new Func1<RxBleConnection, Observable<?>>() {
                    @Override
                    public Observable<?> call(RxBleConnection rxBleConnection) {
                        return rxBleConnection.setupNotification(
                                UUID.fromString(Constants.RESPECK_LIVE_CHARACTERISTIC));
                    }
                })
                .doOnNext(new Action1<Object>() {
                    @Override
                    public void call(Object notificationObservable) {
                        // Notification has been set up
                        Log.i("SpeckService", "Subscribed to RESPECK");
                        Intent respeckFoundIntent = new Intent(Constants.ACTION_RESPECK_CONNECTED);
                        respeckFoundIntent.putExtra(Constants.RESPECK_UUID, RESPECK_UUID);
                        sendBroadcast(respeckFoundIntent);
                    }
                })
                .flatMap(
                        new Func1<Object, Observable<?>>() {
                            @Override
                            public Observable<?> call(Object notificationObservable) {
                                return (Observable) notificationObservable;
                            }
                        })
                .subscribe(
                        new Action1<Object>() {
                            @Override
                            public void call(Object characteristicValue) {
                                // Given characteristic has been changes, process the value
                                respeckHandler.processRESpeckLivePacket((byte[]) characteristicValue);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                // Handle an error here.
                                if (throwable instanceof BleDisconnectedException) {
                                    Log.i("SpeckService", "RESPECK DISCONNECTED: " + throwable.toString());
                                    Intent respeckDisconnectedIntent = new Intent(
                                            Constants.ACTION_RESPECK_DISCONNECTED);
                                    sendBroadcast(respeckDisconnectedIntent);
                                    SpeckBluetoothService.this.reconnectRespeck();
                                } else {
                                    Log.e("SpeckService", "Notification handling error: " + throwable.toString());
                                }
                            }
                        }
                );
    }

    private void reconnectRespeck() {
        respeckLiveSubscription.unsubscribe();
        connectAndNotifyRespeck();
    }

    public void stopSpeckService() {
        Log.i("SpeckService", "Stopping Speck Service");

        // Close bluetooth scanning
        if (scanSubscription != null) {
            scanSubscription.unsubscribe();
        }
        if (respeckLiveSubscription != null) {
            respeckLiveSubscription.unsubscribe();
        }
        if (airspeckSubscription != null) {
            airspeckSubscription.unsubscribe();
        }

        // Close the handlers
        try {
            respeckHandler.closeHandler();
            airspeckHandler.closeHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AirspeckData getMostRecentAirspeckReading() {
        return airspeckHandler.getMostRecentAirspeckData();
    }
}

