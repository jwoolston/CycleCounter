package com.realenvprod.cyclecounter.counter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.realenvprod.cyclecounter.bluetooth.BLEScanResult;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Encapsulation of Counter Sensor data.
 *
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */
public class Counter implements Parcelable {

    private static final String TAG = "Counter";

    public static final byte[] ADVERTISEMENT = new byte[]{
            0x02, 0x01, 0x06, // AD Flags
            0x02, 0x0A, 0x03, // Power Level
            0x06, 0x16, 0x0A, 0x018, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, // Device Information
            0x04, 0x16, 0x0F, 0x18, (byte) 0xFF, // Battery Level
            0x07, 0x16, (byte) 0xC3, 0x35, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF // Cycle Count
    };

    public static final int POWER_LEVEL_DATA_INDEX       = 5;
    public static final int MODEL_NUMBER_DATA_INDEX      = 10;
    public static final int HARDWARE_REVISION_DATA_INDEX = 11;
    public static final int SOFTWARE_REVISION_DATA_INDEX = 12;
    public static final int BATTERY_LEVEL_DATA_INDEX     = 17;
    public static final int CYCLE_COUNT_DATA_INDEX       = 22;

    private String  alias;
    private String  address;
    private long    firstSeen;
    private long    lastSeen;
    private long    initialCount;
    private long    lastCount;
    private double  lastBattery;
    private LatLng  location;
    private String  model;
    private String  hardwareRevision;
    private String  softwareRevision;
    private boolean isKnown;

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
            switch (i) {
                case POWER_LEVEL_DATA_INDEX:
                case MODEL_NUMBER_DATA_INDEX:
                case HARDWARE_REVISION_DATA_INDEX:
                case SOFTWARE_REVISION_DATA_INDEX:
                case BATTERY_LEVEL_DATA_INDEX:
                    continue;
                case CYCLE_COUNT_DATA_INDEX:
                    return true;
                default:
                    if (scanRecord[i] != ADVERTISEMENT[i]) {
                        return false;
                    }
            }
        }
        return true;
    }

    @IntRange(from = 0)
    private static long cycleCountFromAdvertisement(@NonNull byte[] scanRecord) {
        return (((0xFF & scanRecord[CYCLE_COUNT_DATA_INDEX]) << 24)
                | ((0xFF & scanRecord[CYCLE_COUNT_DATA_INDEX + 1]) << 16)
                | ((0xFF & scanRecord[CYCLE_COUNT_DATA_INDEX + 2]) << 8)
                | (0xFF & scanRecord[CYCLE_COUNT_DATA_INDEX + 3]));
    }

    @IntRange(from = 0, to = 100)
    private static int batteryLevelFromAdvertisement(@NonNull byte[] scanRecord) {
        int battery = scanRecord[BATTERY_LEVEL_DATA_INDEX];
        if (battery < 0) {
            // Clamp minimum
            battery = 0;
        } else if (battery > 100) {
            // Clamp maximum
            battery = 100;
        }
        return battery;
    }

    @NonNull
    private static String modelNumberFromAdvertisemet(@NonNull byte[] scanRecord) {
        return ModelNumber.getModelNumberString(scanRecord[MODEL_NUMBER_DATA_INDEX]);
    }

    @NonNull
    private static String hardwareRevisionFromAdvertisement(@NonNull byte[] scanRecord) {
        return HardwareRevision.getHardwareRevisionString(scanRecord[HARDWARE_REVISION_DATA_INDEX]);
    }

    @NonNull
    private static String softwareRevisionFromAdvertisement(@NonNull byte[] scanRecord) {
        return SoftwareRevision.getSoftwareRevisionString(scanRecord[SOFTWARE_REVISION_DATA_INDEX]);
    }

    public Counter(@NonNull Cursor cursor) {
        if (cursor.getColumnCount() != 9) {
            //throw new IllegalArgumentException("Construction from cursor requires all columns.");
        }
        alias = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_ALIAS));
        address = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_ADDRESS));
        firstSeen = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_FIRST_CONNECTED));
        lastSeen = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LAST_CONNECTED));
        initialCount = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_INITIAL_COUNT));
        lastCount = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LAST_COUNT));
        lastBattery = cursor.getDouble(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LAST_BATTERY));
        location = new LatLng(cursor.getDouble(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LATITUDE)),
                              cursor.getDouble(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LONGITUDE)));
        model = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_MODEL));
        hardwareRevision = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_HARDWARE_REVISION));
        softwareRevision = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_SOFTWARE_REVISION));
        isKnown = true;
    }

    public Counter(@NonNull BLEScanResult result) {
        alias = modelNumberFromAdvertisemet(result.scanRecord);
        address = result.device.getAddress();
        firstSeen = System.currentTimeMillis();
        lastSeen = firstSeen;
        initialCount = cycleCountFromAdvertisement(result.scanRecord);
        lastCount = initialCount;
        lastBattery = batteryLevelFromAdvertisement(result.scanRecord);
        location = null; //TODO: Use current location
        model = modelNumberFromAdvertisemet(result.scanRecord);
        hardwareRevision = hardwareRevisionFromAdvertisement(result.scanRecord);
        softwareRevision = softwareRevisionFromAdvertisement(result.scanRecord);
        isKnown = false;
    }

    public Counter(@NonNull Cursor cursor, @NonNull byte[] scanRecord) {
        alias = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_ALIAS));
        address = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_ADDRESS));
        firstSeen = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_FIRST_CONNECTED));
        lastSeen = System.currentTimeMillis();
        initialCount = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_INITIAL_COUNT));
        lastCount = cycleCountFromAdvertisement(scanRecord);
        lastBattery = batteryLevelFromAdvertisement(scanRecord);
        location = new LatLng(cursor.getDouble(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LATITUDE)),
                              cursor.getDouble(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LONGITUDE)));
        model = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_MODEL));
        hardwareRevision = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_HARDWARE_REVISION));
        softwareRevision = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_SOFTWARE_REVISION));
        isKnown = true;
    }

    protected Counter(Parcel in) {
        alias = in.readString();
        address = in.readString();
        firstSeen = in.readLong();
        lastSeen = in.readLong();
        initialCount = in.readLong();
        lastCount = in.readLong();
        lastBattery = in.readDouble();
        location = in.readParcelable(LatLng.class.getClassLoader());
        model = in.readString();
        hardwareRevision = in.readString();
        softwareRevision = in.readString();
        isKnown = in.readByte() != 0;
    }

    @NonNull
    public String getAlias() {
        return alias;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    @IntRange(from = 0)
    public long getFirstSeen() {
        return firstSeen;
    }

    @IntRange(from = 0)
    public long getLastSeen() {
        return lastSeen;
    }

    @IntRange(from = 0)
    public long getInitialCount() {
        return initialCount;
    }

    @IntRange(from = 0)
    public long getLastCount() {
        return lastCount;
    }

    @FloatRange(from = 0, to = 100)
    public double getLastBattery() {
        return lastBattery;
    }

    public LatLng getLocation() {
        return location;
    }

    @NonNull
    public String getModel() {
        return model;
    }

    @NonNull
    public String getHardwareRevision() {
        return hardwareRevision;
    }

    @NonNull
    public String getSoftwareRevision() {
        return softwareRevision;
    }

    public boolean isKnown() {
        return isKnown;
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
        return lastSeen == counter.lastSeen &&
               lastCount == counter.lastCount &&
               Double.compare(counter.lastBattery, lastBattery) == 0 &&
               Objects.equals(address, counter.address) &&
               Objects.equals(model, counter.model) &&
               Objects.equals(hardwareRevision, counter.hardwareRevision) &&
               Objects.equals(softwareRevision, counter.softwareRevision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, lastSeen, lastCount, lastBattery, model, hardwareRevision, softwareRevision);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Counter{");
        sb.append("alias='").append(alias).append('\'');
        sb.append(", address='").append(address).append('\'');
        sb.append(", firstSeen=").append(firstSeen);
        sb.append(", lastSeen=").append(lastSeen);
        sb.append(", initialCount=").append(initialCount);
        sb.append(", lastCount=").append(lastCount);
        sb.append(", lastBattery=").append(lastBattery);
        sb.append(", location=").append(location);
        sb.append(", model='").append(model).append('\'');
        sb.append(", hardwareRevision='").append(hardwareRevision).append('\'');
        sb.append(", softwareRevision='").append(softwareRevision).append('\'');
        sb.append(", isKnown=").append(isKnown);
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
        parcel.writeLong(firstSeen);
        parcel.writeLong(lastSeen);
        parcel.writeLong(initialCount);
        parcel.writeLong(lastCount);
        parcel.writeDouble(lastBattery);
        parcel.writeParcelable(location, i);
        parcel.writeString(model);
        parcel.writeString(hardwareRevision);
        parcel.writeString(softwareRevision);
        parcel.writeByte((byte) (isKnown ? 1 : 0));
    }

    public void updateDatabase(@NonNull ContentResolver contentResolver) {
        Log.d(TAG, "Updating database for reading of known counter: " + this);
        final ContentValues counterValues = new ContentValues();
        counterValues.put(CounterEntry.COLUMN_NAME_ALIAS, alias);
        counterValues.put(CounterEntry.COLUMN_NAME_ADDRESS, address);
        counterValues.put(CounterEntry.COLUMN_NAME_FIRST_CONNECTED, firstSeen);
        counterValues.put(CounterEntry.COLUMN_NAME_INITIAL_COUNT, initialCount);
        counterValues.put(CounterEntry.COLUMN_NAME_LAST_BATTERY, lastBattery);
        counterValues.put(CounterEntry.COLUMN_NAME_LAST_CONNECTED, lastSeen);
        counterValues.put(CounterEntry.COLUMN_NAME_LAST_COUNT, lastCount);
        counterValues.put(CounterEntry.COLUMN_NAME_LATITUDE, location != null ? location.latitude : 0);
        counterValues.put(CounterEntry.COLUMN_NAME_LONGITUDE, location != null ? location.longitude : 0);
        counterValues.put(CounterEntry.COLUMN_NAME_MODEL, model);
        counterValues.put(CounterEntry.COLUMN_NAME_HARDWARE_REVISION, hardwareRevision);
        counterValues.put(CounterEntry.COLUMN_NAME_SOFTWARE_REVISION, softwareRevision);
        contentResolver.update(CounterDatabaseContract.COUNTERS_URI, counterValues,
                               CounterDatabaseContract.SELECTION_ADDRESS_ONLY, new String[] { address });

        final ContentValues readingValues = new ContentValues();
        readingValues.put(CounterEntry.COLUMN_NAME_ADDRESS, address);
        readingValues.put(CounterEntry.COLUMN_NAME_LAST_BATTERY, lastBattery);
        readingValues.put(CounterEntry.COLUMN_NAME_READING_TIME, lastSeen);
        readingValues.put(CounterEntry.COLUMN_NAME_LAST_COUNT, lastCount);
        readingValues.put(CounterEntry.COLUMN_NAME_LATITUDE, location != null ? location.latitude : 0);
        readingValues.put(CounterEntry.COLUMN_NAME_LONGITUDE, location != null ? location.longitude : 0);
        contentResolver.insert(CounterDatabaseContract.READINGS_URI, readingValues);
    }

    @NonNull
    public String getFormattedLastSeen(@NonNull Locale locale) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
                .format(new Date(getLastSeen()));
    }

    private static final class ModelNumber {

        private static final byte BLE_CYCLE_COUNTER = 0x01;

        @NonNull
        static String getModelNumberString(byte modelByte) {
            switch (modelByte) {
                case BLE_CYCLE_COUNTER:
                    return "BLE Magnetic Cycle Counter";
                default:
                    return "Unknown Device";
            }
        }
    }

    private static final class HardwareRevision {

        private static final byte CY8CKIT_042_BLE_A_PROTOTYPE = 0x01;
        private static final byte REV_A                       = 0x02;
        private static final byte REV_B                       = 0x03;

        @NonNull
        static String getHardwareRevisionString(byte hardwareByte) {
            switch (hardwareByte) {
                case CY8CKIT_042_BLE_A_PROTOTYPE:
                    return "BLE Pioneer Prototype";
                case REV_A:
                    return "A";
                case REV_B:
                    return "B";
                default:
                    return "Unknown";
            }
        }
    }

    private static final class SoftwareRevision {

        private static final byte ALPHA = 0x01;

        @NonNull
        static String getSoftwareRevisionString(byte softwareByte) {
            switch (softwareByte) {
                case ALPHA:
                    return "Alpha";
                default:
                    return "Unknown";
            }
        }
    }
}
