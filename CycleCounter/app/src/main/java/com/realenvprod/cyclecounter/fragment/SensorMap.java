package com.realenvprod.cyclecounter.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.realenvprod.cyclecounter.counter.Counter;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */
public class SensorMap extends SupportMapFragment implements OnMapReadyCallback, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "SensorMap";

    private GoogleMap map;
    private Cursor    devicesCursor;

    private final HashMap<Counter, Marker> sensorMarkers = new HashMap<>();

    @Override
    public void onResume() {
        super.onResume();
        getMapAsync(this);
    }

    private void updateSensorMarkersIfAble() {
        Log.d(TAG, "Updating sensor markers: " + (map == null) + "/" + (devicesCursor == null));
        if (map == null) {
            // Clear markers in memory
            sensorMarkers.clear();
        } else {
            if (devicesCursor == null) {
                // Clear all sensors from the map
                for (Marker marker : sensorMarkers.values()) {
                    marker.remove();
                }
                sensorMarkers.clear();
            } else {
                // Add markers
                Log.d(TAG, "Adding markers.");
                Map<Counter, Marker> oldMarkers = new HashMap<>(sensorMarkers);
                sensorMarkers.clear();
                devicesCursor.moveToFirst();
                while (!devicesCursor.isAfterLast()) {
                    final Counter counter = new Counter(devicesCursor);
                    Log.d(TAG, "Marker " + counter.alias + " location: " + counter.location);
                    Marker marker = oldMarkers.get(counter);
                    if (marker != null) {
                        // We have a marker to update
                        counter.updateMarker(marker);
                    } else {
                        marker = counter.buildMarker(map);
                    }
                    sensorMarkers.put(counter, marker);
                    devicesCursor.moveToNext();
                }
                final LatLngBounds.Builder builder = new Builder();
                for (Counter counter : sensorMarkers.keySet()) {
                    builder.include(counter.location);
                }
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 48));
            }
        }
    }

    /**
     * Manipulates the map once available. This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.v(TAG, "onMapReady()");
        map = googleMap;
        updateSensorMarkersIfAble();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        getActivity().getSupportLoaderManager().restartLoader(0, null, this);
        return super.onCreateView(layoutInflater, viewGroup, bundle);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case 0:
                return new CursorLoader(getContext(), CounterDatabaseContract.COUNTERS_URI, null, null, null, null);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case 0:
                devicesCursor = cursor;
                updateSensorMarkersIfAble();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case 0:
                devicesCursor = null;
                break;
        }
    }
}
