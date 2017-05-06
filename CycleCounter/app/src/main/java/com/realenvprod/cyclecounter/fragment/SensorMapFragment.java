package com.realenvprod.cyclecounter.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.realenvprod.cyclecounter.R;
import com.realenvprod.cyclecounter.counter.Counter;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */
public class SensorMapFragment extends SupportMapFragment implements OnMapReadyCallback, LoaderCallbacks<Cursor>,
                                                                     OnClickListener {

    private static final String TAG = "SensorMapFragment";

    private GoogleMap            map;
    private Cursor               devicesCursor;
    private FloatingActionsMenu  fabMenu;
    private FloatingActionButton fabHeatmap;
    private HeatmapTileProvider  heatmapProvider;
    private TileOverlay          heatmapOverlay;

    private final HashMap<Counter, Marker> sensorMarkers = new HashMap<>();

    @Override
    public void onResume() {
        super.onResume();
        getMapAsync(this);
        getActivity().setTitle("Cycle Counter Map");
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

                // Update heatmap, if needed
                if (heatmapOverlay != null) {
                    heatmapProvider.setWeightedData(getHeatmapDataList());
                    heatmapOverlay.clearTileCache();
                }

                // Update camera
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 48));
            }
        }
    }

    private List<WeightedLatLng> getHeatmapDataList() {
        final ArrayList<WeightedLatLng> data = new ArrayList<>();
        long maxCount = 1;
        for (Counter counter : sensorMarkers.keySet()) {
            if (counter.lastCount > maxCount) {
                maxCount = counter.lastCount;
            }
        }
        for (Counter counter : sensorMarkers.keySet()) {
            data.add(new WeightedLatLng(counter.location, counter.lastConnected / ((double) maxCount)));
        }
        return data;
    }

    private void addHeatmap() {
        Log.d(TAG, "Showing heatmap");
        heatmapProvider = new HeatmapTileProvider.Builder().weightedData(getHeatmapDataList()).build();
        // Add a tile overlay to the map, using the heat map tile provider.
        heatmapOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapProvider));
    }

    private void clearHeatmap() {
        Log.d(TAG, "Clearing heatmap");
        if (heatmapOverlay != null) {
            heatmapOverlay.remove();
            heatmapOverlay = null;
            heatmapProvider = null;
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
        final FrameLayout frame = (FrameLayout) layoutInflater.inflate(R.layout.sensor_map_fragment, viewGroup, false);
        final View view = super.onCreateView(layoutInflater, frame, bundle);
        frame.addView(view);
        fabMenu = (FloatingActionsMenu) frame.findViewById(R.id.multiple_actions);
        fabMenu.bringToFront();
        fabHeatmap = (FloatingActionButton) fabMenu.findViewById(R.id.action_toggle_heatmap);
        fabHeatmap.setOnClickListener(this);
        return frame;
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.action_toggle_heatmap:
                if (heatmapOverlay != null) {
                    clearHeatmap();
                } else {
                    addHeatmap();
                }
                fabMenu.collapse();
                break;
            default:
                Log.e(TAG, "Unsupported view id: " + view.getId());
        }
    }
}
