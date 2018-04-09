package com.specknet.airrespeck.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleScanResult;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.FileLogger;
import com.specknet.airrespeck.utils.Utils;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

import static com.specknet.airrespeck.utils.Utils.bytesToHex;

/**
 * Service for connecting to RESpeck and Airspeck sensors, converting the data into a readable format and
 * sending the result to the interested Activities.
 */

public class SpeckBluetoothService extends Service {

    // Bluetooth connection
    private BluetoothAdapter mBluetoothAdapter;
    public static RxBleClient rxBleClient;
    private Subscription airspeckSubscription;
    private Subscription respeckLiveSubscription;

    // Config settings
    private boolean mIsAirspeckEnabled;
    private boolean mIsRESpeckEnabled;

    // The UUIDs will be loaded from Config
    private static String RESPECK_UUID;
    private static String AIRSPECK_UUID;

    // The BLE addresses will be used to connect
    private static String RESPECK_BLE_ADDRESS;
    private static String AIRSPECK_BLE_ADDRESS;

    // Classes to handle received packets
    private RESpeckPacketHandler respeckHandler;
    private AirspeckPacketHandler airspeckHandler;

    private RxBleDevice mAirspeckDevice;
    private RxBleDevice mRESpeckDevice;

    private boolean mIsAirspeckFound;
    private boolean mIsRESpeckFound;

    private String mRESpeckName;

    private Subscription scanSubscription;

    private RxBleConnection.RxBleConnectionState mLastRESpeckConnectionState;

    private boolean mIsServiceRunning = false;

    private BroadcastReceiver airspeckOffSignalReceiver;

