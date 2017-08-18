package com.realenvprod.cyclecounter.fragment;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.realenvprod.cyclecounter.R;
import com.realenvprod.cyclecounter.counter.Counter;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CounterDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CounterDetailFragment extends CounterFragment {

    public static final String TAG = "CounterDetailFragment";

    private static final String ARG_COUNTER = "counter";

    private TextView aliasView;
    private TextView addressView;
    private TextView countView;
    private TextView batteryView;
    private TextView modelView;
    private TextView hardwareView;
    private TextView softwareView;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_counter_details, container, false);
        aliasView = (EditText) view.findViewById(R.id.alias_input);
        addressView = (TextView) view.findViewById(R.id.address_entry);
        countView = (TextView) view.findViewById(R.id.current_count_entry);
        batteryView = (TextView) view.findViewById(R.id.current_battery_entry);
        modelView = (TextView) view.findViewById(R.id.model_number_entry);
        hardwareView = (TextView) view.findViewById(R.id.hardware_revision_entry);
        softwareView = (TextView) view.findViewById(R.id.software_revision_entry);
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
        getActivity().setTitle("Counter Details");
        aliasView.setText(counter.getAlias());
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

    private void deleteCounter() {

    }
}
