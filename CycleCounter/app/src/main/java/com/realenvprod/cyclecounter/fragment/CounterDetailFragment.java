package com.realenvprod.cyclecounter.fragment;


import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.realenvprod.cyclecounter.R;
import com.realenvprod.cyclecounter.counter.Counter;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry;
import com.realenvprod.cyclecounter.view.BatteryView;

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

    private static final String CYCLE_SET_NAME = "Cycles;";
    private static final String BATTERY_SET_NAME = "Battery";

    private static final int READINGS_LOADER_ID = 0x01;
    private static final long SECOND_RANGE       = 1000;
    private static final long MINUTE_RANGE      = SECOND_RANGE * 60;
    private static final long HOURLY_RANGE      = MINUTE_RANGE * 60;
    private static final long DAILY_RANGE       = HOURLY_RANGE * 24;
    private static final long MONTHLY_RANGE     = DAILY_RANGE * 28; // We use 28 days for a month to guarantee proper
    // triggering in all months. The formatter will handle any ambiguity

    LineDataSet countSet;
    LineDataSet batterySet;
    private long lastReadingTime = 0L;

    private EditText    aliasView;
    private TextView    addressView;
    private TextView    lastSeenView;
    private TextView    countView;
    private TextView    batteryView;
    private BatteryView batteryStatus;
    private TextView    modelView;
    private TextView    hardwareView;
    private TextView    softwareView;
    private LineChart   lineChartView;

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
        batteryStatus = (BatteryView) view.findViewById(R.id.current_battery_icon);
        modelView = (TextView) view.findViewById(R.id.model_number_entry);
        hardwareView = (TextView) view.findViewById(R.id.hardware_revision_entry);
        softwareView = (TextView) view.findViewById(R.id.software_revision_entry);
        lineChartView = (LineChart) view.findViewById(R.id.history_chart);
        initializeChart();
        view.findViewById(R.id.delete_counter).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteCounter();
            }
        });
        return view;
    }

    private void initializeChart() {
        lineChartView.setData(new LineData());
        lineChartView.setDescription(null);
        final YAxis batteryAxis = lineChartView.getAxisLeft();
        batteryAxis.setAxisMinimum(0);
        batteryAxis.setAxisMaximum(100);
        batteryAxis.setGranularityEnabled(true);
        batteryAxis.setGranularity(20);
        batteryAxis.setDrawGridLines(false);
        final YAxis countAxis = lineChartView.getAxisRight();
        countAxis.setEnabled(true);
        countAxis.setDrawGridLines(true);
        final XAxis xAxis = lineChartView.getXAxis();
        xAxis.setLabelRotationAngle(20f);
        xAxis.setGranularityEnabled(true);
        xAxis.setGranularity(HOURLY_RANGE); // Hourly
        lineChartView.invalidate();
    }

    @Override
    public void onDestroyView() {
        aliasView = null;
        addressView = null;
        lastSeenView = null;
        countView = null;
        batteryView = null;
        batteryStatus = null;
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

        // Do an initial data fetch for all possible readings
        lastReadingTime = 0L;
        updateFromCounter(counter);
    }

    @Override
    protected void updateFromCounter(@NonNull Counter counter) {
        aliasView.setText(counter.getAlias());
        addressView.setText(counter.getAddress());
        lastSeenView.setText(counter.getFormattedLastSeen(Locale.US));
        countView.setText(Long.toString(counter.getLastCount()));
        batteryView.setText(Integer.toString((int) Math.round(counter.getLastBattery())) + '%');
        batteryStatus.setBatteryLevel((int) Math.round(counter.getLastBattery()));
        modelView.setText(counter.getModel());
        hardwareView.setText(counter.getHardwareRevision());
        softwareView.setText(counter.getSoftwareRevision());
        //getActivity().getSupportLoaderManager().restartLoader(READINGS_LOADER_ID, null, this);
        final Cursor readings = getContext().getContentResolver()
                .query(CounterDatabaseContract.READINGS_URI, CounterDatabaseContract.PROJECTION_READINGS_WITH_TIME,
                       CounterDatabaseContract.SELECTION_COUNTER_DETAILS_READING,
                       new String[]{ counter.getAddress(), Long.toString(lastReadingTime) }, null);
        updateGraph(readings);
        lastReadingTime = counter.getLastSeen();
    }

    private void deleteCounter() {

    }

    private void updateGraph(@Nullable Cursor data) {
        if (data != null && data.getCount() > 0) {
            final LinkedList<Entry> countEntries = new LinkedList<>();
            final LinkedList<Entry> batteryEntries = new LinkedList<>();
            final int timeColumn = data.getColumnIndex(CounterEntry.COLUMN_NAME_READING_TIME);
            final int readingColumn = data.getColumnIndex(CounterEntry.COLUMN_NAME_LAST_COUNT);
            final int batteryColumn = data.getColumnIndex(CounterEntry.COLUMN_NAME_LAST_BATTERY);
            long max = 0;
            data.moveToFirst();
            while (!data.isAfterLast()) {
                final long time = data.getLong(timeColumn);
                if (max < time) {
                    max = time;
                }
                countEntries.add(new Entry(time, data.getLong(readingColumn)));
                batteryEntries.add(new Entry(time, data.getInt(batteryColumn)));
                data.moveToNext();
            }
            data.close();
            final LineData lineData = lineChartView.getData();
            ILineDataSet countSet = lineData.getDataSetByIndex(0);
            ILineDataSet batterySet = lineData.getDataSetByIndex(1);
            if (countSet == null) {
                countSet = new LineDataSet(countEntries, CYCLE_SET_NAME);
                countSet.setAxisDependency(AxisDependency.RIGHT);
                ((LineDataSet) countSet).setColor(getResources().getColor(R.color.colorAccent));
                lineData.addDataSet(countSet);
            } else {
                for (Entry entry : countEntries) {
                    lineData.addEntry(entry, 0);
                }
            }
            final int count = countSet.getEntryCount();
            for (int i = 0; i < count; ++i) {
                Log.i(TAG, "" + countSet.getEntryForIndex(i));
            }
            if (batterySet == null) {
                batterySet = new LineDataSet(batteryEntries, "Battery");
                batterySet.setAxisDependency(AxisDependency.LEFT);
                ((LineDataSet) batterySet).setColor(getResources().getColor(R.color.colorPrimary));
                lineData.addDataSet(batterySet);
            } else {
                for (Entry entry : batteryEntries) {
                    lineData.addEntry(entry, 1);
                }
            }
            lineData.notifyDataChanged();
            lineChartView.notifyDataSetChanged();
            //checkXAxisGranularity();
            //lineChartView.moveViewToX(countSet.getEntryCount());
            lineChartView.invalidate();
            if (lastReadingTime == 0) {
                lastReadingTime = max;
            }
        }
    }

    private void checkXAxisGranularity() {
        final long delta = (long) lineChartView.getXRange();
        final XAxis xAxis = lineChartView.getXAxis();
        final long max = (long) xAxis.getAxisMaximum();
        if (delta >= MONTHLY_RANGE) {

        } else if (delta >= DAILY_RANGE) {

        } else if (delta >= HOURLY_RANGE) {
            // Minute range
            Log.d(TAG, "Using hourly based range.");
            xAxis.setGranularity(HOURLY_RANGE);
            xAxis.setAxisMaximum(max - DAILY_RANGE); // Display the last 1 day
        } else if (delta >= MINUTE_RANGE) {
            // Minute range
            Log.d(TAG, "Using minute based range.");
            xAxis.setGranularity(1 * MINUTE_RANGE); // 10 minute granularity
            xAxis.setAxisMaximum(max - (HOURLY_RANGE / 4)); // Display the last 1/4 hour
        } else {
            // Second range
            Log.d(TAG, "Using second based range.");
            xAxis.setGranularity(SECOND_RANGE);
            xAxis.setAxisMaximum(max - MINUTE_RANGE); // Display the last 1 minute
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(), CounterDatabaseContract.READINGS_URI,
                                CounterDatabaseContract.PROJECTION_READINGS_WITH_TIME,
                                CounterDatabaseContract.SELECTION_ADDRESS_ONLY,
                                new String[]{ counter.getAddress() },
                                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "Loader Reset!");
    }
}
