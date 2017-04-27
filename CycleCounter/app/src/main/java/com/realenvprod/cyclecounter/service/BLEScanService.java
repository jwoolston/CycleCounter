package com.realenvprod.cyclecounter.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import com.realenvprod.cyclecounter.CycleApplication;
import com.realenvprod.cyclecounter.bluetooth.BLEScanResult;
import com.realenvprod.cyclecounter.notification.CycleSensorDiscoveredNotification;
import org.greenrobot.eventbus.EventBus;

public class BLEScanService extends Service {

    private static final String TAG = "BLEScanService";

    private static final int  REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD       = 10000;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean          mScanning;

    private HandlerThread mHandlerThread;
    private Handler       mHandler;

    public BLEScanService() {
        mHandlerThread = new HandlerThread("BLE Scanner");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
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

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Halting Scan.");
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mHandler.postDelayed(new Runnable() {
                        @Override public void run() {
                            scanLeDevice(true);
                        }
                    }, SCAN_PERIOD);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            Log.d(TAG, "Scanning...");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            Log.d(TAG, "Halting Scan.");
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (device == null) {
                        return;
                    }
                    if (mScanning) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        mScanning = false;
                    }
                    EventBus.getDefault().post(new BLEScanResult(device, rssi, scanRecord));
                    if (((CycleApplication) getApplication()).isMainActivityVisibile()) {
                        Log.d(TAG, "Main activity is visible, it will show a dialog.");
                    } else {
                        Log.d(TAG, "Main activity is not visible, showing notification.");
                        CycleSensorDiscoveredNotification.notify(BLEScanService.this, device.getAddress(), 1);
                    }
                }
            };
}
