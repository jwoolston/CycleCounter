package com.realenvprod.cyclecounter.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.realenvprod.cyclecounter.service.BluetoothLeService;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */

public class GattUpdateReceiver extends BroadcastReceiver {

    public interface OnGattUpdate {

        void onUpdateReceived(@NonNull String action, @Nullable String data);
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
            updateListener.onUpdateReceived(action, intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
        }
    }
}
