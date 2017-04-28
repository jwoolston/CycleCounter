package com.realenvprod.cyclecounter.counter.db;

import android.provider.BaseColumns;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */
public final class CounterDatabaseContract {

    // Ensure this class can't be accidentally instantiated
    private CounterDatabaseContract() {
    }

    public static class CounterEntry implements BaseColumns {
        public static final String COUNTERS_TABLE_NAME         = "counters";
        public static final String READINGS_TABLE_NAME         = "readings";
        public static final String COLUMN_NAME_ALIAS           = "alias";
        public static final String COLUMN_NAME_ADDRESS         = "address";
        public static final String COLUMN_NAME_FIRST_CONNECTED = "first_connected";
        public static final String COLUMN_NAME_LAST_CONNECTED  = "last_connected";
        public static final String COLUMN_NAME_READING_TIME    = "reading_time";
        public static final String COLUMN_NAME_INITIAL_COUNT   = "initial_count";
        public static final String COLUMN_NAME_LAST_COUNT      = "last_count";
        public static final String COLUMN_NAME_LAST_BATTERY    = "last_battery";
        public static final String COLUMN_NAME_LATITUDE        = "latitude";
        public static final String COLUMN_NAME_LONGITUDE       = "longitude";
        public static final String COLUMN_NAME_TRACKED         = "tracked";
    }

    public static final String SQL_CREATE_COUNTERS_TABLE = "CREATE TABLE " + CounterEntry.COUNTERS_TABLE_NAME + " ("
                                                           + CounterEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                                                           + CounterEntry.COLUMN_NAME_ADDRESS + " TEXT NOT NULL UNIQUE, "
                                                           + CounterEntry.COLUMN_NAME_ALIAS + " TEXT, "
                                                           + CounterEntry.COLUMN_NAME_FIRST_CONNECTED + " INTEGER, "
                                                           + CounterEntry.COLUMN_NAME_LAST_CONNECTED + " INTEGER, "
                                                           + CounterEntry.COLUMN_NAME_INITIAL_COUNT + " INTEGER, "
                                                           + CounterEntry.COLUMN_NAME_LAST_COUNT + " INTEGER, "
                                                           + CounterEntry.COLUMN_NAME_LAST_BATTERY + " REAL, "
                                                           + CounterEntry.COLUMN_NAME_LATITUDE + " REAL, "
                                                           + CounterEntry.COLUMN_NAME_LONGITUDE + " REAL, "
                                                           + CounterEntry.COLUMN_NAME_TRACKED + " BOOLEAN)";

    public static final String SQL_CREATE_READINGS_TABLE = "CREATE TABLE " + CounterEntry.READINGS_TABLE_NAME + " ("
                                                           + CounterEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                                                           + CounterEntry.COLUMN_NAME_ADDRESS + " TEXT NOT NULL, "
                                                           + CounterEntry.COLUMN_NAME_READING_TIME + " INTEGER UNIQUE, "
                                                           + CounterEntry.COLUMN_NAME_LAST_COUNT + " INTEGER, "
                                                           + CounterEntry.COLUMN_NAME_LAST_BATTERY + " REAL, "
                                                           + CounterEntry.COLUMN_NAME_LATITUDE + " REAL, "
                                                           + CounterEntry.COLUMN_NAME_LONGITUDE + " REAL)";

    public static final String SQL_DELETE_COUNTERS = "DROP TABLE IF EXISTS " + CounterEntry.COUNTERS_TABLE_NAME;
    public static final String SQL_DELETE_READINGS = "DROP TABLE IF EXISTS " + CounterEntry.READINGS_TABLE_NAME;
}
