package com.realenvprod.cyclecounter;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry;
import com.realenvprod.cyclecounter.service.BLEScanService;

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
        final Cursor cursor = getContentResolver().query(CounterDatabaseContract.COUNTERS_URI, null, null,
                                                         null, null);
        final int count = cursor != null ? cursor.getCount() : 0;
        if (cursor != null) {
            cursor.close();
        }
        if (count == 0) {
            final String baseAlias = "Sensor ";
            for (int i = 0; i < 50; ++i) {
                Log.d(TAG, "Adding test sensor: " + baseAlias + i);
                final ContentValues values = new ContentValues();
                values.put(CounterEntry.COLUMN_NAME_ALIAS, baseAlias + i);
                values.put(CounterEntry.COLUMN_NAME_ADDRESS, "00:11:22:33:44:55:66:" + String.format("%02X", i));
                getContentResolver().insert(CounterDatabaseContract.COUNTERS_URI, values);
            }
        } else {
            Log.d(TAG, "Skipping test data population.");
        }
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
