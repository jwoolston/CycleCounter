package com.realenvprod.cyclecounter.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import com.realenvprod.cyclecounter.counter.Counter;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public abstract class CounterFragment extends Fragment {

    protected static final String ARG_COUNTER = "counter";

    protected Counter counter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            counter = getArguments().getParcelable(ARG_COUNTER);
        }
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (counter != null) {
            Counter newCounter = args.getParcelable(ARG_COUNTER);
            if (newCounter != null) {
                counter = newCounter;
                updateFromCounter(counter);
            }
        }
    }

    protected abstract void updateFromCounter(@NonNull Counter counter);
}
