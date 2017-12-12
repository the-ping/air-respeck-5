package com.specknet.airrespeck.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
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
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.activities.MainActivity;
//import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

import static com.specknet.airrespeck.utils.Constants.AIRSPECK_LIVE_CHARACTERISTIC;
import static com.specknet.airrespeck.utils.Constants.AIRSPECK_POWER_CHARACTERISTIC;

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

    // Classes to handle received packets
    private RESpeckPacketHandler respeckHandler;
    private AirspeckPacketHandler airspeckHandler;

    private RxBleDevice mAirspeckDevice;
    private RxBleDevice mRESpeckDevice;

    private boolean mIsAirspeckFound;
    private boolean mIsRESpeckFound;

    private Subscription scanSubscription;

    private RxBleConnection.RxBleConnectionState mLastRESpeckConnectionState;

    private boolean mIsServiceRunning = false;

    private byte[] offCommand = {1};

    private static boolean turn_off = false;



    public SpeckBluetoothService() {

    }

    public static class AirspeckOffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("SpeckService", "Turn off");
            turn_off = true;
        }

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
        respeckHandler = new RESpeckPacketHandler(this);
        airspeckHandler = new AirspeckPacketHandler(this);
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

        // Get Bluetooth address
        AIRSPECK_UUID = mUtils.getProperties().getProperty(Constants.Config.AIRSPECK_UUID);
        RESPECK_UUID = mUtils.getProperties().getProperty(Constants.Config.RESPECK_UUID);
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
                                    if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(
                                            AIRSPECK_UUID)) {
                                        mIsAirspeckFound = true;
                                        SpeckBluetoothService.this.connectToAirspeck();
                                    }
                                }
                                if (mIsRESpeckEnabled && !mIsRESpeckFound) {
                                    if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(RESPECK_UUID)) {
                                        mIsRESpeckFound = true;
                                        Log.i("SpeckService", "Connecting after scanning");
                                        SpeckBluetoothService.this.connectToRESpeck();
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
        mAirspeckDevice = rxBleClient.getBleDevice(AIRSPECK_UUID);
        establishAirspeckConnection();
    }

    private void establishAirspeckConnection() {
        Log.i("SpeckService", "Connecting to Airspeck...");
        airspeckSubscription = mAirspeckDevice.establishConnection(true)
                .flatMap(new Func1<RxBleConnection, Observable<?>>() {
                    @Override
                    public Observable<?> call(RxBleConnection rxBleConnection) {
                        return rxBleConnection.setupNotification(UUID.fromString(
                                AIRSPECK_LIVE_CHARACTERISTIC));
                    }
                })
                .doOnNext(new Action1<Object>() {
                    @Override
                    public void call(Object notificationObservable) {
                        // Notification has been set up
                        Log.i("SpeckService", "Subscribed to Airspeck");
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
                                Log.i("SpeckService", "turnOff: " + turn_off);
                                if (turn_off) {
                                    airspeckSubscription.unsubscribe();
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            turnOffAirspeck();
                                        }
                                    }, 2000);
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                // An error with autoConnect means that we are disconnected
                                Log.e("SpeckService", "Airspeck disconnected: " + throwable.toString());

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
        mAirspeckDevice.establishConnection(true)
        .flatMap(new Func1<RxBleConnection, Observable<?>>() {
            @Override
            public Observable<?> call(RxBleConnection rxBleConnection) {
                return rxBleConnection.writeCharacteristic(UUID.fromString(AIRSPECK_POWER_CHARACTERISTIC),offCommand);

            }
        }).doOnNext(new Action1<Object>() {
            @Override
            public void call(Object notificationObservable) {
                // Notification has been set up
                Log.i("SpeckService", "Subscribed to Airspeck");
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
                        Log.e("SpeckService", "Turning off: " + bytes.toString());
                    }
                },
                new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // An error with autoConnect means that we are disconnected
                        Log.e("SpeckService", "Airspeck disconnected: " + throwable.toString());
                    }
                });
    }

    private void connectToRESpeck() {
        mRESpeckDevice = rxBleClient.getBleDevice(RESPECK_UUID);
        mRESpeckDevice.observeConnectionStateChanges()
                .subscribe(
                        new Action1<RxBleConnection.RxBleConnectionState>() {
                            @Override
                            public void call(RxBleConnection.RxBleConnectionState connectionState) {
                                if (connectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED && mIsServiceRunning) {
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
                                Log.e("SpeckService", "RESpeck bluetooth error: " + throwable.toString());
                            }
                        }
                );
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
    }
}

