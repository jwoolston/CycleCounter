package com.realenvprod.cyclecounter.fragment;


import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.realenvprod.cyclecounter.R;
import com.realenvprod.cyclecounter.counter.Counter;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry;

import java.util.LinkedList;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CounterDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CounterDetailFragment extends CounterFragment implements
                                                           LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = "CounterDetailFragment";

    private static final String ARG_COUNTER = "counter";

    private static final int READINGS_LOADER_ID = 0x01;

    private LineData readingData;

    private EditText aliasView;
    private TextView addressView;
    private TextView lastSeenView;
    private TextView countView;
    private TextView batteryView;
    private TextView modelView;
    private TextView hardwareView;
    private TextView softwareView;
    private LineChart lineChartView;

    public CounterDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param counter {@link Counter} to add.
     *
     * @return A new instance of fragment AddCounterFragment.
     */
    public static CounterDetailFragment newInstance(@NonNull Counter counter) {
        CounterDetailFragment fragment = new CounterDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_COUNTER, counter);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getSupportLoaderManager().initLoader(READINGS_LOADER_ID, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_counter_details, container, false);
        aliasView = (EditText) view.findViewById(R.id.alias_input);
        addressView = (TextView) view.findViewById(R.id.address_entry);
        lastSeenView = (TextView) view.findViewById(R.id.last_seen_entry);
        countView = (TextView) view.findViewById(R.id.current_count_entry);
        batteryView = (TextView) view.findViewById(R.id.current_battery_entry);
        modelView = (TextView) view.findViewById(R.id.model_number_entry);
        hardwareView = (TextView) view.findViewById(R.id.hardware_revision_entry);
        softwareView = (TextView) view.findViewById(R.id.software_revision_entry);
        lineChartView = (LineChart) view.findViewById(R.id.history_chart);
        view.findViewById(R.id.delete_counter).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteCounter();
            }
        });

        updateFromCounter(counter);
        return view;
    }

    @Override
    public void onDestroyView() {
        aliasView = null;
        addressView = null;
        lastSeenView = null;
        countView = null;
        batteryView = null;
        modelView = null;
        hardwareView = null;
        softwareView = null;
        lineChartView = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("Counter Details");
        updateFromCounter(counter);
    }

    @Override
    protected void updateFromCounter(@NonNull Counter counter) {
        aliasView.setText(counter.getAlias());
        addressView.setText(counter.getAddress());
        lastSeenView.setText(counter.getFormattedLastSeen(Locale.US));
        countView.setText(Long.toString(counter.getLastCount()));
        batteryView.setText(Integer.toString((int) Math.round(counter.getLastBattery())));
        modelView.setText(counter.getModel());
        hardwareView.setText(counter.getHardwareRevision());
        softwareView.setText(counter.getSoftwareRevision());
        getActivity().getSupportLoaderManager().restartLoader(READINGS_LOADER_ID, null, this);
    }

    private void deleteCounter() {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(), CounterDatabaseContract.READINGS_URI,
                                CounterDatabaseContract.PROJECTION_READINGS_WITH_TIME,
                                CounterDatabaseContract.SELECTION_ADDRESS_ONLY, new String[] { counter.getAddress() },
                                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "onLoadFinished: " + data.getCount());
        if (data != null) {
            final LinkedList<Entry> newData = new LinkedList<>();
            final int timeColumn = data.getColumnIndex(CounterEntry.COLUMN_NAME_READING_TIME);
            final int readingColumn = data.getColumnIndex(CounterEntry.COLUMN_NAME_LAST_COUNT);
            data.moveToFirst();
            while (!data.isAfterLast()) {
                final long time = data.getLong(timeColumn);

                newData.add(new Entry(time, data.getLong(readingColumn)));
                data.moveToNext();
            }
            Log.v(TAG, "New readings dataset: " + newData);
            data.close();
            LineDataSet dataSet = new LineDataSet(newData, "Cycles");
            dataSet.setColor(getResources().getColor(R.color.colorAccent));
            readingData = new LineData(dataSet);
            lineChartView.setData(readingData);
            lineChartView.invalidate();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "Loader Reset!");
    }
}
