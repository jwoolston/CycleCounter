package com.realenvprod.cyclecounter.service;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import com.realenvprod.cyclecounter.CycleApplication;
import com.realenvprod.cyclecounter.R;
import com.realenvprod.cyclecounter.bluetooth.BLEScanResult;
import com.realenvprod.cyclecounter.counter.Counter;
import com.realenvprod.cyclecounter.counter.UnknownCounterAdapter;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;
import com.realenvprod.cyclecounter.notification.CycleSensorDiscoveredNotification;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import org.greenrobot.eventbus.EventBus;

public class BluetoothLeService extends Service {

    private static final String TAG = "BluetoothLeService";

    private final static int NOTIFICATION_ID = 1;

    public final static String EXTRA_DATA_UUID = BluetoothLeService.class.getCanonicalName()
                                                 + ".EXTRA_DATA_UUID";

    private BluetoothAdapter bluetoothAdapter;
    private String           bluetoothDeviceAddress;
    private BluetoothGatt    bluetoothGatt;
    private boolean          scanning;

    private HandlerThread handlerThread;
    private Handler       handler;

    private final IBinder localBinder = new LocalBinder();

    private final Queue<BLETask> bleTasks = new LinkedList<>();

    private final Object BLETaskLock = new Object();

    private static Notification getNotification(@NonNull Context context) {
        final Notification.Builder builder = new Builder(context)
                .setContentTitle("Cycle counter scan active")
                .setSmallIcon(R.drawable.ic_bluetooth_searching_black_24dp);
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            final String channelId = "com.realenvprod.cyclecounter.notification";
            final String channelName = "Cycle counter scan notification";
            NotificationChannel channel = new NotificationChannel(channelId, channelName,
                                                                  NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }
        return builder.build();
    }

    private class BLETask {

        private final Runnable runnable;

        BLETask(@NonNull Runnable runnable) {
            this.runnable = runnable;
        }

        void execute() {
            synchronized (BLETaskLock) {
                runnable.run();
                try {
                    BLETaskLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(serviceTaskQueue);
            }
        }
    }

    private final Runnable serviceTaskQueue = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Servicing BLE task queue.");
            final BLETask task = bleTasks.poll();
            if (task != null) {
                task.execute();
            } else {
                handler.postDelayed(serviceTaskQueue, 1000);
            }
        }
    };

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
        return localBinder;
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
        bluetoothAdapter = bluetoothManager.getAdapter();

        scanLeDevice(true);

        return START_NOT_STICKY;
    }

    public void scanLeDevice(final boolean enable) {
        if (enable && !scanning) {
            startForeground(NOTIFICATION_ID, getNotification(getApplicationContext()));
            scanning = true;
            Log.d(TAG, "Scanning...");
            ScanSettings.Builder builder = new ScanSettings.Builder();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
                builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
                builder.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT);
            }
            builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
            Log.d(TAG, "Scanner: " + scanner);
            scanner.startScan(null, builder.build(), leScanCallback);
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
        bluetoothDeviceAddress = null;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) {
            return null;
        }

        return bluetoothGatt.getServices();
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final UUID uuid = characteristic.getUuid();
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA_UUID, uuid.toString());
        sendBroadcast(intent);
    }

    // Device scan callback.
    private final ScanCallback leScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final BluetoothDevice device = result.getDevice();
            if (device == null) {
                return;
            }
            final byte[] scanRecord = result.getScanRecord().getBytes();
            if (Counter.isAdvertisement(scanRecord)) {
                Cursor cursor = getContentResolver().query(CounterDatabaseContract.COUNTERS_URI,
                                                           CounterDatabaseContract.PROJECTION_ADDRESS_ONLY,
                                                           CounterDatabaseContract.SELECTION_ADDRESS_ONLY,
                                                           new String[]{ device.getAddress() }, null);
                if (cursor == null || cursor.getCount() == 0) {
                    // This is an unknown counter
                    final Counter counter = new Counter(new BLEScanResult(device, result.getRssi(), scanRecord));
                    if (UnknownCounterAdapter.getInstance().addCounter(counter)) {
                        EventBus.getDefault().post(counter);
                        if (((CycleApplication) getApplication()).isMainActivityVisibile()) {
                            Log.d(TAG, "Main activity is visible, it will show a dialog.");
                        } else {
                            Log.d(TAG, "Main activity is not visible, showing notification.");
                            CycleSensorDiscoveredNotification
                                    .notify(BluetoothLeService.this, counter.getAddress(), 1);
                        }
                    } else {
                        Log.d(TAG, "Ignoring counter - already seen.");
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                } else {
                    // We already know about this counter
                    Log.d(TAG, "Discovered known counter.");
                    cursor.close();
                    cursor = getContentResolver().query(CounterDatabaseContract.COUNTERS_URI, null,
                                                        CounterDatabaseContract.SELECTION_ADDRESS_ONLY,
                                                        new String[]{ device.getAddress() }, null);
                    if (cursor == null || cursor.getCount() == 0) {
                        Log.e(TAG, "Counter indicated as known but cursor returned null or zero length.");
                        return;
                    }
                    cursor.moveToFirst();
                    final Counter counter = new Counter(cursor, scanRecord);
                    // Update counter record and readings record
                    counter.updateDatabase(getContentResolver());
                    EventBus.getDefault().post(counter);
                }
            }
        }
    };
}
