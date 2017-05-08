package com.realenvprod.cyclecounter.fragment;


import static android.content.Context.BIND_AUTO_CREATE;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.realenvprod.cyclecounter.R;
import com.realenvprod.cyclecounter.bluetooth.GattUpdateReceiver;
import com.realenvprod.cyclecounter.counter.Counter;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry;
import com.realenvprod.cyclecounter.service.BluetoothLeService;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddCounterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddCounterFragment extends Fragment implements GattUpdateReceiver.OnGattUpdate {

    private static final String TAG = "AddCounterFragment";

    private static final String ARG_COUNTER = "counter";

    private BluetoothGattCharacteristic cycleCountCharacteristic;
    private BluetoothGattCharacteristic batteryLevelCharacteristic;
    private BluetoothLeService bluetoothLeService;
    private GattUpdateReceiver gattUpdateReceiver;
    private Counter counter;

    private long count = -1;
    private int battery = -1;

    private EditText aliasView;
    private TextView addressView;
    private TextView countView;
    private TextView batteryView;
    private TextView modelView;
    private TextView hardwareView;
    private TextView softwareView;

    private boolean connected = false;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            counter = getArguments().getParcelable(ARG_COUNTER);
        }
        gattUpdateReceiver = new GattUpdateReceiver(this);
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
        (view.findViewById(R.id.save_counter)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCounter();
            }
        });
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
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("Add Cycle Counter");
        addressView.setText(counter.address);
        getContext().registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        Intent gattServiceIntent = new Intent(getContext(), BluetoothLeService.class);
        getActivity().bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        getContext().unregisterReceiver(gattUpdateReceiver);
        getActivity().unbindService(serviceConnection);
        super.onPause();
    }

    @Override
    public void onUpdateReceived(@NonNull String action, @Nullable String data) {
        if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            connected = true;
            //updateConnectionState(R.string.connected);
        } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
            connected = false;
            //updateConnectionState(R.string.disconnected);
        } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            // Show all the supported services and characteristics on the user interface.
            final List<BluetoothGattService> services = bluetoothLeService.getSupportedGattServices();
            displayAndObserveGattServices(services);
        }
    }

    @Override
    public void onCycleCountUpdate(@IntRange(from = 0) long cycleCount) {
        Log.d(TAG, "Cycle count update: " + cycleCount);
        count = cycleCount;
        countView.setText(cycleCount >= 0 ? "" + cycleCount : "Error");
    }

    @Override
    public void onBatteryUpdate(@IntRange(from = 0, to = 100) int batteryLevel) {
        Log.d(TAG, "Battery update: " + batteryLevel + "%");
        battery = batteryLevel;
        batteryView.setText(batteryLevel >= 0 ? "" + batteryLevel + "%" : "Error");
    }

    @Override
    public void onModelNumber(@NonNull String model) {
        Log.d(TAG, "Model number: " + model);
        modelView.setText(model);
    }

    @Override
    public void onHardwareRevisionString(@NonNull String revision) {
        Log.d(TAG, "Hardware version: " + revision);
        hardwareView.setText(revision);
    }

    @Override
    public void onSoftwareRevisionString(@NonNull String revision) {
        Log.d(TAG, "Software version: " + revision);
        softwareView.setText(revision);
    }

    private void saveCounter() {
        final long time = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(CounterEntry.COLUMN_NAME_ALIAS, aliasView.getText().toString());
        values.put(CounterEntry.COLUMN_NAME_ADDRESS, counter.address);
        values.put(CounterEntry.COLUMN_NAME_FIRST_CONNECTED, time);
        values.put(CounterEntry.COLUMN_NAME_INITIAL_COUNT, count);
        values.put(CounterEntry.COLUMN_NAME_LAST_BATTERY, battery);
        values.put(CounterEntry.COLUMN_NAME_LAST_CONNECTED, time);
        values.put(CounterEntry.COLUMN_NAME_LAST_COUNT, count);
        values.put(CounterEntry.COLUMN_NAME_LATITUDE, 0);
        values.put(CounterEntry.COLUMN_NAME_LONGITUDE, 0);
        getContext().getContentResolver().insert(CounterDatabaseContract.COUNTERS_URI, values);
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayAndObserveGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            uuid = gattService.getUuid().toString();
            switch (uuid) {
                case Counter.DEVICE_INFORMATION_SERVICE:
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        switch (gattCharacteristic.getUuid().toString()) {
                            case Counter.MODEL_NUMBER_STRING:
                            case Counter.HARDWARE_REVISION_STRING:
                            case Counter.SOFTWARE_REVISION_STRING:
                                Log.v(TAG, "Reading device info characteristic: " + uuid);
                                bluetoothLeService.readCharacteristic(gattCharacteristic);
                                break;
                        }
                    }
                case Counter.CYCLE_COUNT_SERVICE:
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        if (Counter.CYCLE_COUNT.equals(gattCharacteristic.getUuid().toString())) {
                            Log.v(TAG, "Reading cycle count characteristic.");
                            cycleCountCharacteristic = gattCharacteristic;
                            bluetoothLeService.readCharacteristic(cycleCountCharacteristic);
                            bluetoothLeService.setCharacteristicNotification(cycleCountCharacteristic, true);
                        }
                    }
                    break;
                case Counter.BATTERY_LEVEL_SERVICE:
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        if (Counter.BATTERY_LEVEL.equals(gattCharacteristic.getUuid().toString())) {
                            Log.v(TAG, "Reading battery level characteristic.");
                            batteryLevelCharacteristic = gattCharacteristic;
                            bluetoothLeService.readCharacteristic(batteryLevelCharacteristic);
                            bluetoothLeService.setCharacteristicNotification(batteryLevelCharacteristic, true);
                        }
                    }
                    break;
            }
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            // Automatically connects to the device upon successful start-up initialization.
            bluetoothLeService.connect(counter.address);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
