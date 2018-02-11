package com.realenvprod.cyclecounter.counter.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry;

import static com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry.COUNTERS_TABLE_NAME;
import static com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry.READINGS_TABLE_NAME;

public class CounterSensorProvider extends ContentProvider {

    static final String AUTHORITY = "com.realenvprod.cyclecounter.cyclesensorprovider";

    private static final String TAG = "CounterSensorProvider";

    // Creates a UriMatcher object.
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, COUNTERS_TABLE_NAME, 1);
        sUriMatcher.addURI(AUTHORITY, COUNTERS_TABLE_NAME + "/#", 2);
        sUriMatcher.addURI(AUTHORITY, READINGS_TABLE_NAME, 3);
        sUriMatcher.addURI(AUTHORITY, READINGS_TABLE_NAME + "/#", 4);
    }

    private CounterDatabaseHelper helper;

    public CounterSensorProvider() {
    }

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        if (context != null) {
            helper = new CounterDatabaseHelper(context);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) throws SQLiteException {
        switch (sUriMatcher.match(uri)) {
            case 1:
                // Query all rows of COUNTERS table
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = CounterEntry._ID + " ASC";
                }
                return helper.getReadableDatabase().query(COUNTERS_TABLE_NAME, projection, selection,
                                                          selectionArgs, null, null, sortOrder);
            case 2:
                // Query for a specific row from COUNTERS table
                selection = selection + " " + CounterEntry._ID + " = " + uri.getLastPathSegment();
                return helper.getReadableDatabase().query(COUNTERS_TABLE_NAME, projection, selection,
                                                          selectionArgs, null, null, null);
            case 3:
                // Query all rows of READINGS table
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = CounterEntry._ID + " ASC";
                }
                return helper.getReadableDatabase().query(READINGS_TABLE_NAME, projection, selection,
                                                          selectionArgs, null, null, sortOrder);
            case 4:
                // Query for a specific row from the READINGS table
                selection = selection + " " + CounterEntry._ID + " = " + uri.getLastPathSegment();
                return helper.getReadableDatabase().query(READINGS_TABLE_NAME, projection, selection,
                                                          selectionArgs, null, null, null);
            default:
                Log.e(TAG, "Unsupported Query URI: " + uri);
                throw new IllegalArgumentException("Unsupported Query URI: " + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case 1:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + COUNTERS_TABLE_NAME;
            case 2:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + "." + COUNTERS_TABLE_NAME;
            case 3:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + READINGS_TABLE_NAME;
            case 4:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + "." + READINGS_TABLE_NAME;
            default:
                Log.e(TAG, "Unsupported Mime URI: " + uri);
                throw new IllegalArgumentException("Unsupported Mime URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (sUriMatcher.match(uri)) {
            case 1:
                // Counters table
                if (values.keySet().size() != 0) {
                    final SQLiteDatabase db = helper.getWritableDatabase();
                    final long id = db.insert(COUNTERS_TABLE_NAME, null, values);
                    return ContentUris.withAppendedId(uri, id);
                } else {
                    return null;
                }
            case 3:
                // Readings table
                if (values.keySet().size() != 0) {
                    final SQLiteDatabase db = helper.getWritableDatabase();
                    final long id = db.insert(READINGS_TABLE_NAME, null, values);
                    return ContentUris.withAppendedId(uri, id);
                } else {
                    return null;
                }
            default:
                Log.e(TAG, "Unsupported Insert URI: " + uri);
                throw new IllegalArgumentException("Unsupported Insert URI: " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case 1:
                // Delete from Counters table
                return helper.getWritableDatabase().delete(COUNTERS_TABLE_NAME, selection, selectionArgs);
            case 2:
                // Delete row from Counters table
                selection = selection + " " + CounterEntry._ID + " = " + uri.getLastPathSegment();
                return helper.getWritableDatabase().delete(COUNTERS_TABLE_NAME, selection, selectionArgs);
            case 3:
                // Delete from Readings table
                return helper.getWritableDatabase().delete(READINGS_TABLE_NAME, selection, selectionArgs);
            case 4:
                // Delete row from Readings table
                selection = selection + " " + CounterEntry._ID + " = " + uri.getLastPathSegment();
                return helper.getWritableDatabase().delete(READINGS_TABLE_NAME, selection, selectionArgs);
            default:
                Log.e(TAG, "Unsupported Delete URI: " + uri);
                throw new IllegalArgumentException("Unsupported Delete URI: " + uri);

        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case 1:
                // Delete from Counters table
                return helper.getWritableDatabase().update(COUNTERS_TABLE_NAME, values, selection, selectionArgs);
            case 2:
                // Delete row from Counters table
                selection = selection + " " + CounterEntry._ID + " = " + uri.getLastPathSegment();
                return helper.getWritableDatabase().update(COUNTERS_TABLE_NAME, values, selection, selectionArgs);
            case 3:
                // Delete from Readings table
                return helper.getWritableDatabase().update(READINGS_TABLE_NAME, values, selection, selectionArgs);
            case 4:
                // Delete row from Readings table
                selection = selection + " " + CounterEntry._ID + " = " + uri.getLastPathSegment();
                return helper.getWritableDatabase().update(READINGS_TABLE_NAME, values, selection, selectionArgs);
            default:
                Log.e(TAG, "Unsupported Update URI: " + uri);
                throw new IllegalArgumentException("Unsupported Update URI: " + uri);

        }
    }
}
