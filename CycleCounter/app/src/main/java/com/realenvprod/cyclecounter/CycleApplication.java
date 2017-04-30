package com.realenvprod.cyclecounter;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry;
import com.realenvprod.cyclecounter.service.BLEScanService;

import java.util.Random;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */

public class CycleApplication extends Application implements ActivityLifecycleCallbacks {

    private static final String TAG = "CycleApplication";

    private static final boolean isDebug = true;

    private boolean mainActivityVisibile = false;

    public boolean isMainActivityVisibile() {
        return mainActivityVisibile;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (isDebug) {
            populatedTestDataIfNeeded();
        }
        registerActivityLifecycleCallbacks(this);
        launchScanService();
    }

    private void launchScanService() {
        final Intent intent = new Intent(this, BLEScanService.class);
        startService(intent);
    }

    private void populatedTestDataIfNeeded() {
        final Cursor cursor = getContentResolver().query(CounterDatabaseContract.COUNTERS_URI, null, null, null, null);
        final int count = cursor != null ? cursor.getCount() : 0;
        if (cursor != null) {
            cursor.close();
        }
        if (count == 0) {
            final String baseAlias = "Sensor ";
            final String baseAddress = "00:11:22:33:44:55:66:";
            final LatLng center = new LatLng(38.3372206, -120.758194);
            final double radius = 1610; // meters
            final int deviceCount = 25;
            final double step = 360.0 / (deviceCount - 2);
            final Random random = new Random();

            for (int i = 0; i < deviceCount; ++i) {
                Log.d(TAG, "Adding test sensor: " + baseAlias + i);
                final ContentValues values = new ContentValues();
                values.put(CounterEntry.COLUMN_NAME_ALIAS, baseAlias + i);
                values.put(CounterEntry.COLUMN_NAME_ADDRESS, baseAddress + String.format("%02X", i));
                values.put(CounterEntry.COLUMN_NAME_LAST_COUNT, random.nextInt(100000));
                if (i == 0) {
                    values.put(CounterEntry.COLUMN_NAME_LATITUDE, center.latitude);
                    values.put(CounterEntry.COLUMN_NAME_LONGITUDE, center.longitude);
                } else {
                    final LatLng dest = getDestination(center, radius, step * i);
                    values.put(CounterEntry.COLUMN_NAME_LATITUDE, dest.latitude);
                    values.put(CounterEntry.COLUMN_NAME_LONGITUDE, dest.longitude);
                }
                getContentResolver().insert(CounterDatabaseContract.COUNTERS_URI, values);
            }
        } else {
            Log.d(TAG, "Skipping test data population.");
        }
    }

    private LatLng getDestination(LatLng origin, double radius, double heading) {
        final double R = 6371e3; // metres
        double latitude = Math.asin(Math.sin(Math.toRadians(origin.latitude)) * Math.cos(radius / R)
                                    + Math.cos(Math.toRadians(origin.latitude)) * Math.sin(radius / R)
                                      * Math.cos(Math.toRadians(heading)));
        double longitude = Math.toRadians(origin.longitude) + Math.atan2(Math.sin(Math.toRadians(heading))
                                                                         * Math.sin(radius / R)
                                                                         * Math.cos(Math.toRadians(origin.latitude)),
                                                                         Math.cos(radius / R)
                                                                         - Math.sin(Math.toRadians(origin.latitude))
                                                                           * Math.sin(latitude));
        return new LatLng(Math.toDegrees(latitude), Math.toDegrees(longitude));
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity instanceof MainActivity) {
            mainActivityVisibile = true;
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity instanceof MainActivity) {
            mainActivityVisibile = false;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
