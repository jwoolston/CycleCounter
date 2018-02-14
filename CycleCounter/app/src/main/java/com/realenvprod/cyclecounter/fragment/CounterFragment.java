package com.realenvprod.cyclecounter.fragment;

import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.realenvprod.cyclecounter.R;
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

    protected Counter   counter;
    protected ImageView advertisementIndicator;
    protected Handler   handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            counter = getArguments().getParcelable(ARG_COUNTER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        final View view = inflateView(inflater, container, savedInstanceState);
        advertisementIndicator = view.findViewById(R.id.advertisement_indicator);
        if (advertisementIndicator != null) {
            advertisementIndicator.setColorFilter(R.color.colorPrimary, Mode.SRC_ATOP);
            advertisementIndicator.setVisibility(View.INVISIBLE);
            handler = new Handler();
        }
        return view;
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
            showAdvertisement();
            this.counter = counter;
            updateFromCounter(counter);
        } else {
            Log.w(TAG, "Ignoring counter address mismatch. Expecting: " + this.counter.getAddress() + " Received: " + counter.getAddress());
        }
    }

    protected void showAdvertisement() {
        if (advertisementIndicator != null) {
            advertisementIndicator.setVisibility(View.VISIBLE);
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    advertisementIndicator.setVisibility(View.INVISIBLE);
                }
            }, 1000);
        }
    }

    @NonNull
    protected abstract View inflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                        @Nullable Bundle savedInstanceState);

    protected abstract void updateFromCounter(@NonNull Counter counter);
}
