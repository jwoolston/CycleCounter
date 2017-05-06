package com.realenvprod.cyclecounter.counter;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.realenvprod.cyclecounter.bluetooth.BLEScanResult;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry;

import java.util.Objects;

/**
 * Encapsulation of Counter Sensor data.
 *
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */
public class Counter implements Parcelable {

    public static final byte[] ADVERTISEMENT = new byte[] {
            0x02, 0x01, 0x06, // AD Flags
            0x0E, // Length of Local Name
            0x09, 0x43, 0x79, 0x63, 0x6C, 0x65, 0x20, 0x43, 0x6F, 0x75, 0x6E, 0x74, 0x65, 0x72, // "Cycle Counter"
            0x07, 0x03, 0x0A, 0x18, 0x0F, 0x18, (byte) 0xC3, 0x35 // List of services
    };

    public final String alias;
    public final String address;
    public final long firstConnected;
    public final long lastConnected;
    public final long initialCount;
    public final long lastCount;
    public final double lastBattery;
    public final LatLng location;

    public final boolean isKnown;

    public static final Creator<Counter> CREATOR = new Creator<Counter>() {
        @Override
        public Counter createFromParcel(Parcel in) {
            return new Counter(in);
        }

        @Override
        public Counter[] newArray(int size) {
            return new Counter[size];
        }
    };

    public static boolean isAdvertisement(@NonNull final byte[] scanRecord) {
        if (scanRecord.length < ADVERTISEMENT.length) {
            return false;
        }
        for (int i = 0; i < ADVERTISEMENT.length; ++i) {
            if (scanRecord[i] != ADVERTISEMENT[i]) {
                return false;
            }
        }
        return true;
    }

    public Counter(@NonNull Cursor cursor) {
        if (cursor.getColumnCount() != 9) {
            //throw new IllegalArgumentException("Construction from cursor requires all columns.");
        }
        alias = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_ALIAS));
        address = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_ADDRESS));
        firstConnected = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_FIRST_CONNECTED));
        lastConnected = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LAST_CONNECTED));
        initialCount = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_INITIAL_COUNT));
        lastCount = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LAST_COUNT));
        lastBattery = cursor.getDouble(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LAST_BATTERY));
        location = new LatLng(cursor.getDouble(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LATITUDE)),
                              cursor.getDouble(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LONGITUDE)));
        isKnown = true;
    }

    public Counter(@NonNull BLEScanResult result) {
        alias = result.device.getName();
        address = result.device.getAddress();
        firstConnected = -1;
        lastConnected = -1;
        initialCount = -1;
        lastCount = -1;
        lastBattery = -1;
        location = null;
        isKnown = false;
    }

    protected Counter(Parcel in) {
        alias = in.readString();
        address = in.readString();
        firstConnected = in.readLong();
        lastConnected = in.readLong();
        initialCount = in.readLong();
        lastCount = in.readLong();
        lastBattery = in.readDouble();
        location = in.readParcelable(LatLng.class.getClassLoader());
        isKnown = in.readByte() != 0;
    }

    public void updateMarker(@NonNull Marker marker) {
        marker.setPosition(location);
        marker.setSnippet("Cycles: " + lastCount);
        marker.setTag(this);
    }

    @NonNull
    public Marker buildMarker(@NonNull GoogleMap map) {
        MarkerOptions options = new MarkerOptions();
        options.position(location);
        options.draggable(false);
        options.title(alias);
        options.snippet("Cycles: " + lastCount);
        final Marker marker = map.addMarker(options);
        marker.setTag(this);
        return marker;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Counter counter = (Counter) o;
        return Objects.equals(address, counter.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Counter{");
        sb.append("alias='").append(alias).append('\'');
        sb.append(", address='").append(address).append('\'');
        sb.append(", firstConnected=").append(firstConnected);
        sb.append(", lastConnected=").append(lastConnected);
        sb.append(", initialCount=").append(initialCount);
        sb.append(", lastCount=").append(lastCount);
        sb.append(", lastBattery=").append(lastBattery);
        sb.append(", location=").append(location);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(alias);
        parcel.writeString(address);
        parcel.writeLong(firstConnected);
        parcel.writeLong(lastConnected);
        parcel.writeLong(initialCount);
        parcel.writeLong(lastCount);
        parcel.writeDouble(lastBattery);
        parcel.writeParcelable(location, i);
        parcel.writeByte((byte) (isKnown ? 1 : 0));
    }
}
