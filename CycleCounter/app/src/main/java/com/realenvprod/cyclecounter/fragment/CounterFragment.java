package com.realenvprod.cyclecounter.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import com.realenvprod.cyclecounter.counter.Counter;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
public abstract class CounterFragment extends Fragment {

    private static final String TAG = "CounterFragment";

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
    public void onResume() {
        // Register for scan results
        EventBus.getDefault().register(this);

        super.onResume();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCounterDiscovered(final Counter counter) {
        if (this.counter.getAddress().equals(counter.getAddress())) {
            Log.d(TAG, "Updating counter for this fragment.");
            this.counter = counter;
            updateFromCounter(counter);
        } else {
            Log.w(TAG, "Ignoring counter address mismatch. Expecting: " + this.counter.getAddress() + " Received: " + counter.getAddress());
        }
    }

    protected abstract void updateFromCounter(@NonNull Counter counter);
}
