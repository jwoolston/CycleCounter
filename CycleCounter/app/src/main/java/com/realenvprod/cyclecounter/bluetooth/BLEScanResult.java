package com.realenvprod.cyclecounter.bluetooth;

import android.bluetooth.BluetoothDevice;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */

public class BLEScanResult {

    public final BluetoothDevice device;
    public final int rssi;
    public final byte[] scanRecord;

    public BLEScanResult(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        this.device = device;
        this.rssi = rssi;
        this.scanRecord = scanRecord;
    }

    @Override public String toString() {
        final StringBuffer sb = new StringBuffer("BLEScanResult{");
        sb.append("device=").append(device);
        sb.append(", rssi=").append(rssi);
        sb.append(", scanRecord=");
        if (scanRecord == null) {
            sb.append("null");
        } else {
            sb.append('[');
            for (int i = 0; i < scanRecord.length; ++i) {
                sb.append(i == 0 ? "" : ", ").append(scanRecord[i]);
            }
            sb.append(']');
        }
        sb.append('}');
        return sb.toString();
    }
}
