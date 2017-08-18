package com.realenvprod.cyclecounter.fragment;


import android.content.ContentValues;
import android.content.DialogInterface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.realenvprod.cyclecounter.R;
import com.realenvprod.cyclecounter.counter.Counter;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry;
import com.realenvprod.cyclecounter.view.BatteryView;
import org.greenrobot.eventbus.EventBus;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddCounterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddCounterFragment extends CounterFragment {

    public static final String TAG = "AddCounterFragment";

    private EditText aliasView;
    private TextView addressView;
    private TextView countView;
    private TextView batteryView;
    private TextView modelView;
    private TextView hardwareView;
    private TextView softwareView;
    private BatteryView batteryIcon;

    public AddCounterFragment() {
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
    public static AddCounterFragment newInstance(@NonNull Counter counter) {
        AddCounterFragment fragment = new AddCounterFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_COUNTER, counter);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_add_counter, container, false);
        aliasView = (EditText) view.findViewById(R.id.alias_input);
        addressView = (TextView) view.findViewById(R.id.address_entry);
        countView = (TextView) view.findViewById(R.id.current_count_entry);
        batteryView = (TextView) view.findViewById(R.id.current_battery_entry);
        modelView = (TextView) view.findViewById(R.id.model_number_entry);
        hardwareView = (TextView) view.findViewById(R.id.hardware_revision_entry);
        softwareView = (TextView) view.findViewById(R.id.software_revision_entry);
        batteryIcon = (BatteryView) view.findViewById(R.id.current_battery_icon);
        (view.findViewById(R.id.save_counter)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCounter();
            }
        });

        updateFromCounter(counter);
        return view;
    }

    @Override
    public void onDestroyView() {
        aliasView = null;
        addressView = null;
        countView = null;
        batteryView = null;
        modelView = null;
        hardwareView = null;
        softwareView = null;
        batteryIcon = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("Add Cycle Counter");
        addressView.setText(counter.getAddress());
    }

    @Override
    protected void updateFromCounter(@NonNull Counter counter) {
        countView.setText(Long.toString(counter.getLastCount()));
        batteryView.setText(Integer.toString((int) Math.round(counter.getLastBattery())));
        modelView.setText(counter.getModel());
        hardwareView.setText(counter.getHardwareRevision());
        softwareView.setText(counter.getSoftwareRevision());
    }

    private void saveCounter() {
        final Location location = EventBus.getDefault().getStickyEvent(Location.class);
        final ContentValues values = new ContentValues();
        values.put(CounterEntry.COLUMN_NAME_ALIAS, aliasView.getText().toString());
        values.put(CounterEntry.COLUMN_NAME_ADDRESS, counter.getAddress());
        values.put(CounterEntry.COLUMN_NAME_FIRST_CONNECTED, counter.getFirstConnected());
        values.put(CounterEntry.COLUMN_NAME_INITIAL_COUNT, counter.getInitialCount());
        values.put(CounterEntry.COLUMN_NAME_LAST_BATTERY, counter.getLastBattery());
        values.put(CounterEntry.COLUMN_NAME_LAST_CONNECTED, counter.getLastConnected());
        values.put(CounterEntry.COLUMN_NAME_LAST_COUNT, counter.getLastCount());
        values.put(CounterEntry.COLUMN_NAME_LATITUDE, location != null ? location.getLatitude() : 0);
        values.put(CounterEntry.COLUMN_NAME_LONGITUDE, location != null ? location.getLongitude() : 0);
        values.put(CounterEntry.COLUMN_NAME_MODEL, counter.getModel());
        values.put(CounterEntry.COLUMN_NAME_HARDWARE_REVISION, counter.getHardwareRevision());
        values.put(CounterEntry.COLUMN_NAME_SOFTWARE_REVISION, counter.getSoftwareRevision());
        final Uri rowId = getContext().getContentResolver().insert(CounterDatabaseContract.COUNTERS_URI, values);

        final AlertDialog.Builder builder = new Builder(getActivity());

        if (rowId != null) {
            builder.setTitle("Counter Saved")
                    .setMessage("New counter " + aliasView.getText().toString() + " was successfully saved.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            getActivity().getSupportFragmentManager().popBackStack();
                            dialogInterface.dismiss();
                        }
                    }).show();
        } else {
            builder.setTitle("Counter Save Failed")
                    .setMessage("Error occured while saving new counter " + aliasView.getText().toString())
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
        }
    }
}