    public SpeckBluetoothService() {

    }


    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        new Thread() {
            @Override
            public void run() {
                Log.i("SpeckService", "Starting SpeckService...");
                FileLogger.logToFile(SpeckBluetoothService.this, "Main Bluetooth service started");
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
        loadConfigInstanceVariables();

        // Initializes a Bluetooth adapter. For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(
                Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Get singleton instances of packet handler classes
        respeckHandler = new RESpeckPacketHandler(this);
        airspeckHandler = new AirspeckPacketHandler(this);

        // Register broadcast receiver to receive airspeck off signal
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.AIRSPECK_OFF_ACTION);
        airspeckOffSignalReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("SpeckService", "Got turn off message");
                airspeckSubscription.unsubscribe();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        turnOffAirspeck();
                    }
                }, 2000);
            }
        };
        this.registerReceiver(airspeckOffSignalReceiver, intentFilter);
    }

    private void loadConfigInstanceVariables() {
        // Get references to Utils
        Utils utils = Utils.getInstance();
        Map<String, String> loadedConfig = utils.getConfig(this);

        // Look whether Airspeck is enabled in config
        mIsAirspeckEnabled = !loadedConfig.get(Constants.Config.AIRSPECKP_UUID).isEmpty();

        // Is RESpeck enabled?
        mIsRESpeckEnabled = !loadedConfig.get(Constants.Config.RESPECK_UUID).isEmpty();

        // Get Bluetooth address
        AIRSPECK_UUID = loadedConfig.get(Constants.Config.AIRSPECKP_UUID);
        RESPECK_UUID = loadedConfig.get(Constants.Config.RESPECK_UUID);
    }

    /**
     * Check Bluetooth availability and initiate devices scanning.
     */
    public void startServiceAndBluetoothScanning() {
        mIsServiceRunning = true;

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

                                if ((mIsAirspeckFound || !mIsAirspeckEnabled) &&
                                        (mIsRESpeckFound || !mIsRESpeckEnabled)) {
                                    scanSubscription.unsubscribe();
                                }

                                if (mIsAirspeckEnabled && !mIsAirspeckFound) {
                                    // Process scan result here.
                                    if (AIRSPECK_UUID.contains(":")) {
                                        // Old BLE address
                                        if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(
                                                AIRSPECK_UUID)) {
                                            AIRSPECK_BLE_ADDRESS = AIRSPECK_UUID;
                                            mIsAirspeckFound = true;
                                            SpeckBluetoothService.this.connectToAirspeck();
                                        }
                                    }
                                    else
                                    {
                                        // New UUID
                                        byte[] ba = rxBleScanResult.getScanRecord();
                                        if (ba != null && ba.length == 62) {
                                            byte[] uuid = Arrays.copyOfRange(ba, 7, 15);
                                            Log.i("SpeckService", "uuid from airspeck: " + bytesToHex(uuid));
                                            Log.i("SpeckService", "uuid from config: " + AIRSPECK_UUID.substring(5));
                                            if (bytesToHex(uuid).equalsIgnoreCase(AIRSPECK_UUID.substring(5))) {
                                                mIsAirspeckFound = true;
                                                AIRSPECK_BLE_ADDRESS = rxBleScanResult.getBleDevice().getMacAddress();
                                                Log.i("SpeckService", "Connecting after scanning to: " + AIRSPECK_BLE_ADDRESS);
                                                SpeckBluetoothService.this.connectToAirspeck();
                                            }
                                        }
                                    }
                                }
                                if (mIsRESpeckEnabled && !mIsRESpeckFound) {
                                    if (RESPECK_UUID.contains(":")) {
                                        // Old BLE address
                                        if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(RESPECK_UUID)) {
                                            RESPECK_BLE_ADDRESS = RESPECK_UUID;
                                            mIsRESpeckFound = true;
                                            Log.i("SpeckService", "Connecting after scanning");
                                            SpeckBluetoothService.this.connectToRESpeck();
                                        }
                                    }
                                    else {
                                        // New UUID
                                        byte[] ba = rxBleScanResult.getScanRecord();
                                        if (ba != null && ba.length == 62) {
                                            byte[] uuid = Arrays.copyOfRange(ba, 7, 15);
                                            Log.i("SpeckService", "uuid from respeck: " + bytesToHex(uuid));
                                            Log.i("SpeckService", "uuid from config: " + RESPECK_UUID.substring(5));
                                            if (bytesToHex(uuid).equalsIgnoreCase(RESPECK_UUID.substring(5))) {
                                                mIsRESpeckFound = true;
                                                RESPECK_BLE_ADDRESS = rxBleScanResult.getBleDevice().getMacAddress();
                                                Log.i("SpeckService", "Connecting after scanning to: " + RESPECK_BLE_ADDRESS);
                                                SpeckBluetoothService.this.connectToRESpeck();
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                // Handle an error here.
                                Log.e("SpeckService", "Error while scanning: " + throwable.toString());
                            }
                        }
                );
    }

    private void connectToAirspeck() {
        mAirspeckDevice = rxBleClient.getBleDevice(AIRSPECK_BLE_ADDRESS);
        establishAirspeckConnection();
    }

    private void establishAirspeckConnection() {
        Log.i("SpeckService", "Connecting to Airspeck...");
        FileLogger.logToFile(this, "Connecting to Airspeck");

        airspeckSubscription = mAirspeckDevice.establishConnection(true)
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
                        Log.i("SpeckService", "Subscribed to Airspeck");
                        FileLogger.logToFile(SpeckBluetoothService.this, "Subscribed to Airspeck");
                        Intent airspeckFoundIntent = new Intent(Constants.ACTION_AIRSPECK_CONNECTED);
                        airspeckFoundIntent.putExtra(Constants.Config.AIRSPECKP_UUID, AIRSPECK_UUID);
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
                                //Log.i("SpeckService", "turnOff: " + turn_off);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                // An error with autoConnect means that we are disconnected
                                Log.e("SpeckService", "Airspeck disconnected: " + throwable.toString());
                                FileLogger.logToFile(SpeckBluetoothService.this, "Airspeck disconnected");

                                Intent airspeckDisconnectedIntent = new Intent(
                                        Constants.ACTION_AIRSPECK_DISCONNECTED);
                                sendBroadcast(airspeckDisconnectedIntent);

                                airspeckSubscription.unsubscribe();
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        establishAirspeckConnection();
                                    }
                                }, 2000);
                            }
                        }
                );
    }

    public void turnOffAirspeck() {
        Log.e("SpeckService", "Turning off");
        FileLogger.logToFile(this, "Turning off Airspeck after power off command");
        mAirspeckDevice.establishConnection(true)
                .flatMap(new Func1<RxBleConnection, Observable<?>>() {
                    @Override
                    public Observable<?> call(RxBleConnection rxBleConnection) {
                        return rxBleConnection.writeCharacteristic(UUID.fromString(
                                Constants.AIRSPECK_POWER_OFF_CHARACTERISTIC),
                                Constants.OFF_COMMAND);

                    }
                })
                .subscribe(
                        new Action1<Object>() {
                            @Override
                            public void call(Object bytes) {
                                Log.e("SpeckService", "Turning off: " + bytes.toString());
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {

                                // An error with autoConnect means that we are disconnected
                                Log.e("SpeckService", "Airspeck turned off: " + throwable.toString());
                                FileLogger.logToFile(SpeckBluetoothService.this, "Airspeck turned off");
                                Intent airspeckDisconnectedIntent = new Intent(
                                        Constants.ACTION_AIRSPECK_DISCONNECTED);
                                sendBroadcast(airspeckDisconnectedIntent);

                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        establishAirspeckConnection();
                                    }
                                }, 2000);
                            }
                        });
    }

    private void connectToRESpeck() {
        mRESpeckDevice = rxBleClient.getBleDevice(RESPECK_BLE_ADDRESS);
        mRESpeckName = mRESpeckDevice.getName();
        mRESpeckDevice.observeConnectionStateChanges()
                .subscribe(
                        new Action1<RxBleConnection.RxBleConnectionState>() {
                            @Override
                            public void call(RxBleConnection.RxBleConnectionState connectionState) {
                                if (connectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED && mIsServiceRunning) {
                                    FileLogger.logToFile(SpeckBluetoothService.this, "RESpeck disconnected");
                                    Intent respeckDisconnectedIntent = new Intent(
                                            Constants.ACTION_RESPECK_DISCONNECTED);
                                    sendBroadcast(respeckDisconnectedIntent);

                                    if (mLastRESpeckConnectionState == RxBleConnection.RxBleConnectionState.CONNECTED) {
                                        // If we were just disconnected, try to immediately connect again.
                                        Log.i("SpeckService", "RESpeck connection lost, trying to reconnect");
                                        new Timer().schedule(new TimerTask() {
                                            @Override
                                            public void run() {
                                                establishRESpeckConnection();
                                            }
                                        }, 2000);
                                    } else if (mLastRESpeckConnectionState == RxBleConnection.RxBleConnectionState.CONNECTING) {
                                        // This means we tried to reconnect, but there was a timeout. In this case we
                                        // wait for x seconds before reconnecting
                                        Log.i("SpeckService",
                                                String.format(
                                                        "RESpeck connection timeout, waiting %d seconds before reconnect",
                                                        Constants.RECONNECTION_TIMEOUT_MILLIS / 1000));
                                        new Timer().schedule(new TimerTask() {
                                            @Override
                                            public void run() {
                                                Log.i("SpeckService", "RESpeck reconnecting...");
                                                establishRESpeckConnection();
                                            }
                                        }, Constants.RECONNECTION_TIMEOUT_MILLIS);
                                    }
                                }
                                mLastRESpeckConnectionState = connectionState;
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.e("SpeckService",
                                        "Error occured while listening to RESpeck connection state changes: " +
                                                throwable.getMessage());
                            }
                        }
                );
        establishRESpeckConnection();
    }

    private void establishRESpeckConnection() {
        Log.i("SpeckService", "Connecting to RESpeck...");
        FileLogger.logToFile(this, "Connecting to RESpeck");
        respeckLiveSubscription = mRESpeckDevice.establishConnection(false)
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
                        Log.i("SpeckService", "Subscribed to RESpeck");
                        FileLogger.logToFile(SpeckBluetoothService.this, "Subscribed to RESpeck");
                        Intent respeckFoundIntent = new Intent(Constants.ACTION_RESPECK_CONNECTED);
                        respeckFoundIntent.putExtra(Constants.Config.RESPECK_UUID, RESPECK_UUID);
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
                                Log.e("SpeckService", "RESpeck bluetooth error: " + throwable.toString());
                            }
                        }
                );
    }

    public String getRESpeckFwVersion() {
        return mRESpeckName.substring(4);
    }


    public void stopSpeckService() {
        Log.i("SpeckService", "Stopping Speck Service");
        mIsServiceRunning = false;

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
            Log.e("SpeckService", "Error while closing handlers: " + e.getMessage());
        }

        this.unregisterReceiver(this.airspeckOffSignalReceiver);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        FileLogger.logToFile(this, "Main Bluetooth service stopped by Android");
        return super.onUnbind(intent);
    }
}

