package com.realenvprod.cyclecounter.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.realenvprod.cyclecounter.counter.Counter;
import com.realenvprod.cyclecounter.service.BluetoothLeService;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */

public class GattUpdateReceiver extends BroadcastReceiver {

    public interface OnGattUpdate {

        void onUpdateReceived(@NonNull String action, @Nullable String data);

        void onCycleCountUpdate(@IntRange(from = 0) long cycleCount);

        void onBatteryUpdate(@IntRange(from = 0, to = 100) int batteryLevel);

        void onModelNumber(@NonNull String model);

        void onHardwareRevisionString(@NonNull String revision);

        void onSoftwareRevisionString(@NonNull String revision);
    }

    private final OnGattUpdate updateListener;

    public GattUpdateReceiver(@NonNull OnGattUpdate updateListener) {
        this.updateListener = updateListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            updateListener.onUpdateReceived(action, null);
        } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
            updateListener.onUpdateReceived(action, null);
        } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            // Show all the supported services and characteristics on the user interface.
            updateListener.onUpdateReceived(action, null);
        } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            final String uuid = intent.getStringExtra(BluetoothLeService.EXTRA_DATA_UUID);
            switch (uuid) {
                case Counter.MODEL_NUMBER_STRING:
                    updateListener.onModelNumber(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                    break;
                case Counter.HARDWARE_REVISION_STRING:
                    updateListener.onHardwareRevisionString(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                    break;
                case Counter.SOFTWARE_REVISION_STRING:
                    updateListener.onSoftwareRevisionString(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                    break;
                case Counter.CYCLE_COUNT:
                    updateListener.onCycleCountUpdate(intent.getLongExtra(BluetoothLeService.EXTRA_DATA, -1) / 2);
                    break;
                case Counter.BATTERY_LEVEL:
                    updateListener.onBatteryUpdate(intent.getIntExtra(BluetoothLeService.EXTRA_DATA, -1));
                    break;
            }
        }
    }
}
