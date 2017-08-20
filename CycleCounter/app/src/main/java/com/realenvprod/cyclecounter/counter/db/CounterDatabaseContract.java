package com.realenvprod.cyclecounter.counter.db;

import static com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry.COLUMN_NAME_ADDRESS;
import static com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry.COLUMN_NAME_READING_TIME;
import static com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry.COUNTERS_TABLE_NAME;

import android.net.Uri;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */
public final class CounterDatabaseContract {

    public static final Uri BASE_URI                      = Uri.parse("content://" + CounterSensorProvider.AUTHORITY);
    public static final Uri COUNTERS_URI                  = Uri.withAppendedPath(BASE_URI, CounterEntry.COUNTERS_TABLE_NAME);
    public static final Uri READINGS_URI                  = Uri.withAppendedPath(BASE_URI, CounterEntry.READINGS_TABLE_NAME);

    // Ensure this class can't be accidentally instantiated
    private CounterDatabaseContract() {
    }

    public static class CounterEntry {
        public static final String _ID                           = "_id";
        public static final String COUNTERS_TABLE_NAME           = "counters";
        public static final String READINGS_TABLE_NAME           = "readings";
        public static final String COLUMN_NAME_ALIAS             = "alias";
        public static final String COLUMN_NAME_ADDRESS           = "address";
        public static final String COLUMN_NAME_FIRST_CONNECTED   = "first_connected";
        public static final String COLUMN_NAME_LAST_CONNECTED    = "last_connected";
        public static final String COLUMN_NAME_READING_TIME      = "reading_time";
        public static final String COLUMN_NAME_INITIAL_COUNT     = "initial_count";
        public static final String COLUMN_NAME_LAST_COUNT        = "last_count";
        public static final String COLUMN_NAME_LAST_BATTERY      = "last_battery";
        public static final String COLUMN_NAME_LATITUDE          = "latitude";
        public static final String COLUMN_NAME_LONGITUDE         = "longitude";
        public static final String COLUMN_NAME_MODEL             = "model";
        public static final String COLUMN_NAME_HARDWARE_REVISION = "hardware_revision";
        public static final String COLUMN_NAME_SOFTWARE_REVISION = "software_revision";
    }

    public static final String SQL_CREATE_COUNTERS_TABLE = "CREATE TABLE " + COUNTERS_TABLE_NAME + " ("
                                                           + CounterEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                                                           + CounterEntry.COLUMN_NAME_ADDRESS
                                                           + " TEXT NOT NULL UNIQUE, "
                                                           + CounterEntry.COLUMN_NAME_ALIAS + " TEXT, "
                                                           + CounterEntry.COLUMN_NAME_FIRST_CONNECTED + " INTEGER, "
                                                           + CounterEntry.COLUMN_NAME_LAST_CONNECTED + " INTEGER, "
                                                           + CounterEntry.COLUMN_NAME_INITIAL_COUNT + " INTEGER, "
                                                           + CounterEntry.COLUMN_NAME_LAST_COUNT + " INTEGER, "
                                                           + CounterEntry.COLUMN_NAME_LAST_BATTERY + " REAL, "
                                                           + CounterEntry.COLUMN_NAME_LATITUDE + " REAL, "
                                                           + CounterEntry.COLUMN_NAME_LONGITUDE + " REAL, "
                                                           + CounterEntry.COLUMN_NAME_MODEL + " TEXT NOT NULL, "
                                                           + CounterEntry.COLUMN_NAME_HARDWARE_REVISION + " TEXT NOT "
                                                           + "NULL, "
                                                           + CounterEntry.COLUMN_NAME_SOFTWARE_REVISION + " TEXT NOT "
                                                           + "NULL)";

    public static final String SQL_CREATE_READINGS_TABLE = "CREATE TABLE " + CounterEntry.READINGS_TABLE_NAME + " ("
                                                           + CounterEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                                                           + CounterEntry.COLUMN_NAME_ADDRESS + " TEXT NOT NULL, "
                                                           + CounterEntry.COLUMN_NAME_READING_TIME + " INTEGER UNIQUE, "
                                                           + CounterEntry.COLUMN_NAME_LAST_COUNT + " INTEGER, "
                                                           + CounterEntry.COLUMN_NAME_LAST_BATTERY + " REAL, "
                                                           + CounterEntry.COLUMN_NAME_LATITUDE + " REAL, "
                                                           + CounterEntry.COLUMN_NAME_LONGITUDE + " REAL)";

    public static final String SQL_DELETE_COUNTERS = "DROP TABLE IF EXISTS " + COUNTERS_TABLE_NAME;
    public static final String SQL_DELETE_READINGS = "DROP TABLE IF EXISTS " + CounterEntry.READINGS_TABLE_NAME;

    public static final String[] PROJECTION_ADDRESS_ONLY = new String[]{ COLUMN_NAME_ADDRESS };

    public static final String[] PROJECTION_READINGS_WITH_TIME = new String[]{ CounterEntry.COLUMN_NAME_READING_TIME,
                                                                               CounterEntry.COLUMN_NAME_LAST_COUNT,
                                                                               CounterEntry.COLUMN_NAME_LAST_BATTERY };

    public static final String SELECTION_ADDRESS_ONLY = COLUMN_NAME_ADDRESS + " = ?";

    public static final String SELECTION_COUNTER_DETAILS_READING = SELECTION_ADDRESS_ONLY + " AND " +
                                                                   COLUMN_NAME_READING_TIME + " > ?";
}
