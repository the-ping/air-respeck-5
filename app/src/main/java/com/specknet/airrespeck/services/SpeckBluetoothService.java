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
import com.specknet.airrespeck.services.airspeckuploadservice.AirspeckRemoteUploadService;
import com.specknet.airrespeck.services.inhaleruploadservice.InhalerRemoteUploadService;
import com.specknet.airrespeck.services.pulseoxuploadservice.PulseoxRemoteUploadService;
import com.specknet.airrespeck.services.respeckuploadservice.RespeckAndDiaryRemoteUploadService;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.FileLogger;
import com.specknet.airrespeck.utils.Utils;

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
    private Subscription pulseoxSubscription;
    private Subscription inhalerSubscription;

    // Config settings
    private boolean mIsAirspeckEnabled;
    private boolean mIsRESpeckEnabled;
    private boolean mIsPulseoxEnabled;
    private boolean mIsInhalerEnabled;
    private boolean mIsUploadData;

    // The UUIDs will be loaded from Config
    private static String RESPECK_UUID;
    private static String AIRSPECK_UUID;
    private static String PULSEOX_UUID;
    private static String INHALER_UUID;

    // The BLE addresses will be used to connect
    private static String RESPECK_BLE_ADDRESS;
    private static String AIRSPECK_BLE_ADDRESS;
    private static String PULSEOX_BLE_ADDRESS;
    private static String INHALER_BLE_ADDRESS;

    // Classes to handle received packets
    private RESpeckPacketHandler respeckHandler;
    private AirspeckPacketHandler airspeckHandler;
    private PulseoxPacketHandler pulseoxHandler;
    private InhalerPacketHandler inhalerHandler;

    private RxBleDevice mAirspeckDevice;
    private RxBleDevice mRESpeckDevice;
    private RxBleDevice mPulseoxDevice;
    private RxBleDevice mInhalerDevice;

    private boolean mIsAirspeckFound;
    private boolean mIsRESpeckFound;
    private boolean mIsPulseoxFound;
    private boolean mIsInhalerFound;

    private String mRESpeckName;
    private String mAirspeckName;
    private String mPulseoxName;
    private String mInhalerName;

    private Subscription scanSubscription;
    private RxBleConnection.RxBleConnectionState mLastRESpeckConnectionState;
    private boolean mIsServiceRunning = false;
    private BroadcastReceiver airspeckOffSignalReceiver;
    private BroadcastReceiver respeckPausedReceiver;

    // Upload classes
    private AirspeckRemoteUploadService mAirspeckUploadService;
    private RespeckAndDiaryRemoteUploadService mRespeckUploadService;
    private PulseoxRemoteUploadService mPulseoxUploadService;
    private InhalerRemoteUploadService mInhalerUploadService;

    private boolean mIsRESpeckPaused;

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
                .setSmallIcon(R.drawable.vec_wireless_active)
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
        pulseoxHandler = new PulseoxPacketHandler(this);
        inhalerHandler = new InhalerPacketHandler(this);

        mIsRESpeckPaused = false;

        // Create data uploading classes if desired
        if (mIsUploadData) {
            if (mIsRESpeckEnabled) {
                mRespeckUploadService = new RespeckAndDiaryRemoteUploadService(this);
            }
            if (mIsAirspeckEnabled) {
                mAirspeckUploadService = new AirspeckRemoteUploadService(this);
            }
            if (mIsPulseoxEnabled) {
                mPulseoxUploadService = new PulseoxRemoteUploadService(this);
            }
            if (mIsInhalerEnabled) {
                mInhalerUploadService = new InhalerRemoteUploadService(this);
            }
        }

        // Register broadcast receiver to receive airspeck off signal
        final IntentFilter intentFilterAirspeckOff = new IntentFilter();
        intentFilterAirspeckOff.addAction(Constants.AIRSPECK_OFF_ACTION);
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
        registerReceiver(airspeckOffSignalReceiver, intentFilterAirspeckOff);

        final IntentFilter intentFilterRESpeckPaused = new IntentFilter();
        intentFilterRESpeckPaused.addAction(Constants.ACTION_RESPECK_RECORDING_PAUSE);
        intentFilterRESpeckPaused.addAction(Constants.ACTION_RESPECK_RECORDING_CONTINUE);
        respeckPausedReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_RESPECK_RECORDING_PAUSE)) {
                    Log.i("SpeckService", "Received message to pause RESpeck recording");
                    mIsRESpeckPaused = true;
                } else if (intent.getAction().equals(Constants.ACTION_RESPECK_RECORDING_CONTINUE)) {
                    Log.i("SpeckService", "Received message to continue RESpeck recording");
                    mIsRESpeckPaused = false;
                }
            }
        };
        registerReceiver(respeckPausedReceiver, intentFilterRESpeckPaused);
    }

    private void loadConfigInstanceVariables() {
        // Get references to Utils
        Utils utils = Utils.getInstance();
        Map<String, String> loadedConfig = utils.getConfig(this);

        // Look whether each of the devices is enabled
        mIsAirspeckEnabled = !loadedConfig.get(Constants.Config.AIRSPECKP_UUID).isEmpty();

        mIsRESpeckEnabled = !loadedConfig.get(Constants.Config.RESPECK_UUID).isEmpty();

        mIsPulseoxEnabled = loadedConfig.containsKey(Constants.Config.PULSEOX_UUID) && !loadedConfig.get(
                Constants.Config.PULSEOX_UUID).isEmpty();
        mIsInhalerEnabled = loadedConfig.containsKey(Constants.Config.INHALER_UUID) && !loadedConfig.get(
                Constants.Config.INHALER_UUID).isEmpty();

        // Do we want to upload the data?
        mIsUploadData = Boolean.parseBoolean(loadedConfig.get(Constants.Config.UPLOAD_TO_SERVER));

        // Get Bluetooth address
        AIRSPECK_UUID = loadedConfig.get(Constants.Config.AIRSPECKP_UUID);
        RESPECK_UUID = loadedConfig.get(Constants.Config.RESPECK_UUID);
        PULSEOX_UUID = loadedConfig.get(Constants.Config.PULSEOX_UUID);
        INHALER_UUID = loadedConfig.get(Constants.Config.INHALER_UUID);
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
        mIsPulseoxFound = false;
        mIsInhalerFound = false;

        rxBleClient = RxBleClient.create(this);

        Log.i("SpeckService", "Scanning..");

        scanForDevices();
    }

    private void scanForDevices() {
        scanSubscription = rxBleClient.scanBleDevices()
                .subscribe(
                        new Action1<RxBleScanResult>() {
                            @Override
                            public void call(RxBleScanResult rxBleScanResult) {
                                Log.i("SpeckService",
                                        "FOUND :" + rxBleScanResult.getBleDevice().getName() + ", " +
                                                rxBleScanResult.getBleDevice().getMacAddress());

                                if ((mIsAirspeckFound || !mIsAirspeckEnabled) &&
                                        (mIsRESpeckFound || !mIsRESpeckEnabled) &&
                                        (mIsPulseoxFound || !mIsPulseoxEnabled) &&
                                        (mIsInhalerFound || !mIsInhalerEnabled)) {
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
                                    } else {
                                        // New UUID
                                        byte[] ba = rxBleScanResult.getScanRecord();
                                        if (ba != null && ba.length == 62) {
                                            byte[] uuid = Arrays.copyOfRange(ba, 7, 15);
                                            Log.i("SpeckService", "uuid from airspeck: " + bytesToHex(uuid));
                                            Log.i("SpeckService", "uuid from config: " + AIRSPECK_UUID.substring(5));
                                            if (bytesToHex(uuid).equalsIgnoreCase(AIRSPECK_UUID.substring(5))) {
                                                mIsAirspeckFound = true;
                                                AIRSPECK_BLE_ADDRESS = rxBleScanResult.getBleDevice().getMacAddress();
                                                Log.i("SpeckService",
                                                        "Connecting after scanning to: " + AIRSPECK_BLE_ADDRESS);
                                                SpeckBluetoothService.this.connectToAirspeck();
                                            }
                                        }
                                    }
                                }
                                if (mIsRESpeckEnabled && !mIsRESpeckFound) {
                                    if (RESPECK_UUID.contains(":")) {
                                        // Old BLE address
                                        if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(
                                                RESPECK_UUID)) {
                                            RESPECK_BLE_ADDRESS = RESPECK_UUID;
                                            mIsRESpeckFound = true;
                                            Log.i("SpeckService", "Connecting after scanning");
                                            SpeckBluetoothService.this.connectToRESpeck();
                                        }
                                    } else {
                                        // New UUID
                                        byte[] ba = rxBleScanResult.getScanRecord();
                                        if (ba != null && ba.length == 62) {
                                            byte[] uuid = Arrays.copyOfRange(ba, 7, 15);
                                            Log.i("SpeckService", "uuid from respeck: " + bytesToHex(uuid));
                                            Log.i("SpeckService", "uuid from config: " + RESPECK_UUID.substring(5));
                                            if (bytesToHex(uuid).equalsIgnoreCase(RESPECK_UUID.substring(5))) {
                                                mIsRESpeckFound = true;
                                                RESPECK_BLE_ADDRESS = rxBleScanResult.getBleDevice().getMacAddress();
                                                Log.i("SpeckService",
                                                        "Connecting after scanning to: " + RESPECK_BLE_ADDRESS);
                                                SpeckBluetoothService.this.connectToRESpeck();
                                            }
                                        }
                                    }
                                }
                                if (mIsPulseoxEnabled && !mIsPulseoxFound) {
                                    if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(PULSEOX_UUID)) {
                                        PULSEOX_BLE_ADDRESS = PULSEOX_UUID; // use BLE address for UUID
                                        mIsPulseoxFound = true;
                                        Log.i("SpeckService", "Pulseox: Connecting after scanning");
                                        SpeckBluetoothService.this.connectToPulseox();
                                    }

                                }
                                if (mIsInhalerEnabled && !mIsInhalerFound) {
                                    if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(INHALER_UUID)) {
                                        INHALER_BLE_ADDRESS = INHALER_UUID; // use BLE address for UUID
                                        mIsInhalerFound = true;
                                        Log.i("SpeckService", "Inhaler: Connecting after scanning");
                                        SpeckBluetoothService.this.connectToInhaler();
                                    }

                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                // Handle an error here.
                                Log.e("SpeckService", "Error while scanning: " + throwable.toString());
                                Log.e("SpeckService", "Scanning again in 10 seconds");

                                // Try again after timeout
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        scanForDevices();
                                    }
                                }, 10000);
                            }
                        });
    }


    private void connectToAirspeck() {
        mAirspeckDevice = rxBleClient.getBleDevice(AIRSPECK_BLE_ADDRESS);
        mAirspeckName = mAirspeckDevice.getName();
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
                                FileLogger.logToFile(SpeckBluetoothService.this,
                                        "Airspeck disconnected: " + throwable.toString());

                                Intent airspeckDisconnectedIntent = new Intent(
                                        Constants.ACTION_AIRSPECK_DISCONNECTED);
                                sendBroadcast(airspeckDisconnectedIntent);

                                airspeckSubscription.unsubscribe();
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        establishAirspeckConnection();
                                    }
                                }, 10000);
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

    private void connectToPulseox() {
        mPulseoxDevice = rxBleClient.getBleDevice(PULSEOX_BLE_ADDRESS);
        mPulseoxName = mPulseoxDevice.getName();
        establishPulseoxConnection();
    }

    private void connectToInhaler() {
        mInhalerDevice = rxBleClient.getBleDevice(INHALER_BLE_ADDRESS);
        mInhalerName = mInhalerDevice.getName();
        establishInhalerConnection();
    }

    private void establishPulseoxConnection() {
        Log.i("SpeckService", "Connecting to Pulseox...");
        FileLogger.logToFile(this, "Connecting to Pulseox");

        pulseoxSubscription = mPulseoxDevice.establishConnection(true)
                .flatMap(new Func1<RxBleConnection, Observable<?>>() {
                    @Override
                    public Observable<?> call(RxBleConnection rxBleConnection) {
                        return rxBleConnection.setupNotification(UUID.fromString(
                                Constants.PULSEOX_CHARACTERISTIC));
                    }
                })
                .doOnNext(new Action1<Object>() {
                    @Override
                    public void call(Object notificationObservable) {
                        // Notification has been set up
                        Log.i("SpeckService", "Subscribed to Pulseox");
                        FileLogger.logToFile(SpeckBluetoothService.this, "Subscribed to Pulseox");
                        Intent pulseoxFoundIntent = new Intent(Constants.ACTION_PULSEOX_CONNECTED);
                        pulseoxFoundIntent.putExtra(Constants.Config.PULSEOX_UUID, PULSEOX_BLE_ADDRESS);
                        sendBroadcast(pulseoxFoundIntent);
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
                                pulseoxHandler.processPulseoxPacket((byte[]) bytes);
                                Log.i("SpeckService", "Pulseoxdata: " + ((byte[]) bytes).length);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                // An error with autoConnect means that we are disconnected
                                Log.e("SpeckService", "Pulseox disconnected: " + throwable.toString());
                                FileLogger.logToFile(SpeckBluetoothService.this, "Pulseox disconnected");

                                Intent pulseoxDisconnectedIntent = new Intent(
                                        Constants.ACTION_PULSEOX_DISCONNECTED);
                                sendBroadcast(pulseoxDisconnectedIntent);

                                pulseoxSubscription.unsubscribe();
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        establishPulseoxConnection();
                                    }
                                }, 2000);
                            }
                        }
                );
    }

    private void establishInhalerConnection() {
        Log.i("SpeckService", "Connecting to Inhaler...");
        FileLogger.logToFile(this, "Connecting to Inhaler");

        inhalerSubscription = mInhalerDevice.establishConnection(true)
                .flatMap(new Func1<RxBleConnection, Observable<?>>() {
                    @Override
                    public Observable<?> call(RxBleConnection rxBleConnection) {
                        return rxBleConnection.setupNotification(UUID.fromString(
                                Constants.INHALER_CHARACTERISTIC));
                    }
                })
                .doOnNext(new Action1<Object>() {
                    @Override
                    public void call(Object notificationObservable) {
                        // Notification has been set up
                        Log.i("SpeckService", "Subscribed to Inhaler");
                        FileLogger.logToFile(SpeckBluetoothService.this, "Subscribed to Inhaler");
                        Intent inhalerFoundIntent = new Intent(Constants.ACTION_INHALER_CONNECTED);
                        inhalerFoundIntent.putExtra(Constants.Config.INHALER_UUID, INHALER_BLE_ADDRESS);
                        sendBroadcast(inhalerFoundIntent);
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
                                inhalerHandler.processInhalerPacket((byte[]) bytes);
                                Log.i("SpeckService", "Inhalerdata: " + ((byte[]) bytes).length);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                // An error with autoConnect means that we are disconnected
                                Log.e("SpeckService", "Inhaler disconnected: " + throwable.toString());
                                FileLogger.logToFile(SpeckBluetoothService.this, "Inhaler disconnected");

                                Intent inhalerDisconnectedIntent = new Intent(
                                        Constants.ACTION_INHALER_DISCONNECTED);
                                sendBroadcast(inhalerDisconnectedIntent);

                                inhalerSubscription.unsubscribe();
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        establishInhalerConnection();
                                    }
                                }, 2000);
                            }
                        }
                );
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
                                        }, 10000);
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
                                FileLogger.logToFile(SpeckBluetoothService.this,
                                        "RESpeck connecting state: " + throwable.toString());
                            }
                        }
                );
        establishRESpeckConnection();
    }

    private void establishRESpeckConnection() {
        Log.i("SpeckService", "Connecting to RESpeck...");
        FileLogger.logToFile(this, "Connecting to RESpeck");

        final String respeck_characteristic;

        if (getRESpeckFwVersion().contains("4")) {
            respeck_characteristic = Constants.RESPECK_LIVE_V4_CHARACTERISTIC;
        } else {
            respeck_characteristic = Constants.RESPECK_LIVE_CHARACTERISTIC;
        }

        respeckLiveSubscription = mRESpeckDevice.establishConnection(false)
                .flatMap(new Func1<RxBleConnection, Observable<?>>() {
                    @Override
                    public Observable<?> call(RxBleConnection rxBleConnection) {
                        return rxBleConnection.setupNotification(
                                UUID.fromString(respeck_characteristic));
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
                                if (!mIsRESpeckPaused) {
                                    // Given characteristic has been changes, process the value
                                    respeckHandler.processRESpeckLivePacket((byte[]) characteristicValue);
                                } else {
                                    Log.i("SpeckService", "RESpeck packet ignored as paused mode on");
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.e("SpeckService", "RESpeck bluetooth error: " + throwable.toString());
                                FileLogger.logToFile(SpeckBluetoothService.this,
                                        "RESpeck data handling error: " + throwable.toString());
                            }
                        }
                );
    }

    public String getRESpeckFwVersion() {
        return mRESpeckName.substring(4);
    }

    public String getAirspeckFwVersion() {
        return mAirspeckName.substring(3);
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

        if (pulseoxSubscription != null) {
            pulseoxSubscription.unsubscribe();
        }

        if (inhalerSubscription != null) {
            inhalerSubscription.unsubscribe();
        }

        // Close the handlers
        try {
            respeckHandler.closeHandler();
            airspeckHandler.closeHandler();
            pulseoxHandler.closeHandler();
            inhalerHandler.closeHandler();
        } catch (Exception e) {
            Log.e("SpeckService", "Error while closing handlers: " + e.getMessage());
        }

        unregisterReceiver(airspeckOffSignalReceiver);
        unregisterReceiver(respeckPausedReceiver);

        // End uploading
        if (mAirspeckUploadService != null) {
            mAirspeckUploadService.stopUploading();
        }
        if (mRespeckUploadService != null) {
            mRespeckUploadService.stopUploading();
        }

        /*if (mPulseoxUploadService != null) {
        mPulseoxUploadService.stopUploading();
        }*/

    }

    @Override
    public boolean onUnbind(Intent intent) {
        FileLogger.logToFile(this, "Main Bluetooth service stopped by Android");
        return super.onUnbind(intent);
    }
}

