package com.realenvprod.cyclecounter.fragment;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.realenvprod.cyclecounter.R;
import com.realenvprod.cyclecounter.counter.Counter;
import com.realenvprod.cyclecounter.counter.CounterAdapter;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract;

/**
 * A fragment representing a list of Items.
 * <p />
 * Activities containing this fragment MUST implement the {@link OnCounterListItemInteractionListener}
 * interface.
 */
public class KnownCounterFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = "KnownCounterFragment";

    private OnCounterListItemInteractionListener listener;
    private CounterAdapter                       adapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public KnownCounterFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static KnownCounterFragment newInstance() {
        KnownCounterFragment fragment = new KnownCounterFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCounterListItemInteractionListener) {
            listener = (OnCounterListItemInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                                       + " must implement OnCounterListItemInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("Known Cycle Counters");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) inflater
                .inflate(R.layout.fragment_counter_list, container, false);

        // Set the adapter
        Context context = recyclerView.getContext();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new CounterAdapter(getContext(), listener);
        recyclerView.setAdapter(adapter);

        getActivity().getSupportLoaderManager().restartLoader(0, null, this);
        return recyclerView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case 0:
                return new CursorLoader(getContext(), CounterDatabaseContract.COUNTERS_URI, null, null, null, null);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case 0:
                adapter.swapCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case 0:
                adapter.swapCursor(null);
                break;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnCounterListItemInteractionListener {
        // TODO: Update argument type and name
        void onCounterListItemInteraction(Counter item);
    }
}
