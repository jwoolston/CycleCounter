package com.realenvprod.cyclecounter.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.realenvprod.cyclecounter.R;
import com.realenvprod.cyclecounter.counter.UnknownCounterAdapter;

/**
 * A fragment representing a list of Items.
 */
public class UnknownCounterFragment extends Fragment {

    public static final String TAG = "KnownCounterFragment";

    private UnknownCounterAdapter adapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public UnknownCounterFragment() {
    }

    public static UnknownCounterFragment newInstance() {
        UnknownCounterFragment fragment = new UnknownCounterFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) inflater
                .inflate(R.layout.fragment_counter_list, container, false);

        // Set the adapter
        Context context = recyclerView.getContext();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = UnknownCounterAdapter.getInstance();
        recyclerView.setAdapter(adapter);

        return recyclerView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("Unknown Cycle Counters");
    }
}
