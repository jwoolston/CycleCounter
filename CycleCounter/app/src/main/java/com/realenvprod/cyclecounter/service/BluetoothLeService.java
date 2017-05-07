package com.realenvprod.cyclecounter.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.realenvprod.cyclecounter.CycleApplication;
import com.realenvprod.cyclecounter.bluetooth.BLEScanResult;
import com.realenvprod.cyclecounter.counter.Counter;
import com.realenvprod.cyclecounter.counter.UnknownCounterAdapter;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;
import com.realenvprod.cyclecounter.notification.CycleSensorDiscoveredNotification;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {

    private static final String TAG = "BluetoothLeService";

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = BluetoothLeService.class.getCanonicalName() + ".ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = BluetoothLeService.class.getCanonicalName() + ".ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = BluetoothLeService.class.getCanonicalName() + ".ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = BluetoothLeService.class.getCanonicalName() + ".ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = BluetoothLeService.class.getCanonicalName() + ".EXTRA_DATA";

    public static final UUID UUID_BATTERY_LEVEL = UUID.fromString(Counter.BATTERY_LEVEL);
    public static final UUID UUID_CYCLE_COUNT = UUID.fromString(Counter.CYCLE_COUNT);

    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt    mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private boolean scanning;

    private HandlerThread handlerThread;
    private Handler       handler;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    public BluetoothLeService() {
        handlerThread = new HandlerThread("BLE Scanner");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        scanLeDevice(true);

        return START_NOT_STICKY;
    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Halting Scan.");
                    scanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    handler.postDelayed(new Runnable() {
                        @Override public void run() {
                            scanLeDevice(true);
                        }
                    }, SCAN_PERIOD);
                }
            }, SCAN_PERIOD);

            scanning = true;
            Log.d(TAG, "Scanning...");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            Log.d(TAG, "Halting Scan.");
            scanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(@NonNull final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
            && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        mBluetoothDeviceAddress = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_CYCLE_COUNT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(Counter.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        if (UUID_BATTERY_LEVEL.equals(characteristic.getUuid())) {
            final int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            Log.d(TAG, String.format("Received battery level: %d%%", batteryLevel));
            intent.putExtra(EXTRA_DATA, String.valueOf(batteryLevel));
        } else if (UUID_CYCLE_COUNT.equals(characteristic.getUuid())) {
            final int cycleCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            Log.d(TAG, String.format("Received cycle count: %d", cycleCount));
            intent.putExtra(EXTRA_DATA, String.valueOf(cycleCount));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    // Device scan callback.
    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (device == null) {
                        return;
                    }
                    if (scanning) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        scanning = false;
                    }
                    if (Counter.isAdvertisement(scanRecord)) {
                        Cursor cursor = getContentResolver().query(CounterDatabaseContract.COUNTERS_URI,
                                                                   CounterDatabaseContract.PROJECTION_ADDRESS_ONLY,
                                                                   CounterDatabaseContract.SELECTION_ADDRESS_ONLY,
                                                                   new String[]{ device.getAddress() }, null);
                        if (cursor == null || cursor.getCount() == 0) {
                            // This is an unknown counter
                            final Counter counter = new Counter(new BLEScanResult(device, rssi, scanRecord));
                            if (UnknownCounterAdapter.getInstance().addCounter(counter)) {
                                EventBus.getDefault().post(counter);
                                if (((CycleApplication) getApplication()).isMainActivityVisibile()) {
                                    Log.d(TAG, "Main activity is visible, it will show a dialog.");
                                } else {
                                    Log.d(TAG, "Main activity is not visible, showing notification.");
                                    CycleSensorDiscoveredNotification.notify(BluetoothLeService.this, counter.address, 1);
                                }
                            } else {
                                Log.d(TAG, "Ignoring counter - already seen.");
                            }
                            if (cursor != null) {
                                cursor.close();
                            }
                        } else {
                            // We already know about this counter
                            cursor.close();
                            cursor = getContentResolver().query(CounterDatabaseContract.COUNTERS_URI, null,
                                                                CounterDatabaseContract.SELECTION_ADDRESS_ONLY,
                                                                new String[]{ device.getAddress() }, null);
                            if (cursor == null || cursor.getCount() == 0) {
                                Log.e(TAG, "Counter indicated as known but cursor returned null or zero length.");
                                return;
                            }
                            cursor.moveToFirst();
                            EventBus.getDefault().post(new Counter(cursor));
                        }
                    } else {
                        Log.v(TAG, "Detected advertisement did not pass check.");
                        Log.v(TAG, "Observered: " + Arrays.toString(scanRecord));
                        Log.v(TAG, "Seeking:    " + Arrays.toString(Counter.ADVERTISEMENT));
                    }
                }
            };

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                    mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };
}
