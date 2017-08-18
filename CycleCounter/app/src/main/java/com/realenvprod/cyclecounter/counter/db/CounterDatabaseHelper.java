package com.realenvprod.cyclecounter.counter.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.support.annotation.NonNull;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */

public class CounterDatabaseHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = Environment.getExternalStorageDirectory().getAbsolutePath() +
        "/CounterDatabase.db";

    public CounterDatabaseHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CounterDatabaseContract.SQL_CREATE_COUNTERS_TABLE);
        db.execSQL(CounterDatabaseContract.SQL_CREATE_READINGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO: Implement an upgrade policy that saves data
        db.execSQL(CounterDatabaseContract.SQL_DELETE_COUNTERS);
        db.execSQL(CounterDatabaseContract.SQL_CREATE_READINGS_TABLE);
        onCreate(db);
    }
}
