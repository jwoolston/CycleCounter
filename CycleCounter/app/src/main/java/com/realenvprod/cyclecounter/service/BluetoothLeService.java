package com.realenvprod.cyclecounter.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import com.realenvprod.cyclecounter.CycleApplication;
import com.realenvprod.cyclecounter.bluetooth.BLEScanResult;
import com.realenvprod.cyclecounter.counter.Counter;
import com.realenvprod.cyclecounter.counter.UnknownCounterAdapter;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;
import com.realenvprod.cyclecounter.notification.CycleSensorDiscoveredNotification;
import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;

public class BluetoothLeService extends Service {

    private static final String TAG = "BluetoothLeService";

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt    mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private boolean scanning;

    private HandlerThread handlerThread;
    private Handler       handler;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
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
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
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
}
