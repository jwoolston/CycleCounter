package com.realenvprod.cyclecounter.counter;

import android.database.Cursor;
import android.support.annotation.NonNull;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry;

/**
 * Encapsulation of Counter Sensor data.
 *
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */
public class Counter {

    public final String alias;
    public final String address;
    public final long firstConnected;
    public final long lastConnected;
    public final long initialCount;
    public final long lastCount;
    public final double lastBattery;
    public final LatLng location;

    public Counter(@NonNull Cursor cursor) {
        alias = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_ALIAS));
        address = cursor.getString(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_ADDRESS));
        firstConnected = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_FIRST_CONNECTED));
        lastConnected = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LAST_CONNECTED));
        initialCount = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_INITIAL_COUNT));
        lastCount = cursor.getLong(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LAST_COUNT));
        lastBattery = cursor.getDouble(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LAST_BATTERY));
        location = new LatLng(cursor.getDouble(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LATITUDE)),
                              cursor.getDouble(cursor.getColumnIndex(CounterEntry.COLUMN_NAME_LONGITUDE)));
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
}
