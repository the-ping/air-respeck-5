package com.specknet.airrespeck.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
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

    private final String TAG = "SpeckService";

    // Keep track of connection state
    Timer mRespeckConnectionTimer;

    private boolean mIsRESpeckPaused;

    public SpeckBluetoothService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            startMyOwnForeground();
    }

    private void startMyOwnForeground(){
        final int SERVICE_NOTIFICATION_ID = 8598001;
        String NOTIFICATION_CHANNEL_ID = "com.specknet.airrespeck";
        String channelName = "Airrespeck Bluetooth Service";
        NotificationChannel chan = null;
        chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.vec_wireless_active)
                .setContentTitle("Airrespeck Bluetooth Service")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(SERVICE_NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        new Thread() {
            @Override
            public void run() {
                Log.i(TAG, "Starting SpeckService...");
                FileLogger.logToFile(SpeckBluetoothService.this, "Main Bluetooth service started");
                startInForeground();
                initSpeckService();
                startServiceAndBluetoothScanning();
            }
        }.start();
        return START_STICKY;
    }

    private void startInForeground() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Notification notification = new Notification.Builder(this).setContentTitle(
                    getText(R.string.notification_speck_title)).setContentText(
                    getText(R.string.notification_speck_text)).setSmallIcon(
                    R.drawable.vec_wireless_active).setContentIntent(pendingIntent).build();

            // Just use a "random" service ID
            final int SERVICE_NOTIFICATION_ID = 8598001;
            startForeground(SERVICE_NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        stopSpeckService();
        Log.i(TAG, "SpeckService has been stopped");
        super.onDestroy();
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
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
                Log.i(TAG, "Got turn off message");
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
                    Log.i(TAG, "Received message to pause RESpeck recording");
                    mIsRESpeckPaused = true;
                } else if (intent.getAction().equals(Constants.ACTION_RESPECK_RECORDING_CONTINUE)) {
                    Log.i(TAG, "Received message to continue RESpeck recording");
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
            Toast.makeText(getApplicationContext(), "This device does not support Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }

        mIsAirspeckFound = false;
        mIsRESpeckFound = false;
        mIsPulseoxFound = false;
        mIsInhalerFound = false;

        rxBleClient = RxBleClient.create(this);

        Log.i(TAG, "Scanning..");

        scanForDevices();
    }

    private void scanForDevices() {
        scanSubscription = rxBleClient.scanBleDevices().subscribe(rxBleScanResult -> {
            Log.i(TAG,
                    "FOUND :" + rxBleScanResult.getBleDevice().getName() + ", " + rxBleScanResult.getBleDevice().getMacAddress());

            if ((mIsAirspeckFound || !mIsAirspeckEnabled) && (mIsRESpeckFound || !mIsRESpeckEnabled) && (mIsPulseoxFound || !mIsPulseoxEnabled) && (mIsInhalerFound || !mIsInhalerEnabled)) {
                scanSubscription.unsubscribe();
            }

            if (mIsAirspeckEnabled && !mIsAirspeckFound) {
                // Process scan result here.
                if (AIRSPECK_UUID.contains(":")) {
                    // Old BLE address
                    if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(AIRSPECK_UUID)) {
                        AIRSPECK_BLE_ADDRESS = AIRSPECK_UUID;
                        mIsAirspeckFound = true;
                        SpeckBluetoothService.this.connectToAirspeck();
                    }
                } else {
                    // New UUID
                    byte[] ba = rxBleScanResult.getScanRecord();
                    if (ba != null && ba.length == 62) {
                        byte[] uuid = Arrays.copyOfRange(ba, 7, 15);
                        Log.i(TAG, "uuid from airspeck: " + bytesToHex(uuid));
                        Log.i(TAG, "uuid from config: " + AIRSPECK_UUID.substring(5));
                        if (bytesToHex(uuid).equalsIgnoreCase(AIRSPECK_UUID.substring(5))) {
                            mIsAirspeckFound = true;
                            AIRSPECK_BLE_ADDRESS = rxBleScanResult.getBleDevice().getMacAddress();
                            Log.i(TAG, "Connecting after scanning to: " + AIRSPECK_BLE_ADDRESS);
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
                        Log.i(TAG, "Connecting after scanning");
                        SpeckBluetoothService.this.connectToRESpeck();
                    }
                } else {
                    // New UUID
                    byte[] ba = rxBleScanResult.getScanRecord();
                    if (ba != null && ba.length == 62) {
                        byte[] uuid = Arrays.copyOfRange(ba, 7, 15);
                        byte[] uuid4 = Arrays.copyOfRange(ba, 15, 23);
                        Log.i(TAG, "uuid from respeck: " + bytesToHex(uuid));
                        Log.i(TAG, "uuid from respeck4: " + bytesToHex(uuid4));
                        Log.i(TAG, "uuid from config: " + RESPECK_UUID.substring(5));
                        if (bytesToHex(uuid).equalsIgnoreCase(RESPECK_UUID.substring(5)) || bytesToHex(
                                uuid4).equalsIgnoreCase(RESPECK_UUID.substring(5))) {
                            mIsRESpeckFound = true;
                            RESPECK_BLE_ADDRESS = rxBleScanResult.getBleDevice().getMacAddress();
                            Log.i(TAG, "Connecting after scanning to: " + RESPECK_BLE_ADDRESS);
                            SpeckBluetoothService.this.connectToRESpeck();
                        }
                    }
                }
            }
            if (mIsPulseoxEnabled && !mIsPulseoxFound) {
                if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(PULSEOX_UUID)) {
                    PULSEOX_BLE_ADDRESS = PULSEOX_UUID; // use BLE address for UUID
                    mIsPulseoxFound = true;
                    Log.i(TAG, "Pulseox: Connecting after scanning");
                    SpeckBluetoothService.this.connectToPulseox();
                }

            }
            if (mIsInhalerEnabled && !mIsInhalerFound) {
                if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(INHALER_UUID)) {
                    INHALER_BLE_ADDRESS = INHALER_UUID; // use BLE address for UUID
                    mIsInhalerFound = true;
                    Log.i(TAG, "Inhaler: Connecting after scanning");
                    SpeckBluetoothService.this.connectToInhaler();
                }

            }
        }, throwable -> {
            // Handle an error here.
            Log.e(TAG, "Error while scanning: " + throwable.toString());
            Log.e(TAG, "Scanning again in 10 seconds");

            // Try again after timeout
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    scanForDevices();
                }
            }, 10000);
        });
    }


    private void connectToAirspeck() {
        mAirspeckDevice = rxBleClient.getBleDevice(AIRSPECK_BLE_ADDRESS);
        mAirspeckName = mAirspeckDevice.getName();
        establishAirspeckConnection();
    }

    private void establishAirspeckConnection() {
        Log.i(TAG, "Connecting to Airspeck...");
        FileLogger.logToFile(this, "Connecting to Airspeck");

        airspeckSubscription = mAirspeckDevice.establishConnection(true).flatMap(
                (Func1<RxBleConnection, Observable<?>>) rxBleConnection -> rxBleConnection.setupNotification(
                        UUID.fromString(Constants.AIRSPECK_LIVE_CHARACTERISTIC))).doOnNext(notificationObservable -> {
            // Notification has been set up
            Log.i(TAG, "Subscribed to Airspeck");
            FileLogger.logToFile(SpeckBluetoothService.this, "Subscribed to Airspeck");
            Intent airspeckFoundIntent = new Intent(Constants.ACTION_AIRSPECK_CONNECTED);
            airspeckFoundIntent.putExtra(Constants.Config.AIRSPECKP_UUID, AIRSPECK_UUID);
            sendBroadcast(airspeckFoundIntent);
        }).flatMap(
                (Func1<Object, Observable<?>>) notificationObservable -> (Observable) notificationObservable) // <-- Notification has been set up, now observe value changes.
                .subscribe(bytes -> {
                    airspeckHandler.processAirspeckPacket((byte[]) bytes);
                    //Log.i(TAG, "turnOff: " + turn_off);
                }, throwable -> {
                    // An error with autoConnect means that we are disconnected
                    String stackTrace = FileLogger.getStackTraceAsString(throwable);
                    Log.e(TAG, "Airspeck disconnected: " + stackTrace);
                    FileLogger.logToFile(SpeckBluetoothService.this, "Airspeck disconnected: " + stackTrace);

                    Intent airspeckDisconnectedIntent = new Intent(Constants.ACTION_AIRSPECK_DISCONNECTED);
                    sendBroadcast(airspeckDisconnectedIntent);

                    airspeckSubscription.unsubscribe();
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            establishAirspeckConnection();
                        }
                    }, 10000);
                });
    }

    public void turnOffAirspeck() {
        Log.e(TAG, "Turning off");
        FileLogger.logToFile(this, "Turning off Airspeck after power off command");
        mAirspeckDevice.establishConnection(true).flatMap(
                (Func1<RxBleConnection, Observable<?>>) rxBleConnection -> rxBleConnection.writeCharacteristic(
                        UUID.fromString(Constants.AIRSPECK_POWER_OFF_CHARACTERISTIC), Constants.OFF_COMMAND)).subscribe(
                bytes -> Log.e(TAG, "Turning off: " + bytes.toString()), throwable -> {

                    // An error with autoConnect means that we are disconnected
                    Log.e(TAG, "Airspeck turned off: " + throwable.toString());
                    FileLogger.logToFile(SpeckBluetoothService.this, "Airspeck turned off");
                    Intent airspeckDisconnectedIntent = new Intent(Constants.ACTION_AIRSPECK_DISCONNECTED);
                    sendBroadcast(airspeckDisconnectedIntent);

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            establishAirspeckConnection();
                        }
                    }, 2000);
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
        Log.i(TAG, "Connecting to Pulseox...");
        FileLogger.logToFile(this, "Connecting to Pulseox");

        pulseoxSubscription = mPulseoxDevice.establishConnection(true).flatMap(
                (Func1<RxBleConnection, Observable<?>>) rxBleConnection -> rxBleConnection.setupNotification(
                        UUID.fromString(Constants.PULSEOX_CHARACTERISTIC))).doOnNext(notificationObservable -> {
            // Notification has been set up
            Log.i(TAG, "Subscribed to Pulseox");
            FileLogger.logToFile(SpeckBluetoothService.this, "Subscribed to Pulseox");
            Intent pulseoxFoundIntent = new Intent(Constants.ACTION_PULSEOX_CONNECTED);
            pulseoxFoundIntent.putExtra(Constants.Config.PULSEOX_UUID, PULSEOX_BLE_ADDRESS);
            sendBroadcast(pulseoxFoundIntent);
        }).flatMap(
                (Func1<Object, Observable<?>>) notificationObservable -> (Observable) notificationObservable) // <-- Notification has been set up, now observe value changes.
                .subscribe(bytes -> {
                    pulseoxHandler.processPulseoxPacket((byte[]) bytes);
                    Log.i(TAG, "Pulseoxdata: " + ((byte[]) bytes).length);
                }, throwable -> {
                    // An error with autoConnect means that we are disconnected
                    Log.e(TAG, "Pulseox disconnected: " + throwable.toString());
                    FileLogger.logToFile(SpeckBluetoothService.this, "Pulseox disconnected");

                    Intent pulseoxDisconnectedIntent = new Intent(Constants.ACTION_PULSEOX_DISCONNECTED);
                    sendBroadcast(pulseoxDisconnectedIntent);

                    pulseoxSubscription.unsubscribe();
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            establishPulseoxConnection();
                        }
                    }, 2000);
                });
    }

    private void establishInhalerConnection() {
        Log.i(TAG, "Connecting to Inhaler...");
        FileLogger.logToFile(this, "Connecting to Inhaler");

        inhalerSubscription = mInhalerDevice.establishConnection(true).flatMap(
                (Func1<RxBleConnection, Observable<?>>) rxBleConnection -> rxBleConnection.setupNotification(
                        UUID.fromString(Constants.INHALER_CHARACTERISTIC))).doOnNext(notificationObservable -> {
            // Notification has been set up
            Log.i(TAG, "Subscribed to Inhaler");
            FileLogger.logToFile(SpeckBluetoothService.this, "Subscribed to Inhaler");
            Intent inhalerFoundIntent = new Intent(Constants.ACTION_INHALER_CONNECTED);
            inhalerFoundIntent.putExtra(Constants.Config.INHALER_UUID, INHALER_BLE_ADDRESS);
            sendBroadcast(inhalerFoundIntent);
        }).flatMap(
                (Func1<Object, Observable<?>>) notificationObservable -> (Observable) notificationObservable) // <-- Notification has been set up, now observe value changes.
                .subscribe(bytes -> {
                    inhalerHandler.processInhalerPacket((byte[]) bytes);
                    Log.i(TAG, "Inhalerdata: " + ((byte[]) bytes).length);
                }, throwable -> {
                    // An error with autoConnect means that we are disconnected
                    Log.e(TAG, "Inhaler disconnected: " + throwable.toString());
                    FileLogger.logToFile(SpeckBluetoothService.this, "Inhaler disconnected");

                    Intent inhalerDisconnectedIntent = new Intent(Constants.ACTION_INHALER_DISCONNECTED);
                    sendBroadcast(inhalerDisconnectedIntent);

                    inhalerSubscription.unsubscribe();
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            establishInhalerConnection();
                        }
                    }, 2000);
                });
    }

    private void connectToRESpeck() {
        mRESpeckDevice = rxBleClient.getBleDevice(RESPECK_BLE_ADDRESS);
        mRESpeckName = mRESpeckDevice.getName();
        mRESpeckDevice.observeConnectionStateChanges().subscribe(connectionState -> {
            if (connectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED && mIsServiceRunning) {
                FileLogger.logToFile(SpeckBluetoothService.this, "RESpeck disconnected");
                Intent respeckDisconnectedIntent = new Intent(Constants.ACTION_RESPECK_DISCONNECTED);
                sendBroadcast(respeckDisconnectedIntent);

                if (mLastRESpeckConnectionState == RxBleConnection.RxBleConnectionState.CONNECTED) {
                    // If we were just disconnected, try to immediately connect again.
                    Log.i(TAG, "RESpeck connection lost, trying to reconnect");
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            establishRESpeckConnection();
                        }
                    }, 10000);
                } else if (mLastRESpeckConnectionState == RxBleConnection.RxBleConnectionState.CONNECTING) {
                    // This means we tried to reconnect, but there was a timeout. In this case we
                    // wait for x seconds before reconnecting
                    Log.i(TAG, String.format("RESpeck connection timeout, waiting %d seconds before reconnect",
                            Constants.RECONNECTION_TIMEOUT_MILLIS / 1000));
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Log.i(TAG, "RESpeck reconnecting...");
                            establishRESpeckConnection();
                        }
                    }, Constants.RECONNECTION_TIMEOUT_MILLIS);
                }
            }
            mLastRESpeckConnectionState = connectionState;
        }, throwable -> {
            Log.e(TAG, "Error occured while listening to RESpeck connection state changes: " + throwable.getMessage());
            FileLogger.logToFile(SpeckBluetoothService.this, "RESpeck connecting state: " + throwable.toString());
        });
        establishRESpeckConnection();
    }

    /*
    private void startRespeckScanTimeoutTask() {
        TimerTask reconnectionTask = new TimerTask() {
            @Override
            public void run() {
                Log.i(TAG, "Restarting Respeck scanning after 5 minutes of lost connection.");
                reconnectRespeck();
            }
        };
        mRespeckConnectionTimer = new Timer();
        mRespeckConnectionTimer.schedule(reconnectionTask, 1000*60*2);
    }

    private void stopRespeckScanTimeoutTask() {
        mRespeckConnectionTimer.cancel();
    }


    private void reconnectRespeck() {
        respeckLiveSubscription.unsubscribe();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                establishRESpeckConnection();
            }
        }, 2000);
    }
    */

    private void establishRESpeckConnection() {
        Log.i(TAG, "Connecting to RESpeck...");
        FileLogger.logToFile(this, "Connecting to RESpeck");

        final String respeck_characteristic;

        if (getRESpeckFwVersion().contains("4") || getRESpeckFwVersion().contains("5") || getRESpeckFwVersion().contains("6")) {
            respeck_characteristic = Constants.RESPECK_LIVE_V4_CHARACTERISTIC;
        } else {
            respeck_characteristic = Constants.RESPECK_LIVE_CHARACTERISTIC;
        }

        respeckLiveSubscription = mRESpeckDevice.establishConnection(false).flatMap(
                (Func1<RxBleConnection, Observable<?>>) rxBleConnection -> rxBleConnection.setupNotification(
                        UUID.fromString(respeck_characteristic))).doOnNext(notificationObservable -> {
            // Notification has been set up
            Log.i(TAG, "Subscribed to RESpeck");
            FileLogger.logToFile(SpeckBluetoothService.this, "Subscribed to RESpeck");
            Intent respeckFoundIntent = new Intent(Constants.ACTION_RESPECK_CONNECTED);
            respeckFoundIntent.putExtra(Constants.Config.RESPECK_UUID, RESPECK_UUID);
            sendBroadcast(respeckFoundIntent);
        }).flatMap(
                (Func1<Object, Observable<?>>) notificationObservable -> (Observable) notificationObservable).subscribe(
                characteristicValue -> {
                    if (!mIsRESpeckPaused) {
                        // Given characteristic has been changes, process the value
                        respeckHandler.processRESpeckLivePacket((byte[]) characteristicValue);
                    } else {
                        Log.i(TAG, "RESpeck packet ignored as paused mode on");
                    }
                }, throwable -> {
                    // An error with autoConnect means that we are disconnected
                    String stackTrace = FileLogger.getStackTraceAsString(throwable);
                    Log.e(TAG, "Respeck disconnected: " + stackTrace);
                    FileLogger.logToFile(SpeckBluetoothService.this, "Respeck disconnected: " + stackTrace);

                    Intent respeckDisconnectedIntent = new Intent(Constants.ACTION_RESPECK_DISCONNECTED);
                    sendBroadcast(respeckDisconnectedIntent);
                });
    }

    public String getRESpeckFwVersion() {
        if (mRESpeckName.charAt(3) == '6') {
            return mRESpeckName.substring(3);
        }
        else {
            return mRESpeckName.substring(4);
        }
    }

    public String getAirspeckFwVersion() {
        return mAirspeckName.substring(2);
    }


    public void stopSpeckService() {
        Log.i(TAG, "Stopping Speck Service");
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
            Log.e(TAG, "Error while closing handlers: " + e.getMessage());
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

